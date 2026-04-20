package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import eu.kalafatic.evolution.controller.manager.*;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.properties.*;

public class PropertiesPage extends AEvoPage {

	private boolean isUpdating = false;
	private Canvas statusCanvas;
	private Label modeIndicatorLabel;

	private OrchestratorGroup orchestratorGroup;
	private LlmSettingsGroup llmSettingsGroup;
	private OllamaSettingsGroup ollamaSettingsGroup;
	private AgentsGroup agentsGroup;
	private AdditionalAiToolsGroup additionalAiToolsGroup;
	private McpOpenAiGroup mcpOpenAiGroup;
	private AiChatModelsGroup aiChatModelsGroup;
	private ModelsGroup modelsGroup;

	public PropertiesPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, editor, orchestrator);
		createControl();
	}

	private void createControl() {
		Composite comp = toolkit.createComposite(this);
		comp.setLayout(new GridLayout(1, false));

		modeIndicatorLabel = new Label(comp, SWT.CENTER);
		modeIndicatorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modeIndicatorLabel.setText("AI MODE INDICATOR");

		Font bannerDefault = org.eclipse.jface.resource.JFaceResources.getBannerFont();
		FontData[] bannerData = bannerDefault.getFontData();
		for (FontData fd : bannerData) fd.setStyle(SWT.BOLD);
		final Font bannerFont = new Font(getDisplay(), bannerData);
		modeIndicatorLabel.setFont(bannerFont);
		modeIndicatorLabel.addDisposeListener(e -> {
			if (bannerFont != null && !bannerFont.isDisposed()) bannerFont.dispose();
		});

		Group statusGroup = new Group(comp, SWT.NONE);
		statusGroup.setText("Orchestration Status");
		statusGroup.setLayout(new GridLayout(1, false));
		statusGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		statusCanvas = new Canvas(statusGroup, SWT.DOUBLE_BUFFERED | SWT.BORDER);
		statusCanvas.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		((GridData) statusCanvas.getLayoutData()).heightHint = 53;
		statusCanvas.addPaintListener(e -> {
			if (orchestrator == null) {
				e.gc.drawString("No Orchestrator selected", 10, 10);
				return;
			}
			String id = orchestrator.getId();
			double progress = OrchestrationStatusManager.getInstance().getProgress(id);
			String status = OrchestrationStatusManager.getInstance().getStatus(id);
			e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			e.gc.fillRectangle(0, 0, statusCanvas.getClientArea().width, statusCanvas.getClientArea().height);
			e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
			e.gc.fillRectangle(10, 30, statusCanvas.getClientArea().width - 20, 20);
			e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
			e.gc.fillRectangle(10, 30, (int) ((statusCanvas.getClientArea().width - 20) * progress), 20);
			e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
			e.gc.drawRectangle(10, 30, statusCanvas.getClientArea().width - 20, 20);
			e.gc.drawString("Status: " + status, 10, 60);
			e.gc.drawString("Progress: " + (int) (progress * 100) + "%", 10, 10);
		});
		Runnable timer = new Runnable() {
			@Override public void run() {
				if (!statusCanvas.isDisposed()) {
					statusCanvas.redraw();
					Display.getDefault().timerExec(1000, this);
				}
			}
		};
		Display.getDefault().timerExec(1000, timer);

		orchestratorGroup = new OrchestratorGroup(toolkit, comp, editor, orchestrator);
		llmSettingsGroup = new LlmSettingsGroup(toolkit, comp, editor, orchestrator);
		ollamaSettingsGroup = new OllamaSettingsGroup(toolkit, comp, editor, orchestrator, this);
		agentsGroup = new AgentsGroup(toolkit, comp, editor, orchestrator);
		additionalAiToolsGroup = new AdditionalAiToolsGroup(toolkit, comp, editor, orchestrator);
		mcpOpenAiGroup = new McpOpenAiGroup(toolkit, comp, editor, orchestrator, this);
		aiChatModelsGroup = new AiChatModelsGroup(toolkit, comp, editor, orchestrator, this);
		modelsGroup = new ModelsGroup(toolkit, comp, editor, orchestrator);

		ModifyListener ml = e -> {
			if (orchestrator != null && !isUpdating) {
				syncModelWithUI();
				editor.setDirty(true);
			}
		};
		orchestratorGroup.addModifyListener(ml);
		llmSettingsGroup.addModifyListener(ml);
		ollamaSettingsGroup.addModifyListener(ml);
		additionalAiToolsGroup.addModifyListener(ml);
		mcpOpenAiGroup.addModifyListener(ml);
		aiChatModelsGroup.addModifyListener(ml);
		modelsGroup.addModifyListener(ml);

		this.setContent(comp);
		this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		updatePropertiesInfo();
		updateModeDisplay();
	}

	public void updateModeDisplay() {
		if (orchestrator == null || modeIndicatorLabel == null || modeIndicatorLabel.isDisposed()) return;
		AiMode mode = orchestrator.getAiMode();
		modeIndicatorLabel.setText(mode.getName().toUpperCase() + " MODE ACTIVE");
		modeIndicatorLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		switch (mode) {
			case LOCAL: modeIndicatorLabel.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN)); break;
			case HYBRID: modeIndicatorLabel.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE)); break;
			case REMOTE: modeIndicatorLabel.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA)); break;
		}
	}

	@Override
	public void refreshUI() {
		if (orchestrator == null || isUpdating) return;
		isUpdating = true;
		orchestratorGroup.updateUI(); llmSettingsGroup.updateUI(); ollamaSettingsGroup.updateUI(); agentsGroup.updateUI(); additionalAiToolsGroup.updateUI(); mcpOpenAiGroup.updateUI(); aiChatModelsGroup.updateUI(); modelsGroup.updateUI();
		isUpdating = false;
		updateModeDisplay();
	}

	public void updatePropertiesInfo() {
		scheduleRefresh();
	}

	public void syncModelWithUI() {
		if (orchestrator == null || isUpdating) return;
		isUpdating = true;
		orchestratorGroup.updateModel(); llmSettingsGroup.updateModel(); ollamaSettingsGroup.updateModel(); additionalAiToolsGroup.updateModel(); mcpOpenAiGroup.updateModel(); aiChatModelsGroup.updateModel(); modelsGroup.updateModel();
		isUpdating = false;
		updateModeDisplay();
	}

	public void updateAiChatUrl(String url) { additionalAiToolsGroup.updateUI(); }

	@Override
	public void setOrchestrator(Orchestrator orchestrator) {
		super.setOrchestrator(orchestrator);
		if (orchestratorGroup != null) orchestratorGroup.setOrchestrator(orchestrator);
		if (llmSettingsGroup != null) llmSettingsGroup.setOrchestrator(orchestrator);
		if (ollamaSettingsGroup != null) ollamaSettingsGroup.setOrchestrator(orchestrator);
		if (agentsGroup != null) agentsGroup.setOrchestrator(orchestrator);
		if (additionalAiToolsGroup != null) additionalAiToolsGroup.setOrchestrator(orchestrator);
		if (mcpOpenAiGroup != null) mcpOpenAiGroup.setOrchestrator(orchestrator);
		if (aiChatModelsGroup != null) aiChatModelsGroup.setOrchestrator(orchestrator);
		if (modelsGroup != null) modelsGroup.setOrchestrator(orchestrator);
	}
}
