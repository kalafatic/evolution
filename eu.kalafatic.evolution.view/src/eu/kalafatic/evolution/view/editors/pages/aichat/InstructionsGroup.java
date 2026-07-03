package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.model.orchestration.ChatSession;
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
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * @evo:16:A reason=darwin-checkbox-ui
 */
public class InstructionsGroup extends AEvoGroup {
    private StyledText requestText;
    private Button iterativeCheck, selfIterativeCheck, darwinCheck, autoApproveCheck, gitAutomationCheck, stepModeCheck;
    private org.eclipse.swt.widgets.Combo maxIterationsCombo;
    private Scale expansionScale;
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

    public InstructionsGroup(FormToolkit toolkit, Composite promptParent, Composite controlsParent, AiChatPage page, Orchestrator orchestrator) {
        super(page.getEditor(), orchestrator);
        this.page = page;
        createSplitControl(toolkit, promptParent, controlsParent);
    }

    private void createControl(FormToolkit toolkit, Composite parent, boolean nested) {
    	if (nested) {
            group = GUIFactory.INSTANCE.createComposite(parent);
           
            // Add a separator line
            org.eclipse.swt.widgets.Label separator = GUIFactory.INSTANCE.createLabel(group, "", org.eclipse.swt.SWT.SEPARATOR | org.eclipse.swt.SWT.HORIZONTAL);
            separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        } else {
            group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Instructions", 1, true);
        }
        requestText = new StyledText(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        page.setupContextAssist(requestText);
        GridData requestGridData = new GridData(GridData.FILL_BOTH);
        requestGridData.heightHint = 30;
        requestText.setLayoutData(requestGridData);
        
        Composite composite = GUIFactory.INSTANCE.createComposite(group,2, SWT.BORDER);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));


        attachmentArea = GUIFactory.INSTANCE.createComposite(group, 1, SWT.BORDER);

        createButtonsAndSettings(toolkit, composite);

