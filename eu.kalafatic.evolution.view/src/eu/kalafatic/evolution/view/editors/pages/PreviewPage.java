package eu.kalafatic.evolution.view.editors.pages;

import java.io.StringWriter;
import java.text.Collator;
import java.util.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class PreviewPage extends Composite {
    private StyledText text;
    private Orchestrator orchestrator;

    public PreviewPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.orchestrator = orchestrator;
        createControl();
    }

    private void createControl() {
        this.setLayout(new FillLayout());
        text = new StyledText(this, SWT.H_SCROLL | SWT.V_SCROLL);
        text.setEditable(false);
    }

    public void sortWords() {
        if (orchestrator == null) return;
        Resource res = orchestrator.eResource();
        if (res == null) return;

        StringWriter writer = new StringWriter();
        try {
            res.save(new java.io.OutputStream() {
                @Override public void write(int b) throws java.io.IOException { writer.write(b); }
            }, null);
        } catch (java.io.IOException e) { e.printStackTrace(); }

        String editorText = writer.toString();
        StringTokenizer tokenizer = new StringTokenizer(editorText, " \t\n\r\f!@#%^&*()-_=+~[]{};:'\",.<>/?|\\\\");
        List<String> words = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) words.add(tokenizer.nextToken());
        Collections.sort(words, Collator.getInstance());
        StringWriter sw = new StringWriter();
        for (String w : words) sw.write(w + System.lineSeparator());
        text.setText(sw.toString());
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }
}
