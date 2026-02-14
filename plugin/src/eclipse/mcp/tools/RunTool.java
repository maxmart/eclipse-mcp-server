package eclipse.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import java.util.LinkedHashSet;

public class RunTool implements IMcpTool {

    @Override
    public String getName() {
        return "run";
    }

    @Override
    public String getDescription() {
        return "Convenience tool: terminates existing launches of the given config, refreshes the project from filesystem, builds it, and launches the configuration. Use this after editing Java files externally. Always pass the 'project' parameter and 'files' parameter to avoid slow workspace-wide refresh.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject nameProp = new JsonObject();
        nameProp.addProperty("type", "string");
        nameProp.addProperty("description", "Name of the launch configuration to run");
        props.add("name", nameProp);

        JsonObject projectProp = new JsonObject();
        projectProp.addProperty("type", "string");
        projectProp.addProperty("description", "Project name to refresh and build. Recommended to avoid slow workspace-wide refresh. If omitted, refreshes and builds all open projects.");
        props.add("project", projectProp);

        JsonObject filesProp = new JsonObject();
        filesProp.addProperty("type", "array");
        JsonObject itemsProp = new JsonObject();
        itemsProp.addProperty("type", "string");
        filesProp.add("items", itemsProp);
        filesProp.addProperty("description", "Optional: list of project-relative file paths that changed (e.g. [\"src/com/example/Main.java\"]). When provided, only refreshes these files instead of the entire project. Much faster for small changes.");
        props.add("files", filesProp);

        JsonObject modeProp = new JsonObject();
        modeProp.addProperty("type", "string");
        modeProp.addProperty("description", "Launch mode: 'run' or 'debug' (default: 'run')");
        props.add("mode", modeProp);

        schema.add("properties", props);

        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
        required.add("name");
        schema.add("required", required);

        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        String configName = arguments.get("name").getAsString();
        String projectName = arguments.has("project") ? arguments.get("project").getAsString() : null;
        JsonArray files = arguments.has("files") ? arguments.getAsJsonArray("files") : null;
        String mode = arguments.has("mode") ? arguments.get("mode").getAsString() : ILaunchManager.RUN_MODE;

        JsonArray steps = new JsonArray();

        // 1. Terminate existing launches of this config
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        int terminated = 0;
        for (ILaunch launch : manager.getLaunches()) {
            if (!launch.isTerminated() && launch.getLaunchConfiguration().getName().equals(configName)) {
                launch.terminate();
                terminated++;
            }
        }
        steps.add("Terminated " + terminated + " existing launch(es)");

        // 2. Refresh and build inside a Job so Eclipse shows progress
        final Exception[] jobError = new Exception[1];
        final JsonArray jobSteps = new JsonArray();

        Job refreshAndBuild = new Job("MCP: Preparing " + configName) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    SubMonitor sub = SubMonitor.convert(monitor, 100);

                    // Refresh
                    if (projectName != null && files != null && files.size() > 0) {
                        // Fast path: only refresh the specific files that changed
                        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
                        if (!project.exists()) {
                            throw new IllegalArgumentException("Project not found: " + projectName);
                        }
                        LinkedHashSet<IContainer> parents = new LinkedHashSet<>();
                        for (int i = 0; i < files.size(); i++) {
                            IPath filePath = new Path(files.get(i).getAsString());
                            IResource resource = project.findMember(filePath);
                            if (resource != null) {
                                resource.refreshLocal(IResource.DEPTH_ZERO, null);
                            } else {
                                parents.add(project.getFolder(filePath.removeLastSegments(1)));
                            }
                        }
                        for (IContainer parent : parents) {
                            parent.refreshLocal(IResource.DEPTH_ONE, null);
                        }
                        jobSteps.add("Refreshed " + files.size() + " file(s) in " + projectName);
                    } else if (projectName != null) {
                        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
                        if (!project.exists()) {
                            throw new IllegalArgumentException("Project not found: " + projectName);
                        }
                        sub.subTask("Refreshing " + projectName + "...");
                        project.refreshLocal(IResource.DEPTH_INFINITE, sub.split(30));
                        jobSteps.add("Refreshed project: " + projectName);
                    } else {
                        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                        SubMonitor refreshMonitor = sub.split(30);
                        refreshMonitor.setWorkRemaining(projects.length);
                        for (IProject project : projects) {
                            if (project.isOpen()) {
                                refreshMonitor.subTask("Refreshing " + project.getName() + "...");
                                project.refreshLocal(IResource.DEPTH_INFINITE, refreshMonitor.split(1));
                            }
                        }
                        jobSteps.add("Refreshed all open projects");
                    }

                    // Wait for auto-build
                    sub.subTask("Waiting for auto-build...");
                    Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, sub.split(10));

                    // Build
                    if (projectName != null) {
                        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
                        sub.subTask("Building " + projectName + "...");
                        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, sub.split(60));
                        jobSteps.add("Built project: " + projectName);
                    } else {
                        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                        SubMonitor buildMonitor = sub.split(60);
                        buildMonitor.setWorkRemaining(projects.length);
                        for (IProject project : projects) {
                            if (project.isOpen()) {
                                buildMonitor.subTask("Building " + project.getName() + "...");
                                project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, buildMonitor.split(1));
                            }
                        }
                        jobSteps.add("Built all open projects");
                    }

                    return Status.OK_STATUS;
                } catch (Exception e) {
                    jobError[0] = e;
                    return Status.error("Failed: " + e.getMessage(), e);
                }
            }
        };

        refreshAndBuild.setUser(true);
        refreshAndBuild.schedule();
        refreshAndBuild.join();

        if (jobError[0] != null) {
            throw jobError[0];
        }

        for (int i = 0; i < jobSteps.size(); i++) {
            steps.add(jobSteps.get(i));
        }

        // 3. Launch
        ILaunchConfiguration config = LaunchTool.findConfig(configName);
        if (config == null) {
            throw new IllegalArgumentException("Launch configuration not found: " + configName);
        }

        final ILaunchConfiguration cfg = config;
        final Exception[] error = new Exception[1];

        Display display = PlatformUI.getWorkbench().getDisplay();
        display.syncExec(() -> {
            try {
                cfg.launch(mode, null);
            } catch (Exception e) {
                error[0] = e;
            }
        });

        if (error[0] != null) {
            throw error[0];
        }

        steps.add("Launched: " + configName + " (" + mode + ")");

        JsonObject result = new JsonObject();
        result.add("steps", steps);
        result.addProperty("success", true);
        return result;
    }
}
