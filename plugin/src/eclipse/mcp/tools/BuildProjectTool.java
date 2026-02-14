package eclipse.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

public class BuildProjectTool implements IMcpTool {

    @Override
    public String getName() {
        return "build_project";
    }

    @Override
    public String getDescription() {
        return "Build (compile) a project. If 'name' is provided, builds that project. Otherwise builds all open projects. Waits for any auto-build to finish first.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject nameProp = new JsonObject();
        nameProp.addProperty("type", "string");
        nameProp.addProperty("description", "Optional: project name. If omitted, builds all open projects.");
        props.add("name", nameProp);

        schema.add("properties", props);
        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        String name = arguments.has("name") ? arguments.get("name").getAsString() : null;
        JsonArray built = new JsonArray();

        final Exception[] jobError = new Exception[1];

        Job buildJob = new Job("MCP: Building " + (name != null ? name : "all projects")) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    SubMonitor sub = SubMonitor.convert(monitor, 100);

                    // Wait for any pending auto-build to finish
                    sub.subTask("Waiting for auto-build...");
                    Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, sub.split(10));

                    // Build
                    if (name != null) {
                        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
                        if (!project.exists()) {
                            throw new IllegalArgumentException("Project not found: " + name);
                        }
                        sub.subTask("Building " + name + "...");
                        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, sub.split(90));
                        built.add(name);
                    } else {
                        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                        SubMonitor buildMonitor = sub.split(90);
                        buildMonitor.setWorkRemaining(projects.length);
                        for (IProject project : projects) {
                            if (project.isOpen()) {
                                buildMonitor.subTask("Building " + project.getName() + "...");
                                project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, buildMonitor.split(1));
                                built.add(project.getName());
                            }
                        }
                    }

                    return Status.OK_STATUS;
                } catch (Exception e) {
                    jobError[0] = e;
                    return Status.error("Failed: " + e.getMessage(), e);
                }
            }
        };

        buildJob.setUser(true);
        buildJob.schedule();
        buildJob.join();

        if (jobError[0] != null) {
            throw jobError[0];
        }

        JsonObject result = new JsonObject();
        result.add("built", built);
        return result;
    }
}
