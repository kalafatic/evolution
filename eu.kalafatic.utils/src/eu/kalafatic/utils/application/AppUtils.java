package eu.kalafatic.utils.application;
import org.eclipse.ui.IEditorDescriptor;
import java.io.File;
public class AppUtils {
    public static void openEditor(IEditorDescriptor desc, File file) {}
    public static void openEditor(File file) {}
    public static IEditorDescriptor getDefaultEditorDesc() { return null; }
    public static AppUtils getInstance() { return new AppUtils(); }
    public void createProject(String name) {}
}
