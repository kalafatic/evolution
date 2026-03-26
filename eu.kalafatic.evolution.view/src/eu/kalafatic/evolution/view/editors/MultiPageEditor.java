package eu.kalafatic.evolution.view.editors;


import java.io.File;
import java.io.StringWriter;
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
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;

import eu.kalafatic.evolution.controller.manager.OllamaService;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import eu.kalafatic.evolution.model.orchestration.Agent;

/**
 * An example showing how to create a multi-page editor.
 * This example has 4 pages:
 * <ul>
 * <li>page 0 is a chat interface.
 * <li>page 1 contains a nested text editor.
 * <li>page 2 allows you to change the font used in page 3
 * <li>page 3 shows the words in page 1 in sorted order
 * </ul>
 */
public class MultiPageEditor extends MultiPageEditorPart implements IResourceChangeListener{
	
	/** The Constant ID. */
	public static final String ID = "eu.kalafatic.evolution.view.editors.MaintainEditor";

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

	/**
	 * Creates a multi-page editor example.
	 */
	public MultiPageEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
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
		new org.eclipse.swt.widgets.Label(composite, SWT.NONE).setText("Request:");
		requestText = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData requestGridData = new GridData(GridData.FILL_BOTH);
        requestGridData.heightHint = 100;
		requestText.setLayoutData(requestGridData);
		
		// Loading a local GGUF model file
//		Model model = Model.load("path/to/model.gguf");
//		String output = model.generate("Hello world!");

		Button sendButton = new Button(composite, SWT.PUSH);
		sendButton.setText("Send");
        Runnable sendAction = () -> {
            String request = requestText.getText().trim();
            if (request.isEmpty()) return;
            
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
                ollamaService = new OllamaService(url, model)
                        .setTemperature(temperature)
                        .setNumPredict(1024)
                        .setTopP(0.9f)
                        .setTopK(40)
                        .setRepeatPenalty(1.1f);
            }

            String currentResponse = responseText.getText();
            responseText.setText(currentResponse + (currentResponse.isEmpty() ? "" : "\n\n") + "You: " + request + "\n\nOllama: thinking...");
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
		new org.eclipse.swt.widgets.Label(composite, SWT.NONE).setText("Response:");
		responseText = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		GridData responseGridData = new GridData(GridData.FILL_BOTH);
        responseGridData.heightHint = 200;
		responseText.setLayoutData(responseGridData);
        responseText.setEditable(false);

        // Status Bar
        Composite statusBar = new Composite(composite, SWT.NONE);
        statusBar.setLayout(new GridLayout(4, false));
        statusBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new org.eclipse.swt.widgets.Label(statusBar, SWT.NONE).setText("Ollama Status:");
        ollamaStatusLabel = new org.eclipse.swt.widgets.Label(statusBar, SWT.NONE);
        ollamaStatusLabel.setText("Unknown");
        ollamaStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new org.eclipse.swt.widgets.Label(statusBar, SWT.NONE).setText("Model:");
        modelStatusLabel = new org.eclipse.swt.widgets.Label(statusBar, SWT.NONE);
        modelStatusLabel.setText("Not Configured");
        modelStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        updateStatusInfo();

