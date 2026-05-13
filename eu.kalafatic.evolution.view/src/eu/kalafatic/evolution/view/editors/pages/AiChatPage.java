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
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.ResultType;
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
import eu.kalafatic.evolution.model.orchestration.ChatSession;
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
import eu.kalafatic.evolution.model.orchestration.FeedbackLevel;
import java.util.List;
import java.util.stream.Collectors;
import eu.kalafatic.evolution.controller.orchestration.ModeRouter;
import eu.kalafatic.evolution.controller.orchestration.PlatformMode;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import org.eclipse.jface.dialogs.IDialogSettings;
import eu.kalafatic.evolution.view.application.Activator;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.WorkflowStepRegistry;
import eu.kalafatic.evolution.controller.workflow.WorkflowStep;
import eu.kalafatic.evolution.controller.workflow.StepModeController;
import eu.kalafatic.evolution.controller.workflow.WorkflowStatus;

/**
 * @evo:16:A reason=darwin-mode-sync
 */
public class AiChatPage extends AEvoPage implements RuntimeEventListener {
	private boolean isUpdating = false;
	private Label modeIndicatorLabel;
	private ContentProposalAdapter assistAdapter;
	private OllamaService ollamaService;
	private ChatSession currentSession;
	private Composite content;
	private long lastStatusUpdate = 0;

	private ChatMgmtGroup chatMgmtGroup;
	private AiSettingsGroup aiSettingsGroup;
	private InstructionsGroup instructionsGroup;
	private ChatGroup chatGroup;
	private SystemStatusGroup systemStatusGroup;
	private FeedbackGroup feedbackGroup;

	private int editingMessageIndex = -1;
	private String editingVariantId = null;

	private static class SessionState {
		Thread orchestrationSession;
		TaskContext currentContext;
		String activeTaskId;
		eu.kalafatic.evolution.model.orchestration.Task currentStackTask;
		boolean isRunning = false;
		boolean isPaused = false;
	}

	private java.util.Map<String, SessionState> sessionStates = new java.util.HashMap<>();

	private SessionState getSessionState(String sessionId) {
		return sessionStates.computeIfAbsent(sessionId, k -> new SessionState());
	}

	private SessionState getCurrentSessionState() {
		return getSessionState(getCurrentSessionName());
	}

	private Color colorUser, colorEvolution, colorPlanner, colorArchitect, colorJavaDev, colorTester, colorReviewer, colorError, colorWhite, colorLocal, colorHybrid, colorRemote, colorWaiting, colorLightOrange;
	private Font chatFont, bannerFont;
	private Color lightGreen;

