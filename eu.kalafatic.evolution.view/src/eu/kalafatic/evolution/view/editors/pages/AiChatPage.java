package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import eu.kalafatic.evolution.controller.manager.NeuronService;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevSupervisor;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import eu.kalafatic.evolution.view.editors.pages.aichat.*;
import eu.kalafatic.evolution.view.dialogs.ProjectSetupWizardDialog;
import org.eclipse.jface.window.Window;
import eu.kalafatic.evolution.controller.manager.EnvironmentSuggestionService;
import java.util.List;

public class AiChatPage extends SharedScrolledComposite {
	private MultiPageEditor editor;
	private Orchestrator orchestrator;
	private boolean isUpdating = false;
	private Label modeIndicatorLabel;
	private TaskContext currentContext;
	private OllamaService ollamaService;
	private Map<String, String> threads = new HashMap<>();
	private Map<String, StyleRange[]> threadStyles = new HashMap<>();
	private String currentThread = "Default";
	private Composite content;
	private FormToolkit toolkit;
	private long lastStatusUpdate = 0;

	private ChatMgmtGroup chatMgmtGroup;
	private AiSettingsGroup aiSettingsGroup;
	private InstructionsGroup instructionsGroup;
	private HistoryGroup historyGroup;
	private SystemStatusGroup systemStatusGroup;
	private Thread orchestrationThread;
	private SatisfactionGroup satisfactionGroup;
	private ApprovalGroup approvalGroup;
	private InputGroup inputGroup;

	private Color colorUser, colorEvolution, colorPlanner, colorArchitect, colorJavaDev, colorTester, colorReviewer, colorError, colorWhite, colorLocal, colorHybrid, colorRemote;
	private Font chatFont, bannerFont;

