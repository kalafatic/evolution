package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.io.FileWriter;

import org.eclipse.core.resources.IFile;
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
import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevSupervisor;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.ChatThread;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import eu.kalafatic.evolution.view.editors.pages.aichat.*;
import eu.kalafatic.evolution.view.dialogs.ProjectSetupWizardDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.emf.ecore.util.EcoreUtil;
import eu.kalafatic.evolution.controller.manager.EnvironmentSuggestionService;
import java.util.List;
import java.util.stream.Collectors;

public class AiChatPage extends AEvoPage {
	private boolean isUpdating = false;
	private Label modeIndicatorLabel;
	private TaskContext currentContext;
	private OllamaService ollamaService;
	private ChatThread currentThread;
	private Composite content;
	private long lastStatusUpdate = 0;

	private ChatMgmtGroup chatMgmtGroup;
	private AiSettingsGroup aiSettingsGroup;
	private InstructionsGroup instructionsGroup;
	private ChatGroup chatGroup;
	private SystemStatusGroup systemStatusGroup;
	private Thread orchestrationThread;
	private SatisfactionGroup satisfactionGroup;
	private ApprovalGroup approvalGroup;
	private InputGroup inputGroup;
	private eu.kalafatic.evolution.model.orchestration.Task currentStackTask;

	private Color colorUser, colorEvolution, colorPlanner, colorArchitect, colorJavaDev, colorTester, colorReviewer, colorError, colorWhite, colorLocal, colorHybrid, colorRemote, colorWaiting, colorLightOrange;
	private Font chatFont, bannerFont;
	private Color lightGreen;

