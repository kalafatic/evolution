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

        ModifyListener ml = e -> {
            if (orchestrator != null && !isUpdating) {
                updateModelFromFields();
                editor.setDirty(true);
            }
        };

        addModifyListenerToGroup(gitGroup, ml);
        addModifyListenerToGroup(mavenGroup, ml);
        addModifyListenerToGroup(fileGroup, ml);
        addModifyListenerToGroup(databaseGroup, ml);
        addModifyListenerToGroup(eclipseGroup, ml);
        addModifyListenerToGroup(compilerGroup, ml);

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updateUIFromModel();
    }

    private void addModifyListenerToGroup(Object group, ModifyListener ml) {
        try {
            java.lang.reflect.Method m = group.getClass().getMethod("getTextFields");
            Text[] texts = (Text[]) m.invoke(group);
            for (Text t : texts) t.addModifyListener(ml);
        } catch (Exception e) {}
    }

    public void updateUIFromModel() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        gitGroup.updateUI(); mavenGroup.updateUI(); fileGroup.updateUI(); databaseGroup.updateUI(); eclipseGroup.updateUI(); compilerGroup.updateUI();
        isUpdating = false;
    }

    private void updateModelFromFields() {
        resetBackgrounds();
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        gitGroup.updateModel(); mavenGroup.updateModel(); fileGroup.updateModel(); databaseGroup.updateModel(); eclipseGroup.updateModel(); compilerGroup.updateModel();
        isUpdating = false;
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        updateUIFromModel();
    }

    @Override
    public void dispose() {
        if (successColor != null && !successColor.isDisposed()) successColor.dispose();
        if (toolkit != null) toolkit.dispose();
        super.dispose();
    }

    private void resetBackgrounds() {
        Color defaultColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        gitGroup.getGroup().setBackground(defaultColor);
        mavenGroup.getGroup().setBackground(defaultColor);
        fileGroup.getGroup().setBackground(defaultColor);
        databaseGroup.getGroup().setBackground(defaultColor);
        compilerGroup.getGroup().setBackground(defaultColor);
        eclipseGroup.getGroup().setBackground(defaultColor);
        if (orchestrator.getGit() != null) orchestrator.getGit().setTestStatus(null);
        if (orchestrator.getMaven() != null) orchestrator.getMaven().setTestStatus(null);
        if (orchestrator.getFileConfig() != null) orchestrator.getFileConfig().setTestStatus(null);
        if (orchestrator.getDatabase() != null) orchestrator.getDatabase().setTestStatus(null);
        if (orchestrator.getCompiler() != null) orchestrator.getCompiler().setTestStatus(null);
        if (orchestrator.getEclipse() != null) orchestrator.getEclipse().setTestStatus(null);
    }
}
