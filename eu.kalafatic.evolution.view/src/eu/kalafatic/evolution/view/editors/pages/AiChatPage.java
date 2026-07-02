package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import eu.kalafatic.evolution.controller.manager.EnvironmentSuggestionService;
import eu.kalafatic.evolution.controller.manager.NeuronService;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.orchestration.ConversationOutputController;
import eu.kalafatic.evolution.controller.orchestration.MessagePriority;
import eu.kalafatic.evolution.controller.orchestration.ModeRouter;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.workflow.WorkflowStatus;
import eu.kalafatic.evolution.controller.workflow.WorkflowStep;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.FeedbackLevel;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.application.Activator;
import eu.kalafatic.evolution.view.dialogs.ProjectSetupWizardDialog;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.aichat.ChatGroup;
import eu.kalafatic.evolution.view.editors.pages.aichat.ChatMgmtGroup;
import eu.kalafatic.evolution.view.editors.pages.aichat.FeedbackGroup;
import eu.kalafatic.evolution.view.editors.pages.aichat.InstructionsGroup;
import eu.kalafatic.evolution.view.editors.pages.aichat.SystemStatusGroup;
import eu.kalafatic.evolution.view.projection.ProjectionService;
import eu.kalafatic.evolution.view.projection.RuntimeProjection;

/**
 * @evo:16:A reason=darwin-mode-sync
 */
public class AiChatPage extends AEvoPage {
	private boolean isUpdating = false;
	private Label modeIndicatorLabel;
	private ContentProposalAdapter assistAdapter;
	private OllamaService ollamaService;
	private ChatSession currentSession;
	private Composite content;
	private long lastStatusUpdate = 0;

	private ChatMgmtGroup chatMgmtGroup;
	private InstructionsGroup instructionsGroup;
	private ChatGroup chatGroup;
	private SystemStatusGroup systemStatusGroup;
	private FeedbackGroup feedbackGroup;
	private ConversationOutputController outputController;
	private String currentTurnId;

	private int editingMessageIndex = -1;
	private String editingVariantId = null;

	private final java.util.function.Consumer<RuntimeProjection> chatProjectionObserver = projection -> {
		if (isDisposed()) return;
		if (projection.getSessionId().equals(getCurrentSessionName())) {
			// Handle special view updates from projection (like token requests)
			projection.getEvents().stream()
				.filter(e -> e.getType() == RuntimeEventType.VIEW_UPDATED && "TokenRequest".equals(e.getSource()))
				.reduce((first, second) -> second) // get last
				.ifPresent(e -> {
					Object[] payload = (Object[]) e.getPayload();
					String provider = (String) payload[0];
					@SuppressWarnings("unchecked")
					java.util.concurrent.CompletableFuture<String> future = (java.util.concurrent.CompletableFuture<String>) payload[1];
					if (!future.isDone()) {
						Display.getDefault().asyncExec(() -> {
							String token = requestToken(provider);
							if (token != null) {
								java.util.Map<String, Object> settings = new java.util.HashMap<>();
								settings.put("token_" + provider, token);
								OrchestratorServiceImpl.getInstance().updateConfiguration(getCurrentSessionName(), settings);
								future.complete(token);
							} else {
								future.completeExceptionally(new Exception("Token request cancelled by user."));
							}
						});
					}
				});
		}
	};



	private final java.util.function.Consumer<ChatMessage> messageSubscriber = msg -> {
		if (isDisposed()) return;
		Display.getDefault().asyncExec(() -> {
			if (isDisposed()) return;
			String turnId = msg.getTurnId();
			if (turnId == null) return;

			String sid;
			if (turnId.contains("__")) {
				sid = turnId.substring(0, turnId.lastIndexOf("__"));
			} else if (turnId.contains("_")) {
				sid = turnId.substring(0, turnId.lastIndexOf("_"));
			} else {
				sid = getCurrentSessionName();
			}
			chatGroup.addMessageToSession(sid, msg);
		});
	};