	public AiChatPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, editor, orchestrator);
		initResources();
		createControl();
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (chatFont != null && !chatFont.isDisposed()) chatFont.dispose();
				if (bannerFont != null && !bannerFont.isDisposed()) bannerFont.dispose();
				if (colorWaiting != null && !colorWaiting.isDisposed()) colorWaiting.dispose();
				if (colorLightOrange != null && !colorLightOrange.isDisposed()) colorLightOrange.dispose();
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
		colorLocal = display.getSystemColor(SWT.COLOR_GREEN);
		colorHybrid = display.getSystemColor(SWT.COLOR_CYAN);
		colorRemote = display.getSystemColor(SWT.COLOR_MAGENTA);
		colorWaiting = new Color(display, 255, 140, 0); // Dark Orange
		colorLightOrange = new Color(display, 255, 224, 189);
		lightGreen = new Color(Display.getDefault(), 220, 255, 220);

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
		chatGroup = new ChatGroup(toolkit, content, editor, orchestrator, chatFont, this);
		chatGroup.setEditCallback((index, oldText) -> {
			Display.getDefault().asyncExec(() -> {
				InputDialog dlg = new InputDialog(getShell(), "Edit Message", "Modify the message content:", oldText, null);
				if (dlg.open() == Window.OK) {
					chatGroup.updateMessage(index, dlg.getValue());
					editor.setDirty(true);
				}
			});
		});
		instructionsGroup = new InstructionsGroup(toolkit, chatGroup.getControl(), this, orchestrator, true);
		systemStatusGroup = new SystemStatusGroup(toolkit, content, editor, orchestrator);
		satisfactionGroup = new SatisfactionGroup(content, editor, orchestrator, this);
		approvalGroup = new ApprovalGroup(content, editor, orchestrator, this);
		inputGroup = new InputGroup(content, editor, orchestrator, this);

		initializeThreads();

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
				eu.kalafatic.evolution.controller.tools.ShellTool shell = new eu.kalafatic.evolution.controller.tools.ShellTool();
				shell.execute("git init", getProjectRoot(), null);
				Display.getDefault().asyncExec(() -> processLogEntry("Evo: Git repository initialized successfully."));
			} catch (Exception e) {
				Display.getDefault().asyncExec(() -> processLogEntry("Error initializing git: " + e.getMessage()));
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
		String modelName = "UNKNOWN";
		if (mode == AiMode.LOCAL) {
			modelName = (orchestrator.getOllama() != null) ? orchestrator.getOllama().getModel() : "NONE";
		} else {
			modelName = orchestrator.getRemoteModel();
		}
		if (modelName == null) modelName = "NOT SET";

		modeIndicatorLabel.setText(mode.getName().toUpperCase() + " MODE ACTIVE - " + modelName.toUpperCase());
		modeIndicatorLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		modeIndicatorLabel.setBackground(lightGreen);
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

		eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance()
		    .updateToken(orchestrator, remoteModel, aiSettingsGroup.getRemoteToken());
		
		if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());		
    	
    	PromptInstructions promptInstructions = orchestrator.getAiChat().getPromptInstructions();
    	
    	if (promptInstructions == null) {
    		promptInstructions = OrchestrationFactory.eINSTANCE.createPromptInstructions();
    		orchestrator.getAiChat().setPromptInstructions(promptInstructions);
    	}        	
          
        promptInstructions.setIterativeMode(instructionsGroup.isIterative());
        promptInstructions.setSelfIterativeMode(instructionsGroup.isSelfIterative());
        promptInstructions.setAutoApprove(instructionsGroup.isAutoApprove());
        promptInstructions.setPreferredMaxIterations(instructionsGroup.getMaxIterations());
        promptInstructions.setGitAutomation(instructionsGroup.isGitAutomationCheck());
		
		
		orchestrator.getAiChat().setUrl(aiSettingsGroup.getRemoteUrl());
		editor.setDirty(true);
		updateModeDisplay();
		isUpdating = false;
	}

	public void handleSend() {
		instructionsGroup.resetBackground();
		String request = instructionsGroup.getRequest();
		if (request.isEmpty()) return;
		if (currentContext != null && currentContext.isWaitingForInput()) {
			provideInput(request);
			instructionsGroup.setRequest("");
			return;
		}
		if (currentThread == null) initializeThreads();
	
		if (orchestrator != null && orchestrator.getAiChat() != null ) {
			PromptInstructions promptInstructions = orchestrator.getAiChat().getPromptInstructions();
			
			if (promptInstructions != null && promptInstructions.isSelfIterativeMode()) {
				startSelfDevAction(request); 
				return;
			}			
		}
		
		if (orchestrator != null) {
			if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
			if (orchestrator.getLlm() == null) orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
			NeuronService.getInstance().train(orchestrator, request);
			editor.setDirty(true);
			if (orchestrator.getId() == null || orchestrator.getId().isEmpty()) orchestrator.setId("chat-" + System.currentTimeMillis());
		}
		if (!chatGroup.getText().isEmpty()) chatGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
		chatGroup.appendText("You: " + request, colorUser, SWT.BOLD);
		chatGroup.appendText("\n\nEvo: Initializing orchestration...", colorEvolution, SWT.ITALIC);
		instructionsGroup.setRequest("");
		instructionsGroup.setOrchestrationRunning(true);
		chatGroup.setThinking(true);
		chatGroup.resetLogCount();

		TaskRequest taskRequest = new TaskRequest(request, getProjectRoot());
		taskRequest.getContext().put("orchestrator", orchestrator);
		taskRequest.getContext().put("threadId", getCurrentThreadName());

		orchestrationThread = new Thread(() -> {
			try {
				TaskResult result = OrchestratorServiceImpl.getInstance().execute(taskRequest);
				String taskId = result.getId();
				int lastProcessedIndex = 0;
				boolean approvalShown = false;
				boolean inputShown = false;

				// Monitor logs and result
				while (result.getStatus() == TaskResult.Status.RUNNING ||
				       result.getStatus() == TaskResult.Status.WAITING_FOR_APPROVAL ||
				       result.getStatus() == TaskResult.Status.WAITING_FOR_INPUT) {

				    if (result.getStatus() == TaskResult.Status.WAITING_FOR_APPROVAL && !approvalShown) {
				        final String msg = result.getWaitingMessage();
				        Display.getDefault().asyncExec(() -> {
				            if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
				                modeIndicatorLabel.setText("WAITING FOR USER APPROVAL...");
				                modeIndicatorLabel.setBackground(colorWaiting);
				            }
				            approvalGroup.show(msg); updateScrolledContent();
				        });
				        approvalShown = true;
				    } else if (result.getStatus() != TaskResult.Status.WAITING_FOR_APPROVAL && approvalShown) {
					Display.getDefault().asyncExec(() -> { updateModeDisplay(); approvalGroup.hide(); });
					approvalShown = false;
				    }

				    if (result.getStatus() == TaskResult.Status.WAITING_FOR_INPUT && !inputShown) {
				        final String msg = result.getWaitingMessage();
				        Display.getDefault().asyncExec(() -> {
				            if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
				                modeIndicatorLabel.setText("WAITING FOR USER INPUT...");
				                modeIndicatorLabel.setBackground(colorWaiting);
				            }
				            handleClarify();
				            inputGroup.show(msg); updateScrolledContent();
				        });
				        inputShown = true;
				    } else if (result.getStatus() != TaskResult.Status.WAITING_FOR_INPUT && inputShown) {
					Display.getDefault().asyncExec(() -> { updateModeDisplay(); inputGroup.hide(); });
					inputShown = false;
				    }

				    // Poll logs
				    int toIndex = result.getLogs().size();
				    if (lastProcessedIndex < toIndex) {
				        final List<String> newLogs = new java.util.ArrayList<>(result.getLogs().subList(lastProcessedIndex, toIndex));
				        lastProcessedIndex = toIndex; // Update immediately to avoid duplicate processing
				        if (!newLogs.isEmpty()) {
				            Display.getDefault().asyncExec(() -> {
				                for (String log : newLogs) {
				                    processLogEntry(log);
				                    chatGroup.incrementLogCount();
				                }
				            });
				        }
				    }

				    Thread.sleep(500);
				    result = OrchestratorServiceImpl.getInstance().getTaskResult(taskId);
				    if (result == null) break;
				}

				// Final log drain
				if (result != null) {
					final TaskResult finalRes = result;
					Display.getDefault().asyncExec(() -> {
						if (currentStackTask != null) {
							currentStackTask.setStatus(finalRes.getStatus() == TaskResult.Status.SUCCESS ? eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE : eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
							currentStackTask.setResultSummary(finalRes.getResponse());
						}
					});
					int toIndex = result.getLogs().size();
					if (lastProcessedIndex < toIndex) {
						final List<String> newLogs = new java.util.ArrayList<>(result.getLogs().subList(lastProcessedIndex, toIndex));
						Display.getDefault().asyncExec(() -> {
							for (String log : newLogs) {
								processLogEntry(log);
								chatGroup.incrementLogCount();
							}
						});
					}
				}

				final TaskResult finalResult = result;
				Display.getDefault().asyncExec(() -> {
					instructionsGroup.resetBackground();
					if (currentStackTask != null) {
						currentStackTask.setStatus(finalResult.getStatus() == TaskResult.Status.SUCCESS ? eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE : eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
						currentStackTask.setResultSummary(finalResult.getResponse());
					}
					if (!chatGroup.isDisposed()) {
						chatGroup.setThinking(false);

						if (!finalResult.getFileChanges().isEmpty()) {
							chatGroup.appendText("\n\nResult Summary:\n" + String.join("\n", finalResult.getFileChanges()), colorUser, SWT.NORMAL);
						}

						chatGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
						chatGroup.appendText("Final Response: " + finalResult.getResponse(), colorEvolution, SWT.BOLD);
						editor.setDirty(true);
						satisfactionGroup.setVisible(true); updateScrolledContent();
					}
				});
			} catch (Exception e) {
				Display.getDefault().asyncExec(() -> {
					if (!chatGroup.isDisposed()) {
						chatGroup.setThinking(false);
						chatGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
						chatGroup.appendText("Error: " + (e instanceof InterruptedException ? "Orchestration stopped by user." : e.getMessage()), colorError, SWT.BOLD);
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
			chatGroup.setThinking(!isPaused);
		}
	}

	public void handleStop() {
		instructionsGroup.resetBackground();
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

	private void initializeThreads() {
		if (orchestrator.getAiChat() == null) {
			orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
		}
		List<ChatThread> threadList = orchestrator.getAiChat().getThreads();
		if (threadList.isEmpty()) {
			ChatThread defaultThread = OrchestrationFactory.eINSTANCE.createChatThread();
			defaultThread.setId("Default");
			threadList.add(defaultThread);
		}
		currentThread = threadList.get(0);
		chatGroup.setThread(currentThread);
		updateThreadCombo();
	}

	private void updateThreadCombo() {
		String[] ids = orchestrator.getAiChat().getThreads().stream()
				.map(ChatThread::getId).toArray(String[]::new);
		chatMgmtGroup.updateThreadCombo(ids, currentThread.getId());
	}

	public void createNewThread() {
		InputDialog dlg = new InputDialog(getShell(), "New Chat Thread", "Enter thread description:", "task", null);
		if (dlg.open() == Window.OK) {
			String taskName = dlg.getValue();
			if (taskName != null && !taskName.trim().isEmpty()) {
				String dateStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
				String id = dateStr + "_" + taskName.trim();

				ChatThread newThread = OrchestrationFactory.eINSTANCE.createChatThread();
				newThread.setId(id);
				orchestrator.getAiChat().getThreads().add(newThread);
				currentThread = newThread;
				chatGroup.setThread(currentThread);
				updateThreadCombo();
				editor.setDirty(true);
			}
		}
	}

	public void switchThread(String threadId) {
		orchestrator.getAiChat().getThreads().stream()
				.filter(t -> t.getId().equals(threadId))
				.findFirst()
				.ifPresent(t -> {
					currentThread = t;
					chatGroup.setThread(currentThread);
				});
	}

	public void selectThreadByDate() {
		List<ChatThread> sortedThreads = orchestrator.getAiChat().getThreads().stream()
				.sorted((t1, t2) -> t2.getId().compareTo(t1.getId()))
				.collect(Collectors.toList());

		if (sortedThreads.isEmpty()) return;

		String[] items = sortedThreads.stream().map(ChatThread::getId).toArray(String[]::new);
		LabelProvider lp = new LabelProvider();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), lp);
		dialog.setElements(items);
		dialog.setTitle("Select Thread by Date");
		dialog.setMessage("Select a thread to load (sorted by date):");
		if (dialog.open() == Window.OK) {
			String selected = (String) dialog.getFirstResult();
			if (selected != null) {
				chatMgmtGroup.setThreadSelection(selected);
				switchThread(selected);
			}
		}
	}

	public void cleanChat() {
		chatGroup.clear();
		editor.setDirty(true);
	}

	private void startSelfDevAction(String request) {
		instructionsGroup.resetBackground();
		if (request == null || request.isEmpty()) request = "Analyze the project and suggest improvements.";
		if (currentContext != null && currentContext.isWaitingForInput()) {
			provideInput(request);
			instructionsGroup.setRequest("");
			return;
		}
		final String finalRequest = request;
		if (orchestrator != null) {
			if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
			if (orchestrator.getLlm() == null) orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
			if (orchestrator.getId() == null || orchestrator.getId().isEmpty()) orchestrator.setId("selfdev-" + System.currentTimeMillis());
		}
		if (!chatGroup.getText().isEmpty()) chatGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
		chatGroup.appendText("User [SELF-DEV]: " + finalRequest, colorUser, SWT.BOLD);
		chatGroup.appendText("\n\nEvo: Initializing Self-Development Supervisor loop...", colorEvolution, SWT.ITALIC | SWT.BOLD);
		instructionsGroup.setRequest("");
		instructionsGroup.setOrchestrationRunning(true);
		chatGroup.setThinking(true);
		orchestrationThread = new Thread(() -> {
			try {
				File projectRoot = getProjectRoot();
				TaskContext context = new TaskContext(orchestrator, projectRoot);
				context.setThreadId(getCurrentThreadName());
				context.getInstructionFiles().addAll(instructionsGroup.getInstructionFiles());
				this.currentContext = context;
				context.addLogListener(log -> Display.getDefault().asyncExec(() -> { if (!chatGroup.isDisposed()) processLogEntry(log); }));
				context.addApprovalListener(message -> Display.getDefault().asyncExec(() -> {
					if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
						modeIndicatorLabel.setText("WAITING FOR USER APPROVAL...");
						modeIndicatorLabel.setBackground(colorWaiting);
					}
					approvalGroup.show(message); updateScrolledContent();
				}));
				context.addInputListener(message -> Display.getDefault().asyncExec(() -> {
					if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
						modeIndicatorLabel.setText("WAITING FOR USER INPUT...");
						modeIndicatorLabel.setBackground(colorWaiting);
					}
					handleClarify();
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
				String summary = context.getOrchestrator().getTasks().stream()
						.filter(t -> t.getResultSummary() != null && !t.getResultSummary().isEmpty())
						.map(t -> "- " + t.getResultSummary())
						.collect(Collectors.joining("\n"));

				Display.getDefault().asyncExec(() -> {
					instructionsGroup.resetBackground();
					if (currentStackTask != null) {
						currentStackTask.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE);
						currentStackTask.setResultSummary("Self-Development session finished.");
					}
					if (!chatGroup.isDisposed()) {
						chatGroup.setThinking(false);

						if (!summary.isEmpty()) {
							chatGroup.appendText("\n\nResult Summary: " + summary, colorUser, SWT.NORMAL);
						}

						chatGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
						chatGroup.appendText("Final Response: Self-Development session finished. Status: " + session.getStatus(), colorEvolution, SWT.BOLD);
						editor.setDirty(true);
						satisfactionGroup.setVisible(true); updateScrolledContent();
					}
				});
			} catch (Exception e) {
				Display.getDefault().asyncExec(() -> { if (!chatGroup.isDisposed()) { chatGroup.setThinking(false); chatGroup.appendText("\n\n", colorWhite, SWT.NORMAL); chatGroup.appendText("Supervisor Error: " + (e instanceof InterruptedException ? "Orchestration stopped by user." : e.getMessage()), colorError, SWT.BOLD); } });
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
			try (FileWriter writer = new FileWriter(path)) { writer.write(chatGroup.getText()); }
			catch (Exception e) { chatGroup.appendText("\nError saving file: " + e.getMessage(), colorError, SWT.BOLD); }
		}
	}

	public void copyConversationToClipboard() {
		String fullText = chatGroup.getText();
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

	@Override
	public void setOrchestrator(Orchestrator orchestrator) {
		super.setOrchestrator(orchestrator);
		this.ollamaService = null;
	}

	@Override
	protected void refreshUI() {
		if (orchestrator != null && aiSettingsGroup != null && !isUpdating) {
			isUpdating = true;
			aiSettingsGroup.updateUI();
			instructionsGroup.updateUI();
			updateStatusInfo();
			updateModeDisplay();
			isUpdating = false;
		}
	}

	public void updateUI() {
		scheduleRefresh();
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
		if (orchestrationThread != null) {
			// We find the current task ID from the service (simplified, assuming one active task)
			// In a real multi-tenant scenario, we'd need to track which taskId belongs to this page
			OrchestratorServiceImpl.getInstance().provideApproval(orchestrator.getId(), approved);
		}
		if (currentContext != null) {
			currentContext.provideApproval(approved);
			approvalGroup.hide();
			updateModeDisplay();
			updateScrolledContent();
		}
	}

	public void handleReview() {
		editor.showApprovalPage();
	}

	public void handleClarify() {
		instructionsGroup.focusAndHighlight(colorLightOrange);
	}

	public void handleOpenDiff(String path) {
		if (path == null || path.isEmpty()) return;
		File projectRoot = getProjectRoot();
		File file = new File(projectRoot, path);
		if (file.exists()) {
			IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
			if (iFile != null) {
				editor.showComparePage(iFile);
			}
		}
	}

	public void handleSimpleSolution() {
		handleExecuteProposal("Execute the simplest working solution.");
	}

	public void handleExecuteProposal(String request) {
		instructionsGroup.setRequest(request);
		handleSend();
	}

	public void runTask(eu.kalafatic.evolution.model.orchestration.Task task) {
		if (task == null) return;
		this.currentStackTask = task;

		// 1. Switch to thread or create one
		if (task.getId() != null) {
			boolean exists = orchestrator.getAiChat().getThreads().stream()
					.anyMatch(t -> t.getId().equals(task.getId()));
			if (!exists) {
				ChatThread newThread = OrchestrationFactory.eINSTANCE.createChatThread();
				newThread.setId(task.getId());
				orchestrator.getAiChat().getThreads().add(newThread);
			}
			switchThread(task.getId());
			updateThreadCombo();
		}

		// 2. Set instructions
		String prompt = task.getDescription();
		if (prompt == null || prompt.isEmpty()) prompt = task.getName();
		instructionsGroup.setRequest(prompt);

		// 3. Set mode
		if ("SELF_DEV_MODE".equals(task.getType())) {
			instructionsGroup.setSelfIterative(true);
		} else if ("ASSISTED_CODING".equals(task.getType()) || "DARWIN_MODE".equals(task.getType())) {
			instructionsGroup.setIterative(true);
		} else {
			instructionsGroup.setIterative(false);
			instructionsGroup.setSelfIterative(false);
		}

		// 4. Send
		handleSend();
	}

	public void provideInput(String input) {
		instructionsGroup.resetBackground();
		if (orchestrationThread != null) {
			OrchestratorServiceImpl.getInstance().provideInput(orchestrator.getId(), input);
		}
		if (currentContext != null) {
			currentContext.provideInput(input);
			inputGroup.hide();
			updateModeDisplay();
			updateScrolledContent();
		}
	}

	private void processLogEntry(String log) {
		if (log == null || log.isEmpty()) return;
		
		Color color = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
		int style = SWT.NORMAL;
		if (log.startsWith("Evo:") || log.startsWith("Orchestrator:")) { color = colorEvolution; style = SWT.ITALIC; }
		else if (log.contains("Agent [") && log.contains("Planner")) { color = colorPlanner; style = SWT.BOLD; }
		else if (log.contains("Agent [") && log.contains("Architect")) { color = colorArchitect; style = SWT.BOLD; }
		else if (log.contains("Agent [") && log.contains("JavaDev")) { color = colorJavaDev; style = SWT.BOLD; }
		else if (log.contains("Agent [") && log.contains("Tester")) { color = colorTester; style = SWT.BOLD; }
		else if (log.contains("Agent [") && log.contains("Reviewer")) { color = colorReviewer; style = SWT.BOLD; }
		else if (log.startsWith("Orchestrator Error:") || log.contains("Exception:")) { color = colorError; style = SWT.BOLD; }
		chatGroup.appendText("\n" + log, color, style);
		editor.setDirty(true);
	}

	public String getCurrentThreadName() { return currentThread != null ? currentThread.getId() : "Default"; }

	public MultiPageEditor getEditor() { return editor; }

	public FormToolkit getToolkit() { return toolkit; }

	public void setupContextAssist(StyledText text) {
		IContentProposalProvider proposalProvider = (contents, position) -> {
			String prefix = contents.substring(0, position);
			int lastSpace = prefix.lastIndexOf(' '); if (lastSpace != -1) prefix = prefix.substring(lastSpace + 1);
			String finalPrefix = prefix;

			List<String> allProposals = new java.util.ArrayList<>();

			// Magic Commands
			if (contents.startsWith("/")) {
				allProposals.add("/create class ");
				allProposals.add("/create test for ");
				allProposals.add("/fix all warnings");
				allProposals.add("/analyze project structure");
				allProposals.add("/refactor ");
				allProposals.add("/explain ");
				allProposals.add("/apply best practices");
				allProposals.add("/generate javadoc");
				allProposals.add("/optimize imports");
				allProposals.add("/find security vulnerabilities");
				allProposals.add("/help");
			}

			// Neuron Proposals
			String[] neuronProposals = NeuronService.getInstance().getProposals(orchestrator, finalPrefix);
			for (String p : neuronProposals) {
				if (!allProposals.contains(p)) allProposals.add(p);
			}

			IContentProposal[] result = new IContentProposal[allProposals.size()];
			for (int i = 0; i < allProposals.size(); i++) {
				final String proposal = allProposals.get(i);
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

				// Copy custom providers for resolution during test
				if (orchestrator != null) {
				    tempOrch.getAiProviders().addAll(org.eclipse.emf.ecore.util.EcoreUtil.copyAll(orchestrator.getAiProviders()));
				}

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
					if (newToken != null) {
					    eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().updateToken(orchestrator, provider, newToken);
					    aiSettingsGroup.setRemoteToken(newToken);
					    syncModelWithUI();
					    future.complete(newToken);
					} else future.completeExceptionally(new Exception("Token request cancelled by user."));
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
