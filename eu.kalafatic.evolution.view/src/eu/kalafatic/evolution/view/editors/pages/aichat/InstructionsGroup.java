package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

/**
 * @evo:16:A reason=darwin-checkbox-ui
 */
public class InstructionsGroup extends AEvoGroup {
    private StyledText requestText;
    private Button iterativeCheck, selfIterativeCheck, darwinCheck, autoApproveCheck, gitAutomationCheck, stepModeCheck;
    private org.eclipse.swt.widgets.Spinner maxIterationsSpinner;
    private Button sendButton, pauseButton, stopButton, attachButton;
    private Composite attachmentArea;
    private List<String> instructionFiles = new ArrayList<>();
    private AiChatPage page;

    public InstructionsGroup(FormToolkit toolkit, Composite parent, AiChatPage page, Orchestrator orchestrator) {
        this(toolkit, parent, page, orchestrator, false);
    }

    public InstructionsGroup(FormToolkit toolkit, Composite parent, AiChatPage page, Orchestrator orchestrator, boolean nested) {
        super(page.getEditor(), orchestrator);
        this.page = page;
        createControl(toolkit, parent, nested);
    }

    private void createControl(FormToolkit toolkit, Composite parent, boolean nested) {
    	if (nested) {
            group = SWTFactory.createComposite(parent);
           
            // Add a separator line
            org.eclipse.swt.widgets.Label separator = SWTFactory.createLabel(group, "", org.eclipse.swt.SWT.SEPARATOR | org.eclipse.swt.SWT.HORIZONTAL);
            separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        } else {
            group = SWTFactory.createExpandableGroup(toolkit, parent, "Instructions", 1, true);
        }
        requestText = new StyledText(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        page.setupContextAssist(requestText);
        GridData requestGridData = new GridData(GridData.FILL_BOTH);
        requestGridData.heightHint = 66;
        requestText.setLayoutData(requestGridData);
        
        Composite composite = SWTFactory.createComposite(group,2);
        attachmentArea = SWTFactory.createComposite(group, SWT.BORDER, 1);

        // Left side: Buttons
        Composite btnComp = SWTFactory.createComposite(composite,5);

        sendButton = SWTFactory.createButton(btnComp, "▶️ Send");
        sendButton.setBackground(lightGreen);
        sendButton.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());
        sendButton.setToolTipText("Start a classic, iterative or autonomous iterative self-development session to improve the codebase.");

        pauseButton = SWTFactory.createButton(btnComp, "⏸️ Pause");
        pauseButton.setEnabled(true);
        pauseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handlePause();
            }
        });

        stopButton = SWTFactory.createButton(btnComp, "⏹️ Stop");
        stopButton.setEnabled(true);
        stopButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handleStop();
            }
        });

        attachButton = SWTFactory.createButton(btnComp, "\ud83d\udcce" + "Attach MD");
       
