package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import eu.kalafatic.evolution.controller.manager.*;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class PropertiesPage extends ScrolledComposite {

	OllamaConfigManager.OllamaDefaults defaults = OllamaConfigManager.getDefaults();

	private MultiPageEditor editor;
	private Orchestrator orchestrator;
	private boolean isUpdating = false;
	private Canvas statusCanvas;
	private Label modeIndicatorLabel;
	private Text orchIdText, orchNameText, llmModelText, llmTempText, ollamaUrlText, ollamaModelText, ollamaPathText,
			ollamaVersionText;
	private Button offlineBtn;
	private Combo aiModeCombo;
	private Combo remoteModelCombo;
	private Text localModelText, hybridModelText;
	private Text mcpUrlText, openAiTokenText, openAiModelText;
	private Table agentsTable;
	private SashForm sashForm;
	private Text aiChatUrlText, neuronAiUrlText,
			compilerSourceText;
	private ControlDecoration ollamaUrlDecorator, ollamaPathDecorator, llmTempDecorator;
	private OllamaService ollamaService;

	public PropertiesPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		this.editor = editor;
		this.orchestrator = orchestrator;
		this.setExpandHorizontal(true);
		this.setExpandVertical(true);
		createControl();
	}

	private void createControl() {

		ollamaService = new OllamaService(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel());
		
		Composite comp = new Composite(this, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));

		modeIndicatorLabel = new Label(comp, SWT.CENTER);
		modeIndicatorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modeIndicatorLabel.setText("AI MODE INDICATOR");

		Font bannerDefault = org.eclipse.jface.resource.JFaceResources.getBannerFont();
		FontData[] bannerData = bannerDefault.getFontData();
		for (FontData fd : bannerData) {
			fd.setStyle(SWT.BOLD);
		}
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
		((GridData) statusCanvas.getLayoutData()).heightHint = 80;
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
			@Override
			public void run() {
				if (!statusCanvas.isDisposed()) {
					statusCanvas.redraw();
					Display.getDefault().timerExec(1000, this);
				}
			}
		};
		Display.getDefault().timerExec(1000, timer);

		sashForm = new SashForm(comp, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		Group orchGroup = SWTFactory.createMaximizableGroup(sashForm, "Orchestrator", 3);
		SWTFactory.createLabel(orchGroup, "ID:");
		orchIdText = new Text(orchGroup, SWT.BORDER);
		orchIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(orchGroup, orchIdText);
		SWTFactory.createLabel(orchGroup, "Name:");
		orchNameText = new Text(orchGroup, SWT.BORDER);
		orchNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(orchGroup, orchNameText);

		Group llmGroup = SWTFactory.createMaximizableGroup(sashForm, "LLM Settings", 3);
		SWTFactory.createLabel(llmGroup, "Model:");
		llmModelText = new Text(llmGroup, SWT.BORDER);
		llmModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(llmGroup, llmModelText);
		SWTFactory.createLabel(llmGroup, "Temperature:");
		llmTempText = new Text(llmGroup, SWT.BORDER);
		llmTempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(llmGroup, llmTempText);

		Group ollamaGroup = SWTFactory.createMaximizableGroup(sashForm, "Ollama Settings", 3);
		SWTFactory.createLabel(ollamaGroup, "URL:");
		ollamaUrlText = new Text(ollamaGroup, SWT.BORDER);
		ollamaUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(ollamaGroup, ollamaUrlText);
		SWTFactory.createLabel(ollamaGroup, "Model:");
		ollamaModelText = new Text(ollamaGroup, SWT.BORDER);
		ollamaModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(ollamaGroup, ollamaModelText);
		SWTFactory.createLabel(ollamaGroup, "Select Model:");
		selectModel(ollamaGroup, ollamaService);
		SWTFactory.createLabel(ollamaGroup, "");
		SWTFactory.createLabel(ollamaGroup, "Model Path:");
		ollamaPathText = new Text(ollamaGroup, SWT.BORDER);
		ollamaPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button browseOllamaBtn = new Button(ollamaGroup, SWT.PUSH);
		browseOllamaBtn.setText("...");
		browseOllamaBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String p = new org.eclipse.swt.widgets.DirectoryDialog(getShell(), SWT.OPEN).open();
				if (p != null)
					ollamaPathText.setText(p);
			}
		});
		SWTFactory.createLabel(ollamaGroup, "Version:");
		ollamaVersionText = new Text(ollamaGroup, SWT.BORDER | SWT.READ_ONLY);
		ollamaVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createLabel(ollamaGroup, "");

		Group agentsGroup = SWTFactory.createMaximizableGroup(sashForm, "Agents", 1);
		agentsTable = new Table(agentsGroup, SWT.BORDER | SWT.FULL_SELECTION);
		agentsTable.setHeaderVisible(true);
		agentsTable.setLinesVisible(true);
		agentsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		String[] headers = { "ID", "Type", "Execution Mode" };
		int[] widths = { 100, 100, 120 };
		for (int i = 0; i < headers.length; i++) {
			TableColumn col = new TableColumn(agentsTable, SWT.NONE);
			col.setText(headers[i]);
			col.setWidth(widths[i]);
		}

		Group othersGroup = SWTFactory.createMaximizableGroup(sashForm, "Additional AI & Tools", 3);
		SWTFactory.createLabel(othersGroup, "AI Chat URL:");
		aiChatUrlText = new Text(othersGroup, SWT.BORDER);
		aiChatUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(othersGroup, aiChatUrlText);
		SWTFactory.createLabel(othersGroup, "Neuron AI URL:");
		neuronAiUrlText = new Text(othersGroup, SWT.BORDER);
		neuronAiUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(othersGroup, neuronAiUrlText);
		SWTFactory.createLabel(othersGroup, "Compiler Source:");
		compilerSourceText = new Text(othersGroup, SWT.BORDER);
		compilerSourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(othersGroup, compilerSourceText);

		Group mcpOpenAiGroup = SWTFactory.createMaximizableGroup(sashForm, "MCP & OpenAI (Hybrid Architecture)", 3);
		SWTFactory.createLabel(mcpOpenAiGroup, "AI Mode:");
		aiModeCombo = new Combo(mcpOpenAiGroup, SWT.READ_ONLY);
		aiModeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
		for (eu.kalafatic.evolution.model.orchestration.AiMode mode : eu.kalafatic.evolution.model.orchestration.AiMode
				.values()) {
			aiModeCombo.add(mode.getName());
		}
		aiModeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (orchestrator != null && !isUpdating) {
					updateModelFromFields();
					editor.setDirty(true);
				}
			}
		});

		SWTFactory.createLabel(mcpOpenAiGroup, "Offline Mode (Legacy):");
		offlineBtn = new Button(mcpOpenAiGroup, SWT.CHECK);
		offlineBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
		SWTFactory.createLabel(mcpOpenAiGroup, "MCP Server URL:");
		mcpUrlText = new Text(mcpOpenAiGroup, SWT.BORDER);
		mcpUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(mcpOpenAiGroup, mcpUrlText);
		SWTFactory.createLabel(mcpOpenAiGroup, "OpenAI Token:");
		openAiTokenText = new Text(mcpOpenAiGroup, SWT.BORDER | SWT.PASSWORD);
		openAiTokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(mcpOpenAiGroup, openAiTokenText);
		SWTFactory.createLabel(mcpOpenAiGroup, "OpenAI Model:");
		openAiModelText = new Text(mcpOpenAiGroup, SWT.BORDER);
		openAiModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(mcpOpenAiGroup, openAiModelText);

		Group aiModelGroup = SWTFactory.createMaximizableGroup(sashForm, "AI Chat Models (per Mode)", 3);
		SWTFactory.createLabel(aiModelGroup, "Local Model:");
		localModelText = new Text(aiModelGroup, SWT.BORDER);
		localModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(aiModelGroup, localModelText);
		SWTFactory.createLabel(aiModelGroup, "Hybrid Model:");
		hybridModelText = new Text(aiModelGroup, SWT.BORDER);
		hybridModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTFactory.createEditButton(aiModelGroup, hybridModelText);
		SWTFactory.createLabel(aiModelGroup, "Remote Model:");
		remoteModelCombo = new Combo(aiModelGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		remoteModelCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
		for (String providerName : AiProviders.PROVIDERS.keySet()) {
			remoteModelCombo.add(providerName);
		}
		remoteModelCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (orchestrator != null && !isUpdating) {
					String provider = remoteModelCombo.getText();
					ProviderConfig config = AiProviders.PROVIDERS.get(provider);
					if (config != null) {
						String url = config.getEndpointUrl();
						aiChatUrlText.setText(url != null ? url : "");
					}
					updateModelFromFields();
					editor.setDirty(true);
				}
			}
		});

		sashForm.setWeights(new int[] { 10, 10, 20, 20, 10, 15, 15 });

		ollamaUrlDecorator = new ControlDecoration(ollamaUrlText, SWT.TOP | SWT.LEFT);
		ollamaUrlDecorator.setImage(
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		ollamaUrlDecorator.hide();
		ollamaPathDecorator = new ControlDecoration(ollamaPathText, SWT.TOP | SWT.LEFT);
		ollamaPathDecorator.setImage(
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		ollamaPathDecorator.hide();
		llmTempDecorator = new ControlDecoration(llmTempText, SWT.TOP | SWT.LEFT);
		llmTempDecorator.setImage(
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		llmTempDecorator.hide();

		ModifyListener ml = e -> {
			if (orchestrator != null && !isUpdating) {
				updateModelFromFields();
				validateFields();
				editor.setDirty(true);
			}
		};
		Text[] texts = { orchIdText, orchNameText, llmModelText, llmTempText, ollamaUrlText, ollamaModelText,
				ollamaPathText, aiChatUrlText,
				neuronAiUrlText, compilerSourceText, mcpUrlText, openAiTokenText, openAiModelText, localModelText,
				hybridModelText };
		for (Text t : texts)
			t.addModifyListener(ml);
		offlineBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (orchestrator != null && !isUpdating) {
					updateModelFromFields();
					editor.setDirty(true);
				}
			}
		});

		this.setContent(comp);
		this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		updatePropertiesInfo();
		updateModeDisplay();
	}

	private void updateModeDisplay() {
		if (orchestrator == null || modeIndicatorLabel == null || modeIndicatorLabel.isDisposed()) return;

		AiMode mode = orchestrator.getAiMode();
		modeIndicatorLabel.setText(mode.getName().toUpperCase() + " MODE ACTIVE");
		modeIndicatorLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_WHITE));

		switch (mode) {
			case LOCAL:
				modeIndicatorLabel.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
				break;
			case HYBRID:
				modeIndicatorLabel.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
				break;
			case REMOTE:
				modeIndicatorLabel.setBackground(getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA));
				break;
		}
	}

	private void selectModel(Group ollamaGroup, OllamaService ollamaService) {
		SWTFactory.selectModel(ollamaGroup, ollamaService).addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int idx = ((Combo) e.getSource()).getSelectionIndex();
				if (idx >= 0 && ollamaService != null) {
					List<eu.kalafatic.evolution.controller.manager.OllamaModel> models = ollamaService.loadModels();
					if (idx < models.size()) {
						llmModelText.setText(models.get(idx).getName());
						updateModelFromFields();
						editor.setDirty(true);
					}
				}
			}
		});