		int index = addPage(composite);
		setPageText(index, "AI Chat");
	}

    private void updatePropertiesInfo() {
        if (orchestrator == null) return;

        orchIdText.setText(orchestrator.getId() != null ? orchestrator.getId() : "");
        orchNameText.setText(orchestrator.getName() != null ? orchestrator.getName() : "");

        if (orchestrator.getLlm() != null) {
            llmModelText.setText(orchestrator.getLlm().getModel() != null ? orchestrator.getLlm().getModel() : "");
            llmTempText.setText(String.valueOf(orchestrator.getLlm().getTemperature()));
        }

        if (orchestrator.getOllama() != null) {
            ollamaUrlText.setText(orchestrator.getOllama().getUrl() != null ? orchestrator.getOllama().getUrl() : "");
            ollamaModelText.setText(orchestrator.getOllama().getModel() != null ? orchestrator.getOllama().getModel() : "");
            ollamaPathText.setText(orchestrator.getOllama().getPath() != null ? orchestrator.getOllama().getPath() : "");

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
            gitRepoText.setText(orchestrator.getGit().getRepositoryUrl() != null ? orchestrator.getGit().getRepositoryUrl() : "");
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
            neuronAiUrlText.setText(orchestrator.getNeuronAI().getUrl() != null ? orchestrator.getNeuronAI().getUrl() : "");
        }

        if (orchestrator.getCompiler() != null) {
            compilerSourceText.setText(orchestrator.getCompiler().getSourceVersion() != null ? orchestrator.getCompiler().getSourceVersion() : "");
        }
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
                ollamaService = new OllamaService(url, model)
                        .setTemperature(temperature)
                        .setNumPredict(1024)
                        .setTopP(0.9f)
                        .setTopK(40)
                        .setRepeatPenalty(1.1f);
            }

            modelStatusLabel.setText(model != null ? model : "Not Configured");

            new Thread(() -> {
                boolean isOnline = ollamaService.ping();
                Display.getDefault().asyncExec(() -> {
                    if (ollamaStatusLabel.isDisposed()) return;
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
	 * Creates page 1 of the multi-page editor,
	 * which contains a text editor.
	 */
	void createPage1() {
		try {
			editor = new TextEditor();
			int index = addPage(editor, getEditorInput());
			setPageText(index, editor.getTitle());
		} catch (PartInitException e) {
			ErrorDialog.openError(
				getSite().getShell(),
				"Error creating nested text editor",
				null,
				e.getStatus());
		}
	}
	/**
	 * Creates page 2 of the multi-page editor,
	 * which allows you to change the font used in page 3.
	 */
	void createPage2() {
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
                e.gc.drawString("Progress: " + (int)(progress * 100) + "%", 10, 10);
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
        orchGroup.setLayout(new GridLayout(2, false));
        orchGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(orchGroup, SWT.NONE).setText("ID:");
        orchIdText = new Text(orchGroup, SWT.BORDER | SWT.READ_ONLY);
        orchIdText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(orchGroup, SWT.NONE).setText("Name:");
        orchNameText = new Text(orchGroup, SWT.BORDER | SWT.READ_ONLY);
        orchNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // --- LLM Group ---
        Group llmGroup = new Group(composite, SWT.NONE);
        llmGroup.setText("LLM Settings");
        llmGroup.setLayout(new GridLayout(2, false));
        llmGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(llmGroup, SWT.NONE).setText("Model:");
        llmModelText = new Text(llmGroup, SWT.BORDER | SWT.READ_ONLY);
        llmModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(llmGroup, SWT.NONE).setText("Temperature:");
        llmTempText = new Text(llmGroup, SWT.BORDER | SWT.READ_ONLY);
        llmTempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // --- Ollama Group ---
        Group ollamaGroup = new Group(composite, SWT.NONE);
        ollamaGroup.setText("Ollama Settings");
        ollamaGroup.setLayout(new GridLayout(2, false));
        ollamaGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(ollamaGroup, SWT.NONE).setText("URL:");
        ollamaUrlText = new Text(ollamaGroup, SWT.BORDER | SWT.READ_ONLY);
        ollamaUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(ollamaGroup, SWT.NONE).setText("Model:");
        ollamaModelText = new Text(ollamaGroup, SWT.BORDER | SWT.READ_ONLY);
        ollamaModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(ollamaGroup, SWT.NONE).setText("Model Path:");
        ollamaPathText = new Text(ollamaGroup, SWT.BORDER | SWT.READ_ONLY);
        ollamaPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(ollamaGroup, SWT.NONE).setText("Version:");
        ollamaVersionText = new Text(ollamaGroup, SWT.BORDER | SWT.READ_ONLY);
        ollamaVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

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
        gitMavenGroup.setLayout(new GridLayout(2, false));
        gitMavenGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(gitMavenGroup, SWT.NONE).setText("Git Repo:");
        gitRepoText = new Text(gitMavenGroup, SWT.BORDER | SWT.READ_ONLY);
        gitRepoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(gitMavenGroup, SWT.NONE).setText("Git Branch:");
        gitBranchText = new Text(gitMavenGroup, SWT.BORDER | SWT.READ_ONLY);
        gitBranchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(gitMavenGroup, SWT.NONE).setText("Maven Goals:");
        mavenGoalsText = new Text(gitMavenGroup, SWT.BORDER | SWT.READ_ONLY);
        mavenGoalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(gitMavenGroup, SWT.NONE).setText("Maven Profiles:");
        mavenProfilesText = new Text(gitMavenGroup, SWT.BORDER | SWT.READ_ONLY);
        mavenProfilesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // --- Others Group ---
        Group othersGroup = new Group(composite, SWT.NONE);
        othersGroup.setText("Additional AI & Tools");
        othersGroup.setLayout(new GridLayout(2, false));
        othersGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(othersGroup, SWT.NONE).setText("AI Chat URL:");
        aiChatUrlText = new Text(othersGroup, SWT.BORDER | SWT.READ_ONLY);
        aiChatUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(othersGroup, SWT.NONE).setText("Neuron AI URL:");
        neuronAiUrlText = new Text(othersGroup, SWT.BORDER | SWT.READ_ONLY);
        neuronAiUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(othersGroup, SWT.NONE).setText("Compiler Source:");
        compilerSourceText = new Text(othersGroup, SWT.BORDER | SWT.READ_ONLY);
        compilerSourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        sc.setContent(composite);
        sc.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		int index = addPage(sc);
		setPageText(index, "Properties");
	}
	/**
	 * Creates page 3 of the multi-page editor,
	 * which shows the sorted text.
	 */
	void createPage3() {
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
		createPage1();
		createPage2();
		createPage3();
	}
	/**
	 * The <code>MultiPageEditorPart</code> implementation of this 
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}
	/**
	 * Saves the multi-page editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {
		getEditor(1).doSave(monitor);
	}
	/**
	 * Saves the multi-page editor's document as another file.
	 * Also updates the text for page 0's tab, and updates this multi-page editor's input
	 * to correspond to the nested editor's.
	 */
	public void doSaveAs() {
		IEditorPart editor = getEditor(1);
		editor.doSaveAs();
		setPageText(1, editor.getTitle());
		setInput(editor.getEditorInput());
	}
	/* (non-Javadoc)
	 * Method declared on IEditorPart
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(1);
		IDE.gotoMarker(getEditor(1), marker);
	}
	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method
	 * checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	public void init(IEditorSite site, IEditorInput editorInput)
		throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput) && !(editorInput instanceof OrchestratorEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput or OrchestratorEditorInput");
		super.init(site, editorInput);

        if (editorInput instanceof OrchestratorEditorInput) {
            this.orchestrator = ((OrchestratorEditorInput) editorInput).getOrchestrator();
            setPartName(editorInput.getName());
        }

        // If we have an orchestrator, set it as selection
        if (this.orchestrator != null) {
            getSite().setSelectionProvider(new ISelectionProvider() {
                @Override public void setSelection(org.eclipse.jface.viewers.ISelection selection) {}
                @Override public void removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener listener) {}
                @Override public org.eclipse.jface.viewers.ISelection getSelection() { return new StructuredSelection(orchestrator); }
                @Override public void addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener listener) {}
            });
        }
	}

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IPropertySheetPage.class) {
            return adapter.cast(new PropertySheetPage());
        }
        return super.getAdapter(adapter);
    }
	/* (non-Javadoc)
	 * Method declared on IEditorPart.
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
	public void resourceChanged(final IResourceChangeEvent event){
		if(event.getType() == IResourceChangeEvent.PRE_CLOSE){
			Display.getDefault().asyncExec(() -> {
				IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
				for (int i = 0; i<pages.length; i++){
					if(((FileEditorInput)editor.getEditorInput()).getFile().getProject().equals(event.getResource())){
						IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
						pages[i].closeEditor(editorPart,true);
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

		String editorText =
			editor.getDocumentProvider().getDocument(editor.getEditorInput()).get();

		StringTokenizer tokenizer =
			new StringTokenizer(editorText, " \t\n\r\f!@#\u0024%^&*()-_=+`~[]{};:'\",.<>/?|\\");
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
}