	public AiChatPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, editor, orchestrator);
		
		this.outputController = ConversationOutputController.getInstance();
		this.outputController.subscribe(messageSubscriber);
		createControl();
		ProjectionService.getInstance().subscribe(chatProjectionObserver);
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				ProjectionService.getInstance().unsubscribe(chatProjectionObserver);
				if (outputController != null) outputController.unsubscribe(messageSubscriber);

				// Shut down all sessions associated with this page/orchestrator
				if (orchestrator != null && orchestrator.getAiChat() != null) {
					orchestrator.getAiChat().getSessions().forEach(s -> {
						if (s.getId() != null) {
							OrchestratorServiceImpl.getInstance().shutdownSession(s.getId());
						}
					});
				}

				if (chatFont != null && !chatFont.isDisposed()) chatFont.dispose();
				if (bannerFont != null && !bannerFont.isDisposed()) bannerFont.dispose();
				if (colorWaiting != null && !colorWaiting.isDisposed()) colorWaiting.dispose();
				if (colorLightOrange != null && !colorLightOrange.isDisposed()) colorLightOrange.dispose();
			}
		});
	}


	private void createControl() {
		content = toolkit.createComposite(this);
		content.setLayout(new GridLayout(1, false));
		this.setContent(content);

		modeIndicatorLabel = new Label(content, SWT.CENTER);
		modeIndicatorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modeIndicatorLabel.setFont(bannerFont);
		setTextSafe(modeIndicatorLabel, "INITIALIZING...");

		systemStatusGroup = new SystemStatusGroup(toolkit, content, editor, orchestrator);
		chatMgmtGroup = new ChatMgmtGroup(toolkit, content, editor, orchestrator, this);

		// Main resizable area
		SashForm mainSash = new SashForm(content, SWT.VERTICAL | SWT.SMOOTH);
		GridData sashGd = new GridData(GridData.FILL_BOTH);
		sashGd.heightHint = 800; // Give it a reasonable initial size
		mainSash.setLayoutData(sashGd);

		chatGroup = new ChatGroup(toolkit, mainSash, editor, orchestrator, chatFont, this);
		chatGroup.setEditCallback((index, oldText) -> {
			Display.getDefault().asyncExec(() -> {
				InputDialog dlg = new InputDialog(getShell(), "Edit Message", "Modify the message content:", oldText, null);
				if (dlg.open() == Window.OK) {
					chatGroup.updateMessage(index, dlg.getValue());
					editor.setDirty(true);
				}
			});
		});

		// Composite for the resizable instructions prompt
		Composite promptContainer = toolkit.createComposite(mainSash);
		promptContainer.setLayout(new FillLayout());

		// Footer container for instruction buttons and feedback
		Composite footerContainer = toolkit.createComposite(mainSash);
		footerContainer.setLayout(new GridLayout(1, false));

		instructionsGroup = new InstructionsGroup(toolkit, promptContainer, footerContainer, this, orchestrator);
		feedbackGroup = new FeedbackGroup(toolkit, footerContainer, editor, orchestrator, this);

		mainSash.setWeights(new int[] { 50, 20, 30 });

		initializeSessions();

		Runnable timer = new Runnable() {
			public void run() {
				Display.getDefault().asyncExec(() -> {
					if (!content.isDisposed()) {
						String sid = getCurrentSessionName();
						eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(sid);
						if (session != null) {
							double progress = session.getStatusManager().getProgress(sid);
							String status = session.getStatusManager().getStatus(sid);
							systemStatusGroup.updateProgress(status, (int) (progress * 100));
						}
						Display.getDefault().timerExec(500, this);
					}
				});
			}
		};
		Display.getDefault().asyncExec(() -> {
			if (!content.isDisposed()) {
				Display.getDefault().timerExec(500, timer);
			}
		});

		updateStatusInfo();
		updateModeDisplay();
		updateScrolledContent();

		if (lastStatusUpdate == 0) Display.getDefault().asyncExec(() -> checkEnvironment());
		
		content.getParent().layout(true);
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
		final File projectRoot = getProjectRoot();
		new Thread(() -> {
			try {
				eu.kalafatic.evolution.controller.tools.ShellTool shell = new eu.kalafatic.evolution.controller.tools.ShellTool();
				shell.execute("git init", projectRoot, null);
				Display.getDefault().asyncExec(() -> processLogEntry("Evo: Git repository initialized successfully."));
			} catch (Exception e) {
				Display.getDefault().asyncExec(() -> processLogEntry("Error initializing git: " + e.getMessage()));
			}
		}).start();
	}

	public void updateScrolledContent() {
		if (content == null || content.isDisposed()) return;
		content.layout(true);
		this.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public void updateModeDisplay() {
		if (modeIndicatorLabel == null || modeIndicatorLabel.isDisposed()) return;
		RuntimeProjection projection = ProjectionService.getInstance().getProjection(getCurrentSessionName());
		java.util.Map<String, Object> config = projection.getConfiguration();

		int modeVal = (int) config.getOrDefault("aiMode", orchestrator != null ? orchestrator.getAiMode().getValue() : 0);
		AiMode mode = AiMode.get(modeVal);

		String modelName = (String) config.getOrDefault(mode == AiMode.LOCAL ? "localModel" : "remoteModel", null);
		if (modelName == null && orchestrator != null) {
			modelName = (mode == AiMode.LOCAL) ? orchestrator.getLocalModel() : orchestrator.getRemoteModel();
		}
		if (modelName == null) modelName = "NOT SET";

		File projectRoot = getProjectRoot();
		String targetPath = projectRoot != null ? projectRoot.getAbsolutePath() : "UNKNOWN";

		setTextSafe(modeIndicatorLabel, mode.getName().toUpperCase() + " - " + modelName.toUpperCase() + " - " + targetPath);
		setForegroundSafe(modeIndicatorLabel, Display.getDefault().getSystemColor(SWT.COLOR_BLACK));

		Color targetBg = lightGreen;
		switch (mode) {
			case LOCAL: targetBg = colorLocal; break;
			case HYBRID: targetBg = colorHybrid; break;
			case REMOTE: targetBg = colorRemote; break;
			case MEDIATED: targetBg = colorHybrid; break;
		}
		setBackgroundSafe(modeIndicatorLabel, targetBg);
	}

	public void saveLastUsedSettings() {
		if (orchestrator == null || Activator.getDefault() == null) return;
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		if (settings == null) return;
		IDialogSettings section = settings.getSection("AiChatSettings");
		if (section == null) section = settings.addNewSection("AiChatSettings");

		section.put("AiMode", orchestrator.getAiMode().getValue());
		if (orchestrator.getLocalModel() != null) section.put("LocalModel", orchestrator.getLocalModel());
		if (orchestrator.getRemoteModel() != null) section.put("RemoteModel", orchestrator.getRemoteModel());
		if (chatMgmtGroup != null && chatMgmtGroup.getRemoteToken() != null) section.put("RemoteToken_" + orchestrator.getRemoteModel(), chatMgmtGroup.getRemoteToken());
		if (chatMgmtGroup != null && chatMgmtGroup.getRemoteUrl() != null) section.put("RemoteUrl_" + orchestrator.getRemoteModel(), chatMgmtGroup.getRemoteUrl());

		if (currentSession != null) {
			section.put("iterativeMode", currentSession.isIterativeMode());
			section.put("selfIterativeMode", currentSession.isSelfIterativeMode());
			section.put("darwinMode", currentSession.isDarwinMode());
			section.put("gitAutomation", currentSession.isGitAutomation());
			section.put("maxIterations", currentSession.getMaxIterations());
			section.put("stepMode", currentSession.isStepMode());
			if (currentSession.getTargetPath() != null) section.put("targetPath", currentSession.getTargetPath());
			if (currentSession.getTargetType() != null) section.put("targetType", currentSession.getTargetType());
			section.put("autoApprove", currentSession.isAutoApprove());
			section.put("expansion", currentSession.getExpansion());
		}
	}

	public void loadLastUsedSettings() {
		if (orchestrator == null || Activator.getDefault() == null) return;
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		if (settings == null) return;
		IDialogSettings section = settings.getSection("AiChatSettings");
		if (section == null) return;

		try {
			int modeVal = section.getInt("AiMode");
			orchestrator.setAiMode(AiMode.get(modeVal));
		} catch (NumberFormatException e) {}

		String localModel = section.get("LocalModel");
		if (localModel != null) {
			orchestrator.setLocalModel(localModel);
			if (orchestrator.getOllama() != null) orchestrator.getOllama().setModel(localModel);
		}

		String remoteModel = section.get("RemoteModel");
		if (remoteModel != null) {
			orchestrator.setRemoteModel(remoteModel);
			String token = section.get("RemoteToken_" + remoteModel);
			if (token != null) {
				eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance()
					.updateToken(orchestrator, remoteModel, token);
			}
			String url = section.get("RemoteUrl_" + remoteModel);
			if (url != null) {
				if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
				orchestrator.getAiChat().setUrl(url);
			}
		}

		if (currentSession != null) {
			if (section.get("iterativeMode") != null) currentSession.setIterativeMode(section.getBoolean("iterativeMode"));
			if (section.get("selfIterativeMode") != null) currentSession.setSelfIterativeMode(section.getBoolean("selfIterativeMode"));
			if (section.get("darwinMode") != null) currentSession.setDarwinMode(section.getBoolean("darwinMode"));
			if (section.get("gitAutomation") != null) currentSession.setGitAutomation(section.getBoolean("gitAutomation"));
			try { if (section.get("maxIterations") != null) currentSession.setMaxIterations(section.getInt("maxIterations")); } catch (NumberFormatException e) {}
			if (section.get("stepMode") != null) currentSession.setStepMode(section.getBoolean("stepMode"));
			String targetPath = section.get("targetPath");
			if (targetPath != null) currentSession.setTargetPath(targetPath);
			String targetType = section.get("targetType");
			if (targetType != null) currentSession.setTargetType(targetType);
			if (section.get("autoApprove") != null) currentSession.setAutoApprove(section.getBoolean("autoApprove"));
			try { if (section.get("expansion") != null) currentSession.setExpansion(section.getInt("expansion")); } catch (NumberFormatException e) {}

			// Also update orchestrator fields if they exist and match
			orchestrator.setDarwinMode(currentSession.isDarwinMode());
			if (currentSession.getAiMode() != null) orchestrator.setAiMode(currentSession.getAiMode());
			if (currentSession.getLocalModel() != null) orchestrator.setLocalModel(currentSession.getLocalModel());
			if (currentSession.getRemoteModel() != null) orchestrator.setRemoteModel(currentSession.getRemoteModel());
		}
	}


	public void handleSend() throws IOException {
		instructionsGroup.resetBackground();
		String request = instructionsGroup.getRequest();
		String currentSessionId = getCurrentSessionName();
		RuntimeProjection projection = ProjectionService.getInstance().getProjection(currentSessionId);

		// Check for active steps in Step Mode - allow resumption even if command is already running
		eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(currentSessionId);
		WorkflowStep activeStep = (session != null) ? session.getWorkflowRegistry().getActiveStepForSession(currentSessionId) : null;
		if (activeStep != null && activeStep.getStatus() == WorkflowStatus.WAITING_USER) {
			String lower = request.toLowerCase().trim();
			WorkflowStatus targetStatus = WorkflowStatus.COMPLETED;
			if (lower.equals("retry")) targetStatus = WorkflowStatus.RETRY;
			else if (lower.equals("skip")) targetStatus = WorkflowStatus.SKIPPED;

			instructionsGroup.setOrchestrationRunning(true);
			OrchestratorServiceImpl.getInstance().resumeStep(currentSessionId, activeStep.getId(), targetStatus);
			instructionsGroup.setRequest("");
			return;
		}

		// Check if we are waiting for user input or approval and unblock via chat if possible
		boolean isWaiting = projection.isRunning() && projection.isWaitingForUser();

		if (isWaiting) {
			String lower = request.toLowerCase().trim();

			if (editingVariantId != null && request.toUpperCase().startsWith("EDIT PROPOSAL")) {
				provideInput(request);
				chatGroup.handleApproveDarwinVariant(editingMessageIndex, editingVariantId);
				editingVariantId = null;
				editingMessageIndex = -1;
				instructionsGroup.setRequest("");
				return;
			}

			boolean isApproval = projection.isWaitingForUser();

			if (isApproval) {
				if (lower.matches("^(yes|y|ok|okay|approve|proceed|go ahead|yep|sure)$") || lower.contains("approve variant")) {
					provideApproval(true);
					instructionsGroup.setRequest("");
					return;
				} else if (lower.matches("^(no|n|reject|stop|cancel|abort)$")) {
					provideApproval(false);
					instructionsGroup.setRequest("");
					return;
				}
			}

			provideInput(request);
			instructionsGroup.setRequest("");
			return;
		}

		if (projection.isRunning()) return; // Prevent duplicate sessions for same ID

		if (request.isEmpty()) {
			if (AiMode.MEDIATED.getName().equals(chatMgmtGroup.getAiModeCombo().getItem(chatMgmtGroup.getAiModeCombo().getSelectionIndex()))) {
				//processLogEntry("Evo: Please provide a request or instruction to proceed.");
				request = Files.readString(java.nio.file.Path.of("md", "mediated.md"), StandardCharsets.UTF_8);
			} else {
				//processLogEntry("Evo: Request is empty. Please enter a valid instruction or question.");
				return;				
			}
		}

		if (currentSession == null) initializeSessions();

		// --- FAST MODE ROUTING: Determine if this is a simple chat request before starting Self-Dev/Darwin ---
		ModeRouter modeRouter = new ModeRouter();
		PlatformMode detectedMode = modeRouter.routeFast(request, orchestrator);
		boolean isSimpleChat = (detectedMode != null && detectedMode.getType() == PlatformType.SIMPLE_CHAT);
		boolean isMediated = (detectedMode != null && detectedMode.getType() == PlatformType.HYBRID_MANUAL_EXPORT) || (orchestrator != null && orchestrator.getAiMode() == AiMode.MEDIATED);

		// AUTOMATIC TARGETING FOR SELF-DEV
		if (isMediated && detectedMode != null && detectedMode.getType() == PlatformType.SELF_DEV_MODE) {
			String evoRepo = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance().findEvolutionRepository();
			if (evoRepo != null && currentSession != null) {
				currentSession.setTargetPath(evoRepo);
				processLogEntry("Evo: Self-Development detected. Automatically targeted local evolution repository: " + evoRepo);
			}
		}

		// Start Self-Dev Supervisor if (Self-Development OR Darwin mode is enabled) AND it's NOT a simple chat.
		// Darwin is now the unified basic flow for all implementation requests.
		boolean isSelfDev = (boolean) projection.getConfiguration().getOrDefault("selfIterativeMode",
				currentSession != null ? currentSession.isSelfIterativeMode() :
				(orchestrator != null && orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null && orchestrator.getAiChat().getPromptInstructions().isSelfIterativeMode()));

		boolean isDarwin = (boolean) projection.getConfiguration().getOrDefault("darwinMode",
				currentSession != null ? currentSession.isDarwinMode() :
				(orchestrator != null && orchestrator.isDarwinMode()));

		if (!isSimpleChat && (isSelfDev || isDarwin)) {
			startSelfDevAction(request);
			return;
		}
		
		if (assistAdapter != null) assistAdapter.closeProposalPopup();

		String sessionId = getCurrentSessionName();
		currentTurnId = sessionId + "__" + System.currentTimeMillis();

		outputController.submitMessage(sessionId, currentTurnId, "You", request, "user", MessagePriority.NORMAL, false);
		outputController.submitMessage(sessionId, currentTurnId, "Evo", "Initializing orchestration...", "ai", MessagePriority.PROGRESS, false);
		instructionsGroup.setRequest("");
		chatGroup.resetLogCount();

		TaskRequest taskRequest = new TaskRequest(request, getProjectRoot());
		taskRequest.getContext().put("orchestrator", orchestrator);
		taskRequest.getContext().put("sessionId", sessionId);

		OrchestratorServiceImpl.getInstance().submit(sessionId, taskRequest);
	}

	public void handlePause() {
		RuntimeProjection projection = ProjectionService.getInstance().getProjection(getCurrentSessionName());
		OrchestratorServiceImpl.getInstance().setPaused(getCurrentSessionName(), !projection.isPaused());
	}

	public void handleStop() {
		instructionsGroup.resetBackground();
		// Shut down and cleanup the session resources - this will trigger KERNEL_SHUTDOWN event
		OrchestratorServiceImpl.getInstance().shutdownSession(getCurrentSessionName());
	}

	private String requestToken(String provider) {
		InputDialog dlg = new InputDialog(getShell(), "API Token Required", "Please enter the API token for " + provider + ":", "", null);
		if (dlg.open() == Window.OK) return dlg.getValue();
		return null;
	}

	private void initializeSessions() {
		if (orchestrator.getAiChat() == null) {
			orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
		}
		List<ChatSession> sessionList = orchestrator.getAiChat().getSessions();
		if (sessionList.isEmpty()) {
			ChatSession defaultSession = OrchestrationFactory.eINSTANCE.createChatSession();
			defaultSession.setId("Default");
			defaultSession.setIterativeMode(true);
			defaultSession.setDarwinMode(true);
			sessionList.add(defaultSession);
		}
		currentSession = sessionList.get(0);
		chatGroup.setSession(currentSession);
		updateSessionCombo();
	}

	private void updateSessionCombo() {
		String[] ids = orchestrator.getAiChat().getSessions().stream()
				.map(ChatSession::getId).toArray(String[]::new);
		chatMgmtGroup.updateSessionCombo(ids, currentSession.getId());
	}

	public void createNewSession() {
		InputDialog dlg = new InputDialog(getShell(), "New Chat Session", "Enter session description:", "task", null);
		if (dlg.open() == Window.OK) {
			String taskName = dlg.getValue();
			createNewSession(taskName);
		}
	}

	/**
	 * @evo:17:A reason=reusable-thread-creation
	 */
	public void createNewSession(String taskName) {
		if (taskName != null && !taskName.trim().isEmpty()) {
			String dateStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
			String id = dateStr + "_" + taskName.trim();

			ChatSession newSession = OrchestrationFactory.eINSTANCE.createChatSession();
			newSession.setId(id);

			// Copy general settings from current session
			if (currentSession != null) {
				newSession.setIterativeMode(currentSession.isIterativeMode());
				newSession.setSelfIterativeMode(currentSession.isSelfIterativeMode());
				newSession.setDarwinMode(currentSession.isDarwinMode());
				newSession.setGitAutomation(currentSession.isGitAutomation());
				newSession.setMaxIterations(currentSession.getMaxIterations());
				newSession.setStepMode(currentSession.isStepMode());
				newSession.setTargetPath(currentSession.getTargetPath());
				newSession.setTargetType(currentSession.getTargetType());
				newSession.setAutoApprove(currentSession.isAutoApprove());
				newSession.setAiMode(currentSession.getAiMode());
				newSession.setLocalModel(currentSession.getLocalModel());
				newSession.setRemoteModel(currentSession.getRemoteModel());
				newSession.setExpansion(currentSession.getExpansion());
			}

			orchestrator.getAiChat().getSessions().add(newSession);
			currentSession = newSession;

			// Pre-select model from last used settings (and other general settings)
			loadLastUsedSettings();
			scheduleRefresh();
			chatGroup.setSession(currentSession);
			updateSessionCombo();

			editor.setDirty(true);
		}
	}

	public void switchSession(String sessionId) {
		orchestrator.getAiChat().getSessions().stream()
				.filter(t -> t.getId().equals(sessionId))
				.findFirst()
				.ifPresent(t -> {
					currentSession = t;
					chatGroup.setSession(currentSession);

					RuntimeProjection projection = ProjectionService.getInstance().getProjection(sessionId);

					instructionsGroup.setOrchestrationRunning(projection.isRunning());
					instructionsGroup.setPaused(projection.isPaused());
					chatGroup.setThinking(projection.isRunning() && !projection.isPaused());

					// Reset progress panel for new session
					if (chatGroup.isDisposed()) return;
					chatGroup.getControl().getDisplay().asyncExec(() -> {
						if (chatGroup.isDisposed()) return;
						// We clear the progress panel by sending an empty/reset message if no active progress event exists
						// For now, let's just trigger a browser refresh which will naturally handle the message state
						chatGroup.refreshUI();
					});

					// Synchronize model with session settings
					if (t.getAiMode() != null) orchestrator.setAiMode(t.getAiMode());
					if (t.getLocalModel() != null) orchestrator.setLocalModel(t.getLocalModel());
					if (t.getRemoteModel() != null) orchestrator.setRemoteModel(t.getRemoteModel());

					updateModeDisplay();
					updateScrolledContent();

					// Force UI groups to reload from the new session object
					if (chatMgmtGroup != null) chatMgmtGroup.scheduleRefresh();
					if (instructionsGroup != null) instructionsGroup.scheduleRefresh();
				});
	}

	public void selectSessionByDate() {
		List<ChatSession> sortedSessions = orchestrator.getAiChat().getSessions().stream()
				.sorted((t1, t2) -> t2.getId().compareTo(t1.getId()))
				.collect(Collectors.toList());

		if (sortedSessions.isEmpty()) return;

		String[] items = sortedSessions.stream().map(ChatSession::getId).toArray(String[]::new);
		LabelProvider lp = new LabelProvider();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), lp);
		dialog.setElements(items);
		dialog.setTitle("Select Session by Date");
		dialog.setMessage("Select a thread to load (sorted by date):");
		if (dialog.open() == Window.OK) {
			String selected = (String) dialog.getFirstResult();
			if (selected != null) {
				chatMgmtGroup.setSessionSelection(selected);
				switchSession(selected);
			}
		}
	}

	public void cleanChat() {
		chatGroup.clear();
		editor.setDirty(true);
	}

	private void startSelfDevAction(String request) {
		RuntimeProjection projection = ProjectionService.getInstance().getProjection(getCurrentSessionName());
		if (projection.isRunning()) return;

		instructionsGroup.resetBackground();
		if (request == null || request.isEmpty()) request = "Analyze the project and suggest improvements.";

		final String finalRequest = request;
	
		boolean isSelfDev = (boolean) projection.getConfiguration().getOrDefault("selfIterativeMode",
				currentSession != null ? currentSession.isSelfIterativeMode() :
				(orchestrator != null && orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null && orchestrator.getAiChat().getPromptInstructions().isSelfIterativeMode()));

		boolean isDarwin = (boolean) projection.getConfiguration().getOrDefault("darwinMode",
				currentSession != null ? currentSession.isDarwinMode() :
				(orchestrator != null && orchestrator.isDarwinMode()));

		final String modeLabel = isDarwin ? "DARWIN" : (isSelfDev ? "SELF-DEV" : "EVO");
		String idPrefix = isSelfDev ? "selfdev-" : (isDarwin ? "darwin-" : "chat-");

		if (orchestrator != null) {
			orchestrator.setDarwinMode(isDarwin);
			if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
			if (orchestrator.getLlm() == null) orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
			NeuronService.getInstance().train(orchestrator, finalRequest, "coding", 5);
			if (orchestrator.getId() == null || orchestrator.getId().isEmpty()) orchestrator.setId(idPrefix + System.currentTimeMillis());
		}

		String sessionId = getCurrentSessionName();
		currentTurnId = sessionId + "__" + System.currentTimeMillis();

		outputController.submitMessage(sessionId, currentTurnId, "User [" + modeLabel + "]", finalRequest, "user", MessagePriority.NORMAL, false);
		String loopSuffix = (isSelfDev) ? " Supervisor loop" : " loop";
		outputController.submitMessage(sessionId, currentTurnId, "Evo", "Initializing " + modeLabel + loopSuffix + "...", "ai", MessagePriority.PROGRESS, false);

		// Initialize progress panel in webview
		org.json.JSONObject progressInit = new org.json.JSONObject();
		progressInit.put("iterationCount", 0);
		progressInit.put("generation", 1);
		progressInit.put("lineage", "initial");
		progressInit.put("stage", "ITERATION_START");
		progressInit.put("autoApprove", currentSession != null ? currentSession.isAutoApprove() : false);
		progressInit.put("gitAutomation", currentSession != null ? currentSession.isGitAutomation() : false);
		progressInit.put("maxIterations", currentSession != null ? currentSession.getMaxIterations() : 1);
		progressInit.put("startTime", System.currentTimeMillis());
		progressInit.put("currentTask", finalRequest);
		outputController.submitMessage(sessionId, currentTurnId, "Evo", progressInit.toString(), "evolution-progress", MessagePriority.PROGRESS, false);
		instructionsGroup.setRequest("");

		TaskRequest taskRequest = new TaskRequest(finalRequest, getProjectRoot());
		taskRequest.getContext().put("orchestrator", orchestrator);
		taskRequest.getContext().put("sessionId", sessionId);

		OrchestratorServiceImpl.getInstance().submit(sessionId, taskRequest);
	}

	public File getProjectRoot() {
		if (orchestrator != null && orchestrator.getAiMode() == AiMode.MEDIATED && currentSession != null) {
			String targetPath = currentSession.getTargetPath();
			if (targetPath != null && !targetPath.isEmpty()) {
				File targetFile = new File(targetPath);
				if (targetFile.exists() && targetFile.isDirectory()) {
					return targetFile;
				}
			}
		}

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
		dialog.setFileName(currentSession + ".txt");
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
		RuntimeProjection projection = ProjectionService.getInstance().getProjection(getCurrentSessionName());
		String model = (String) projection.getConfiguration().getOrDefault("localModel", orchestrator != null ? orchestrator.getLocalModel() : "Not Configured");
		String url = (orchestrator != null && orchestrator.getOllama() != null) ? orchestrator.getOllama().getUrl() : "http://localhost:11434";

		ollamaService = OllamaManager.getInstance().getService(url);
		systemStatusGroup.updateModelStatus(model != null ? model : "Not Configured");
		new Thread(() -> {
			boolean isOnline = ollamaService.ping();
			Display.getDefault().asyncExec(() -> {
				if (!systemStatusGroup.isDisposed()) {
					systemStatusGroup.updateOllamaStatus((isOnline ? "Online (" : "Offline (") + url + ")", Display.getDefault().getSystemColor(isOnline ? SWT.COLOR_DARK_GREEN : SWT.COLOR_RED));
				}
			});
		}).start();
	}

	@Override
	public void setOrchestrator(Orchestrator orchestrator) {
		super.setOrchestrator(orchestrator);
		this.ollamaService = null;
	}

	@Override
	protected void refreshUI() {
		if (chatMgmtGroup != null && !isUpdating) {
			isUpdating = true;
			try {
				RuntimeProjection projection = ProjectionService.getInstance().getProjection(getCurrentSessionName());

				chatMgmtGroup.updateUI();
				instructionsGroup.updateUI();
				if (feedbackGroup != null) feedbackGroup.updateUI();
				updateStatusInfo();

				// Update based on projection
				if (projection.isWaitingForUser()) {
					setTextSafe(modeIndicatorLabel, "WAITING FOR USER...");
					setBackgroundSafe(modeIndicatorLabel, colorWaiting);
				} else {
					updateModeDisplay();
				}

				instructionsGroup.setOrchestrationRunning(projection.isRunning());
				instructionsGroup.setPaused(projection.isPaused());
				chatGroup.setThinking(projection.isRunning() && !projection.isPaused());

				// Centralize layout at the end of refresh
				updateScrolledContent();
			} finally {
				isUpdating = false;
			}
		}
	}

	public void updateUI() {
		scheduleRefresh();
	}

	private void resumeWaitingSessions() {
		// This should now be handled by OrchestratorServiceImpl directly if needed
		// or via events.
	}

	public void submitFeedback(int satisfaction, String comments) {
		if (orchestrator != null) {
			if (orchestrator.getSelfDevSession() != null) {
				if (orchestrator.getSelfDevSession().getIterations().isEmpty()) orchestrator.getSelfDevSession().getIterations().add(OrchestrationFactory.eINSTANCE.createIteration());
				eu.kalafatic.evolution.model.orchestration.Iteration last = orchestrator.getSelfDevSession().getIterations().get(orchestrator.getSelfDevSession().getIterations().size() - 1);
				if (last.getEvaluationResult() == null) last.setEvaluationResult(OrchestrationFactory.eINSTANCE.createEvaluationResult());
				last.getEvaluationResult().setUserSatisfaction(satisfaction); last.setComments(comments);
			}

			boolean isDarwin = currentSession != null ? currentSession.isDarwinMode() : orchestrator.isDarwinMode();
			boolean isSelfDev = currentSession != null ? currentSession.isSelfIterativeMode() : (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null && orchestrator.getAiChat().getPromptInstructions().isSelfIterativeMode());
			String category = (isSelfDev || isDarwin) ? "coding" : "chat";

			// Centralized Feedback Persistence
			eu.kalafatic.evolution.controller.services.FeedbackService.getInstance().recordFeedback(orchestrator, category, satisfaction);

			eu.kalafatic.evolution.controller.manager.NeuronService.getInstance().train(orchestrator, comments, category, satisfaction);
			editor.setDirty(true); 
			updateModeDisplay();
			scheduleRefresh();
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("Thank You"); mb.setMessage("Your feedback has been recorded and will be used to improve the AI."); mb.open();
		}
	}

	public void provideApproval(boolean approved) {
		provideApproval(getCurrentSessionName(), approved);
	}

	public void provideApproval(String sessionId, boolean approved) {
		if (sessionId.equals(getCurrentSessionName())) {
			if (approved) {
				chatGroup.markLastWaitingAsApproved();
				instructionsGroup.resetBackground();
			}
			outputController.submitMessage(sessionId, currentTurnId != null ? currentTurnId : sessionId, "You", approved ? "Approved" : "Rejected", "user", MessagePriority.NORMAL, false);
		}

		String taskId = sessionId != null ? sessionId : orchestrator.getId();
		OrchestratorServiceImpl.getInstance().provideApproval(taskId, approved);

		if (sessionId.equals(getCurrentSessionName())) {
			updateModeDisplay();
			scheduleRefresh();
		}
	}

	public void handleReview() {
		editor.showApprovalPage();
	}

	public void handleClarify() {
		instructionsGroup.focusAndHighlight(colorLightOrange, null);
		chatGroup.focusWaitingMessage();
		expandFeedbackSection();
	}

	public void expandFeedbackSection() {
		if (feedbackGroup != null && !feedbackGroup.getGroup().isDisposed()) {
			org.eclipse.ui.forms.widgets.Section section = (org.eclipse.ui.forms.widgets.Section) feedbackGroup.getGroup().getParent();
			if (!section.isExpanded()) {
				section.setExpanded(true);
				updateScrolledContent();
			}
		}
	}

	public void handleQuote(String text) {
		if (text == null || text.isEmpty()) return;
		String current = instructionsGroup.getRequest();
		String quote = "> " + text.replace("\n", "\n> ") + "\n\n";
		instructionsGroup.setRequest(current + (current.isEmpty() ? "" : "\n\n") + quote);
		instructionsGroup.focusAndHighlight(colorWhite, null);
		instructionsGroup.setCaretToEnd();
	}

	public void handleOpenDiff(String path) {
		if (path == null || path.isEmpty()) return;

		if (path.startsWith("file://")) {
			path = path.substring(7);
			// On Windows, file:///C:/path/to/file -> /C:/path/to/file or C:/path/to/file
			if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
				path = path.substring(1);
			}
		}

		// Strip status prefix if present (e.g. "M src/File.java" -> "src/File.java")
		if (path.length() > 2 && (path.startsWith("M ") || path.startsWith("A ") || path.startsWith("D "))) {
		    path = path.substring(2);
		}

		File projectRoot = getProjectRoot();
		File file = path.contains(":") ? new File(path) : new File(projectRoot, path);

		// Ensure workspace is in sync before looking for the file
		try {
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(org.eclipse.core.resources.IResource.DEPTH_INFINITE, null);
		} catch (org.eclipse.core.runtime.CoreException e1) {
			// Ignore
		}

		if (file.exists()) {
			IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
			if (iFile != null) {
				final String finalPath = path;
				Display.getDefault().asyncExec(() -> {
					if (isDisposed()) return;
					editor.refreshNavigator(iFile);

					// Open the actual file editor
					try {
						IDE.openEditor(editor.getSite().getPage(), iFile);
					} catch (Exception e) {
						// Fallback if editor cannot be opened
					}

					editor.showComparePage(iFile);
					chatGroup.selectFile(finalPath);
				});
			}
		}
	}

	public void handleOpenMediatedEditor(String path) {
		if (path == null || path.isEmpty()) return;

		if (path.startsWith("file://")) {
			path = path.substring(7);
		}

		// On Windows, file:///C:/path -> /C:/path (after stripping file://)
		// We remove the leading / for Windows drive letters to get C:/path
		if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
			path = path.substring(1);
		}

		File projectRoot = getProjectRoot();
		File file = path.contains(":") ? new File(path) : new File(projectRoot, path);

		if (file.exists()) {
			IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
			if (iFile != null) {
				Display.getDefault().asyncExec(() -> {
					if (isDisposed()) return;
					try {
						IDE.openEditor(editor.getSite().getPage(), iFile, eu.kalafatic.evolution.view.editors.MediatedEditor.ID);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}
	}

	public void handleOpenAllFiles(String pathsCsv) {
		if (pathsCsv == null || pathsCsv.isEmpty()) return;
		String[] paths = pathsCsv.split(",");
		List<IFile> files = new ArrayList<>();
		File projectRoot = getProjectRoot();

		for (String path : paths) {
			File file = path.contains(":") ? new File(path) : new File(projectRoot, path);
			if (file.exists()) {
				IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(file.getAbsolutePath()));
				if (iFile != null) files.add(iFile);
			}
		}

		if (!files.isEmpty()) {
			Display.getDefault().asyncExec(() -> {
				try {
					IDE.openEditor(editor.getSite().getPage(),
						new eu.kalafatic.evolution.view.editors.FileCollectionEditorInput(files, "Collection of " + files.size() + " files"),
						eu.kalafatic.evolution.view.editors.FileCollectionEditor.ID);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
	}

	public void handleSimpleSolution() throws IOException {
		String sid = getCurrentSessionName();
		RuntimeProjection projection = ProjectionService.getInstance().getProjection(sid);
		if (projection.isRunning() && projection.isWaitingForUser()) {
			provideInput(sid, "Execute the simplest working solution.");
		} else {
			handleExecuteProposal("Execute the simplest working solution.");
		}
	}

	public void handleForceSolution() throws IOException {
		String sid = getCurrentSessionName();
		RuntimeProjection projection = ProjectionService.getInstance().getProjection(sid);
		if (projection.isRunning() && projection.isWaitingForUser()) {
			provideInput(sid, "Force Solution");
		} else {
			handleExecuteProposal("Force Solution");
		}
	}

	public void handleEditDarwinVariant(int index, String variantId, String text) {
		this.editingMessageIndex = index;
		this.editingVariantId = variantId;
		instructionsGroup.setRequest(text);
		instructionsGroup.focusAndHighlight(colorLightOrange, null);
		instructionsGroup.setCaretToEnd();
	}

	public void handleExecuteProposal(String request) throws IOException {
		instructionsGroup.setRequest(request);
		handleSend();
	}

	public void updateConfiguration(java.util.Map<String, Object> settings) {
		OrchestratorServiceImpl.getInstance().updateConfiguration(getCurrentSessionName(), settings);
	}

	public void handleFeedbackLevelChange(FeedbackLevel level) {
		if (orchestrator != null) {
			List<eu.kalafatic.evolution.model.orchestration.Task> tasks = orchestrator.getTasks();
			if (!tasks.isEmpty()) {
				tasks.get(0).setFeedbackLevel(level);
			}
			chatGroup.setFeedbackLevel(level);
			editor.setDirty(true);
			scheduleRefresh();
		}
	}

	private String getSessionId(eu.kalafatic.evolution.model.orchestration.Task task) {
		if (task == null) return "Default";
		if (task.eContainer() instanceof eu.kalafatic.evolution.model.orchestration.Task) {
			return getSessionId((eu.kalafatic.evolution.model.orchestration.Task) task.eContainer());
		}
		return task.getId() != null ? task.getId() : "Default";
	}

	public void runTask(Task task) throws IOException {
		if (task == null) return;

		// 1. Switch to thread or create one
		String sessionId = getSessionId(task);
		boolean exists = orchestrator.getAiChat().getSessions().stream()
				.anyMatch(t -> t.getId().equals(sessionId));
		if (!exists) {
			ChatSession newSession = OrchestrationFactory.eINSTANCE.createChatSession();
			newSession.setId(sessionId);
			orchestrator.getAiChat().getSessions().add(newSession);
		}
		switchSession(sessionId);
		updateSessionCombo();

		// 2. Set instructions
		String prompt = task.getPrompt();
		if (prompt == null || prompt.isEmpty()) prompt = task.getDescription();
		if (prompt == null || prompt.isEmpty()) prompt = task.getName();
		instructionsGroup.setRequest(prompt);

		// 3. Set mode
		instructionsGroup.setIterative(task.isIterativeMode());
		instructionsGroup.setSelfIterative(task.isSelfIterativeMode());
		instructionsGroup.setDarwin(task.isDarwinMode());
		instructionsGroup.setGitAutomation(task.isGitAutomation());
		instructionsGroup.setMaxIterations(task.getMaxIterations());
		instructionsGroup.setAutoApprove(!task.isApprovalRequired());

		if ("SELF_DEV_MODE".equals(task.getType())) {
			instructionsGroup.setSelfIterative(true);
		} else if ("DARWIN_MODE".equals(task.getType())) {
			instructionsGroup.setDarwin(true);
		} else if ("ASSISTED_CODING".equals(task.getType())) {
			instructionsGroup.setIterative(true);
		}
		instructionsGroup.updateModel();

		// 4. Send
		handleSend();
	}

	public void provideInput(String input) {
		provideInput(getCurrentSessionName(), input);
	}

	public void provideInput(String sessionId, String input) {
		if (sessionId.equals(getCurrentSessionName())) {
			if (assistAdapter != null) assistAdapter.closeProposalPopup();
			instructionsGroup.resetBackground();
			clearWaitingMessages();
			outputController.submitMessage(sessionId, currentTurnId != null ? currentTurnId : sessionId, "You", input, "user", MessagePriority.NORMAL, false);
		}

		String taskId = sessionId != null ? sessionId : orchestrator.getId();
		OrchestratorServiceImpl.getInstance().provideInput(taskId, input);

		if (sessionId.equals(getCurrentSessionName())) {
			updateModeDisplay();
			scheduleRefresh();
		}
	}

	private void clearWaitingMessages() {
		if (currentSession != null) {
			currentSession.getMessages().forEach(m -> {
				String agentType = m.getAgentType();
				if (agentType != null && agentType.contains("waiting")) {
					m.setAgentType(agentType.replace("waiting", "response").trim());
				}
			});
			chatGroup.scheduleRefresh();
		}
	}

	private void processLogEntry(String log) {
		processLogEntry(log, getCurrentSessionName());
	}

	public void handleLegacyLog(String sessionId, String log, Color color, int style) {
		processLogEntry(log, sessionId);
	}

	private void processLogEntry(String log, String sessionId) {
		if (log == null || log.isEmpty()) return;
		
		String trimmedText = log.trim();

		// Robust cleaning of contamination from standard Java/Evolution logs
		trimmedText = trimmedText.replaceAll("^[A-Z][a-z]{2} \\d{2}, \\d{4} \\d{1,2}:\\d{2}:\\d{2} [AP]M .*$", "") // Date markers
				.replaceAll("^INFO: ", "")
				.replaceAll("^WARNING: ", "")
				.replaceAll("^SEVERE: ", "")
				.trim();

		// Strip leading [Default], [KERNEL], etc. repeated tags that contaminate the sender detection
		while (trimmedText.startsWith("[") && trimmedText.contains("]")) {
			int end = trimmedText.indexOf("]");
			String tag = trimmedText.substring(1, end);
			if (tag.equals("Default") || tag.equals("KERNEL") || tag.equals("CONTEXT") || tag.equals("MEMORY") || tag.equals("PIPELINE") || tag.equals("LOOP")) {
				trimmedText = trimmedText.substring(end + 1).trim();
			} else {
				break;
			}
		}

		if (trimmedText.isEmpty()) return;

        String sender = "Evo";
        String content = trimmedText;
        String agentType = "ai";
        MessagePriority priority = MessagePriority.PROGRESS;

        java.util.regex.Pattern logPattern = java.util.regex.Pattern.compile("^([A-Z][A-Z0-9-]*)(?:\\s+\\[(.*?)\\])?(?:\\s+\\(\\d{2}:\\d{2}:\\d{2}\\))?:\\s*([\\s\\S]*)$", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = logPattern.matcher(trimmedText);

        if (matcher.find()) {
            sender = matcher.group(1);
            String extra = matcher.group(2);
            content = matcher.group(3);

            String senderUpper = sender.toUpperCase();
            if (senderUpper.startsWith("USER")) agentType = "user";
            else if (senderUpper.startsWith("EVO")) agentType = "ai";
            else if (senderUpper.startsWith("TOOL")) agentType = "tool";
            else if (senderUpper.startsWith("LLMROUTER")) agentType = "orchestrator";

            String agentSource = (sender + (extra != null ? "-" + extra : "")).toLowerCase();
            if (agentSource.contains("planner")) agentType = "planner";
            else if (agentSource.contains("architect")) agentType = "architect";
            else if (agentSource.contains("javadev")) agentType = "javadev";
            else if (agentSource.contains("tester")) agentType = "tester";
            else if (agentSource.contains("reviewer")) agentType = "reviewer";
            else if (agentSource.contains("analytic") || agentSource.contains("analysis")) agentType = "analytic";
            else if (agentSource.contains("general")) agentType = "general";
            else if (agentSource.contains("terminal")) agentType = "terminal";
            else if (agentSource.contains("file")) agentType = "file";
            else if (agentSource.contains("maven")) agentType = "maven";
            else if (agentSource.contains("git")) agentType = "git";
            else if (agentSource.contains("structure")) agentType = "structure";
            else if (agentSource.contains("websearch")) agentType = "websearch";
            else if (agentSource.contains("quality")) agentType = "quality";
            else if (agentSource.contains("orchestrator")) agentType = "orchestrator";
            else if (agentSource.contains("darwinengine")) agentType = "darwin";

            if (agentSource.contains("thinking")) agentType = "thinking";
            else if (agentSource.contains("response") && !agentType.equals("darwin")) {
		agentType = "response";
		priority = MessagePriority.NORMAL;
            }
        } else if (trimmedText.startsWith("Final Response: ")) {
            sender = "Final Response";
            content = trimmedText.substring(16);
            agentType = "final-response";
            priority = MessagePriority.FINAL;
        } else if (trimmedText.startsWith("Error: ")) {
            sender = "Error";
            content = trimmedText.substring(7);
            agentType = "error";
            priority = MessagePriority.FINAL;
        } else if (trimmedText.startsWith("Result Summary: ")) {
            sender = "Result Summary";
            content = trimmedText.substring(16);
            agentType = "result-summary";
            priority = MessagePriority.FINAL;
        }

        if (content.contains("[DARWIN_BRANCHES]")) {
            agentType = "darwin-branches waiting";
            content = content.replace("[DARWIN_BRANCHES]", "").trim();
            priority = MessagePriority.USER_ACTION_REQUIRED;
        }

        java.util.regex.Pattern approvedPattern = java.util.regex.Pattern.compile("\\[(APPROVED|REJECTED|KEPT):([^]]+)\\]");
        java.util.regex.Matcher approvedMatcher = approvedPattern.matcher(content);
        if (approvedMatcher.find()) {
            String status = approvedMatcher.group(1).toLowerCase();
            String variantId = approvedMatcher.group(2);
            if (!agentType.contains(status)) {
                agentType = agentType.replace("waiting", "").trim();
                if (agentType.isEmpty()) agentType = "ai";
                agentType += " " + status + ":" + variantId;
            }
            content = content.replace(approvedMatcher.group(0), "").trim();

            // If this was a darwin-branches message that is now resolved,
            // we strip the JSON to keep the history clean, as the UI already rendered the selection.
            if (agentType.contains("darwin-branches")) {
                 content = content.replaceAll("\\[[\\s\\S]*\\]", "").trim();
                 if (content.isEmpty()) content = "Variant " + variantId + " " + status + ".";
            }

            priority = MessagePriority.NORMAL;
        }

        boolean needsApproval = (content.toLowerCase().contains("waiting for user") ||
                content.toLowerCase().contains("guidance?") ||
                content.toLowerCase().contains("clarify") ||
                content.toLowerCase().contains("clarification") ||
                content.contains("[PROPOSAL:") ||
                content.toLowerCase().contains("ambiguous") ||
                content.toLowerCase().contains("approve") ||
                content.toLowerCase().contains("approval") ||
                content.toLowerCase().contains("proceed?")) &&
                !content.contains("AUTO_INFER") &&
                !content.contains("BRANCH_PARALLEL") &&
                !content.contains("Interpretation State: CLEAR");

        if (needsApproval && !agentType.contains("user")) {
		if (!agentType.contains("waiting")) agentType += " waiting";
            priority = MessagePriority.USER_ACTION_REQUIRED;
        }

        // Clean up technical markers for human-readability
        content = content.replaceAll("\\[KERNEL\\]", "")
                        .replaceAll("\\[STRATEGY\\]", "")
                        .replaceAll("\\[ANALYSIS\\]", "")
                        .replaceAll("\\[DIAGNOSIS\\]", "")
                        .replaceAll("\\[SUPERVISOR\\]", "")
                        .replaceAll("\\[EVO\\]", "")
                        .replaceAll("\\[DARWIN\\]", "")
                        .replaceAll("\\[DARWINENGINE\\]", "")
                        .replaceAll("\\[THINKING\\]", "")
                        .replaceAll("\\[ORCHESTRATOR\\]", "")
                        .trim();

        outputController.submitMessage(sessionId, currentTurnId != null ? currentTurnId : sessionId, sender, content, agentType, priority, priority == MessagePriority.FINAL);
	}

	public String getCurrentSessionName() { return currentSession != null ? currentSession.getId() : "Default"; }

	public ChatSession getCurrentSession() { return currentSession; }

	public MultiPageEditor getEditor() { return editor; }

	public FormToolkit getToolkit() { return toolkit; }

	/**
	 * @evo:14:A reason=categorized-assist
	 */
	private String getCategory() {
		if (instructionsGroup != null && instructionsGroup.getGroup() != null && !instructionsGroup.getGroup().isDisposed()) {
			if (instructionsGroup.isSelfIterative() || instructionsGroup.isIterative() || instructionsGroup.isDarwin()) return "coding";
		} else if (currentSession != null) {
			if (currentSession.isSelfIterativeMode() || currentSession.isIterativeMode() || currentSession.isDarwinMode()) return "coding";
		} else if (orchestrator != null) {
			if (orchestrator.isDarwinMode()) return "coding";
		}
		return "chat";
	}

	public void setupContextAssist(StyledText text) {
		// Ensure NeuronService is initialized early
		NeuronService.getInstance();

		if (orchestrator != null && orchestrator.getNeuronAI() == null) {
			orchestrator.setNeuronAI(eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createNeuronAI());
		}

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
			String category = getCategory();
			String[] neuronProposals = NeuronService.getInstance().getProposals(orchestrator, finalPrefix, category);
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
			@Override public void setControlContents(org.eclipse.swt.widgets.Control control, String contents, int cursorPosition) {
				StyledText st = (StyledText) control;
				st.setText(contents);
				st.setSelection(cursorPosition);
			}
			@Override public void insertControlContents(org.eclipse.swt.widgets.Control control, String contents, int cursorPosition) {
				StyledText st = (StyledText) control;
				String textContent = st.getText();
				int selectionStart = st.getCaretOffset();
				int wordStart = selectionStart;
				while (wordStart > 0 && !Character.isWhitespace(textContent.charAt(wordStart - 1))) {
					wordStart--;
				}
				st.replaceTextRange(wordStart, selectionStart - wordStart, contents);
				st.setSelection(wordStart + cursorPosition);
				st.setFocus();
			}
			@Override public String getControlContents(org.eclipse.swt.widgets.Control control) { return ((StyledText) control).getText(); }
			@Override public int getCursorPosition(org.eclipse.swt.widgets.Control control) { return ((StyledText) control).getCaretOffset(); }
			@Override public org.eclipse.swt.graphics.Rectangle getInsertionBounds(org.eclipse.swt.widgets.Control control) { return ((StyledText) control).getBounds(); }
			@Override public void setCursorPosition(org.eclipse.swt.widgets.Control control, int index) { ((StyledText) control).setSelection(index); }
		};
		KeyStroke ks = null; try { ks = KeyStroke.getInstance("Ctrl+Space"); } catch (Exception e) {}
		assistAdapter = new ContentProposalAdapter(text, contentAdapter, proposalProvider, ks, null);
		assistAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
		assistAdapter.setAutoActivationDelay(100);
		assistAdapter.setAutoActivationCharacters("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ/".toCharArray());
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
					saveLastUsedSettings();
					MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("AI Connection Success"); mb.setMessage("Connected to AI provider successfully and settings saved.\nResponse: " + response); mb.open();
				});
			} catch (Exception ex) {
				Display.getDefault().asyncExec(() -> { if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("AI Connection Failed"); mb.setMessage("Error connecting to AI provider (settings NOT saved): " + ex.getMessage()); mb.open(); });
			}
		}).start();
	}
}
