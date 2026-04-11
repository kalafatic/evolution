package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.tools.*;

public class ToolsPage extends SharedScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private boolean isUpdating = false;

    private FormToolkit toolkit;
    private Color successColor;

    private GitGroup gitGroup;
    private MavenGroup mavenGroup;
    private FileGroup fileGroup;
    private DatabaseGroup databaseGroup;
    private EclipseGroup eclipseGroup;
    private CompilerGroup compilerGroup;
    private TerminalGroup terminalGroup;

    public ToolsPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        this.toolkit = new FormToolkit(parent.getDisplay());
        createControl();
    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new GridLayout(1, false));

        successColor = new Color(getDisplay(), 200, 240, 200); // Light cool green

        gitGroup = new GitGroup(toolkit, comp, editor, orchestrator, successColor);
        mavenGroup = new MavenGroup(toolkit, comp, editor, orchestrator, successColor);
        fileGroup = new FileGroup(toolkit, comp, editor, orchestrator, successColor);
        databaseGroup = new DatabaseGroup(toolkit, comp, editor, orchestrator, successColor);
        eclipseGroup = new EclipseGroup(toolkit, comp, editor, orchestrator, successColor);
        compilerGroup = new CompilerGroup(toolkit, comp, editor, orchestrator, successColor);
        terminalGroup = new TerminalGroup(toolkit, comp, editor, orchestrator, successColor);

        ModifyListener ml = e -> {
            if (orchestrator != null && !isUpdating) {
                updateModelFromFields();
                editor.setDirty(true);
            }
        };

        gitGroup.addModifyListener(ml);
        mavenGroup.addModifyListener(ml);
        fileGroup.addModifyListener(ml);
        databaseGroup.addModifyListener(ml);
        eclipseGroup.addModifyListener(ml);
        compilerGroup.addModifyListener(ml);
        terminalGroup.addModifyListener(ml);

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updateUIFromModel();
    }

    public void updateUIFromModel() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        gitGroup.updateUI(); mavenGroup.updateUI(); fileGroup.updateUI(); databaseGroup.updateUI(); eclipseGroup.updateUI(); compilerGroup.updateUI(); terminalGroup.updateUI();
        isUpdating = false;
    }

    private void updateModelFromFields() {
        resetBackgrounds();
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        gitGroup.updateModel(); mavenGroup.updateModel(); fileGroup.updateModel(); databaseGroup.updateModel(); eclipseGroup.updateModel(); compilerGroup.updateModel(); terminalGroup.updateModel();
        isUpdating = false;
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        if (gitGroup != null) gitGroup.setOrchestrator(orchestrator);
        if (mavenGroup != null) mavenGroup.setOrchestrator(orchestrator);
        if (fileGroup != null) fileGroup.setOrchestrator(orchestrator);
        if (databaseGroup != null) databaseGroup.setOrchestrator(orchestrator);
        if (eclipseGroup != null) eclipseGroup.setOrchestrator(orchestrator);
        if (compilerGroup != null) compilerGroup.setOrchestrator(orchestrator);
        if (terminalGroup != null) terminalGroup.setOrchestrator(orchestrator);
        updateUIFromModel();
    }

    @Override
    public void dispose() {
        if (successColor != null && !successColor.isDisposed()) successColor.dispose();
        if (toolkit != null) toolkit.dispose();
        super.dispose();
    }

    private void resetBackgrounds() {
        gitGroup.resetStatus();
        mavenGroup.resetStatus();
        fileGroup.resetStatus();
        databaseGroup.resetStatus();
        compilerGroup.resetStatus();
        eclipseGroup.resetStatus();
    }
}
