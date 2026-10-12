package eu.kalafatic.evolution.view.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Thread-safe utility methods for displaying standardized error dialogs, stack trace detail
 * inspectors, and clipboard copy operations across Eclipse RCP workbench views.
 */
public final class EvoDialogUtils {

    private EvoDialogUtils() {
        // Utility class
    }

    /**
     * Displays a thread-safe modal error dialog with full exception stack trace details
     * and a "Copy Stack Trace" clipboard button.
     *
     * @param shell target Shell (may be null, in which case active shell/display is resolved)
     * @param title dialog title
     * @param message user-facing summary message
     * @param exception underlying exception/throwable (may be null)
     */
    public static void showErrorDialog(final Shell shell, final String title, final String message, final Throwable exception) {
        Display display = (shell != null && !shell.isDisposed()) ? shell.getDisplay() : Display.getDefault();
        if (display == null || display.isDisposed()) {
            return;
        }

        display.asyncExec(() -> {
            Shell parentShell = (shell != null && !shell.isDisposed()) ? shell : display.getActiveShell();

            String fullDetails = message != null ? message : "";
            String stackTraceStr = "";

            if (exception != null) {
                StringWriter sw = new StringWriter();
                exception.printStackTrace(new PrintWriter(sw));
                stackTraceStr = sw.toString();
                if (!fullDetails.isEmpty()) {
                    fullDetails += "\n\nStack Trace:\n" + stackTraceStr;
                } else {
                    fullDetails = "Stack Trace:\n" + stackTraceStr;
                }
            }

            final String dialogText = fullDetails;
            final String copyableTrace = stackTraceStr;

            MessageDialog dialog = new MessageDialog(
                    parentShell,
                    title != null ? title : "Operation Error",
                    null,
                    message != null ? message : "An unexpected error occurred.",
                    MessageDialog.ERROR,
                    new String[] { "Copy Details", IDialogConstants.OK_LABEL },
                    1) {

                @Override
                protected Control createCustomArea(Composite parent) {
                    if (copyableTrace.isEmpty()) {
                        return null;
                    }
                    Composite comp = new Composite(parent, SWT.NONE);
                    comp.setLayout(new GridLayout(1, false));
                    comp.setLayoutData(new GridData(GridData.FILL_BOTH));

                    Text detailsText = new Text(comp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
                    GridData gd = new GridData(GridData.FILL_BOTH);
                    gd.heightHint = 150;
                    gd.widthHint = 500;
                    detailsText.setLayoutData(gd);
                    detailsText.setText(dialogText);
                    return comp;
                }

                @Override
                protected void buttonPressed(int buttonId) {
                    if (buttonId == 0) { // Copy Details button
                        Clipboard clipboard = new Clipboard(getShell().getDisplay());
                        try {
                            clipboard.setContents(
                                    new Object[] { dialogText },
                                    new Transfer[] { TextTransfer.getInstance() }
                            );
                        } finally {
                            clipboard.dispose();
                        }
                    } else {
                        super.buttonPressed(buttonId);
                    }
                }
            };

            dialog.open();
        });
    }
}
