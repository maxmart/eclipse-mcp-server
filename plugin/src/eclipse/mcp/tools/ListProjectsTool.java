package eclipse.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

public class ListProjectsTool implements IMcpTool {

    @Override
    public String getName() {
        return "list_projects";
    }

    @Override
    public String getDescription() {
        return "List all projects in the Eclipse workspace";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        JsonArray list = new JsonArray();
        for (IProject project : projects) {
            JsonObject p = new JsonObject();
            p.addProperty("name", project.getName());
            p.addProperty("open", project.isOpen());
            p.addProperty("location", project.getLocation().toOSString());
            list.add(p);
        }
        JsonObject result = new JsonObject();
        result.add("projects", list);
        return result;
    }
}
