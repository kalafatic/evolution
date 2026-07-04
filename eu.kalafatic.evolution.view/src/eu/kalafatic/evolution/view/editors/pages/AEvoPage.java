package eu.kalafatic.evolution.view.editors.pages;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.projection.ProjectionService;
import eu.kalafatic.evolution.view.projection.RuntimeProjection;

/**
 * Abstract superclass for Evolution Editor pages.
 */
public abstract class AEvoPage extends SharedScrolledComposite implements RuntimeEventListener {

    protected MultiPageEditor editor;
    protected Orchestrator orchestrator;
    protected FormToolkit toolkit;
    protected AtomicBoolean refreshPending = new AtomicBoolean(false);
    protected boolean needsRefresh = false;
    
    protected Color colorUser, colorEvolution, colorPlanner, colorArchitect, colorJavaDev, colorTester, colorReviewer, colorError, colorWhite, colorLocal, colorHybrid, colorRemote, colorWaiting, colorLightOrange;
    protected Font chatFont, bannerFont;
    protected Color lightGreen, lightRed;

    protected Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            if (notification.isTouch()) return;
            scheduleRefresh();
        }
    };

    private final Consumer<RuntimeProjection> projectionObserver = projection -> {
        if (!isDisposed() && projection.getSessionId().equals(getCurrentSessionName())) {
            scheduleRefresh();
        }
    };

    public AEvoPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.toolkit = new FormToolkit(parent.getDisplay());

        setExpandHorizontal(true);
        setExpandVertical(true);

        ProjectionService.getInstance().subscribe(projectionObserver);
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().add(modelAdapter);
        }
        
        initResources();
    }
    
    protected void initResources() {
		//Display display = getDisplay();
		Display display = Display.getCurrent(); // safer in UI thread
		colorUser = display.getSystemColor(SWT.COLOR_DARK_BLUE);
		colorEvolution = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
		colorPlanner = display.getSystemColor(SWT.COLOR_DARK_CYAN);
		colorArchitect = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		colorJavaDev = display.getSystemColor(SWT.COLOR_BLUE);
		colorTester = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
		colorReviewer = display.getSystemColor(SWT.COLOR_MAGENTA);
		colorError = display.getSystemColor(SWT.COLOR_RED);
		colorWhite = display.getSystemColor(SWT.COLOR_WHITE);
		colorLocal = display.getSystemColor(SWT.COLOR_GREEN);
		colorHybrid = display.getSystemColor(SWT.COLOR_GRAY);
		colorRemote = display.getSystemColor(SWT.COLOR_MAGENTA);
		colorWaiting = new Color(display, 255, 140, 0); // Dark Orange
		colorLightOrange = new Color(display, 255, 200, 150);
		lightGreen = new Color(Display.getDefault(), 220, 255, 220);
		lightRed = new Color(Display.getDefault(), 255, 220, 220);

		Font defaultFont = JFaceResources.getDefaultFont();
		FontData[] fontData = defaultFont.getFontData();
		for (FontData fd : fontData) fd.setHeight(11);
		chatFont = new Font(display, fontData);

		Font bannerDefault = JFaceResources.getBannerFont();
		FontData[] bannerData = bannerDefault.getFontData();
		for (FontData fd : bannerData) fd.setStyle(SWT.BOLD);
		bannerFont = new Font(display, bannerData);
	}

    protected String getCurrentSessionName() {
        return (orchestrator != null && orchestrator.getId() != null) ? orchestrator.getId() : "Default";
    }

    @Override
    public void onEvent(RuntimeEvent event) {
        scheduleRefresh();
    }

    private long lastRefreshTime = 0;
    private static final long REFRESH_THROTTLE_MS = 100;

    /**
     * Coalesces multiple refresh requests.
     */
    public void scheduleRefresh() {
        needsRefresh = true;
        if (refreshPending.compareAndSet(false, true)) {
            long now = System.currentTimeMillis();
            long delay = Math.max(0, REFRESH_THROTTLE_MS - (now - lastRefreshTime));

            Display.getDefault().asyncExec(() -> {
                Display.getDefault().timerExec((int)delay, () -> {
                    try {
                        refreshPending.set(false);
                        if (!isDisposed() && isVisible()) {
                            lastRefreshTime = System.currentTimeMillis();
                            needsRefresh = false;
                            refreshUI();
                        }
                    } catch (Exception e) {
                        // Log error but don't crash the background process
                        System.err.println("Error during UI refresh in " + getClass().getSimpleName() + ": " + e.getMessage());
                    }
                });
            });
        }
    }

    @Override
    public void setVisible(boolean visible) {
        boolean wasVisible = isVisible();
        super.setVisible(visible);
        if (visible && !wasVisible && needsRefresh) {
            scheduleRefresh();
        }
    }

    /**
     * Updates the UI components with data from the model.
     * To be implemented by subclasses.
     */
    protected abstract void refreshUI();

    protected void setTextSafe(Text text, String value) {
        if (text == null || text.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!text.getText().equals(safeValue)) {
            text.setText(safeValue);
        }
    }

    protected void setTextSafe(Label label, String value) {
        if (label == null || label.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!label.getText().equals(safeValue)) {
            label.setText(safeValue);
        }
    }

    protected void setTextSafe(org.eclipse.swt.widgets.Group group, String value) {
        if (group == null || group.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!group.getText().equals(safeValue)) {
            group.setText(safeValue);
        }
    }

    protected void setTextSafe(Button button, String value) {
        if (button == null || button.isDisposed()) return;
        String safeValue = value != null ? value : "";
        if (!button.getText().equals(safeValue)) {
            button.setText(safeValue);
        }
    }

    protected void setBackgroundSafe(Control control, Color color) {
        if (control == null || control.isDisposed()) return;
        if (color != null && !color.equals(control.getBackground())) {
            control.setBackground(color);
        }
    }

    protected void setForegroundSafe(Control control, Color color) {
        if (control == null || control.isDisposed()) return;
        if (color != null && !color.equals(control.getForeground())) {
            control.setForeground(color);
        }
    }

    protected void setSelectionSafe(Button button, boolean selected) {
        if (button == null || button.isDisposed()) return;
        if (button.getSelection() != selected) {
            button.setSelection(selected);
        }
    }

    protected void setEnabledSafe(Control control, boolean enabled) {
        if (control == null || control.isDisposed()) return;
        if (control.getEnabled() != enabled) {
            control.setEnabled(enabled);
        }
    }

    protected void setVisibleSafe(Control control, boolean visible) {
        if (control == null || control.isDisposed()) return;
        if (control.getVisible() != visible) {
            control.setVisible(visible);
        }
    }

    protected void setToolTipSafe(Control control, String toolTip) {
        if (control == null || control.isDisposed()) return;
        String safeToolTip = toolTip != null ? toolTip : "";
        if (!safeToolTip.equals(control.getToolTipText())) {
            control.setToolTipText(safeToolTip);
        }
    }

    protected void setLayoutDataSafe(Control control, Object layoutData) {
        if (control == null || control.isDisposed()) return;
        if (layoutData != null && layoutData != control.getLayoutData()) {
            control.setLayoutData(layoutData);
        }
    }

    protected void selectSafe(Combo combo, String value) {
        if (combo == null || combo.isDisposed()) return;
        String safeValue = value != null ? value : "";
        int index = combo.indexOf(safeValue);
        if (index >= 0 && combo.getSelectionIndex() != index) {
            combo.select(index);
        } else if (index < 0 && !combo.getText().equals(safeValue)) {
            combo.setText(safeValue);
        }
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().remove(modelAdapter);
        }
        this.orchestrator = orchestrator;
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().add(modelAdapter);
        }
        scheduleRefresh();
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    @Override
    public void dispose() {
        ProjectionService.getInstance().unsubscribe(projectionObserver);
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().remove(modelAdapter);
        }
        if (toolkit != null) {
            toolkit.dispose();
        }
        super.dispose();
    }
    
    public MultiPageEditor getEditor() {
		return editor;
	}

    public void setDirty(boolean dirty) {
        if (editor != null) {
            editor.setDirty(dirty);
        }
    }
}
