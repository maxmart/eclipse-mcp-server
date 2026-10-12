package eclipse.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.buildship.core.GradleBuild;
import org.eclipse.buildship.core.GradleCore;
import org.eclipse.buildship.core.GradleWorkspace;
import org.eclipse.buildship.core.SynchronizationResult;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GradleRefreshTool implements IMcpTool {

    @Override
    public String getName() {
        return "gradle_refresh";
    }

    @Override
    public String getDescription() {
        return "Synchronize Gradle project(s) with the Eclipse workspace via Buildship — the equivalent of "
                + "right-click -> Gradle -> Refresh Gradle Project. Re-resolves dependencies and reapplies the Gradle "
                + "model. If 'name' is provided, syncs the whole Gradle build that project belongs to (Buildship always "
                + "syncs an entire build, so sibling projects are synced too); otherwise syncs every Gradle build in the "
                + "workspace. Returns 'builds' (number of distinct Gradle builds synchronized) and 'synced' (every "
                + "project actually covered). Non-Gradle projects are reported under 'skipped'.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject nameProp = new JsonObject();
        nameProp.addProperty("type", "string");
        nameProp.addProperty("description",
                "Optional: project name. If omitted, synchronizes every Gradle build in the workspace.");
        props.add("name", nameProp);

        schema.add("properties", props);
        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        String name = arguments.has("name") ? arguments.get("name").getAsString() : null;
        JsonArray synced = new JsonArray();
        JsonArray skipped = new JsonArray();
        JsonArray errors = new JsonArray();

        final Exception[] jobError = new Exception[1];
        final int[] builds = new int[1];

        Job syncJob = new Job("MCP: Gradle refresh " + (name != null ? name : "all projects")) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    GradleWorkspace workspace = GradleCore.getWorkspace();

                    if (name != null) {
                        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
                        if (!project.exists()) {
                            throw new IllegalArgumentException("Project not found: " + name);
                        }
                        Optional<GradleBuild> build = workspace.getBuild(project);
                        if (build.isEmpty()) {
                            skipped.add(name);
                            return Status.OK_STATUS;
                        }
                        GradleBuild target = build.get();
                        // Buildship synchronizes the entire build, not a single project — list every open project
                        // belonging to that build so the report reflects what was actually synced.
                        List<String> covered = projectsOf(workspace, target);
                        SubMonitor sub = SubMonitor.convert(monitor, 1);
                        sub.subTask("Synchronizing " + name + "...");
                        if (synchronize(target, name, sub.split(1))) {
                            builds[0]++;
                            for (String projectName : covered) {
                                synced.add(projectName);
                            }
                        }
                        return Status.OK_STATUS;
                    }

                    // No name: group all open projects by their Gradle build so each build syncs only once.
                    // Builds are equal when they share a build configuration, which collapses the whole monorepo
                    // into a single synchronize call when every module belongs to one composite build.
                    Map<GradleBuild, List<String>> byBuild = new LinkedHashMap<>();
                    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
                        if (!project.isOpen()) {
                            continue;
                        }
                        Optional<GradleBuild> build = workspace.getBuild(project);
                        if (build.isPresent()) {
                            byBuild.computeIfAbsent(build.get(), key -> new ArrayList<>()).add(project.getName());
                        } else {
                            skipped.add(project.getName());
                        }
                    }

                    SubMonitor sub = SubMonitor.convert(monitor, byBuild.size());
                    for (Map.Entry<GradleBuild, List<String>> entry : byBuild.entrySet()) {
                        List<String> projectNames = entry.getValue();
                        sub.subTask("Synchronizing " + projectNames.get(0) + "...");
                        if (synchronize(entry.getKey(), projectNames.get(0), sub.split(1))) {
                            builds[0]++;
                            for (String projectName : projectNames) {
                                synced.add(projectName);
                            }
                        }
                    }
                    return Status.OK_STATUS;
                } catch (Exception e) {
                    jobError[0] = e;
                    return new Status(IStatus.ERROR, "eclipse.mcp.server", "Failed: " + e.getMessage(), e);
                }
            }

            private boolean synchronize(GradleBuild build, String representative, IProgressMonitor monitor) {
                SynchronizationResult result = build.synchronize(monitor);
                IStatus status = result.getStatus();
                if (status.isOK()) {
                    return true;
                }
                JsonObject error = new JsonObject();
                error.addProperty("build", representative);
                error.addProperty("message", describe(status));
                errors.add(error);
                return false;
            }

            // A sync with several problems comes back as a MultiStatus whose own message is generic,
            // so include the children — they carry the actual failures.
            private String describe(IStatus status) {
                StringBuilder message = new StringBuilder(String.valueOf(status.getMessage()));
                for (IStatus child : status.getChildren()) {
                    message.append("\n").append(describe(child));
                }
                return message.toString();
            }

            private List<String> projectsOf(GradleWorkspace workspace, GradleBuild build) {
                List<String> names = new ArrayList<>();
                for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
                    if (!project.isOpen()) {
                        continue;
                    }
                    Optional<GradleBuild> other = workspace.getBuild(project);
                    if (other.isPresent() && other.get().equals(build)) {
                        names.add(project.getName());
                    }
                }
                return names;
            }
        };

        syncJob.setUser(true);
        syncJob.schedule();
        syncJob.join();

        if (jobError[0] != null) {
            throw jobError[0];
        }

        JsonObject result = new JsonObject();
        result.addProperty("builds", builds[0]);
        result.add("synced", synced);
        result.add("skipped", skipped);
        result.add("errors", errors);
        return result;
    }
}
