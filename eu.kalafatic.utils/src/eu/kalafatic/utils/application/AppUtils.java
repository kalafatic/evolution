package eu.kalafatic.utils.application;
import java.io.File;
import org.eclipse.ui.IEditorDescriptor;
public class AppUtils {
    public static void openEditor(IEditorDescriptor d, File f) {}
    public static void openEditor(File f) {}
    public static IEditorDescriptor getDefaultEditorDesc() { return null; }
    public static AppUtils getInstance() { return new AppUtils(); }
    public void createProject(String name) {}
}