//		combo.addSelectionListener(new SelectionAdapter() {
//            @Override public void widgetSelected(SelectionEvent e) {
//                int idx = combo.getSelectionIndex();
//                if (idx >= 0) {
//                    eu.kalafatic.evolution.controller.manager.OllamaModel m = models.get(idx);
//                    ollamaModelText.setText(m.getName()); llmModelText.setText(m.getName()); llmTempText.setText("0.7");
//                    updateModelFromFields(); updatePropertiesInfo();
//                }
//            }
//        });
	}

	public void updatePropertiesInfo() {
		if (orchestrator == null || isUpdating)
			return;
		isUpdating = true;
		orchIdText.setText(orchestrator.getId() != null ? orchestrator.getId() : "");
		orchNameText.setText(orchestrator.getName() != null ? orchestrator.getName() : "");
		if (orchestrator.getLlm() != null) {
			llmModelText.setText(orchestrator.getLlm().getModel() != null ? orchestrator.getLlm().getModel() : "");
			llmTempText.setText(String.valueOf(orchestrator.getLlm().getTemperature()));
		}
		if (orchestrator.getOllama() != null) {
			ollamaUrlText.setText(orchestrator.getOllama().getUrl() != null ? orchestrator.getOllama().getUrl() : "");
			ollamaModelText
					.setText(orchestrator.getOllama().getModel() != null ? orchestrator.getOllama().getModel() : "");
			ollamaPathText
					.setText(orchestrator.getOllama().getPath() != null ? orchestrator.getOllama().getPath() : "");
			ollamaService = new OllamaService(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel());
			new Thread(() -> {
				String v = (ollamaService != null && ollamaService.ping()) ? ollamaService.getVersion() : "Offline";
				Display.getDefault().asyncExec(() -> {
					if (!ollamaVersionText.isDisposed())
						ollamaVersionText.setText(v);
				});
			}).start();
		}
		agentsTable.removeAll();
		for (Agent a : orchestrator.getAgents()) {
			TableItem item = new TableItem(agentsTable, SWT.NONE);
			item.setText(0, a.getId() != null ? a.getId() : "");
			item.setText(1, a.getType() != null ? a.getType() : "");
			item.setText(2, a.getExecutionMode() != null ? a.getExecutionMode().name() : "");
		}
		if (orchestrator.getAiChat() != null)
			aiChatUrlText.setText(orchestrator.getAiChat().getUrl() != null ? orchestrator.getAiChat().getUrl() : "");
		if (orchestrator.getNeuronAI() != null)
			neuronAiUrlText
					.setText(orchestrator.getNeuronAI().getUrl() != null ? orchestrator.getNeuronAI().getUrl() : "");
		if (orchestrator.getCompiler() != null)
			compilerSourceText.setText(orchestrator.getCompiler().getSourceVersion() != null
					? orchestrator.getCompiler().getSourceVersion()
					: "");

		aiModeCombo.select(orchestrator.getAiMode().getValue());
		

		mcpUrlText.setText(orchestrator.getMcpServerUrl() != null ? orchestrator.getMcpServerUrl() : "");
		openAiTokenText.setText(orchestrator.getOpenAiToken() != null ? orchestrator.getOpenAiToken() : "");
		openAiModelText.setText(orchestrator.getOpenAiModel() != null ? orchestrator.getOpenAiModel() : "");
		localModelText.setText(orchestrator.getLocalModel() != null ? orchestrator.getLocalModel() : "");
		hybridModelText.setText(orchestrator.getHybridModel() != null ? orchestrator.getHybridModel() : "");
		if (orchestrator.getRemoteModel() != null) {
			int index = remoteModelCombo.indexOf(orchestrator.getRemoteModel());
			if (index >= 0)
				remoteModelCombo.select(index);
		}
		offlineBtn.setSelection(orchestrator.isOfflineMode());

		isUpdating = false;
	}

	private void validateFields() {
		boolean urlValid = !ollamaUrlText.getText().isEmpty() && ollamaUrlText.getText().startsWith("http");
		if (!urlValid) {
			ollamaUrlDecorator.setDescriptionText("Invalid Ollama URL");
			ollamaUrlDecorator.show();
		} else
			ollamaUrlDecorator.hide();
		File f = new File(ollamaPathText.getText());
		if (!ollamaPathText.getText().isEmpty() && !f.exists()) {
			ollamaPathDecorator.setDescriptionText("Ollama path does not exist");
			ollamaPathDecorator.show();
		} else
			ollamaPathDecorator.hide();
		try {
			Float.parseFloat(llmTempText.getText());
			llmTempDecorator.hide();
		} catch (NumberFormatException e) {
			llmTempDecorator.setDescriptionText("Temperature must be a number");
			llmTempDecorator.show();
		}
	}

	private void updateModelFromFields() {
		if (orchestrator == null || isUpdating)
			return;
		isUpdating = true;
		orchestrator.setId(orchIdText.getText());
		orchestrator.setName(orchNameText.getText());

		if (orchestrator.getLlm() == null) {
			orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
		}
		orchestrator.getLlm().setModel(llmModelText.getText());
		try {
			orchestrator.getLlm().setTemperature(Float.parseFloat(llmTempText.getText()));
		} catch (NumberFormatException e) {
		}

		if (orchestrator.getOllama() == null) {
			orchestrator.setOllama(OrchestrationFactory.eINSTANCE.createOllama());
		}
		orchestrator.getOllama().setUrl(ollamaUrlText.getText());
		orchestrator.getOllama().setModel(ollamaModelText.getText());
		orchestrator.getOllama().setPath(ollamaPathText.getText());

		if (orchestrator.getAiChat() == null) {
			orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
		}
		orchestrator.getAiChat().setUrl(aiChatUrlText.getText());

		if (orchestrator.getNeuronAI() == null) {
			orchestrator.setNeuronAI(OrchestrationFactory.eINSTANCE.createNeuronAI());
		}
		orchestrator.getNeuronAI().setUrl(neuronAiUrlText.getText());

		if (orchestrator.getCompiler() == null) {
			orchestrator.setCompiler(OrchestrationFactory.eINSTANCE.createCompiler());
		}
		orchestrator.getCompiler().setSourceVersion(compilerSourceText.getText());

		orchestrator.setAiMode(AiMode.get(aiModeCombo.getSelectionIndex()));	
		orchestrator.setMcpServerUrl(mcpUrlText.getText());
		orchestrator.setOpenAiToken(openAiTokenText.getText());
		orchestrator.setOpenAiModel(openAiModelText.getText());
		orchestrator.setLocalModel(localModelText.getText());
		orchestrator.setHybridModel(hybridModelText.getText());
		orchestrator.setRemoteModel(remoteModelCombo.getText());
		orchestrator.setOfflineMode(offlineBtn.getSelection());

		ollamaService = new OllamaService(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel());
		isUpdating = false;
		updateModeDisplay();
	}

	public void setOrchestrator(Orchestrator orchestrator) {
		this.orchestrator = orchestrator;
		updatePropertiesInfo();
		updateModeDisplay();
	}
}
