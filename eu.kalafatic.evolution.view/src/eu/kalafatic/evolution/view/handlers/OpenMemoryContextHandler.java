package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * Handler for command eu.kalafatic.evolution.view.openMemoryContext.
 * Navigates to the Memory / Context UI and propagates the current application,
 * project, selection, or server/session context.
 */
public class OpenMemoryContextHandler extends AbstractHandler {

    public static final String COMMAND_ID = "eu.kalafatic.evolution.view.openMemoryContext";
    public static final String PARAM_SCOPE = "eu.kalafatic.evolution.view.openMemoryContext.scope";
    public static final String PARAM_ASSOCIATED_ID = "eu.kalafatic.evolution.view.openMemoryContext.associatedId";
    public static final String PARAM_CALLER = "eu.kalafatic.evolution.view.openMemoryContext.caller";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        String caller = event.getParameter(PARAM_CALLER);
        String scopeStr = event.getParameter(PARAM_SCOPE);
        String associatedId = event.getParameter(PARAM_ASSOCIATED_ID);

        if ("PROPERTIES".equalsIgnoreCase(caller) || "PROPERTIES".equalsIgnoreCase(scopeStr)) {
            System.out.println("PROPERTIES_MEMORY_OPEN");
        } else if ("SERVER".equalsIgnoreCase(caller) || "SERVER".equalsIgnoreCase(scopeStr)) {
            System.out.println("SERVER_MEMORY_OPEN");
        } else {
            System.out.println("MAIN_MENU_MEMORY_OPEN");
        }

        MemoryScope scope = null;
        if (scopeStr != null && !scopeStr.trim().isEmpty()) {
            try {
                scope = MemoryScope.valueOf(scopeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                scope = null;
            }
        }

        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event) != null
                ? HandlerUtil.getActiveWorkbenchWindow(event).getActivePage()
                : null;

        MultiPageEditor editor = null;
        if (page != null) {
            IEditorPart activeEditor = page.getActiveEditor();
            if (activeEditor instanceof MultiPageEditor) {
                editor = (MultiPageEditor) activeEditor;
            } else {
                for (org.eclipse.ui.IEditorReference ref : page.getEditorReferences()) {
                    IEditorPart part = ref.getEditor(false);
                    if (part instanceof MultiPageEditor) {
                        editor = (MultiPageEditor) part;
                        break;
                    }
                }
            }
        }

        if (editor != null) {
            if (associatedId == null || associatedId.trim().isEmpty()) {
                if (editor.getOrchestrator() != null && editor.getOrchestrator().getId() != null) {
                    associatedId = editor.getOrchestrator().getId();
                }
            }

            System.out.println("MEMORY_CONTEXT_RESOLVED: scope=" + (scope != null ? scope.name() : "ALL") + ", associatedId=" + (associatedId != null ? associatedId : "NONE"));
            System.out.println("MEMORY_SCOPE_RESOLVED: " + (scope != null ? scope.name() : "ALL"));

            editor.showContextPage(scope, associatedId);

            System.out.println("MEMORY_UI_REFRESH: Refreshed Memory / Context UI for scope=" + (scope != null ? scope.name() : "ALL"));
        } else {
            System.out.println("MEMORY_CONTEXT_RESOLVED: No active MultiPageEditor found; defaulting to application scope");
            System.out.println("MEMORY_SCOPE_RESOLVED: " + (scope != null ? scope.name() : "ALL"));
            System.out.println("MEMORY_UI_REFRESH: MultiPageEditor unavailable");
        }

        return null;
    }
}
