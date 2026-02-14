package eclipse.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import java.util.LinkedHashSet;

public class RefreshProjectTool implements IMcpTool {

    @Override
    public String getName() {
        return "refresh_project";
    }

    @Override
    public String getDescription() {
        return "Refresh project(s) from the filesystem so Eclipse picks up external changes. If 'name' is provided, refreshes that project. Pass 'files' with a list of project-relative paths to only refresh those files (much faster than a full project refresh).";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject nameProp = new JsonObject();
        nameProp.addProperty("type", "string");
        nameProp.addProperty("description", "Optional: project name. If omitted, refreshes all open projects.");
        props.add("name", nameProp);

        JsonObject filesProp = new JsonObject();
        filesProp.addProperty("type", "array");
        JsonObject itemsProp = new JsonObject();
        itemsProp.addProperty("type", "string");
        filesProp.add("items", itemsProp);
        filesProp.addProperty("description", "Optional: list of project-relative file paths that changed (e.g. [\"src/com/example/Main.java\"]). When provided, only refreshes these files instead of the entire project. Much faster for small changes.");
        props.add("files", filesProp);

        schema.add("properties", props);
        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        String name = arguments.has("name") ? arguments.get("name").getAsString() : null;
        JsonArray files = arguments.has("files") ? arguments.getAsJsonArray("files") : null;
        JsonArray refreshed = new JsonArray();

        final Exception[] jobError = new Exception[1];

        Job refreshJob = new Job("MCP: Refreshing " + (name != null ? name : "all projects")) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    if (name != null && files != null && files.size() > 0) {
                        // Fast path: only refresh the specific files that changed
                        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
                        if (!project.exists()) {
                            throw new IllegalArgumentException("Project not found: " + name);
                        }
                        LinkedHashSet<IContainer> parents = new LinkedHashSet<>();
                        for (int i = 0; i < files.size(); i++) {
                            IPath filePath = new Path(files.get(i).getAsString());
                            IResource resource = project.findMember(filePath);
                            if (resource != null) {
                                // Existing file: refresh it directly
                                resource.refreshLocal(IResource.DEPTH_ZERO, null);
                            } else {
                                // New file: need to refresh the parent folder so Eclipse discovers it
                                parents.add(project.getFolder(filePath.removeLastSegments(1)));
                            }
                        }
                        for (IContainer parent : parents) {
                            parent.refreshLocal(IResource.DEPTH_ONE, null);
                        }
                        refreshed.add(name + " (" + files.size() + " files)");
                    } else if (name != null) {
                        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
                        if (!project.exists()) {
                            throw new IllegalArgumentException("Project not found: " + name);
                        }
                        SubMonitor sub = SubMonitor.convert(monitor, 1);
                        sub.subTask("Refreshing " + name + "...");
                        project.refreshLocal(IResource.DEPTH_INFINITE, sub.split(1));
                        refreshed.add(name);
                    } else {
                        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                        SubMonitor sub = SubMonitor.convert(monitor, projects.length);
                        for (IProject project : projects) {
                            if (project.isOpen()) {
                                sub.subTask("Refreshing " + project.getName() + "...");
                                project.refreshLocal(IResource.DEPTH_INFINITE, sub.split(1));
                                refreshed.add(project.getName());
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

        refreshJob.setUser(true);
        refreshJob.schedule();
        refreshJob.join();

        if (jobError[0] != null) {
            throw jobError[0];
        }

        JsonObject result = new JsonObject();
        result.add("refreshed", refreshed);
        return result;
    }
}