	public AiChatPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		this.editor = editor;
		this.setExpandHorizontal(true);
		this.setExpandVertical(true);
		this.orchestrator = orchestrator;
		this.toolkit = new FormToolkit(parent.getDisplay());
		initResources();
		createControl();
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (chatFont != null && !chatFont.isDisposed()) chatFont.dispose();
				if (bannerFont != null && !bannerFont.isDisposed()) bannerFont.dispose();
				if (toolkit != null) toolkit.dispose();
			}
		});
	}

	private void initResources() {
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
		colorLocal = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		colorHybrid = display.getSystemColor(SWT.COLOR_DARK_BLUE);
		colorRemote = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);

		Font defaultFont = JFaceResources.getDefaultFont();
		FontData[] fontData = defaultFont.getFontData();
		for (FontData fd : fontData) fd.setHeight(11);
		chatFont = new Font(display, fontData);

		Font bannerDefault = JFaceResources.getBannerFont();
		FontData[] bannerData = bannerDefault.getFontData();
		for (FontData fd : bannerData) fd.setStyle(SWT.BOLD);
		bannerFont = new Font(display, bannerData);
	}

	private void createControl() {
		content = toolkit.createComposite(this);
		content.setLayout(new GridLayout(1, false));
		this.setContent(content);

		modeIndicatorLabel = new Label(content, SWT.CENTER);
		modeIndicatorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modeIndicatorLabel.setFont(bannerFont);
		modeIndicatorLabel.setText("INITIALIZING...");

		chatMgmtGroup = new ChatMgmtGroup(toolkit, content, editor, orchestrator, this);
		aiSettingsGroup = new AiSettingsGroup(toolkit, content, this, orchestrator);
		instructionsGroup = new InstructionsGroup(toolkit, content, this, orchestrator);
		historyGroup = new HistoryGroup(toolkit, content, editor, orchestrator, chatFont, this);
		historyGroup.setEditCallback((index, oldText) -> {
			Display.getDefault().asyncExec(() -> {
				InputDialog dlg = new InputDialog(getShell(), "Edit Message", "Modify the message content:", oldText, null);
				if (dlg.open() == Window.OK) {
					historyGroup.updateMessage(index, dlg.getValue());
					editor.setDirty(true);
				}
			});
		});
		systemStatusGroup = new SystemStatusGroup(toolkit, content, editor, orchestrator);
		satisfactionGroup = new SatisfactionGroup(content, editor, orchestrator, this);
		approvalGroup = new ApprovalGroup(content, editor, orchestrator, this);
		inputGroup = new InputGroup(content, editor, orchestrator, this);

		threads.put(currentThread, "");
		threadStyles.put(currentThread, new StyleRange[0]);

		Runnable timer = new Runnable() {
			public void run() {
				if (!content.isDisposed()) {
					String id = orchestrator != null ? orchestrator.getId() : null;
					if (id != null) {
						double progress = OrchestrationStatusManager.getInstance().getProgress(id);
						String status = OrchestrationStatusManager.getInstance().getStatus(id);
						systemStatusGroup.updateProgress(status, (int) (progress * 100));
					}
					Display.getDefault().timerExec(500, this);
				}
			}
		};
		Display.getDefault().timerExec(500, timer);

		updateStatusInfo();
		updateModeDisplay();
		updateScrolledContent();

		if (lastStatusUpdate == 0) Display.getDefault().asyncExec(() -> checkEnvironment());
	}

	private void checkEnvironment() {
		if (orchestrator == null) return;
		File projectRoot = getProjectRoot();
		List<EnvironmentSuggestionService.Suggestion> suggestions = EnvironmentSuggestionService.getSuggestions(orchestrator, projectRoot);

		boolean hasCriticalMissing = suggestions.stream().anyMatch(s -> s.isMissing);

		if (hasCriticalMissing) {
			ProjectSetupWizardDialog dialog = new ProjectSetupWizardDialog(getShell(), suggestions);
			if (dialog.open() == Window.OK) {
				List<EnvironmentSuggestionService.Suggestion> selected = dialog.getSelectedSuggestions();
				EnvironmentSuggestionService.applySetup(orchestrator, selected);

				// Handle Git Init if selected
				if (selected.stream().anyMatch(s -> "Git".equals(s.field))) {
					handleGitInit();
				}

				editor.setDirty(true);
				updateUI();
			}
		}
	}

	private void handleGitInit() {
		new Thread(() -> {
			try {
				eu.kalafatic.evolution.controller.orchestration.ShellTool shell = new eu.kalafatic.evolution.controller.orchestration.ShellTool();
				shell.execute("git init", getProjectRoot(), null);
				processLogEntry("Evo: Git repository initialized successfully.");
			} catch (Exception e) {
				processLogEntry("Error initializing git: " + e.getMessage());
			}
		}).start();
	}

	public void updateScrolledContent() {
		if (content == null || content.isDisposed()) return;
		content.layout(true, true);
		this.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public void updateModeDisplay() {
		if (orchestrator == null || modeIndicatorLabel == null || modeIndicatorLabel.isDisposed()) return;
		AiMode mode = orchestrator.getAiMode();
		modeIndicatorLabel.setText(mode.getName().toUpperCase() + " MODE ACTIVE");
		modeIndicatorLabel.setForeground(colorWhite);
		switch (mode) {
		case LOCAL: modeIndicatorLabel.setBackground(colorLocal); break;
		case HYBRID: modeIndicatorLabel.setBackground(colorHybrid); break;
		case REMOTE: modeIndicatorLabel.setBackground(colorRemote); break;
		}
		boolean remoteVisible = mode == AiMode.HYBRID || mode == AiMode.REMOTE;
		if (aiSettingsGroup != null) {
			SWTFactory.setControlEnabled(remoteVisible, true, aiSettingsGroup.getRemoteComposite().getChildren());
			updateScrolledContent();
		}
	}

	public void syncModelWithUI() {
		if (orchestrator == null || isUpdating) return;
		isUpdating = true;
		orchestrator.setAiMode(AiMode.get(aiSettingsGroup.getAiModeIndex()));
		String remoteModel = aiSettingsGroup.getRemoteModel();
		orchestrator.setRemoteModel(remoteModel);
		ProviderConfig config = AiProviders.PROVIDERS.get(remoteModel);
		if (config != null) orchestrator.setOpenAiModel(config.getDefaultModel());
		orchestrator.setOpenAiToken(aiSettingsGroup.getRemoteToken());
		orchestrator.setIterativeMode(instructionsGroup.isIterative());
		orchestrator.setSelfIterativeMode(instructionsGroup.isSelfIterative());
		if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
		orchestrator.getAiChat().setUrl(aiSettingsGroup.getRemoteUrl());
		editor.setDirty(true);
		updateModeDisplay();
		isUpdating = false;
	}

	public void handleSend() {
		String request = instructionsGroup.getRequest();
		if (request.isEmpty()) return;
		if (orchestrator != null && (orchestrator.isIterativeMode() || orchestrator.isSelfIterativeMode())) {
			startSelfDevAction(request); return;
		}
		if (orchestrator != null) {
			if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
			if (orchestrator.getLlm() == null) orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
			NeuronService.getInstance().train(orchestrator, request);
			editor.setDirty(true);
			if (orchestrator.getId() == null || orchestrator.getId().isEmpty()) orchestrator.setId("chat-" + System.currentTimeMillis());
		}
		if (!historyGroup.getText().isEmpty()) historyGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
		historyGroup.appendText("You: " + request, colorUser, SWT.BOLD);
		historyGroup.appendText("\n\nEvo: Initializing orchestration...", colorEvolution, SWT.ITALIC);
		threads.put(currentThread, historyGroup.getText());
		threadStyles.put(currentThread, historyGroup.getStyleRanges());
		instructionsGroup.setRequest("");
		instructionsGroup.setOrchestrationRunning(true);
		historyGroup.setThinking(true);
		orchestrationThread = new Thread(() -> {
			try {
				EvolutionOrchestrator evolutionOrchestrator = new EvolutionOrchestrator();
				File projectRoot = getProjectRoot();
				TaskContext context = new TaskContext(orchestrator, projectRoot);
				context.getInstructionFiles().addAll(instructionsGroup.getInstructionFiles());
				this.currentContext = context;
				Display.getDefault().asyncExec(() -> editor.setCurrentContext(context));
				context.addApprovalListener(message -> Display.getDefault().asyncExec(() -> {
					approvalGroup.show(message); updateScrolledContent();
				}));
				context.addInputListener(message -> Display.getDefault().asyncExec(() -> {
					inputGroup.show(message); updateScrolledContent();
				}));
				context.addTokenRequestListener((provider, future) -> Display.getDefault().asyncExec(() -> {
					String token = requestToken(provider);
					if (token != null) { aiSettingsGroup.setRemoteToken(token); syncModelWithUI(); future.complete(token); }
					else future.completeExceptionally(new Exception("Token request cancelled by user."));
				}));
				context.addLogListener(log -> Display.getDefault().asyncExec(() -> {
					if (!historyGroup.isDisposed()) {
						processLogEntry(log);
						threads.put(currentThread, historyGroup.getText());
						threadStyles.put(currentThread, historyGroup.getStyleRanges());
					}
				}));
				String result = evolutionOrchestrator.execute(request, context);
				Display.getDefault().asyncExec(() -> {
					if (!historyGroup.isDisposed()) {
						historyGroup.setThinking(false);
						historyGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
						historyGroup.appendText("Evo: " + result, colorEvolution, SWT.BOLD);
						threads.put(currentThread, historyGroup.getText());
						threadStyles.put(currentThread, historyGroup.getStyleRanges());
						satisfactionGroup.setVisible(true); updateScrolledContent();
					}
				});
			} catch (Exception e) {
				Display.getDefault().asyncExec(() -> {
					if (!historyGroup.isDisposed()) {
						historyGroup.setThinking(false);
						historyGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
						historyGroup.appendText("Error: " + (e instanceof InterruptedException ? "Orchestration stopped by user." : e.getMessage()), colorError, SWT.BOLD);
						threads.put(currentThread, historyGroup.getText());
						threadStyles.put(currentThread, historyGroup.getStyleRanges());
					}
				});
			} finally {
				Display.getDefault().asyncExec(() -> {
					instructionsGroup.setOrchestrationRunning(false);
					orchestrationThread = null;
				});
			}
		});
		orchestrationThread.start();
	}

	public void handlePause() {
		if (currentContext != null) {
			boolean isPaused = !currentContext.isPaused();
			currentContext.setPaused(isPaused);
			instructionsGroup.setPaused(isPaused);
			historyGroup.setThinking(!isPaused);
		}
	}

	public void handleStop() {
		if (orchestrationThread != null && orchestrationThread.isAlive()) {
			if (currentContext != null) currentContext.setPaused(false);
			orchestrationThread.interrupt();
		}
	}

	private String requestToken(String provider) {
		InputDialog dlg = new InputDialog(getShell(), "API Token Required", "Please enter the API token for " + provider + ":", "", null);
		if (dlg.open() == Window.OK) return dlg.getValue();
		return null;
	}

	public void createNewThread() {
		InputDialog dlg = new InputDialog(getShell(), "New Chat Thread", "Enter thread name:", "Thread " + (threads.size() + 1), null);
		if (dlg.open() == Window.OK) {
			String name = dlg.getValue();
			if (name != null && !name.trim().isEmpty() && !threads.containsKey(name)) {
				threads.put(currentThread, historyGroup.getText());
				threadStyles.put(currentThread, historyGroup.getStyleRanges());
				currentThread = name;
				threads.put(currentThread, "");
				threadStyles.put(currentThread, new StyleRange[0]);
				chatMgmtGroup.updateThreadCombo(threads.keySet().toArray(new String[0]), currentThread);
				historyGroup.clear();
			}
		}
	}

	public void switchThread(String name) {
		threads.put(currentThread, historyGroup.getText());
		threadStyles.put(currentThread, historyGroup.getStyleRanges());
		currentThread = name;
		historyGroup.setText(threads.getOrDefault(currentThread, ""));
		historyGroup.setStyleRanges(threadStyles.getOrDefault(currentThread, new StyleRange[0]));
		historyGroup.setSelection(historyGroup.getText().length());
	}

	public void cleanChat() {
		historyGroup.clear();
		threads.put(currentThread, "");
		threadStyles.put(currentThread, new StyleRange[0]);
	}

	private void startSelfDevAction(String request) {
		if (request == null || request.isEmpty()) request = "Analyze the project and suggest improvements.";
		final String finalRequest = request;
		if (orchestrator != null) {
			if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
			if (orchestrator.getLlm() == null) orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
			if (orchestrator.getId() == null || orchestrator.getId().isEmpty()) orchestrator.setId("selfdev-" + System.currentTimeMillis());
		}
		if (!historyGroup.getText().isEmpty()) historyGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
		historyGroup.appendText("User [SELF-DEV]: " + finalRequest, colorUser, SWT.BOLD);
		historyGroup.appendText("\n\nEvo: Initializing Self-Development Supervisor loop...", colorEvolution, SWT.ITALIC | SWT.BOLD);
		instructionsGroup.setRequest("");
		instructionsGroup.setOrchestrationRunning(true);
		historyGroup.setThinking(true);
		orchestrationThread = new Thread(() -> {
			try {
				File projectRoot = getProjectRoot();
				TaskContext context = new TaskContext(orchestrator, projectRoot);
				context.getInstructionFiles().addAll(instructionsGroup.getInstructionFiles());
				this.currentContext = context;
				context.addLogListener(log -> Display.getDefault().asyncExec(() -> { if (!historyGroup.isDisposed()) processLogEntry(log); }));
				context.addApprovalListener(message -> Display.getDefault().asyncExec(() -> {
					approvalGroup.show(message); updateScrolledContent();
				}));
				context.addInputListener(message -> Display.getDefault().asyncExec(() -> {
					inputGroup.show(message); updateScrolledContent();
				}));
				context.addTokenRequestListener((provider, future) -> Display.getDefault().asyncExec(() -> {
					String token = requestToken(provider);
					if (token != null) { aiSettingsGroup.setRemoteToken(token); syncModelWithUI(); future.complete(token); }
					else future.completeExceptionally(new Exception("Token request cancelled by user."));
				}));
				SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
				session.setId("session-" + System.currentTimeMillis());
				session.setMaxIterations(5); session.setInitialRequest(finalRequest);
				orchestrator.setSelfDevSession(session);
				SelfDevSupervisor supervisor = new SelfDevSupervisor(session, context);
				supervisor.startSession();
				Display.getDefault().asyncExec(() -> {
					if (!historyGroup.isDisposed()) {
						historyGroup.setThinking(false);
						historyGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
						historyGroup.appendText("Evo: Self-Development session finished. Status: " + session.getStatus(), colorEvolution, SWT.BOLD);
						editor.setDirty(true);
						satisfactionGroup.setVisible(true); updateScrolledContent();
					}
				});
			} catch (Exception e) {
				Display.getDefault().asyncExec(() -> { if (!historyGroup.isDisposed()) { historyGroup.setThinking(false); historyGroup.appendText("\n\n", colorWhite, SWT.NORMAL); historyGroup.appendText("Supervisor Error: " + (e instanceof InterruptedException ? "Orchestration stopped by user." : e.getMessage()), colorError, SWT.BOLD); } });
			} finally {
				Display.getDefault().asyncExec(() -> {
					instructionsGroup.setOrchestrationRunning(false);
					orchestrationThread = null;
				});
			}
		});
		orchestrationThread.start();
	}

	public File getProjectRoot() {
		File projectRoot = null;
		if (editor.getEditorInput() instanceof IFileEditorInput) {
			projectRoot = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject().getLocation().toFile();
		} else if (orchestrator != null && orchestrator.eResource() != null) {
			org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
			if (uri.isPlatformResource()) {
				String path = uri.toPlatformString(true);
				projectRoot = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path)).getProject().getLocation().toFile();
			}
		}
		return projectRoot != null ? projectRoot : new File(System.getProperty("java.io.tmpdir"));
	}

	public void saveChatToFile() {
		org.eclipse.swt.widgets.FileDialog dialog = new org.eclipse.swt.widgets.FileDialog(getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] { "*.txt", "*.*" });
		dialog.setFileName(currentThread + ".txt");
		String path = dialog.open();
		if (path != null) {
			try (FileWriter writer = new FileWriter(path)) { writer.write(historyGroup.getText()); }
			catch (Exception e) { historyGroup.appendText("\nError saving file: " + e.getMessage(), colorError, SWT.BOLD); }
		}
	}

	public void copyConversationToClipboard() {
		String fullText = historyGroup.getText();
		if (fullText != null && !fullText.isEmpty()) {
			org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(getDisplay());
			cb.setContents(new Object[] { fullText }, new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
			cb.dispose();
		}
	}

	public void updateStatusInfo() {
		if (orchestrator != null && orchestrator.getOllama() != null) {
			String url = orchestrator.getOllama().getUrl(); String model = orchestrator.getOllama().getModel();
			if (ollamaService == null) {
				float temp = orchestrator.getLlm() != null ? orchestrator.getLlm().getTemperature() : 0.7f;
				ollamaService = new OllamaService(url, model).setTemperature(temp);
			}
			systemStatusGroup.updateModelStatus(model != null ? model : "Not Configured");
			new Thread(() -> {
				boolean isOnline = ollamaService.ping();
				Display.getDefault().asyncExec(() -> systemStatusGroup.updateOllamaStatus((isOnline ? "Online (" : "Offline (") + url + ")", Display.getDefault().getSystemColor(isOnline ? SWT.COLOR_DARK_GREEN : SWT.COLOR_RED)));
			}).start();
		} else {
			systemStatusGroup.updateOllamaStatus("Not Configured", Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
			systemStatusGroup.updateModelStatus("Not Configured");
		}
	}

	public void setOrchestrator(Orchestrator orchestrator) {
		this.orchestrator = orchestrator; this.ollamaService = null;
		updateUI();
	}

	public void updateUI() {
		if (orchestrator != null && aiSettingsGroup != null && !isUpdating) {
			isUpdating = true;
			aiSettingsGroup.updateUI();
			instructionsGroup.updateUI();
			updateStatusInfo();
			updateModeDisplay();
			isUpdating = false;
		}
	}

	public void submitFeedback(int satisfaction, String comments) {
		if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
			if (orchestrator.getSelfDevSession().getIterations().isEmpty()) orchestrator.getSelfDevSession().getIterations().add(OrchestrationFactory.eINSTANCE.createIteration());
			eu.kalafatic.evolution.model.orchestration.Iteration last = orchestrator.getSelfDevSession().getIterations().get(orchestrator.getSelfDevSession().getIterations().size() - 1);
			if (last.getEvaluationResult() == null) last.setEvaluationResult(OrchestrationFactory.eINSTANCE.createEvaluationResult());
			last.getEvaluationResult().setUserSatisfaction(satisfaction); last.setComments(comments);
			NeuronService.getInstance().train(orchestrator, comments, satisfaction);
			editor.setDirty(true); satisfactionGroup.setVisible(false); updateScrolledContent();
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("Thank You"); mb.setMessage("Your feedback has been recorded and will be used to improve the AI."); mb.open();
		}
	}

	public void provideApproval(boolean approved) {
		if (currentContext != null) { currentContext.provideApproval(approved); approvalGroup.hide(); updateScrolledContent(); }
	}

	public void handleReview() {
		editor.showApprovalPage();
	}

	public void provideInput(String input) {
		if (currentContext != null) { currentContext.provideInput(input); inputGroup.hide(); updateScrolledContent(); }
	}

	private void processLogEntry(String log) {
		if (log == null || log.isEmpty()) return;
		
		Display.getDefault().asyncExec(() -> {
		    Color color = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);


		    int style = SWT.NORMAL;
			if (log.startsWith("Evo:") || log.startsWith("Orchestrator:")) { color = colorEvolution; style = SWT.ITALIC; }
			else if (log.contains("Agent [") && log.contains("Planner")) { color = colorPlanner; style = SWT.BOLD; }
			else if (log.contains("Agent [") && log.contains("Architect")) { color = colorArchitect; style = SWT.BOLD; }
			else if (log.contains("Agent [") && log.contains("JavaDev")) { color = colorJavaDev; style = SWT.BOLD; }
			else if (log.contains("Agent [") && log.contains("Tester")) { color = colorTester; style = SWT.BOLD; }
			else if (log.contains("Agent [") && log.contains("Reviewer")) { color = colorReviewer; style = SWT.BOLD; }
			else if (log.startsWith("Orchestrator Error:") || log.contains("Exception:")) { color = colorError; style = SWT.BOLD; }
			historyGroup.appendText("\n" + log, color, style);
		});
		
		
	}

	public String getCurrentThreadName() { return currentThread; }

	public MultiPageEditor getEditor() { return editor; }

	public FormToolkit getToolkit() { return toolkit; }

	public void setupContextAssist(StyledText text) {
		IContentProposalProvider proposalProvider = (contents, position) -> {
			String prefix = contents.substring(0, position);
			int lastSpace = prefix.lastIndexOf(' '); if (lastSpace != -1) prefix = prefix.substring(lastSpace + 1);
			String finalPrefix = prefix;
			String[] proposals = NeuronService.getInstance().getProposals(orchestrator, finalPrefix);
			IContentProposal[] result = new IContentProposal[proposals.length];
			for (int i = 0; i < proposals.length; i++) {
				final String proposal = proposals[i];
				result[i] = new IContentProposal() {
					@Override public String getContent() { return proposal; }
					@Override public int getCursorPosition() { return proposal.length(); }
					@Override public String getLabel() { return proposal; }
					@Override public String getDescription() { return null; }
				};
			}
			return result;
		};
		IControlContentAdapter contentAdapter = new IControlContentAdapter() {
			@Override public void setControlContents(org.eclipse.swt.widgets.Control control, String contents, int cursorPosition) { ((StyledText) control).setText(contents); ((StyledText) control).setSelection(cursorPosition); }
			@Override public void insertControlContents(org.eclipse.swt.widgets.Control control, String contents, int cursorPosition) {
				StyledText st = (StyledText) control; String ct = st.getText(); int ss = st.getSelection().x;
				int ws = ss; while (ws > 0 && !Character.isWhitespace(ct.charAt(ws - 1))) ws--;
				st.setText(ct.substring(0, ws) + contents + ct.substring(ss)); st.setSelection(ws + cursorPosition);
			}
			@Override public String getControlContents(org.eclipse.swt.widgets.Control control) { return ((StyledText) control).getText(); }
			@Override public int getCursorPosition(org.eclipse.swt.widgets.Control control) { return ((StyledText) control).getCaretOffset(); }
			@Override public org.eclipse.swt.graphics.Rectangle getInsertionBounds(org.eclipse.swt.widgets.Control control) { return ((StyledText) control).getBounds(); }
			@Override public void setCursorPosition(org.eclipse.swt.widgets.Control control, int index) { ((StyledText) control).setSelection(index); }
		};
		KeyStroke ks = null; try { ks = KeyStroke.getInstance("Ctrl+Space"); } catch (Exception e) {}
		ContentProposalAdapter adapter = new ContentProposalAdapter(text, contentAdapter, proposalProvider, ks, null);
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		adapter.setAutoActivationDelay(200);
		adapter.setAutoActivationCharacters("abcdefghijklmnopqrstuvwxyz".toCharArray());
	}

	public void testAiConnectionRemote(int modeIndex, String remoteModel, String token, String apiUrl) {
		new Thread(() -> {
			try {
				Orchestrator tempOrch = OrchestrationFactory.eINSTANCE.createOrchestrator();
				tempOrch.setAiMode(AiMode.get(modeIndex)); tempOrch.setRemoteModel(remoteModel); tempOrch.setOpenAiToken(token);
				tempOrch.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat()); tempOrch.getAiChat().setUrl(apiUrl);
				tempOrch.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
				tempOrch.setHybridModel(orchestrator.getHybridModel()); tempOrch.setLocalModel(orchestrator.getLocalModel());
				if (orchestrator.getOllama() != null) {
					tempOrch.setOllama(OrchestrationFactory.eINSTANCE.createOllama());
					tempOrch.getOllama().setUrl(orchestrator.getOllama().getUrl()); tempOrch.getOllama().setModel(orchestrator.getOllama().getModel());
				}
				LlmRouter router = new LlmRouter();
				float temp = orchestrator.getLlm() != null ? orchestrator.getLlm().getTemperature() : 0.7f;
				String proxyUrl = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null;
				TaskContext context = new TaskContext(tempOrch, null);
				context.addTokenRequestListener((provider, future) -> Display.getDefault().asyncExec(() -> {
					String newToken = requestToken(provider);
					if (newToken != null) { aiSettingsGroup.setRemoteToken(newToken); syncModelWithUI(); future.complete(newToken); }
					else future.completeExceptionally(new Exception("Token request cancelled by user."));
				}));
				String response = router.testConnection(tempOrch, temp, proxyUrl, context);
				Display.getDefault().asyncExec(() -> {
					if (isDisposed()) return;
					orchestrator.setAiMode(AiMode.get(modeIndex)); orchestrator.setRemoteModel(remoteModel); orchestrator.setOpenAiToken(token);
					if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
					orchestrator.getAiChat().setUrl(apiUrl);
					if (orchestrator.getLlm() == null) orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
					editor.setDirty(true); updateModeDisplay(); updateStatusInfo();
					MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("AI Connection Success"); mb.setMessage("Connected to AI provider successfully and settings saved.\nResponse: " + response); mb.open();
				});
			} catch (Exception ex) {
				Display.getDefault().asyncExec(() -> { if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("AI Connection Failed"); mb.setMessage("Error connecting to AI provider (settings NOT saved): " + ex.getMessage()); mb.open(); });
			}
		}).start();
	}
}
