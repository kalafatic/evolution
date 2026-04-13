/*******************************************************************************
 * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3 
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt  
 * 
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.views;

import static eu.kalafatic.utils.constants.FConstants.PREFERENCES;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;

import eu.kalafatic.utils.actions.ClearAction;
import eu.kalafatic.utils.constants.FConstants;
import eu.kalafatic.utils.constants.FCoreImageConstants;
import eu.kalafatic.utils.constants.FUIConstants;
import eu.kalafatic.utils.interfaces.AViewer;
import eu.kalafatic.utils.interfaces.ILog;
import eu.kalafatic.utils.log.LoggerUtils;
import eu.kalafatic.utils.preferences.ECorePreferences;
import eu.kalafatic.utils.providers.ConsoleTableContentProvider;
import eu.kalafatic.utils.providers.ConsoleTableLabelProvider;

/**
 * The Class class ConsoleView.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ConsoleView extends AViewer {

	/** The Constant ID. */
	public static final String ID = "eu.kalafatic.gemini.core.views.ConsoleView";

	/** The clear action. */
	private Action clearAction;

	/** The text. */
	private Text text;

	/** The table. */
	private Table table;

	/** The trace col. */
	private TableColumn typeCol, dateCol, nameCol, msgCol, traceCol;

	/** The lock. */
	public final Lock lock = new ReentrantLock(true);

	/**
	 * Instantiates a new console view.
	 */
	public ConsoleView() {
		setTitleImage(FCoreImageConstants.CONSOLE_IMG);
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets .Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		createContents(parent);
		makeActions();
		hookContextMenu();
		contributeToActionBars();
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the contents.
	 * @param parent the parent
	 */
	private void createContents(Composite parent) {
		viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

		((TableViewer) viewer).setLabelProvider(new ConsoleTableLabelProvider());
		((TableViewer) viewer).setContentProvider(new ConsoleTableContentProvider());
		((TableViewer) viewer).setUseHashlookup(true);

		PREFERENCES.putBoolean(ECorePreferences.LOG_CONSOLE.getName(), true);

		table = ((TableViewer) viewer).getTable();

		initColumns();

		table.layout(true, true);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setInput(FConstants.LOGS);

		initListeners();

		getSite().setSelectionProvider(viewer);
		getSite().getPage().addSelectionListener(this);

	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#initListeners()
	 */
	@Override
	public void initListeners() {
		table.addControlListener(new ControlListener() {
			@Override
			public void controlResized(ControlEvent e) {
				int lineCount = getLineCount();

				if (lineCount > ILog.MAX_LOGS) {
					PREFERENCES.putInt(ECorePreferences.MAX_LOGS.getName(), lineCount);
					LoggerUtils.getInstance().initPool();
					refresh();
				}
			}

			@Override
			public void controlMoved(ControlEvent e) {}
		});
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#initColumns()
	 */
	@Override
	public void initColumns() {
		typeCol = new TableColumn(table, SWT.LEFT, 0);
		typeCol.setText("Type");
		typeCol.setWidth(30);
		typeCol.pack();

		dateCol = new TableColumn(table, SWT.LEFT, 1);
		dateCol.setText("Date");
		dateCol.setWidth(FUIConstants.BUTTON_WIDTH);
		typeCol.pack();

		nameCol = new TableColumn(table, SWT.LEFT, 2);
		nameCol.setText("Info");
		nameCol.setWidth(FUIConstants.BUTTON_WIDTH);
		typeCol.pack();

		msgCol = new TableColumn(table, SWT.LEFT, 3);
		msgCol.setText("Message");
		msgCol.setWidth(FUIConstants.LABEL_WIDTH);
		typeCol.pack();

		traceCol = new TableColumn(table, SWT.LEFT, 4);
		traceCol.setText("Trace");
		traceCol.setWidth(FUIConstants.LONG_TEXT_WIDTH);
		typeCol.pack();
	}

	// ---------------------------------------------------------------

	/**
	 * Refresh.
	 */
	public void refresh() {
		if (lock.tryLock()) {
			try {
				Display.getDefault().asyncExec(refresh);
			} finally {
				lock.unlock();
			}
		}
	}

	// ---------------------------------------------------------------

	/** The refresh. */
	private final Runnable refresh = new Runnable() {
		@Override
		public void run() {
			lock.lock();
			try {
				if (viewer != null && viewer.getControl() != null && !viewer.getControl().isDisposed() && viewer.getControl().isVisible()) {

					viewer.refresh();
				}
			} catch (Exception e) {
				// e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}
	};

	// ---------------------------------------------------------------

	/**
	 * Make actions.
	 */
	private void makeActions() {
		clearAction = new ClearAction(((TableViewer) viewer));
		clearAction.setImageDescriptor(FCoreImageConstants.CLEAR_DESC);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IViewer#hookContextMenu()
	 */
	@Override
	public void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				ConsoleView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.AViewer#contributeToActionBars()
	 */
	@Override
	public void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.AViewer#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillContextMenu(IMenuManager manager) {
		manager.add(clearAction);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.AViewer#fillLocalPullDown(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
	public void fillLocalPullDown(IMenuManager manager) {
		manager.add(clearAction);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.AViewer#fillLocalToolBar(org.eclipse.jface.action.IToolBarManager)
	 */
	@Override
	public void fillLocalToolBar(IToolBarManager manager) {
		manager.add(clearAction);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the line count.
	 * @return the line count
	 */
	private int getLineCount() {
		int tableHeight = table.getBounds().height;
		int fontHeight = table.getFont().getFontData()[0].getHeight();
		return tableHeight / fontHeight + 1;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the text.
	 * @return the text
	 */
	public Text getText() {
		return text;
	}
}
