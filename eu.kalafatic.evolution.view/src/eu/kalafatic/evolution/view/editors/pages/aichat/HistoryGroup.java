package eu.kalafatic.evolution.view.editors.pages.aichat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class HistoryGroup {
    private Composite group;
    private StyledText responseText;

    public HistoryGroup(FormToolkit toolkit, Composite parent, Font chatFont) {
        createControl(toolkit, parent, chatFont);
    }

    private void createControl(FormToolkit toolkit, Composite parent, Font chatFont) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Conversation History", 1, false);
        responseText = new StyledText(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        GridData responseGridData = new GridData(GridData.FILL_BOTH);
        responseGridData.heightHint = 250;
        responseText.setLayoutData(responseGridData);
        responseText.setEditable(false);
        responseText.setFont(chatFont);
        responseText.setMargins(10, 10, 10, 10);
    }

    public void appendText(String text, org.eclipse.swt.graphics.Color color, int style) {
        if (responseText.isDisposed()) return;
        int start = responseText.getCharCount();
        responseText.append(text);
        int length = text.length();
        StyleRange range = new StyleRange(start, length, color, null, style);
        responseText.setStyleRange(range);
        responseText.setSelection(responseText.getCharCount());
    }

    public void clear() { responseText.setText(""); }
    public String getText() { return responseText.getText(); }
    public void setText(String text) { responseText.setText(text); }
    public StyleRange[] getStyleRanges() { return responseText.getStyleRanges(); }
    public void setStyleRanges(StyleRange[] ranges) { responseText.setStyleRanges(ranges); }
    public void setSelection(int offset) { responseText.setSelection(offset); }
    public boolean isDisposed() { return responseText.isDisposed(); }
}
