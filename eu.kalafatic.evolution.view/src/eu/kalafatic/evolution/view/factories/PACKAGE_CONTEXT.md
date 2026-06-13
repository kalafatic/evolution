# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/factories/

## Domain: general

## Components
* `SWTBinding.java`: package eu.kalafatic.evolution.view.factories; import org.eclipse.swt.SWT; import org.eclipse.swt.widgets.Button; import org.eclipse.swt.widgets.Combo; import org.eclipse.swt.widgets.Display; import org.eclipse.swt.widgets.Text; import org.eclipse.swt.widgets.Table; import org.eclipse.jface.viewers.TableViewer; import org.eclipse.jface.viewers.IStructuredContentProvider; import org.eclipse.swt.events.ModifyListener; import org.eclipse.swt.events.SelectionAdapter; import org.eclipse.swt.events.SelectionEvent; import eu.kalafatic.utils.model.ObservableProperty; import eu.kalafatic.utils.model.ObservableList; import eu.kalafatic.utils.model.ModelChangeEvent; public class SWTBinding { public static void bindText(Text text, ObservableProperty<String> property) { property.addChangeListener(event -> { Display.getDefault().asyncExec(() -> { if (!text.isDisposed()) {
