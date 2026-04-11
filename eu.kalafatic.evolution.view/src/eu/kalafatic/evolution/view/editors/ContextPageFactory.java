package eu.kalafatic.evolution.view.editors;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IFileEditorInput;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.ContextPage;

public class ContextPageFactory {

    public static ContextPage createContextPage(MultiPageEditor editor, Orchestrator orchestrator) {
        File projectRoot = null;
        if (editor.getEditorInput() instanceof IFileEditorInput) {
            IProject project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
            projectRoot = project.getLocation().toFile();
        }

        ContextPage page = new ContextPage(editor, orchestrator, projectRoot);
        try {
            int index = editor.addPage(page);
            editor.setPageText(index, "Context");
            return page;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