        requestText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Ctrl+Enter or just Enter
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    if ((e.stateMask & SWT.MODIFIER_MASK) == 0 || (e.stateMask & SWT.CTRL) != 0) {
                        e.doit = false;
                        updateModel();
                        try {
							page.handleSend();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
                    }
                }
                // Ctrl+L: Clean Chat
                if (e.keyCode == 'l' && (e.stateMask & SWT.CTRL) != 0) {
                    e.doit = false;
                    page.cleanChat();
                }
            }
        });
    }

    private void createSplitControl(FormToolkit toolkit, Composite promptParent, Composite controlsParent) {
        group = promptParent; // For compatibility with inherited group field
        requestText = new StyledText(promptParent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        page.setupContextAssist(requestText);
        requestText.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite composite = GUIFactory.INSTANCE.createComposite(controlsParent, 2, SWT.BORDER);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        attachmentArea = GUIFactory.INSTANCE.createComposite(controlsParent, 1, SWT.BORDER);
        attachmentArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        createButtonsAndSettings(toolkit, composite);

        requestText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    if ((e.stateMask & SWT.MODIFIER_MASK) == 0 || (e.stateMask & SWT.CTRL) != 0) {
                        e.doit = false;
                        updateModel();
                        try {
							page.handleSend();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
                    }
                }
                if (e.keyCode == 'l' && (e.stateMask & SWT.CTRL) != 0) {
                    e.doit = false;
                    page.cleanChat();
                }
            }
        });
    }

    private void createButtonsAndSettings(FormToolkit toolkit, Composite parent) {
        // Left side: Buttons
        Composite btnComp = GUIFactory.INSTANCE.createComposite(parent, 5);

        sendButton = GUIFactory.INSTANCE.createButton(btnComp, "▶️ Send");
        sendButton.setBackground(lightGreen);
        sendButton.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());
        sendButton.setToolTipText("Start a classic, iterative or autonomous iterative self-development session to improve the codebase.");

        pauseButton = GUIFactory.INSTANCE.createButton(btnComp, "⏸️ Pause");
        pauseButton.setEnabled(true);
        pauseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handlePause();
            }
        });

        stopButton = GUIFactory.INSTANCE.createButton(btnComp, "⏹️ Stop");
        stopButton.setEnabled(true);
        stopButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handleStop();
            }
        });

        attachButton = GUIFactory.INSTANCE.createButton(btnComp, "\ud83d\udcce" + "Attach MD");        
        

        // Right side: Checkboxes and Spinners
        Composite settingsComp = GUIFactory.INSTANCE.createComposite(parent, 10);
        
        // 4. Expansion Depth Slider        
        GUIFactory.INSTANCE.createLabel(settingsComp, "Expansion Depth (Atomic - Multiple):", SWT.NONE, 200);
        expansionScale = new Scale(settingsComp, SWT.HORIZONTAL);
        expansionScale.setMinimum(1);
        expansionScale.setMaximum(10);
        expansionScale.setIncrement(1);
        expansionScale.setSelection(5);       
        expansionScale.setPageIncrement(2);
        expansionScale.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        expansionScale.setToolTipText("Adjust the depth of evolutionary iterations and architectural branching.");           

        expansionScale.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (isUpdating) return;
                int val = expansionScale.getSelection();

                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("expansion", val);
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });
        
        

        autoApproveCheck = GUIFactory.INSTANCE.createCheckButton(settingsComp, "Auto-Approve");
        autoApproveCheck.setToolTipText("Automatically approve plans and file deletions.");
        autoApproveCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean sel = autoApproveCheck.getSelection();
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("autoApprove", sel);
                page.updateConfiguration(settings);

                ChatSession session = page.getCurrentSession();
                if (session != null) {
                    session.setAutoApprove(sel);
                }
                PromptInstructions pi = (orchestrator != null && orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getPromptInstructions() : null;
                if (pi != null) {
                    pi.setAutoApprove(sel);
                }

                page.saveLastUsedSettings();
            }
        });

        selfIterativeCheck = GUIFactory.INSTANCE.createCheckButton(settingsComp, "Self Development");
        selfIterativeCheck.setToolTipText("Enable autonomous iterative development to improve the codebase.");
        selfIterativeCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean sel = selfIterativeCheck.getSelection();
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("selfIterativeMode", sel);
                if (sel) {
                    settings.put("iterativeMode", true);
                    settings.put("darwinMode", true);
                }
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });
        
        gitAutomationCheck = GUIFactory.INSTANCE.createCheckButton(settingsComp, "Auto-Git");
        gitAutomationCheck.setToolTipText("Automatically create branches and commit changes.");
        gitAutomationCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("gitAutomation", gitAutomationCheck.getSelection());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });

        stepModeCheck = GUIFactory.INSTANCE.createCheckButton(settingsComp, "Step Mode");
        stepModeCheck.setToolTipText("Enable step-by-step execution control in the workflow graph.");
        stepModeCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("stepMode", stepModeCheck.getSelection());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });
        
        darwinCheck = GUIFactory.INSTANCE.createCheckButton(settingsComp, "Darwin");
        darwinCheck.setSelection(true);
        darwinCheck.setToolTipText("Enable Darwin style iterations (multiple branches, survival of the fittest).");
        darwinCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean sel = darwinCheck.getSelection();
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("darwinMode", sel);
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });

        iterativeCheck = GUIFactory.INSTANCE.createCheckButton(settingsComp, "Iterative");
        iterativeCheck.setSelection(true);
        iterativeCheck.setToolTipText("Enable iterative development based on your prompt.");
        iterativeCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("iterativeMode", iterativeCheck.getSelection());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });
        

        org.eclipse.swt.widgets.Label minIterLabel = GUIFactory.INSTANCE.createLabel(settingsComp, "Min Iterations",SWT.NONE,70);
        minIterLabel.setToolTipText("Minimum number of evolutionary iterations to perform before convergence.");

        maxIterationsCombo = new org.eclipse.swt.widgets.Combo(settingsComp, SWT.BORDER | SWT.READ_ONLY);
        for (int i = 1; i <= 20; i++) {
            maxIterationsCombo.add(String.valueOf(i));
        }
        maxIterationsCombo.select(3); // Default to 4 (index 3)
        maxIterationsCombo.setToolTipText("Set the minimum number of generations for Darwin evolution.");
        maxIterationsCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (isUpdating) return;
                String text = maxIterationsCombo.getText();
                try {
                    int val = Integer.parseInt(text);
                    java.util.Map<String, Object> settings = new java.util.HashMap<>();
                    settings.put("maxIterations", val);
                    page.updateConfiguration(settings);
                    page.saveLastUsedSettings();
                } catch (NumberFormatException ex) {
                    // Ignore
                }
            }
        });
                

        attachButton.setToolTipText("Add External Instructions (.md)");
        attachButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                org.eclipse.swt.widgets.FileDialog dialog = new org.eclipse.swt.widgets.FileDialog(parent.getShell(), SWT.OPEN | SWT.MULTI);
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
                try {
					page.handleSend();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
    }

    private boolean isUpdating = false;

    @Override
    protected void refreshUI() {
        if (!isUpdating) {
            isUpdating = true;
            try {
                eu.kalafatic.evolution.view.projection.RuntimeProjection projection = eu.kalafatic.evolution.view.projection.ProjectionService.getInstance().getProjection(page.getCurrentSessionName());
                java.util.Map<String, Object> config = projection.getConfiguration();

                ChatSession session = page.getCurrentSession();
                PromptInstructions pi = (orchestrator != null && orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getPromptInstructions() : null;

                setSelectionSafe(iterativeCheck, (Boolean) config.getOrDefault("iterativeMode", session != null ? session.isIterativeMode() : (pi != null ? pi.isIterativeMode() : true)));
                setSelectionSafe(selfIterativeCheck, (Boolean) config.getOrDefault("selfIterativeMode", session != null ? session.isSelfIterativeMode() : (pi != null ? pi.isSelfIterativeMode() : false)));
                setSelectionSafe(darwinCheck, (Boolean) config.getOrDefault("darwinMode", session != null ? session.isDarwinMode() : (orchestrator != null ? orchestrator.isDarwinMode() : true)));
                setSelectionSafe(gitAutomationCheck, (Boolean) config.getOrDefault("gitAutomation", session != null ? session.isGitAutomation() : (pi != null ? pi.isGitAutomation() : false)));
                setSelectionSafe(stepModeCheck, (Boolean) config.getOrDefault("stepMode", session != null ? session.isStepMode() : (pi != null ? pi.isStepMode() : false)));
                setSelectionSafe(autoApproveCheck, (Boolean) config.getOrDefault("autoApprove", session != null ? session.isAutoApprove() : (pi != null ? pi.isAutoApprove() : false)));

                if (expansionScale != null && !expansionScale.isDisposed()) {
                    int defaultExpansion = session != null ? session.getExpansion() : 5;
                    int expansion = (Integer) config.getOrDefault("expansion", defaultExpansion);
                    if (expansionScale.getSelection() != expansion) {
                        expansionScale.setSelection(expansion);
                    }
                }

                int defaultMaxIter = session != null ? session.getMaxIterations() : (pi != null ? pi.getPreferredMaxIterations() : 4);
                if (defaultMaxIter <= 0) defaultMaxIter = 4;

                int maxIter = (Integer) config.getOrDefault("maxIterations", defaultMaxIter);
                if (maxIterationsCombo != null && !maxIterationsCombo.isDisposed()) {
                    String valStr = String.valueOf(maxIter);
                    if (!maxIterationsCombo.getText().equals(valStr)) {
                        int index = maxIterationsCombo.indexOf(valStr);
                        if (index >= 0) {
                            maxIterationsCombo.select(index);
                        } else {
                            maxIterationsCombo.setText(valStr);
                        }
                    }
                }
            } finally {
                isUpdating = false;
            }
        }
    }

    @Override
    public void updateModel() {
        // Model is now updated directly in control listeners.
    }

    public String getRequest() { return requestText.getText().trim(); }
    public void setRequest(String text) { setTextSafe(requestText, text); }

    public void setCaretToEnd() {
        if (requestText != null && !requestText.isDisposed()) {
            requestText.setSelection(requestText.getCharCount());
            requestText.setFocus();
        }
    }

    public void focusAndHighlight(Color bgColor, Color fgColor) {
        if (requestText != null && !requestText.isDisposed()) {
            requestText.setFocus();
            setBackgroundSafe(requestText, bgColor);
            setForegroundSafe(requestText, fgColor);
        }
    }

    public void resetBackground() {
        if (requestText != null && !requestText.isDisposed()) {
            setBackgroundSafe(requestText, colorWhite);
            setForegroundSafe(requestText, null);
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
    public void setAutoApprove(boolean autoApprove) {
        if (autoApproveCheck != null && !autoApproveCheck.isDisposed()) {
            autoApproveCheck.setSelection(autoApprove);

            java.util.Map<String, Object> settings = new java.util.HashMap<>();
            settings.put("autoApprove", autoApprove);
            page.updateConfiguration(settings);

            ChatSession session = page.getCurrentSession();
            if (session != null) {
                session.setAutoApprove(autoApprove);
            }
        }
    }
    public int getMaxIterations() {
        try {
            return Integer.parseInt(maxIterationsCombo.getText());
        } catch (NumberFormatException e) {
            return 4;
        }
    }
    public void setMaxIterations(int maxIterations) {
        String valStr = String.valueOf(maxIterations);
        int index = maxIterationsCombo.indexOf(valStr);
        if (index >= 0) {
            maxIterationsCombo.select(index);
        } else {
            maxIterationsCombo.setText(valStr);
        }
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
        setEnabledSafe(sendButton, !running);
        setEnabledSafe(pauseButton, true);
        setEnabledSafe(stopButton, true);
        setTextSafe(pauseButton, "⏸️ Pause");
    }

    public void setPaused(boolean paused) {
        setTextSafe(pauseButton, paused ? "▶️ Resume" : "⏸️ Pause");
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
        page.scheduleRefresh();
    }
}