	public AiChatPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, editor, orchestrator);
		initResources();
		createControl();
		RuntimeEventBus.getInstance().subscribe(this);
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				RuntimeEventBus.getInstance().unsubscribe(AiChatPage.this);
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
		colorLightOrange = new Color(display, 255, 200, 150);
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

		feedbackGroup = new FeedbackGroup(toolkit, content, editor, orchestrator, this);

		initializeSessions();

		Runnable timer = new Runnable() {
			public void run() {
				Display.getDefault().asyncExec(() -> {
					if (!content.isDisposed()) {
						String id = orchestrator != null ? orchestrator.getId() : null;
						if (id != null) {
							double progress = OrchestrationStatusManager.getInstance().getProgress(id);
							String status = OrchestrationStatusManager.getInstance().getStatus(id);
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
		
		content.getParent().layout(true, true);
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
		case MEDIATED: modeIndicatorLabel.setBackground(colorHybrid); break;
		}
		if (aiSettingsGroup != null) {
			updateScrolledContent();
		}
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
		if (aiSettingsGroup.getRemoteToken() != null) section.put("RemoteToken_" + orchestrator.getRemoteModel(), aiSettingsGroup.getRemoteToken());
		if (aiSettingsGroup.getRemoteUrl() != null) section.put("RemoteUrl_" + orchestrator.getRemoteModel(), aiSettingsGroup.getRemoteUrl());
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
	}

	public void syncModelWithUI() {
		if (orchestrator == null || isUpdating) return;
		isUpdating = true;
		orchestrator.setAiMode(AiMode.get(aiSettingsGroup.getAiModeIndex()));

		String localModel = aiSettingsGroup.getLocalModel();
		orchestrator.setLocalModel(localModel);
		orchestrator.setHybridModel(localModel);
		if (orchestrator.getOllama() != null) {
			orchestrator.getOllama().setModel(localModel);
		}

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
        orchestrator.setDarwinMode(instructionsGroup.isDarwin());

        if (currentSession != null) {
            currentSession.setIterativeMode(instructionsGroup.isIterative());
            currentSession.setSelfIterativeMode(instructionsGroup.isSelfIterative());
            currentSession.setDarwinMode(instructionsGroup.isDarwin());
            currentSession.setGitAutomation(instructionsGroup.isGitAutomationCheck());
            currentSession.setMaxIterations(instructionsGroup.getMaxIterations());
            currentSession.setStepMode(instructionsGroup.isStepMode());
        }

        boolean wasAutoApprove = promptInstructions.isAutoApprove();
        boolean isAutoApprove = instructionsGroup.isAutoApprove();
        promptInstructions.setAutoApprove(isAutoApprove);
        promptInstructions.setPreferredMaxIterations(instructionsGroup.getMaxIterations());
        promptInstructions.setGitAutomation(instructionsGroup.isGitAutomationCheck());
        promptInstructions.setStepMode(instructionsGroup.isStepMode());
		
		if (!wasAutoApprove && isAutoApprove) {
			resumeWaitingSessions();
		}

		orchestrator.getAiChat().setUrl(aiSettingsGroup.getRemoteUrl());
		saveLastUsedSettings();
		editor.setDirty(true);
		updateModeDisplay();
		isUpdating = false;
	}

	public void handleSend() {
		instructionsGroup.resetBackground();
		String request = instructionsGroup.getRequest();
		SessionState state = getCurrentSessionState();

		// Check for active steps in Step Mode - allow resumption even if command is already running
		WorkflowStep activeStep = WorkflowStepRegistry.getInstance().getActiveStepForSession(getCurrentSessionName());
		if (activeStep != null && activeStep.getStatus() == WorkflowStatus.WAITING_USER) {
			String lower = request.toLowerCase().trim();
			if (lower.equals("retry")) {
				instructionsGroup.setOrchestrationRunning(true);
				StepModeController.getInstance().resumeStep(activeStep.getId(), WorkflowStatus.RETRY);
				instructionsGroup.setRequest("");
				return;
			} else if (lower.equals("skip")) {
				instructionsGroup.setOrchestrationRunning(true);
				StepModeController.getInstance().resumeStep(activeStep.getId(), WorkflowStatus.SKIPPED);
				instructionsGroup.setRequest("");
				return;
			} else {
				// Treat any other input (empty, "next", or random comments) as "CONTINUE"
				instructionsGroup.setOrchestrationRunning(true);
				StepModeController.getInstance().resumeStep(activeStep.getId(), WorkflowStatus.COMPLETED);
				instructionsGroup.setRequest("");
				return;
			}
		}

		// Check if we are waiting for user input or approval and unblock via chat if possible
		boolean isWaiting = false;
		if (state.currentContext != null) {
			isWaiting = state.currentContext.isWaitingForInput() || state.currentContext.isWaitingForApproval();
		} else if (state.activeTaskId != null) {
			TaskResult result = OrchestratorServiceImpl.getInstance().getTaskResult(state.activeTaskId);
			if (result != null) {
				isWaiting = result.getStatus() == TaskResult.Status.WAITING_FOR_INPUT || result.getStatus() == TaskResult.Status.WAITING_FOR_APPROVAL;
			}
		}

		if (isWaiting) {
			String lower = request.toLowerCase().trim();

			if (editingVariantId != null && request.toUpperCase().startsWith("EDIT PROPOSAL")) {
				instructionsGroup.setOrchestrationRunning(true);
				provideInput(request);
				chatGroup.handleApproveDarwinVariant(editingMessageIndex, editingVariantId);
				editingVariantId = null;
				editingMessageIndex = -1;
				instructionsGroup.setRequest("");
				return;
			}

			boolean isApproval = false;
			if (state.currentContext != null) isApproval = state.currentContext.isWaitingForApproval();
			else {
				TaskResult result = OrchestratorServiceImpl.getInstance().getTaskResult(state.activeTaskId);
				if (result != null) isApproval = result.getStatus() == TaskResult.Status.WAITING_FOR_APPROVAL;
			}

			if (isApproval) {
				if (lower.matches("^(yes|y|ok|okay|approve|proceed|go ahead|yep|sure)$") || lower.contains("approve variant")) {
					instructionsGroup.setOrchestrationRunning(true);
					provideApproval(true);
					instructionsGroup.setRequest("");
					return;
				} else if (lower.matches("^(no|n|reject|stop|cancel|abort)$")) {
					instructionsGroup.setOrchestrationRunning(true);
					provideApproval(false);
					instructionsGroup.setRequest("");
					return;
				}
			}

			instructionsGroup.setOrchestrationRunning(true);
			provideInput(request);
			instructionsGroup.setRequest("");
			return;
		}

		if (state.isRunning) return; // Prevent duplicate sessions for same ID

		if (request.isEmpty()) return;

		if (currentSession == null) initializeSessions();

		// --- FAST MODE ROUTING: Determine if this is a simple chat or atomic task request before starting Self-Dev/Darwin ---
		ModeRouter modeRouter = new ModeRouter();
		PlatformMode detectedMode = modeRouter.routeFast(request, orchestrator);
		boolean isSimpleChat = (detectedMode != null && detectedMode.getType() == PlatformType.SIMPLE_CHAT);
		boolean isMediated = (detectedMode != null && detectedMode.getType() == PlatformType.HYBRID_MANUAL_EXPORT) || (orchestrator != null && orchestrator.getAiMode() == AiMode.MEDIATED);
		boolean isAtomicTask = eu.kalafatic.evolution.controller.orchestration.IterationManager.isSimpleFileCreate(request);

		// Start Self-Dev Supervisor if (Self-Development OR Darwin mode is enabled) AND it's NOT a simple chat, atomic task OR mediated.
		if (!isSimpleChat && !isAtomicTask && !isMediated && orchestrator != null && orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null &&
		    (orchestrator.getAiChat().getPromptInstructions().isSelfIterativeMode() || orchestrator.isDarwinMode())) {
			startSelfDevAction(request);
			return;
		}
		
		if (orchestrator != null) {
			if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
			if (orchestrator.getLlm() == null) orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
			String category = getCategory();
			NeuronService.getInstance().train(orchestrator, request, category, 3);
			editor.setDirty(true);
			if (orchestrator.getId() == null || orchestrator.getId().isEmpty()) orchestrator.setId("chat-" + System.currentTimeMillis());
		}
		if (assistAdapter != null) assistAdapter.closeProposalPopup();
		chatGroup.appendText("You: " + request, colorUser, SWT.BOLD);
		chatGroup.appendText("\n\nEvo: Initializing orchestration...", colorEvolution, SWT.ITALIC);
		instructionsGroup.setRequest("");
		state.isRunning = true;
		instructionsGroup.setOrchestrationRunning(true);
		chatGroup.setThinking(true);
		chatGroup.resetLogCount();

		TaskRequest taskRequest = new TaskRequest(request, getProjectRoot());
		taskRequest.getContext().put("orchestrator", orchestrator);
		String sessionId = getCurrentSessionName();
		taskRequest.getContext().put("sessionId", sessionId);

		TaskContext context = new TaskContext(orchestrator, getProjectRoot());
		context.setStartTime(java.time.Instant.now());
		context.setSessionId(sessionId);
		state.currentContext = context;
		editor.setCurrentContext(context);

		context.addLogListener(log -> Display.getDefault().asyncExec(() -> {
			if (!chatGroup.isDisposed()) {
				processLogEntry(log, sessionId);
				if (sessionId.equals(getCurrentSessionName())) chatGroup.incrementLogCount();
			}
		}));
		context.addApprovalListener(msg -> Display.getDefault().asyncExec(() -> {
			if (!sessionId.equals(getCurrentSessionName())) return;
			if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
				modeIndicatorLabel.setText("WAITING FOR USER APPROVAL...");
				modeIndicatorLabel.setBackground(colorWaiting);
			}
			chatGroup.markLastAiMessageAsWaiting();
			instructionsGroup.setOrchestrationRunning(false);
			handleClarify();
			feedbackGroup.showApproval(msg); updateScrolledContent();
		}));
		context.addInputListener(msg -> Display.getDefault().asyncExec(() -> {
			if (!sessionId.equals(getCurrentSessionName())) return;
			if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
				modeIndicatorLabel.setText("WAITING FOR USER INPUT...");
				modeIndicatorLabel.setBackground(colorWaiting);
			}
			chatGroup.markLastAiMessageAsWaiting();
			instructionsGroup.setOrchestrationRunning(false);
			handleClarify();
			feedbackGroup.showInput(msg); updateScrolledContent();
		}));

		taskRequest.getContext().put("taskContext", context);

		state.orchestrationSession = new Thread(() -> {
			try {
				eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse response = OrchestratorServiceImpl.getInstance().handle(taskRequest);

				Display.getDefault().asyncExec(() -> {
					if (sessionId.equals(getCurrentSessionName())) instructionsGroup.resetBackground();

					if (response.getResultType() == eu.kalafatic.evolution.controller.orchestration.ResultType.ERROR) {
						if (state.currentStackTask != null) {
							state.currentStackTask.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
							state.currentStackTask.setResultSummary("Error: " + response.getSummary());
						}
						if (sessionId.equals(getCurrentSessionName()) && !chatGroup.isDisposed()) {
							chatGroup.setThinking(false);
							chatGroup.appendText("\n\nError: " + response.getContent(), colorError, SWT.BOLD);
						}
						return;
					}

					if (state.currentStackTask != null) {
						state.currentStackTask.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE);
						state.currentStackTask.setResultSummary(response.getSummary());
					}

					if (sessionId.equals(getCurrentSessionName()) && !chatGroup.isDisposed()) {
						chatGroup.setThinking(false);

						String finalMsg = response.getContent() != null ? response.getContent() : response.getSummary();
						chatGroup.appendText(finalMsg, colorEvolution, SWT.NORMAL);

						editor.setDirty(true);
						feedbackGroup.showSatisfaction(true); updateScrolledContent();
					} else {
						ChatSession targetSession = orchestrator.getAiChat().getSessions().stream().filter(t -> t.getId().equals(sessionId)).findFirst().orElse(null);
						if (targetSession != null) {
							String finalMsg = response.getContent() != null ? response.getContent() : response.getSummary();
							chatGroup.appendTextToSession(targetSession, finalMsg, colorEvolution, SWT.NORMAL);
						}
					}
				});
			} catch (Exception e) {
				Display.getDefault().asyncExec(() -> {
					if (state.currentStackTask != null) {
						state.currentStackTask.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
						state.currentStackTask.setResultSummary("Error: " + e.getMessage());
					}
					if (sessionId.equals(getCurrentSessionName()) && !chatGroup.isDisposed()) {
						chatGroup.setThinking(false);
						chatGroup.appendText("\n\n", colorWhite, SWT.NORMAL);
						chatGroup.appendText("Error: " + (e instanceof InterruptedException ? "Orchestration stopped by user." : e.getMessage()), colorError, SWT.BOLD);
					}
				});
			} finally {
				Display.getDefault().asyncExec(() -> {
					state.isRunning = false;
					if (sessionId.equals(getCurrentSessionName())) {
						instructionsGroup.setOrchestrationRunning(false);
								chatGroup.refreshGitStatus();

								// Force workspace refresh to show new files in navigator
								try {
									ResourcesPlugin.getWorkspace().getRoot().refreshLocal(org.eclipse.core.resources.IResource.DEPTH_INFINITE, null);

									// Identify newly created files and highlight in navigator
									eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider git = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
									List<String> changed = git.getChangedFiles(getProjectRoot(), "HEAD");
									for (String path : changed) {
										if (path.startsWith("A ")) {
											String filePath = path.substring(2);
											IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new org.eclipse.core.runtime.Path(new File(getProjectRoot(), filePath).getAbsolutePath()));
											if (iFile != null) {
												editor.refreshNavigator(iFile);
											}
										}
									}
								} catch (Exception e) {}
					}
					state.orchestrationSession = null;
				});
			}
		});
		state.orchestrationSession.start();
	}

	public void handlePause() {
		SessionState state = getCurrentSessionState();
		if (state.currentContext != null) {
			state.isPaused = !state.currentContext.isPaused();
			state.currentContext.setPaused(state.isPaused);
			instructionsGroup.setPaused(state.isPaused);
			chatGroup.setThinking(!state.isPaused);
		}
	}

	public void handleStop() {
		instructionsGroup.resetBackground();
		SessionState state = getCurrentSessionState();
		if (state.orchestrationSession != null && state.orchestrationSession.isAlive()) {
			if (state.currentContext != null) state.currentContext.setPaused(false);
			state.orchestrationSession.interrupt();
			state.isRunning = false;
			instructionsGroup.setOrchestrationRunning(false);
		}
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

			// Pre-select model from last used settings
			loadLastUsedSettings();
			refreshUI();

			orchestrator.getAiChat().getSessions().add(newSession);
			currentSession = newSession;
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

					SessionState state = getSessionState(sessionId);
					instructionsGroup.setOrchestrationRunning(state.isRunning);
					instructionsGroup.setPaused(state.isPaused);
					chatGroup.setThinking(state.isRunning && !state.isPaused);

					updateModeDisplay();

					// Re-check status if running
					if (state.isRunning && state.activeTaskId != null) {
						TaskResult result = OrchestratorServiceImpl.getInstance().getTaskResult(state.activeTaskId);
						if (result != null) {
							if (result.getStatus() == TaskResult.Status.WAITING_FOR_APPROVAL) {
								feedbackGroup.showApproval(result.getWaitingMessage());
							} else if (result.getStatus() == TaskResult.Status.WAITING_FOR_INPUT) {
								feedbackGroup.showInput(result.getWaitingMessage());
							}
						}
					}
					updateScrolledContent();
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
		SessionState state = getCurrentSessionState();
		if (state.isRunning) return;

		instructionsGroup.resetBackground();
		if (request == null || request.isEmpty()) request = "Analyze the project and suggest improvements.";
		if (state.currentContext != null && state.currentContext.isWaitingForInput()) {
			provideInput(request);
			instructionsGroup.setRequest("");
			return;
		}
		final String finalRequest = request;
		boolean isSelfDev = orchestrator != null && orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null && orchestrator.getAiChat().getPromptInstructions().isSelfIterativeMode();
		boolean isDarwin = orchestrator != null && orchestrator.isDarwinMode();
		final String modeLabel = isDarwin ? "DARWIN" : (isSelfDev ? "SELF-DEV" : "EVO");
		String idPrefix = isSelfDev ? "selfdev-" : (isDarwin ? "darwin-" : "chat-");

		if (orchestrator != null) {
			if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
			if (orchestrator.getLlm() == null) orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
			NeuronService.getInstance().train(orchestrator, finalRequest, "coding", 5);
			if (orchestrator.getId() == null || orchestrator.getId().isEmpty()) orchestrator.setId(idPrefix + System.currentTimeMillis());
		}
		chatGroup.appendText("User [" + modeLabel + "]: " + finalRequest, colorUser, SWT.BOLD);
		String loopSuffix = (isSelfDev) ? " Supervisor loop" : " loop";
		chatGroup.appendText("\n\nEvo: Initializing " + modeLabel + loopSuffix + "...", colorEvolution, SWT.ITALIC | SWT.BOLD);
		instructionsGroup.setRequest("");
		state.isRunning = true;
		instructionsGroup.setOrchestrationRunning(true);
		chatGroup.setThinking(true);
		String sessionId = getCurrentSessionName();
		final File projectRoot = getProjectRoot();
		state.orchestrationSession = new Thread(() -> {
			try {
				TaskContext context = new TaskContext(orchestrator, projectRoot);
				context.setStartTime(java.time.Instant.now());
				context.setSessionId(sessionId);
				context.getInstructionFiles().addAll(instructionsGroup.getInstructionFiles());
				context.setPlatformMode(new eu.kalafatic.evolution.controller.orchestration.ModeRouter().route(finalRequest, orchestrator));
				state.currentContext = context;
				Display.getDefault().asyncExec(() -> editor.setCurrentContext(context));
				context.addLogListener(log -> Display.getDefault().asyncExec(() -> { if (!chatGroup.isDisposed()) processLogEntry(log, sessionId); }));
				context.addApprovalListener(message -> Display.getDefault().asyncExec(() -> {
					if (!sessionId.equals(getCurrentSessionName())) return;
					if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
						modeIndicatorLabel.setText("WAITING FOR USER APPROVAL...");
						modeIndicatorLabel.setBackground(colorWaiting);
					}
					chatGroup.markLastAiMessageAsWaiting();
					instructionsGroup.setOrchestrationRunning(false);
					handleClarify();
					feedbackGroup.showApproval(message); updateScrolledContent();
				}));
				context.addInputListener(message -> Display.getDefault().asyncExec(() -> {
					if (!sessionId.equals(getCurrentSessionName())) return;
					if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
						modeIndicatorLabel.setText("WAITING FOR USER INPUT...");
						modeIndicatorLabel.setBackground(colorWaiting);
					}
					chatGroup.markLastAiMessageAsWaiting();
					instructionsGroup.setOrchestrationRunning(false);
					handleClarify();
					feedbackGroup.showInput(message); updateScrolledContent();
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

				// Assembly Final Response for Supervisor sessions too
				eu.kalafatic.evolution.controller.orchestration.FinalResponseAssembler assembler = new eu.kalafatic.evolution.controller.orchestration.FinalResponseAssembler();
				eu.kalafatic.evolution.controller.orchestration.FinalResponse finalResponse = assembler.assemble(context, modeLabel + " session finished. Status: " + session.getStatus(), session.getStatus() == eu.kalafatic.evolution.model.orchestration.SelfDevStatus.COMPLETED, context.getStartTime());

				Display.getDefault().asyncExec(() -> {
					if (sessionId.equals(getCurrentSessionName())) instructionsGroup.resetBackground();
					if (state.currentStackTask != null) {
						state.currentStackTask.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE);
						state.currentStackTask.setResultSummary("Self-Development session finished.");
					}
					if (sessionId.equals(getCurrentSessionName()) && !chatGroup.isDisposed()) {
						chatGroup.setThinking(false);

						chatGroup.appendText(finalResponse.toString(), colorEvolution, SWT.NORMAL);
						editor.setDirty(true);
						feedbackGroup.showSatisfaction(true); updateScrolledContent();
						if (state.currentStackTask != null) {
							state.currentStackTask.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.DONE);
							state.currentStackTask.setResultSummary("Self-Development session finished. Status: " + session.getStatus());
						}
					}
				});
			} catch (Exception e) {
				Display.getDefault().asyncExec(() -> {
					if (state.currentStackTask != null) {
						state.currentStackTask.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.FAILED);
						state.currentStackTask.setResultSummary("Supervisor Error: " + e.getMessage());
					}
					if (sessionId.equals(getCurrentSessionName()) && !chatGroup.isDisposed()) {
						chatGroup.setThinking(false);
						chatGroup.appendText("Supervisor Error: " + (e instanceof InterruptedException ? "Orchestration stopped by user." : e.getMessage()), colorError, SWT.BOLD);
					}
				});
			} finally {
				Display.getDefault().asyncExec(() -> {
					state.isRunning = false;
					if (sessionId.equals(getCurrentSessionName())) {
						instructionsGroup.setOrchestrationRunning(false);
							chatGroup.refreshGitStatus();

							// Force workspace refresh to show new files in navigator
							try {
								ResourcesPlugin.getWorkspace().getRoot().refreshLocal(org.eclipse.core.resources.IResource.DEPTH_INFINITE, null);

								// Identify newly created files and highlight in navigator
								eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider git = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
								List<String> changed = git.getChangedFiles(projectRoot, "HEAD");
								for (String path : changed) {
									if (path.startsWith("A ")) {
										String filePath = path.substring(2);
										IFile iFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new org.eclipse.core.runtime.Path(new File(projectRoot, filePath).getAbsolutePath()));
										if (iFile != null) {
											editor.refreshNavigator(iFile);
										}
									}
								}
							} catch (Exception e) {}
					}
					state.orchestrationSession = null;
				});
			}
		});
		state.orchestrationSession.start();
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
		if (orchestrator != null && orchestrator.getOllama() != null) {
			String url = orchestrator.getOllama().getUrl(); String model = orchestrator.getOllama().getModel();
			ollamaService = OllamaManager.getInstance().getService(url);
			float temp = orchestrator.getLlm() != null ? orchestrator.getLlm().getTemperature() : 0.7f;
			ollamaService.setTemperature(temp);
			if (model != null) ollamaService.setModel(model);

			systemStatusGroup.updateModelStatus(model != null ? model : "Not Configured");
			new Thread(() -> {
				boolean isOnline = ollamaService.ping();
				Display.getDefault().asyncExec(() -> {
					if (!systemStatusGroup.isDisposed()) {
						systemStatusGroup.updateOllamaStatus((isOnline ? "Online (" : "Offline (") + url + ")", Display.getDefault().getSystemColor(isOnline ? SWT.COLOR_DARK_GREEN : SWT.COLOR_RED));
					}
				});
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
			if (feedbackGroup != null) feedbackGroup.updateUI();
			updateStatusInfo();
			updateModeDisplay();
			isUpdating = false;
		}
	}

	public void updateUI() {
		scheduleRefresh();
	}

	private void resumeWaitingSessions() {
		for (String sessionId : sessionStates.keySet()) {
			SessionState state = sessionStates.get(sessionId);
			if (state != null) {
				if (state.currentContext != null) {
					if (state.currentContext.isWaitingForInput()) {
						provideInput(sessionId, "Approved");
					} else if (state.currentContext.isWaitingForApproval()) {
						provideApproval(sessionId, true);
					}
				}
				if (state.activeTaskId != null) {
					// Check if TaskResult is waiting for approval (legacy OrchestratorServiceImpl tasks)
					TaskResult result = OrchestratorServiceImpl.getInstance().getTaskResult(state.activeTaskId);
					if (result != null && (result.getStatus() == TaskResult.Status.WAITING_FOR_APPROVAL || result.getStatus() == TaskResult.Status.WAITING_FOR_INPUT)) {
						if (result.getStatus() == TaskResult.Status.WAITING_FOR_APPROVAL) {
							provideApproval(sessionId, true);
						} else {
							provideInput(sessionId, "Approved");
						}
					}
				}
			}
		}
	}

	public void submitFeedback(int satisfaction, String comments) {
		if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
			if (orchestrator.getSelfDevSession().getIterations().isEmpty()) orchestrator.getSelfDevSession().getIterations().add(OrchestrationFactory.eINSTANCE.createIteration());
			eu.kalafatic.evolution.model.orchestration.Iteration last = orchestrator.getSelfDevSession().getIterations().get(orchestrator.getSelfDevSession().getIterations().size() - 1);
			if (last.getEvaluationResult() == null) last.setEvaluationResult(OrchestrationFactory.eINSTANCE.createEvaluationResult());
			last.getEvaluationResult().setUserSatisfaction(satisfaction); last.setComments(comments);
			NeuronService.getInstance().train(orchestrator, comments, "coding", satisfaction);
			editor.setDirty(true); feedbackGroup.showSatisfaction(false);
			updateModeDisplay();
			updateScrolledContent();
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("Thank You"); mb.setMessage("Your feedback has been recorded and will be used to improve the AI."); mb.open();
		}
	}

	public void provideApproval(boolean approved) {
		provideApproval(getCurrentSessionName(), approved);
	}

	public void provideApproval(String sessionId, boolean approved) {
		if (approved && sessionId.equals(getCurrentSessionName())) {
			chatGroup.markLastWaitingAsApproved();
			instructionsGroup.resetBackground();
		}
		SessionState state = getSessionState(sessionId);
		if (state.orchestrationSession != null) {
			String taskId = state.activeTaskId != null ? state.activeTaskId : orchestrator.getId();
			OrchestratorServiceImpl.getInstance().provideApproval(taskId, approved);
		}
		if (state.currentContext != null) {
			state.currentContext.provideApproval(approved);
			if (sessionId.equals(getCurrentSessionName())) {
				feedbackGroup.hideApproval();
				updateModeDisplay();
				updateScrolledContent();
			}
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

		// Strip status prefix if present (e.g. "M src/File.java" -> "src/File.java")
		if (path.length() > 2 && (path.startsWith("M ") || path.startsWith("A ") || path.startsWith("D "))) {
		    path = path.substring(2);
		}

		File projectRoot = getProjectRoot();
		File file = new File(projectRoot, path);

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
					editor.refreshNavigator(iFile);
					editor.showComparePage(iFile);
					chatGroup.selectFile(finalPath);
				});
			}
		}
	}

	public void handleSimpleSolution() {
		handleExecuteProposal("Execute the simplest working solution.");
	}

	public void handleEditDarwinVariant(int index, String variantId, String text) {
		this.editingMessageIndex = index;
		this.editingVariantId = variantId;
		instructionsGroup.setRequest(text);
		instructionsGroup.focusAndHighlight(colorLightOrange, null);
		instructionsGroup.setCaretToEnd();
	}

	public void handleExecuteProposal(String request) {
		instructionsGroup.setRequest(request);
		handleSend();
	}

	public void handleFeedbackLevelChange(FeedbackLevel level) {
		if (orchestrator != null) {
			List<eu.kalafatic.evolution.model.orchestration.Task> tasks = orchestrator.getTasks();
			if (!tasks.isEmpty()) {
				tasks.get(0).setFeedbackLevel(level);
			}
			chatGroup.setFeedbackLevel(level);
			editor.setDirty(true);
			updateScrolledContent();
		}
	}

	private String getSessionId(eu.kalafatic.evolution.model.orchestration.Task task) {
		if (task == null) return "Default";
		if (task.eContainer() instanceof eu.kalafatic.evolution.model.orchestration.Task) {
			return getSessionId((eu.kalafatic.evolution.model.orchestration.Task) task.eContainer());
		}
		return task.getId() != null ? task.getId() : "Default";
	}

	public void runTask(eu.kalafatic.evolution.model.orchestration.Task task) {
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

		getCurrentSessionState().currentStackTask = task;

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
		}
		SessionState state = getSessionState(sessionId);
		if (state.orchestrationSession != null) {
			String taskId = state.activeTaskId != null ? state.activeTaskId : orchestrator.getId();
			OrchestratorServiceImpl.getInstance().provideInput(taskId, input);
		}
		if (state.currentContext != null) {
			state.currentContext.provideInput(input);
			if (sessionId.equals(getCurrentSessionName())) {
				feedbackGroup.hideInput();
				updateModeDisplay();
				updateScrolledContent();
			}
		}
	}

	private void clearWaitingMessages() {
		if (currentSession != null) {
			currentSession.getMessages().forEach(m -> {
				if ("waiting".equals(m.getAgentType())) {
					m.setAgentType("response");
				}
			});
			chatGroup.refreshUI();
		}
	}

	private void processLogEntry(String log) {
		processLogEntry(log, getCurrentSessionName());
	}

	private void processLogEntry(String log, String sessionId) {
		if (log == null || log.isEmpty()) return;
		
		Display.getDefault().asyncExec(() -> {
			if (isDisposed()) return;
			Color color = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
			int style = SWT.NORMAL;
			if (log.startsWith("Evo:") || log.startsWith("Orchestrator:")) { color = colorEvolution; style = SWT.ITALIC; }
			else if (log.contains("Agent [") && log.contains("Planner")) { color = colorPlanner; style = SWT.BOLD; }
			else if (log.contains("Agent [") && log.contains("Architect")) { color = colorArchitect; style = SWT.BOLD; }
			else if (log.contains("Agent [") && log.contains("JavaDev")) { color = colorJavaDev; style = SWT.BOLD; }
			else if (log.contains("Agent [") && log.contains("Tester")) { color = colorTester; style = SWT.BOLD; }
			else if (log.contains("Agent [") && log.contains("Reviewer")) { color = colorReviewer; style = SWT.BOLD; }
			else if (log.contains("Agent [") && log.contains("File")) { color = colorJavaDev; style = SWT.BOLD; }
			else if (log.startsWith("Orchestrator Error:") || log.contains("Exception:")) { color = colorError; style = SWT.BOLD; }

			if (sessionId.equals(getCurrentSessionName())) {
				chatGroup.appendText(log, color, style);
			} else {
				ChatSession targetSession = orchestrator.getAiChat().getSessions().stream().filter(t -> t.getId().equals(sessionId)).findFirst().orElse(null);
				if (targetSession != null) {
					chatGroup.appendTextToSession(targetSession, log, color, style);
				}
			}
			editor.setDirty(true);
		});
	}

	public String getCurrentSessionName() { return currentSession != null ? currentSession.getId() : "Default"; }

	public ChatSession getCurrentSession() { return currentSession; }

	public MultiPageEditor getEditor() { return editor; }

	public FormToolkit getToolkit() { return toolkit; }

	/**
	 * @evo:14:A reason=categorized-assist
	 */
	private String getCategory() {
		eu.kalafatic.evolution.model.orchestration.Task currentStackTask = getCurrentSessionState().currentStackTask;
		if (currentStackTask != null) {
			String type = currentStackTask.getType();
			if ("SELF_DEV_MODE".equals(type) || "ASSISTED_CODING".equals(type) || "DARWIN_MODE".equals(type)) {
				return "coding";
			}
		}
		if (instructionsGroup != null) {
			if (instructionsGroup.isSelfIterative() || instructionsGroup.isIterative() || instructionsGroup.isDarwin()) return "coding";
		}
		return "chat";
	}

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
		assistAdapter.setAutoActivationCharacters("abcdefghijklmnopqrstuvwxyz/".toCharArray());
	}

	@Override
	public void onEvent(RuntimeEvent event) {
		String sessionId = event.getSessionId();
		if (sessionId == null || !sessionId.equals(getCurrentSessionName())) return;

		Display.getDefault().asyncExec(() -> {
			if (isDisposed()) return;
			switch (event.getType()) {
				case STEP_WAITING:
					if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
						modeIndicatorLabel.setText("WAITING FOR STEP APPROVAL...");
						modeIndicatorLabel.setBackground(colorWaiting);
					}
					instructionsGroup.setOrchestrationRunning(false);
					instructionsGroup.focusAndHighlight(colorLightOrange, null);
					break;
				case STEP_RESUMED:
					if (modeIndicatorLabel != null && !modeIndicatorLabel.isDisposed()) {
						updateModeDisplay();
					}
					instructionsGroup.setOrchestrationRunning(true);
					break;
				default:
					break;
			}
		});
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
					saveLastUsedSettings();
					MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("AI Connection Success"); mb.setMessage("Connected to AI provider successfully and settings saved.\nResponse: " + response); mb.open();
				});
			} catch (Exception ex) {
				Display.getDefault().asyncExec(() -> { if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("AI Connection Failed"); mb.setMessage("Error connecting to AI provider (settings NOT saved): " + ex.getMessage()); mb.open(); });
			}
		}).start();
	}
}
