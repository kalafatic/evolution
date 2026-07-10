package eu.kalafatic.evolution.view.editors.pages.properties;

import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

public class AgentsGroup extends AEvoGroup {
    private Table agentsTable;

    public AgentsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Agents", 1, false);
        agentsTable = new Table(group, SWT.BORDER | SWT.FULL_SELECTION);
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
    }

    private void scanAndLoadAgentsFromFiles() {
        if (orchestrator == null || editor == null) return;
        File projectRoot = null;
        if (editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput) {
            projectRoot = ((org.eclipse.ui.IFileEditorInput) editor.getEditorInput()).getFile().getProject().getLocation().toFile();
        }
        if (projectRoot == null || !projectRoot.exists()) return;

        List<File> agentFiles = new ArrayList<>();
        findAgentFiles(projectRoot, agentFiles);

        for (File f : agentFiles) {
            String filename = f.getName();
            String id = filename;
            if (id.toLowerCase().endsWith(".agent")) {
                id = id.substring(0, id.length() - 6);
            } else if (id.toLowerCase().endsWith("agent")) {
                id = id.substring(0, id.length() - 5);
            }
            if (id.isEmpty()) id = filename;

            String type = "Local File";
            eu.kalafatic.evolution.model.orchestration.ExecutionMode mode = eu.kalafatic.evolution.model.orchestration.ExecutionMode.SERIAL;

            try {
                String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
                if (content.startsWith("{")) {
                    String parsedId = extractValueByKey(content, "id");
                    if (parsedId != null) id = parsedId;
                    String parsedType = extractValueByKey(content, "type");
                    if (parsedType != null) type = parsedType;
                    String parsedMode = extractValueByKey(content, "executionMode");
                    if (parsedMode != null) {
                        try {
                            mode = eu.kalafatic.evolution.model.orchestration.ExecutionMode.get(parsedMode);
                            if (mode == null) {
                                mode = eu.kalafatic.evolution.model.orchestration.ExecutionMode.valueOf(parsedMode.toUpperCase());
                            }
                        } catch (Exception e) {}
                    }
                } else {
                    Properties props = new Properties();
                    try (StringReader reader = new StringReader(content)) {
                        props.load(reader);
                        if (props.containsKey("id")) id = props.getProperty("id");
                        if (props.containsKey("type")) type = props.getProperty("type");
                        if (props.containsKey("executionMode")) {
                            try {
                                String modeStr = props.getProperty("executionMode").trim();
                                mode = eu.kalafatic.evolution.model.orchestration.ExecutionMode.get(modeStr);
                                if (mode == null) {
                                    mode = eu.kalafatic.evolution.model.orchestration.ExecutionMode.valueOf(modeStr.toUpperCase());
                                }
                            } catch (Exception e) {}
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors, fallback to defaults
            }

            final String finalId = id;
            boolean exists = orchestrator.getAgents().stream().anyMatch(a -> finalId.equalsIgnoreCase(a.getId()));
            if (!exists) {
                Agent agent = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAgent();
                agent.setId(id);
                agent.setType(type);
                agent.setExecutionMode(mode);
                orchestrator.getAgents().add(agent);
                editor.setDirty(true);
            }
        }
    }

    private void findAgentFiles(File dir, List<File> result) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.startsWith(".") || name.equalsIgnoreCase("target") || name.equalsIgnoreCase("bin") || name.equalsIgnoreCase("build")) {
                    continue;
                }
                findAgentFiles(f, result);
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith("agent") || name.endsWith(".agent")) {
                    result.add(f);
                }
            }
        }
    }

    private String extractValueByKey(String content, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(content);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            scanAndLoadAgentsFromFiles();

            agentsTable.removeAll();
            for (Agent a : orchestrator.getAgents()) {
                TableItem item = new TableItem(agentsTable, SWT.NONE);
                item.setText(0, a.getId() != null ? a.getId() : "");
                item.setText(1, a.getType() != null ? a.getType() : "");
                item.setText(2, a.getExecutionMode() != null ? a.getExecutionMode().name() : "");
            }
        }
    }
}
