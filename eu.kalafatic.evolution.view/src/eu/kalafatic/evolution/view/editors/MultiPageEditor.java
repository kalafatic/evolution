package eu.kalafatic.evolution.view.editors;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Collator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.provider.OrchestrationGraphContentProvider;
import eu.kalafatic.evolution.view.provider.OrchestrationGraphLabelProvider;
import eu.kalafatic.evolution.view.wizards.OllamaSettingsPage.OllamaModel;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.manager.OllamaService;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gef.editparts.ZoomManager;

import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.manager.OllamaConfigManager;
import eu.kalafatic.evolution.controller.manager.OllamaConfigManager.OllamaDefaults;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import eu.kalafatic.evolution.model.orchestration.Agent;

/**
 * An example showing how to create a multi-page editor. This example has 4
 * pages:
 * <ul>
 * <li>page 0 is a chat interface.
 * <li>page 1 contains a nested text editor.
 * <li>page 2 allows you to change the font used in page 3
 * <li>page 3 shows the words in page 1 in sorted order
 * </ul>
 */
public class MultiPageEditor extends MultiPageEditorPart implements IResourceChangeListener, ISelectionListener {

	/** The Constant ID. */
	public static final String ID = "eu.kalafatic.evolution.view.editors.MultiPageEditor";

	public static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
	public static final String DEFAULT_OLLAMA_DIR = System.getProperty("user.home") + File.separator + "ollama";
	public static final String DEFAULT_OLLAMA_MODELS_DIR= DEFAULT_OLLAMA_DIR + File.separator + "models";

	/** The text editor used in page 1. */
	private TextEditor editor;

	/** The font chosen in page 2. */
	private Font font;

	/** The text widget used in page 3. */
	private StyledText text;
	private StyledText requestText;
	private StyledText responseText;

	private Orchestrator orchestrator;
	private Canvas statusCanvas;
	private OllamaService ollamaService;

	private org.eclipse.swt.widgets.Label ollamaStatusLabel;
	private org.eclipse.swt.widgets.Label modelStatusLabel;

	private ResourceSet resourceSet = new ResourceSetImpl();

	// Properties fields
	private Text orchIdText;
	private Text orchNameText;
	private Text llmModelText;
	private Text llmTempText;
	private Text ollamaUrlText;
	private Text ollamaModelText;
	private Text ollamaPathText;
	private Text ollamaVersionText;
	private Table agentsTable;
	private Text gitRepoText;
	private Text gitBranchText;
	private Text mavenGoalsText;
	private Text mavenProfilesText;
	private Text aiChatUrlText;
	private Text neuronAiUrlText;
	private Text compilerSourceText;

	private ControlDecoration ollamaUrlDecorator;
	private ControlDecoration ollamaPathDecorator;
	private ControlDecoration llmTempDecorator;
	private ControlDecoration gitRepoDecorator;

	private boolean dirty = false;
	private boolean isUpdating = false;

	private GraphViewer viewer;
	private Orchestrator currentOrchestrator;
	private ZoomManager zoomManager;
	private Browser browser;
	private Text urlText;

	public class OllamaModel {
		private String name;
		private long size;

		public OllamaModel(String name, long size) {
			this.name = name;
			this.size = size;
		}

		public String getName() {
			return name;
		}

		public long getSize() {
			return size;
		}
	}

