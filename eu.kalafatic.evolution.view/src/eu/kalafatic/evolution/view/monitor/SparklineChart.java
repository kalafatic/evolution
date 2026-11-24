package eu.kalafatic.evolution.view.monitor;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Lightweight SWT line chart for a single live metric. Uses only SWT {@code Canvas}
 * drawing primitives (no graph library), adapts to the active SWT theme, and keeps a
 * bounded history buffer so memory never grows unboundedly.
 */
public final class SparklineChart extends Canvas {

    private final Deque<Double> history = new ArrayDeque<>();
    private final int maxPoints;
    private final String title;
    private final String unit;
    private double maxObserved = 0.0;
    private Color lineColor;

    public SparklineChart(Composite parent, int maxPoints, String title, String unit, int r, int g, int b) {
        super(parent, SWT.DOUBLE_BUFFERED);
        this.maxPoints = Math.max(8, maxPoints);
        this.title = title;
        this.unit = unit;
        this.lineColor = new Color(getDisplay(), r, g, b);
        addListener(SWT.Paint, this::paint);
        addListener(SWT.Dispose, e -> { if (lineColor != null) lineColor.dispose(); });
    }

    public void push(double value) {
        if (value < 0) return; // skip unavailable values
        history.addLast(value);
        if (value > maxObserved) maxObserved = value;
        while (history.size() > maxPoints) history.removeFirst();
        if (!isDisposed()) redraw();
    }

    public void clear() {
        history.clear();
        maxObserved = 0.0;
        if (!isDisposed()) redraw();
    }

    private void paint(org.eclipse.swt.widgets.Event e) {
        GC gc = e.gc;
        int w = getClientArea().width;
        int h = getClientArea().height;
        Color bg = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        Color fg = getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
        Color grid = getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);

        gc.setBackground(bg);
        gc.fillRectangle(0, 0, w, h);

        int padTop = 16, padBottom = 12, padLeft = 6, padRight = 6;
        int plotH = h - padTop - padBottom;
        int plotW = w - padLeft - padRight;

        // Title + current value
        Double cur = history.isEmpty() ? null : history.peekLast();
        String header = title + (cur != null ? "  " + fmt(cur) + unit : "");
        gc.setForeground(fg);
        gc.drawText(header, padLeft, 2, true);

        if (plotH <= 0 || plotW <= 0) return;

        // Grid lines (25/50/75/100%).
        gc.setForeground(grid);
        for (int pct : new int[]{25, 50, 75, 100}) {
            int y = padTop + plotH - (plotH * pct / 100);
            gc.drawLine(padLeft, y, padLeft + plotW, y);
        }

        // Line chart.
        if (history.size() >= 2) {
            double top = maxObserved > 0 ? maxObserved : 100.0;
            gc.setForeground(lineColor);
            int n = history.size();
            int i = 0;
            double prevX = -1, prevY = -1;
            for (double v : history) {
                double x = padLeft + (plotW * i / (double) (n - 1));
                double y = padTop + plotH - (plotH * Math.min(1.0, v / top));
                if (prevX >= 0) {
                    gc.drawLine((int) prevX, (int) prevY, (int) x, (int) y);
                }
                prevX = x; prevY = y;
                i++;
            }
            // Max value label.
            gc.setForeground(fg);
            gc.drawText("max " + fmt(maxObserved) + unit, padLeft, padTop + plotH + 1, true);
        }
    }

    private static String fmt(double v) {
        if (v >= 100) return String.format("%.0f", v);
        if (v >= 10) return String.format("%.1f", v);
        return String.format("%.1f", v);
    }
}
