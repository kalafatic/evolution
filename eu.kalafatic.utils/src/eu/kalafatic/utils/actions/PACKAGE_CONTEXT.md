# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.utils/src/eu/kalafatic/utils/actions/

## Domain: general

## Components
* `ClearAction.java`: package eu.kalafatic.utils.actions; import java.util.List; import java.util.concurrent.locks.Lock; import java.util.concurrent.locks.ReentrantLock; import org.eclipse.jface.action.Action; import org.eclipse.jface.viewers.TableViewer; import org.eclipse.swt.widgets.Display; import eu.kalafatic.utils.model.LogElement; public class ClearAction extends Action { private final TableViewer viewer; private final Lock lock = new ReentrantLock(true); public ClearAction(TableViewer viewer) { this.viewer = viewer; this.setText("Clear"); this.setToolTipText("Clear"); }
