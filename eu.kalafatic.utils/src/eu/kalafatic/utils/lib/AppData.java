package eu.kalafatic.utils.lib;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.utils.dialogs.GeminiSplashHandler;
import org.eclipse.swt.widgets.TrayItem;
public class AppData {
    private static AppData instance = new AppData();
    public static AppData getInstance() { return instance; }
    public Object getSplashHandler() { return null; }
    public List<Object> getSplashUsersUsers() { return new ArrayList<>(); }
    public void setTrayItem(TrayItem item) {}
}
