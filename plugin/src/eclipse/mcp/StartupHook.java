package eclipse.mcp;

import org.eclipse.ui.IStartup;

public class StartupHook implements IStartup {

    @Override
    public void earlyStartup() {
        Activator.getInstance().startServer();
    }
}
