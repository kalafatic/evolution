package eu.kalafatic.evolution.view.editors.pages.properties;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalResult;
import eu.kalafatic.evolution.controller.manager.removal.ModelRemovalStatus;

/**
 * Final, truthful removal summary. Each model reports its runtime, registry,
 * configuration, artifact and overall result independently.
 */
public class ModelRemovalReportDialog extends Dialog {

    private final List<ModelRemovalResult> results;

    public ModelRemovalReportDialog(Shell parentShell, List<ModelRemovalResult> results) {
        super(parentShell);
        this.results = results;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        area.setLayout(new GridLayout(1, false));
        ((GridLayout) area.getLayout()).marginWidth = 12;
        ((GridLayout) area.getLayout()).marginHeight = 12;

        long success = results.stream().filter(r -> r.getStatus() == ModelRemovalStatus.SUCCESS).count();
        long partial = results.stream().filter(r -> r.getStatus() == ModelRemovalStatus.PARTIAL_SUCCESS).count();
        long failed = results.stream().filter(r -> r.getStatus() == ModelRemovalStatus.FAILED).count();

        Label heading = new Label(area, SWT.WRAP);
        heading.setText("Removal results — " + success + " success, " + partial
                + " partial, " + failed + " failed");
        heading.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());
        heading.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        TableViewer viewer = new TableViewer(area, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createColumn(viewer, "Model", 200, r -> r.getModelName());
        createColumn(viewer, "Runtime", 170, ModelRemovalResult::getRuntimeOutcome);
        createColumn(viewer, "Registry", 130, ModelRemovalResult::getRegistryOutcome);
        createColumn(viewer, "Configuration", 130, ModelRemovalResult::getConfigurationOutcome);
        createColumn(viewer, "Artifact", 170, ModelRemovalResult::getArtifactOutcome);
        createColumn(viewer, "Result", 120, r -> r.getStatus().name());

        viewer.setInput(results);

        Label details = new Label(area, SWT.WRAP);
        details.setText("Verification checks the EMF registry, default model references, the runtime "
                + "and the artifact on disk. A cache refresh alone is never treated as proof of removal.");
        details.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        return area;
    }

    private void createColumn(TableViewer viewer, String title, int width,
                              java.util.function.Function<ModelRemovalResult, String> value) {
        TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
        column.getColumn().setText(title);
        column.getColumn().setWidth(width);
        column.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof ModelRemovalResult result) {
                    String text = value.apply(result);
                    return text != null ? text : "";
                }
                return "";
            }
        });
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Model Removal Results");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