//        attachmentArea = new Composite(btnComp, SWT.BORDER);
//        attachmentArea.setLayout(new GridLayout(1, false));
//        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
//		gd.widthHint = SWTFactory.LABEL_WIDTH;
//		gd.heightHint = SWTFactory.BUTTON_HEIGHT;
//        attachmentArea.setLayoutData(gd);

        // Right side: Checkboxes and Spinners
        Composite settingsComp = SWTFactory.createComposite(composite,SWT.NO , 7);        

        selfIterativeCheck = SWTFactory.createCheckButton(settingsComp, "Self Development");
        selfIterativeCheck.setToolTipText("Enable autonomous iterative development to improve the codebase.");
        selfIterativeCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (selfIterativeCheck.getSelection()) {
                    iterativeCheck.setSelection(true);
                    darwinCheck.setSelection(true);
                }
                page.syncModelWithUI();
            }
        });

        darwinCheck = SWTFactory.createCheckButton(settingsComp, "Darwin");
        darwinCheck.setSelection(true);
        darwinCheck.setToolTipText("Enable Darwin style iterations (multiple branches, survival of the fittest).");
        darwinCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        autoApproveCheck = SWTFactory.createCheckButton(settingsComp, "Auto-Approve");
        autoApproveCheck.setToolTipText("Automatically approve plans and file deletions.");
        autoApproveCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        gitAutomationCheck = SWTFactory.createCheckButton(settingsComp, "Auto-Git");
        gitAutomationCheck.setToolTipText("Automatically create branches and commit changes.");
        gitAutomationCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        stepModeCheck = SWTFactory.createCheckButton(settingsComp, "Step Mode");
        stepModeCheck.setToolTipText("Enable step-by-step execution control in the workflow graph.");
        stepModeCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        iterativeCheck = SWTFactory.createCheckButton(settingsComp, "Iterative");
        iterativeCheck.setSelection(true);
        iterativeCheck.setToolTipText("Enable iterative development based on your prompt.");
        iterativeCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });
        
        Composite iterComp = SWTFactory.createComposite(settingsComp,2);
        
        maxIterationsSpinner = new org.eclipse.swt.widgets.Spinner(iterComp, SWT.BORDER);
        maxIterationsSpinner.setMinimum(1);
        maxIterationsSpinner.setMaximum(100);
        maxIterationsSpinner.setIncrement(1);
        maxIterationsSpinner.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });
        SWTFactory.createLabel(iterComp, "Max Iterations",SWT.NONE,70);

        attachButton.setToolTipText("Add External Instructions (.md)");
        attachButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                org.eclipse.swt.widgets.FileDialog dialog = new org.eclipse.swt.widgets.FileDialog(group.getShell(), SWT.OPEN | SWT.MULTI);
                dialog.setFilterExtensions(new String[] { "*.md" });
                dialog.setFilterNames(new String[] { "Markdown Files (*.md)" });
                dialog.setFilterPath(page.getProjectRoot().getAbsolutePath());
                if (dialog.open() != null) {
                    for (String fileName : dialog.getFileNames()) {
                        String fullPath = dialog.getFilterPath() + File.separator + fileName;
                        if (!instructionFiles.contains(fullPath)) {
                            instructionFiles.add(fullPath);
                        }
                    }
                    refreshAttachments();
                }
            }
        });

        sendButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateModel();
                page.handleSend();
            }
        });

        requestText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Ctrl+Enter or just Enter
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    if ((e.stateMask & SWT.MODIFIER_MASK) == 0 || (e.stateMask & SWT.CTRL) != 0) {
                        e.doit = false;
                        updateModel();
                        page.handleSend();
                    }
                }
                // Ctrl+N: New Session
                if (e.keyCode == 'n' && (e.stateMask & SWT.CTRL) != 0) {
                    e.doit = false;
                    page.createNewSession();
                }
                // Ctrl+L: Clean Chat
                if (e.keyCode == 'l' && (e.stateMask & SWT.CTRL) != 0) {
                    e.doit = false;
                    page.cleanChat();
                }
            }
        });
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            eu.kalafatic.evolution.model.orchestration.ChatSession thread = page.getCurrentSession();
            if (thread != null) {
                iterativeCheck.setSelection(thread.isIterativeMode());
                selfIterativeCheck.setSelection(thread.isSelfIterativeMode());
                darwinCheck.setSelection(thread.isDarwinMode());
                gitAutomationCheck.setSelection(thread.isGitAutomation());
                maxIterationsSpinner.setSelection(thread.getMaxIterations());
                stepModeCheck.setSelection(thread.isStepMode());
            } else {
                darwinCheck.setSelection(orchestrator.isDarwinMode());
                if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
                    PromptInstructions promptInstructions = orchestrator.getAiChat().getPromptInstructions();
                    iterativeCheck.setSelection(promptInstructions.isIterativeMode());
                    selfIterativeCheck.setSelection(promptInstructions.isSelfIterativeMode());
                    gitAutomationCheck.setSelection(promptInstructions.isGitAutomation());
                    maxIterationsSpinner.setSelection(promptInstructions.getPreferredMaxIterations());
                    stepModeCheck.setSelection(promptInstructions.isStepMode());
                }
            }
            if (orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
                autoApproveCheck.setSelection(orchestrator.getAiChat().getPromptInstructions().isAutoApprove());
            }
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator != null) {
            eu.kalafatic.evolution.model.orchestration.ChatSession thread = page.getCurrentSession();
            if (thread != null) {
                thread.setIterativeMode(iterativeCheck.getSelection());
                thread.setSelfIterativeMode(selfIterativeCheck.getSelection());
                thread.setDarwinMode(darwinCheck.getSelection());
                thread.setGitAutomation(gitAutomationCheck.getSelection());
                thread.setMaxIterations(maxIterationsSpinner.getSelection());
                thread.setStepMode(stepModeCheck.getSelection());
            }

            orchestrator.setDarwinMode(darwinCheck.getSelection());
            if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());

            PromptInstructions promptInstructions = orchestrator.getAiChat().getPromptInstructions();
            if (promptInstructions == null) {
                promptInstructions = OrchestrationFactory.eINSTANCE.createPromptInstructions();
                orchestrator.getAiChat().setPromptInstructions(promptInstructions);
            }
            promptInstructions.setIterativeMode(iterativeCheck.getSelection());
            promptInstructions.setSelfIterativeMode(selfIterativeCheck.getSelection());
            promptInstructions.setAutoApprove(autoApproveCheck.getSelection());
            promptInstructions.setGitAutomation(gitAutomationCheck.getSelection());
            promptInstructions.setPreferredMaxIterations(maxIterationsSpinner.getSelection());
            promptInstructions.setStepMode(stepModeCheck.getSelection());
        }
    }

    public String getRequest() { return requestText.getText().trim(); }
    public void setRequest(String text) { requestText.setText(text); }

    public void setCaretToEnd() {
        if (requestText != null && !requestText.isDisposed()) {
            requestText.setSelection(requestText.getCharCount());
            requestText.setFocus();
        }
    }

    public void focusAndHighlight(Color bgColor, Color fgColor) {
        if (requestText != null && !requestText.isDisposed()) {
            requestText.setFocus();
            requestText.setBackground(bgColor);
            if (fgColor != null) {
                requestText.setForeground(fgColor);
            }
        }
    }

    public void resetBackground() {
        if (requestText != null && !requestText.isDisposed()) {
            requestText.setBackground(null);
            requestText.setForeground(null);
        }
    }
    public boolean isIterative() { return iterativeCheck.getSelection(); }
    public void setIterative(boolean iterative) {
        iterativeCheck.setSelection(iterative);
    }
    public boolean isSelfIterative() { return selfIterativeCheck.getSelection(); }
    public void setSelfIterative(boolean selfIterative) {
        selfIterativeCheck.setSelection(selfIterative);
        if (selfIterative) {
            iterativeCheck.setSelection(true);
            darwinCheck.setSelection(true);
        }
    }
    public boolean isDarwin() { return darwinCheck.getSelection(); }
    public void setDarwin(boolean darwin) {
        darwinCheck.setSelection(darwin);
    }
    public boolean isAutoApprove() { return autoApproveCheck.getSelection(); }
    public int getMaxIterations() { return maxIterationsSpinner.getSelection(); }
    public void setMaxIterations(int maxIterations) {
        maxIterationsSpinner.setSelection(maxIterations);
    }
    public boolean isGitAutomationCheck() { return gitAutomationCheck.getSelection(); }
    public void setGitAutomation(boolean gitAutomation) {
        gitAutomationCheck.setSelection(gitAutomation);
    }
    public boolean isStepMode() { return stepModeCheck.getSelection(); }
    public void setStepMode(boolean stepMode) {
    	stepModeCheck.setSelection(stepMode);
    }

    public void setOrchestrationRunning(boolean running) {
        sendButton.setEnabled(!running);
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);
        pauseButton.setText("⏸️ Pause");
    }

    public void setPaused(boolean paused) {
        pauseButton.setText(paused ? "▶️ Resume" : "⏸️ Pause");
    }

    public List<String> getInstructionFiles() {
        return instructionFiles;
    }

    private void refreshAttachments() {
        for (org.eclipse.swt.widgets.Control child : attachmentArea.getChildren()) {
            child.dispose();
        }
        for (String filePath : instructionFiles) {
            Composite item = page.getToolkit().createComposite(attachmentArea, SWT.BORDER);
            org.eclipse.swt.layout.RowLayout row = new org.eclipse.swt.layout.RowLayout(SWT.HORIZONTAL);
            row.marginTop = 2; row.marginBottom = 2; row.marginLeft = 5; row.marginRight = 5; row.center = true;
            item.setLayout(row);

            org.eclipse.swt.widgets.Label icon = page.getToolkit().createLabel(item, "\ud83d\udcc4");
            org.eclipse.swt.widgets.Label name = page.getToolkit().createLabel(item, new File(filePath).getName());

            Button remove = page.getToolkit().createButton(item, "\u2715", SWT.PUSH | SWT.FLAT);
            remove.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    instructionFiles.remove(filePath);
                    refreshAttachments();
                }
            });
        }
        attachmentArea.layout(true);
        page.updateScrolledContent();
    }
}