	/**
	 * Creates a multi-page editor example.
	 */
	public MultiPageEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
	}

	/**
	 * Creates page 0 for AI Chat.
	 */
	void createAiChatPage() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 1;

		// Request Area
		createLabel(composite, "Request:");
		requestText = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData requestGridData = new GridData(GridData.FILL_BOTH);
		requestGridData.heightHint = 100;
		requestText.setLayoutData(requestGridData);

		Button sendButton = new Button(composite, SWT.PUSH);
		sendButton.setText("Send");
		Runnable sendAction = () -> {
			String request = requestText.getText().trim();
			if (request.isEmpty())
				return;

			if (ollamaService == null) {
				String url = null;
				String model = null;
				float temperature = 0.7f;
				if (orchestrator != null) {
					if (orchestrator.getOllama() != null) {
						url = orchestrator.getOllama().getUrl();
						model = orchestrator.getOllama().getModel();
					}
					if (orchestrator.getLlm() != null) {
						temperature = orchestrator.getLlm().getTemperature();
					}
				}
				ollamaService = new OllamaService(url, model).setTemperature(temperature).setNumPredict(1024)
						.setTopP(0.9f).setTopK(40).setRepeatPenalty(1.1f);
			}

			String currentResponse = responseText.getText();
			responseText.setText(currentResponse + (currentResponse.isEmpty() ? "" : "\n\n") + "You: " + request
					+ "\n\nOllama: thinking...");
			requestText.setText("");

			new Thread(() -> {
				try {
					String reply = ollamaService.chat(request);
					Display.getDefault().asyncExec(() -> {
						String updatedResponse = responseText.getText();
						updatedResponse = updatedResponse.replace("Ollama: thinking...", "Ollama: " + reply);
						responseText.setText(updatedResponse);
						responseText.setSelection(responseText.getCharCount());
					});
				} catch (Exception e) {
					Display.getDefault().asyncExec(() -> {
						String updatedResponse = responseText.getText();
						updatedResponse = updatedResponse.replace("Ollama: thinking...", "Error: " + e.getMessage());
						responseText.setText(updatedResponse);
					});
				}
			}).start();
		};
		sendButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sendAction.run();
			}
		});

		requestText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
					if ((e.stateMask & SWT.MODIFIER_MASK) == 0) {
						e.doit = false;
						sendAction.run();
					}
				}
			}
		});

		// Response Area
		createLabel(composite, "Response:");
		responseText = new StyledText(composite,
				SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		GridData responseGridData = new GridData(GridData.FILL_BOTH);
		responseGridData.heightHint = 200;
		responseText.setLayoutData(responseGridData);
		responseText.setEditable(false);

		// Status Bar
		Composite statusBar = new Composite(composite, SWT.NONE);
		statusBar.setLayout(new GridLayout(4, false));
		statusBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createLabel(statusBar, "Ollama Status:");
		ollamaStatusLabel = new org.eclipse.swt.widgets.Label(statusBar, SWT.NONE);
		ollamaStatusLabel.setText("Unknown");
		ollamaStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createLabel(statusBar, "Model:");
		modelStatusLabel = new org.eclipse.swt.widgets.Label(statusBar, SWT.NONE);
		modelStatusLabel.setText("Not Configured");
		modelStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		updateStatusInfo();

		int index = addPage(composite);
		setPageText(index, "AI Chat");
	}

	private void updatePropertiesInfo() {
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

			ollamaService = new OllamaService(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel())
					.setTemperature(orchestrator.getLlm() != null ? orchestrator.getLlm().getTemperature() : 0.7f)
					.setNumPredict(1024).setTopP(0.9f).setTopK(40).setRepeatPenalty(1.1f);
			
			new Thread(() -> {
				String version = "Offline";
				if (ollamaService != null && ollamaService.ping()) {
					version = ollamaService.getVersion();
				}
				final String v = version;
				Display.getDefault().asyncExec(() -> {
					if (!ollamaVersionText.isDisposed()) {
						ollamaVersionText.setText(v);
					}
				});
			}).start();
		}

		agentsTable.removeAll();
		for (Agent agent : orchestrator.getAgents()) {
			TableItem item = new TableItem(agentsTable, SWT.NONE);
			item.setText(0, agent.getId() != null ? agent.getId() : "");
			item.setText(1, agent.getType() != null ? agent.getType() : "");
			item.setText(2, agent.getExecutionMode() != null ? agent.getExecutionMode().name() : "");
		}

		if (orchestrator.getGit() != null) {
			gitRepoText.setText(
					orchestrator.getGit().getRepositoryUrl() != null ? orchestrator.getGit().getRepositoryUrl() : "");
			gitBranchText.setText(orchestrator.getGit().getBranch() != null ? orchestrator.getGit().getBranch() : "");
		}

		if (orchestrator.getMaven() != null) {
			mavenGoalsText.setText(orchestrator.getMaven().getGoals().toString());
			mavenProfilesText.setText(orchestrator.getMaven().getProfiles().toString());
		}

		if (orchestrator.getAiChat() != null) {
			aiChatUrlText.setText(orchestrator.getAiChat().getUrl() != null ? orchestrator.getAiChat().getUrl() : "");
		}

		if (orchestrator.getNeuronAI() != null) {
			neuronAiUrlText
					.setText(orchestrator.getNeuronAI().getUrl() != null ? orchestrator.getNeuronAI().getUrl() : "");
		}

		if (orchestrator.getCompiler() != null) {
			compilerSourceText.setText(orchestrator.getCompiler().getSourceVersion() != null
					? orchestrator.getCompiler().getSourceVersion()
					: "");
		}
		isUpdating = false;
	}

	private void updateStatusInfo() {
		if (orchestrator != null && orchestrator.getOllama() != null) {
			String url = orchestrator.getOllama().getUrl();
			String model = orchestrator.getOllama().getModel();

			if (ollamaService == null) {
				float temperature = 0.7f;
				if (orchestrator.getLlm() != null) {
					temperature = orchestrator.getLlm().getTemperature();
				}
				ollamaService = new OllamaService(url, model).setTemperature(temperature).setNumPredict(1024)
						.setTopP(0.9f).setTopK(40).setRepeatPenalty(1.1f);
			}

			modelStatusLabel.setText(model != null ? model : "Not Configured");

			new Thread(() -> {
				boolean isOnline = ollamaService.ping();
				Display.getDefault().asyncExec(() -> {
					if (ollamaStatusLabel.isDisposed())
						return;
					if (isOnline) {
						ollamaStatusLabel.setText("Online (" + url + ")");
						ollamaStatusLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
					} else {
						ollamaStatusLabel.setText("Offline (" + url + ")");
						ollamaStatusLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
					}
				});
			}).start();
		} else {
			ollamaStatusLabel.setText("Not Configured");
			modelStatusLabel.setText("Not Configured");
		}
	}

	/**
	 * Creates page 1 of the multi-page editor, which contains a text editor.
	 */
	void createEditorPage() {
		try {
			editor = new TextEditor();
			int index = addPage(editor, getEditorInput());
			setPageText(index, editor.getTitle());
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null, e.getStatus());
		}
	}

	/**
	 * Creates page 2 of the multi-page editor, which allows you to change the font
	 * used in page 3.
	 */
	void createLLMSettingsPage() {
		OllamaDefaults ollamaDefaults = new OllamaConfigManager().getDefaults();
		
		ScrolledComposite sc = new ScrolledComposite(getContainer(), SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		Composite composite = new Composite(sc, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 1;

		// --- Status Group ---
		Group statusGroup = new Group(composite, SWT.NONE);
		statusGroup.setText("Orchestration Status");
		statusGroup.setLayout(new GridLayout(1, false));
		statusGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		statusCanvas = new Canvas(statusGroup, SWT.DOUBLE_BUFFERED | SWT.BORDER);
		GridData canvasData = new GridData(GridData.FILL_HORIZONTAL);
		canvasData.heightHint = 80;
		statusCanvas.setLayoutData(canvasData);

		statusCanvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (orchestrator == null) {
					e.gc.drawString("No Orchestrator selected", 10, 10);
					return;
				}
				String id = orchestrator.getId();
				double progress = OrchestrationStatusManager.getInstance().getProgress(id);
				String status = OrchestrationStatusManager.getInstance().getStatus(id);

				int width = statusCanvas.getClientArea().width;
				int height = statusCanvas.getClientArea().height;

				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
				e.gc.fillRectangle(0, 0, width, height);

				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
				e.gc.fillRectangle(10, 30, width - 20, 20);

				e.gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
				e.gc.fillRectangle(10, 30, (int) ((width - 20) * progress), 20);

				e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
				e.gc.drawRectangle(10, 30, width - 20, 20);

				e.gc.drawString("Status: " + status, 10, 60);
				e.gc.drawString("Progress: " + (int) (progress * 100) + "%", 10, 10);
			}
		});

		// Timer for status refresh
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

		// --- Orchestrator Group ---
		Group orchGroup = new Group(composite, SWT.NONE);
		orchGroup.setText("Orchestrator");
		orchGroup.setLayout(new GridLayout(3, false));
		orchGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createLabel(orchGroup, "ID:");

		orchIdText = new Text(orchGroup, SWT.BORDER);
		orchIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(orchGroup, orchIdText);

		createLabel(orchGroup, "Name:");

		orchNameText = new Text(orchGroup, SWT.BORDER);
		orchNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(orchGroup, orchNameText);

		// --- LLM Group ---
		Group llmGroup = new Group(composite, SWT.NONE);
		llmGroup.setText("LLM Settings");
		llmGroup.setLayout(new GridLayout(3, false));
		llmGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createLabel(llmGroup, "Model:");
		llmModelText = new Text(llmGroup, SWT.BORDER);
		llmModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(llmGroup, llmModelText);

		createLabel(llmGroup, "Temperature:");
		llmTempText = new Text(llmGroup, SWT.BORDER);
		llmTempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(llmGroup, llmTempText);

		// --- Ollama Group ---
		Group ollamaGroup = new Group(composite, SWT.NONE);
		ollamaGroup.setText("Ollama Settings");
		ollamaGroup.setLayout(new GridLayout(3, false));
		ollamaGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createLabel(ollamaGroup,"URL:");
		ollamaUrlText = new Text(ollamaGroup, SWT.BORDER);
		ollamaUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ollamaUrlText.setText(ollamaDefaults.apiUrl);
		createEditButton(ollamaGroup, ollamaUrlText);

		createLabel(ollamaGroup, "Model:");
		ollamaModelText = new Text(ollamaGroup, SWT.BORDER);
		ollamaModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(ollamaGroup, ollamaModelText);

		// Placeholder for alignment
		createLabel(ollamaGroup, "Select Model:");
		selectModel(ollamaGroup);
		createLabel(ollamaGroup, "");

		createLabel(ollamaGroup, "Model Path:");
		ollamaPathText = new Text(ollamaGroup, SWT.BORDER);
		ollamaPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ollamaPathText.setText(ollamaDefaults.binPath);
		
		Button browseOllamaBtn = new Button(ollamaGroup, SWT.PUSH);
		browseOllamaBtn.setText("...");
		browseOllamaBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.swt.widgets.DirectoryDialog dialog = new org.eclipse.swt.widgets.DirectoryDialog(
						getSite().getShell(), SWT.OPEN);
				String path = dialog.open();
				if (path != null) {
					ollamaPathText.setText(path);
				}
			}
		});

		createLabel(ollamaGroup, "Version:");
		ollamaVersionText = new Text(ollamaGroup, SWT.BORDER | SWT.READ_ONLY);
		ollamaVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		new Label(ollamaGroup, SWT.NONE); // Placeholder

		// --- Agents Group ---
		Group agentsGroup = new Group(composite, SWT.NONE);
		agentsGroup.setText("Agents");
		agentsGroup.setLayout(new GridLayout(1, false));
		GridData agentsGd = new GridData(GridData.FILL_BOTH);
		agentsGd.heightHint = 150;
		agentsGroup.setLayoutData(agentsGd);

		agentsTable = new Table(agentsGroup, SWT.BORDER | SWT.FULL_SELECTION);
		agentsTable.setHeaderVisible(true);
		agentsTable.setLinesVisible(true);
		agentsTable.setLayoutData(new GridData(GridData.FILL_BOTH));

		TableColumn colId = new TableColumn(agentsTable, SWT.NONE);
		colId.setText("ID");
		colId.setWidth(100);

		TableColumn colType = new TableColumn(agentsTable, SWT.NONE);
		colType.setText("Type");
		colType.setWidth(100);

		TableColumn colMode = new TableColumn(agentsTable, SWT.NONE);
		colMode.setText("Execution Mode");
		colMode.setWidth(120);

		// --- Git & Maven Group ---
		Group gitMavenGroup = new Group(composite, SWT.NONE);
		gitMavenGroup.setText("Git & Maven");
		gitMavenGroup.setLayout(new GridLayout(3, false));
		gitMavenGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createLabel(gitMavenGroup, "Git Repo:");
		gitRepoText = new Text(gitMavenGroup, SWT.BORDER);
		gitRepoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(gitMavenGroup, gitRepoText);

		createLabel(gitMavenGroup, "Git Branch:");
		gitBranchText = new Text(gitMavenGroup, SWT.BORDER);
		gitBranchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(gitMavenGroup, gitBranchText);

		createLabel(gitMavenGroup, "Maven Goals:");
		mavenGoalsText = new Text(gitMavenGroup, SWT.BORDER);
		mavenGoalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(gitMavenGroup, mavenGoalsText);

		createLabel(gitMavenGroup, "Maven Profiles:");
		mavenProfilesText = new Text(gitMavenGroup, SWT.BORDER);
		mavenProfilesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(gitMavenGroup, mavenProfilesText);

		// --- Others Group ---
		Group othersGroup = new Group(composite, SWT.NONE);
		othersGroup.setText("Additional AI & Tools");
		othersGroup.setLayout(new GridLayout(3, false));
		othersGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createLabel(othersGroup, "AI Chat URL:");
		aiChatUrlText = new Text(othersGroup, SWT.BORDER);
		aiChatUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(othersGroup, aiChatUrlText);

		createLabel(othersGroup, "Neuron AI URL:");
		neuronAiUrlText = new Text(othersGroup, SWT.BORDER);
		neuronAiUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(othersGroup, neuronAiUrlText);

		createLabel(othersGroup, "Compiler Source:");
		compilerSourceText = new Text(othersGroup, SWT.BORDER);
		compilerSourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createEditButton(othersGroup, compilerSourceText);

		// Initialize decorations
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

		gitRepoDecorator = new ControlDecoration(gitRepoText, SWT.TOP | SWT.LEFT);
		gitRepoDecorator.setImage(
				FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		gitRepoDecorator.hide();

		// Add ModifyListeners
		ModifyListener modifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (orchestrator == null || isUpdating)
					return;
				updateModelFromFields();
				validateFields();
				setDirty(true);
			}
		};

		orchIdText.addModifyListener(modifyListener);
		orchNameText.addModifyListener(modifyListener);
		llmModelText.addModifyListener(modifyListener);
		llmTempText.addModifyListener(modifyListener);
		ollamaUrlText.addModifyListener(modifyListener);
		ollamaModelText.addModifyListener(modifyListener);
		ollamaPathText.addModifyListener(modifyListener);
		gitRepoText.addModifyListener(modifyListener);
		gitBranchText.addModifyListener(modifyListener);
		mavenGoalsText.addModifyListener(modifyListener);
		mavenProfilesText.addModifyListener(modifyListener);
		aiChatUrlText.addModifyListener(modifyListener);
		neuronAiUrlText.addModifyListener(modifyListener);
		compilerSourceText.addModifyListener(modifyListener);

		sc.setContent(composite);
		sc.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		int index = addPage(sc);
		setPageText(index, "Properties");
	}

	private void createLabel(Composite parent, String text) {
		GridData gd = new GridData();
		gd.widthHint = 100;

		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(gd);
		label.setText(text);
	}

	/**
	 * Creates page 3 of the multi-page editor, which shows the sorted text.
	 */
	void createPreviewPage() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		FillLayout layout = new FillLayout();
		composite.setLayout(layout);
		text = new StyledText(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		text.setEditable(false);

		int index = addPage(composite);
		setPageText(index, "Preview");
	}

	/**
	 * Creates the pages of the multi-page editor.
	 */
	protected void createPages() {
		createAiChatPage();
		createEditorPage();
		createLLMSettingsPage();
		createPreviewPage();
		createBrowserPage();
		createGrapghPage();
	}

	private Adapter modelAdapter = new AdapterImpl() {
		@Override
		public void notifyChanged(Notification notification) {
			refreshViewer();
		}
	};

	private void createGrapghPage() {
		Composite container = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;
		
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		viewer = new GraphViewer(container, SWT.NONE);
		
		// VERY IMPORTANT: make viewer control fill space
	    Control control = viewer.getControl();
	    control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
		viewer.setContentProvider(new OrchestrationGraphContentProvider());
		viewer.setLabelProvider(new OrchestrationGraphLabelProvider());
		viewer.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING));
		
		// IMPORTANT: You must set the input for the graph to actually render data
	     viewer.setInput(orchestrator != null ? new Object[] { orchestrator } : new Object[0]);
		
		int index = addPage(container);
		setPageText(index, "Graph");
	}
	
	

	private void updateInput(Orchestrator orchestrator) {
		if (currentOrchestrator != null) {
			currentOrchestrator.eAdapters().remove(modelAdapter);
		}
		currentOrchestrator = orchestrator;
		if (currentOrchestrator != null) {
			currentOrchestrator.eAdapters().add(modelAdapter);
			viewer.setInput(new Object[] { currentOrchestrator });
		}
	}

	public void refreshViewer() {
		Display.getDefault().asyncExec(() -> {
			if (viewer != null && !viewer.getControl().isDisposed()) {
				viewer.refresh();
				viewer.applyLayout();
			}
		});
	}

	private void createBrowserPage() {		
		Composite container = new Composite(getContainer(), SWT.NONE);
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		urlText = new Text(container, SWT.BORDER);
		urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		urlText.setText("https://ollama.com");

		Button goButton = new Button(container, SWT.PUSH);
		goButton.setText("Go");
		goButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.setUrl(urlText.getText());
			}
		});

		browser = new Browser(container, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		browser.setUrl(urlText.getText());
		
		int index = addPage(container);
		setPageText(index, "Browser");
	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this
	 * <code>IWorkbenchPart</code> method disposes all nested editors. Subclasses
	 * may extend.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {
	    // 1. Safely handle the nested editor
	    IEditorPart editor = getEditor(1);
	    if (editor != null) {
	        editor.doSave(monitor);
	    } else {
	        // Log a warning or handle the case where the editor page isn't ready
	        System.err.println("Warning: Editor at index 1 is null during save.");
	    }

	    // 2. Safely handle the EMF Resource (Orchestrator)
	    // Added a check for orchestrator != null to prevent NPE on the eResource() call itself
	    if (dirty && orchestrator != null) {
	        org.eclipse.emf.ecore.resource.Resource resource = orchestrator.eResource();
	        
	        if (resource != null) {
	            try {
	                resource.save(java.util.Collections.emptyMap());
	                setDirty(false);
	            } catch (Exception e) {
	                // Use a proper logger in production
	                e.printStackTrace();
	            }
	        }
	    }
	}
	/**
	 * Saves the multi-page editor's document as another file. Also updates the text
	 * for page 0's tab, and updates this multi-page editor's input to correspond to
	 * the nested editor's.
	 */
	public void doSaveAs() {
		IEditorPart editor = getEditor(1);
		editor.doSaveAs();
		setPageText(1, editor.getTitle());
		setInput(editor.getEditorInput());
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(1);
		IDE.gotoMarker(getEditor(1), marker);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method checks
	 * that the input is an instance of <code>IFileEditorInput</code>.
	 */
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput) && !(editorInput instanceof OrchestratorEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput or OrchestratorEditorInput");
		super.init(site, editorInput);

		if (editorInput instanceof OrchestratorEditorInput) {
			this.orchestrator = ((OrchestratorEditorInput) editorInput).getOrchestrator();
			String name = editorInput.getName();
			setPartName(name != null ? name : "Orchestrator");
		} else if (editorInput instanceof IFileEditorInput) {
			this.orchestrator = loadOrchestratorFromFile(((IFileEditorInput) editorInput).getFile());
			if (this.orchestrator != null) {
				String name = this.orchestrator.getName();
				setPartName(name != null ? name : "Orchestrator");
			} else {
				setPartName(editorInput.getName());
			}
		}

		// If we have an orchestrator, set it as selection
		if (this.orchestrator != null) {
			getSite().setSelectionProvider(new ISelectionProvider() {
				@Override
				public void setSelection(org.eclipse.jface.viewers.ISelection selection) {
				}

				@Override
				public void removeSelectionChangedListener(
						org.eclipse.jface.viewers.ISelectionChangedListener listener) {
				}

				@Override
				public org.eclipse.jface.viewers.ISelection getSelection() {
					return new StructuredSelection(orchestrator);
				}

				@Override
				public void addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener listener) {
				}
			});
		}

		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	private Orchestrator loadOrchestratorFromFile(IFile file) {
		String ext = file.getFileExtension();
		if ("xml".equals(ext) || "evo".equals(ext)) {
			try {
				URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
				Resource resource = resourceSet.getResource(uri, true);
				if (!resource.getContents().isEmpty() && resource.getContents().get(0) instanceof EvoProject) {
					EvoProject ep = (EvoProject) resource.getContents().get(0);
					if (!ep.getOrchestrations().isEmpty()) {
						return ep.getOrchestrations().get(0);
					}
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return null;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (part.getSite().getId().equals("eu.kalafatic.views.EvoNavigator")) {
			if (selection instanceof IStructuredSelection) {
				Object first = ((IStructuredSelection) selection).getFirstElement();
				Orchestrator selectedOrch = null;
				if (first instanceof Orchestrator) {
					selectedOrch = (Orchestrator) first;
				} else if (first instanceof IFile) {
					selectedOrch = loadOrchestratorFromFile((IFile) first);
				}

				if (selectedOrch != null && selectedOrch != this.orchestrator) {
					this.orchestrator = selectedOrch;
					this.ollamaService = null; // Reset service to re-initialize with new config
					String name = this.orchestrator.getName();
					setPartName(name != null ? name : "Orchestrator");
					Display.getDefault().asyncExec(() -> {
						updatePropertiesInfo();
						updateStatusInfo();
					});
				}
			}
		}
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IPropertySheetPage.class) {
			return adapter.cast(new PropertySheetPage());
		}
		return super.getAdapter(adapter);
	}

	private void setDirty(boolean dirty) {
		if (this.dirty != dirty) {
			this.dirty = dirty;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}

	@Override
	public boolean isDirty() {
		return dirty || super.isDirty();
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Calculates the contents of page 3 when it is activated.
	 */
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (newPageIndex == 0) {
			updateStatusInfo();
		} else if (newPageIndex == 2) {
			updatePropertiesInfo();
		}
		if (newPageIndex == 3) {
			sortWords();
		}
	}

	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(() -> {
				IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
				for (int i = 0; i < pages.length; i++) {
					if (((FileEditorInput) editor.getEditorInput()).getFile().getProject()
							.equals(event.getResource())) {
						IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
						pages[i].closeEditor(editorPart, true);
					}
				}
			});
		}
	}

	/**
	 * Sets the font related data to be applied to the text in page 3.
	 */
	void setFont() {
		FontDialog fontDialog = new FontDialog(getSite().getShell());
		fontDialog.setFontList(text.getFont().getFontData());
		FontData fontData = fontDialog.open();
		if (fontData != null) {
			if (font != null)
				font.dispose();
			font = new Font(text.getDisplay(), fontData);
			text.setFont(font);
		}
	}

	/**
	 * Sorts the words in page 1, and shows them in page 3.
	 */
	void sortWords() {

		String editorText = editor.getDocumentProvider().getDocument(editor.getEditorInput()).get();

		StringTokenizer tokenizer = new StringTokenizer(editorText, " \t\n\r\f!@#\u0024%^&*()-_=+`~[]{};:'\",.<>/?|\\");
		List<String> editorWords = new ArrayList<>();
		while (tokenizer.hasMoreTokens()) {
			editorWords.add(tokenizer.nextToken());
		}

		Collections.sort(editorWords, Collator.getInstance());
		StringWriter displayText = new StringWriter();
		for (int i = 0; i < editorWords.size(); i++) {
			displayText.write(((String) editorWords.get(i)));
			displayText.write(System.lineSeparator());
		}
		text.setText(displayText.toString());
	}

	private void validateFields() {
		if (ollamaUrlText.getText().isEmpty() || !ollamaUrlText.getText().startsWith("http")) {
			ollamaUrlDecorator.setDescriptionText("Invalid Ollama URL");
			ollamaUrlDecorator.show();
		} else {
			ollamaUrlDecorator.hide();
		}

		File f = new File(ollamaPathText.getText());
		if (!ollamaPathText.getText().isEmpty() && !f.exists()) {
			ollamaPathDecorator.setDescriptionText("Ollama path does not exist");
			ollamaPathDecorator.show();
		} else {
			ollamaPathDecorator.hide();
		}

		try {
			Float.parseFloat(llmTempText.getText());
			llmTempDecorator.hide();
		} catch (NumberFormatException e) {
			llmTempDecorator.setDescriptionText("Temperature must be a number");
			llmTempDecorator.show();
		}

		if (gitRepoText.getText().isEmpty()
				|| (!gitRepoText.getText().startsWith("http") && !gitRepoText.getText().startsWith("git@"))) {
			gitRepoDecorator.setDescriptionText("Invalid Git Repository URL");
			gitRepoDecorator.show();
		} else {
			gitRepoDecorator.hide();
		}
	}

	private void updateModelFromFields() {
		if (orchestrator == null || isUpdating)
			return;
		isUpdating = true;

		orchestrator.setId(orchIdText.getText());
		orchestrator.setName(orchNameText.getText());

		if (orchestrator.getLlm() != null) {
			orchestrator.getLlm().setModel(llmModelText.getText());
			try {
				orchestrator.getLlm().setTemperature(Float.parseFloat(llmTempText.getText()));
			} catch (NumberFormatException e) {
				// Ignore invalid input for model update
			}
		}

		if (orchestrator.getOllama() != null) {
			orchestrator.getOllama().setUrl(ollamaUrlText.getText());
			orchestrator.getOllama().setModel(ollamaModelText.getText());
			orchestrator.getOllama().setPath(ollamaPathText.getText());
		}

		if (orchestrator.getGit() != null) {
			orchestrator.getGit().setRepositoryUrl(gitRepoText.getText());
			orchestrator.getGit().setBranch(gitBranchText.getText());
		}

		if (orchestrator.getMaven() != null) {
			orchestrator.getMaven().getGoals().clear();
			String goals = mavenGoalsText.getText().replace("[", "").replace("]", "");
			for (String goal : goals.split("[,\\s]+")) {
				if (!goal.trim().isEmpty())
					orchestrator.getMaven().getGoals().add(goal.trim());
			}

			orchestrator.getMaven().getProfiles().clear();
			String profiles = mavenProfilesText.getText().replace("[", "").replace("]", "");
			for (String profile : profiles.split("[,\\s]+")) {
				if (!profile.trim().isEmpty())
					orchestrator.getMaven().getProfiles().add(profile.trim());
			}
		}

		if (orchestrator.getAiChat() != null) {
			orchestrator.getAiChat().setUrl(aiChatUrlText.getText());
		}

		if (orchestrator.getNeuronAI() != null) {
			orchestrator.getNeuronAI().setUrl(neuronAiUrlText.getText());
		}

		if (orchestrator.getCompiler() != null) {
			orchestrator.getCompiler().setSourceVersion(compilerSourceText.getText());
		}
		
		ollamaService = new OllamaService(orchestrator.getOllama().getUrl(), orchestrator.getOllama().getModel())
				.setTemperature(orchestrator.getLlm() != null ? orchestrator.getLlm().getTemperature() : 0.7f)
				.setNumPredict(1024).setTopP(0.9f).setTopK(40).setRepeatPenalty(1.1f);
		
		isUpdating = false;
	}

	private void createEditButton(Composite parent, Text textWidget) {
		GridData gd = new GridData();
		gd.widthHint = 100;

		Button btn = new Button(parent, SWT.PUSH);
		btn.setLayoutData(gd);

		btn.setText("Edit");
		btn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Generic edit action - could be expanded to show a dialog
				// For now, it just ensures the field is editable which it already is.
				textWidget.setFocus();
				textWidget.setSelection(0, textWidget.getText().length());
			}
		});
	}

	private Combo selectModel(Composite parent) {
		List<OllamaModel> models = loadModels(); // Load models to populate the combo

		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		if (models != null) {
			for (OllamaModel f : models) {
				combo.add(f.getName());
			}
		}

		// selection listener
		combo.addListener(SWT.Selection, e -> {
			int index = combo.getSelectionIndex();
			if (index >= 0) {
				OllamaModel selected = models.get(index);
				ollamaModelText.setText(selected.getName());
				llmModelText.setText(selected.getName());
				llmTempText.setText("0.7"); // Reset to default for new model
				
				updateModelFromFields(); // Update model with new selection
				updatePropertiesInfo() ; // Update to reflect model change
				updateStatusInfo(); // Update to reflect model change
				
				System.out.println("Selected: " + selected.getName());
			}
		});
		return combo;
	}

	public List<OllamaModel> loadModels() {
		List<OllamaModel> result = new ArrayList<>();

		try {
			URL url = new URL(DEFAULT_OLLAMA_URL + "/api/tags");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			StringBuilder json = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				json.append(line);
			}

			reader.close();

			JSONObject obj = new JSONObject(json.toString());
			JSONArray models = obj.getJSONArray("models");

			for (int i = 0; i < models.length(); i++) {
				JSONObject m = models.getJSONObject(i);

				String name = m.getString("name");
				long size = m.optLong("size", 0);

				result.add(new OllamaModel(name, size));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}
}
