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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class InstructionsGroup extends AEvoGroup {
    private StyledText requestText;
    private Button iterativeCheck, selfIterativeCheck, autoApproveCheck, gitAutomationCheck;
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
            group = toolkit.createComposite(parent);
            org.eclipse.swt.layout.GridLayout layout = new org.eclipse.swt.layout.GridLayout(1, false);
            layout.marginHeight = 5; layout.marginWidth = 5; layout.verticalSpacing = 5;
            group.setLayout(layout);
            group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            // Add a separator line
            org.eclipse.swt.widgets.Label separator = toolkit.createLabel(group, "", org.eclipse.swt.SWT.SEPARATOR | org.eclipse.swt.SWT.HORIZONTAL);
            separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        } else {
            group = SWTFactory.createExpandableGroup(toolkit, parent, "Instructions", 1, true);
        }
        requestText = new StyledText(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        page.setupContextAssist(requestText);
        GridData requestGridData = new GridData(GridData.FILL_BOTH);
        requestGridData.heightHint = 66;
        requestText.setLayoutData(requestGridData);

        attachmentArea = toolkit.createComposite(group);
        attachmentArea.setLayout(new org.eclipse.swt.layout.RowLayout(SWT.HORIZONTAL));
        attachmentArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite composite = toolkit.createComposite(group);
        org.eclipse.swt.layout.GridLayout compLayout = new org.eclipse.swt.layout.GridLayout(9, false);
        compLayout.marginHeight = 0; compLayout.marginWidth = 0;
        composite.setLayout(compLayout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        sendButton = toolkit.createButton(composite, "▶️ Send", SWT.PUSH);
        GridData sendGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        sendGd.widthHint = 70;
        sendGd.heightHint = 28;
        sendButton.setLayoutData(sendGd);
        sendButton.setBackground(lightGreen);
        sendButton.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());
        sendButton.setToolTipText("Start a classic, iterative or autonomous iterative self-development session to improve the codebase.");

        pauseButton = toolkit.createButton(composite, "⏸️ Pause", SWT.PUSH);
        GridData actionGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        actionGd.widthHint = 70;
        actionGd.heightHint = 28;
        pauseButton.setLayoutData(actionGd);
        pauseButton.setEnabled(false);
        pauseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handlePause();
            }
        });

        stopButton = toolkit.createButton(composite, "⏹️ Stop", SWT.PUSH);
        stopButton.setLayoutData(actionGd);
        stopButton.setEnabled(false);
        stopButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handleStop();
            }
        });

        iterativeCheck = toolkit.createButton(composite, "Iterative Development", SWT.CHECK);
        iterativeCheck.setToolTipText("Enable iterative development based on your prompt.");
        iterativeCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (iterativeCheck.getSelection()) selfIterativeCheck.setSelection(false);
                page.syncModelWithUI();
            }
        });

        selfIterativeCheck = toolkit.createButton(composite, "Self Iterative Development", SWT.CHECK);
        selfIterativeCheck.setToolTipText("Enable autonomous iterative development to improve the codebase.");
        selfIterativeCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (selfIterativeCheck.getSelection()) iterativeCheck.setSelection(false);
                page.syncModelWithUI();
            }
        });

        autoApproveCheck = toolkit.createButton(composite, "Auto-Approve", SWT.CHECK);
        autoApproveCheck.setToolTipText("Automatically approve plans and file deletions.");
        autoApproveCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        gitAutomationCheck = toolkit.createButton(composite, "Git Automation", SWT.CHECK);
        gitAutomationCheck.setToolTipText("Automatically create branches and commit changes.");
        gitAutomationCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        toolkit.createLabel(composite, "Max Iterations:");
        maxIterationsSpinner = new org.eclipse.swt.widgets.Spinner(composite, SWT.BORDER);
        maxIterationsSpinner.setMinimum(1);
        maxIterationsSpinner.setMaximum(100);
        maxIterationsSpinner.setIncrement(1);
        maxIterationsSpinner.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        attachButton = toolkit.createButton(composite, "\ud83d\udcce" + "Attach MD", SWT.PUSH);
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
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    if ((e.stateMask & SWT.MODIFIER_MASK) == 0) {
                        e.doit = false;
                        updateModel();
                        page.handleSend();
                    }
                }
            }
        });
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null && orchestrator.getAiChat() != null && orchestrator.getAiChat().getPromptInstructions() != null) {
        	
        	PromptInstructions promptInstructions = orchestrator.getAiChat().getPromptInstructions();
        	
            iterativeCheck.setSelection(promptInstructions.isIterativeMode());
            selfIterativeCheck.setSelection(promptInstructions.isSelfIterativeMode());
            autoApproveCheck.setSelection(promptInstructions.isAutoApprove());
            gitAutomationCheck.setSelection(promptInstructions.isGitAutomation());
            maxIterationsSpinner.setSelection(promptInstructions.getPreferredMaxIterations());
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator != null) {
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
        }
    }

    public String getRequest() { return requestText.getText().trim(); }
    public void setRequest(String text) { requestText.setText(text); }

    public void focusAndHighlight(Color color) {
        if (requestText != null && !requestText.isDisposed()) {
            requestText.setFocus();
            requestText.setBackground(color);
        }
    }

    public void resetBackground() {
        if (requestText != null && !requestText.isDisposed()) {
            requestText.setBackground(null);
        }
    }
    public boolean isIterative() { return iterativeCheck.getSelection(); }
    public boolean isSelfIterative() { return selfIterativeCheck.getSelection(); }
    public boolean isAutoApprove() { return autoApproveCheck.getSelection(); }
    public int getMaxIterations() { return maxIterationsSpinner.getSelection(); }
    public boolean isGitAutomationCheck() { return gitAutomationCheck.getSelection(); }

    public void setOrchestrationRunning(boolean running) {
        sendButton.setEnabled(!running);
        pauseButton.setEnabled(running);
        stopButton.setEnabled(running);
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
