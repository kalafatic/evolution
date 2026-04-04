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
package eu.kalafatic.utils.hack;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.dialogs.PreferencesUtil;

import eu.kalafatic.utils.constants.FTextConstants;
import eu.kalafatic.utils.lib.EView;

/**
 * The Class class StatusLineContributionItem.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class StatusLineContributionItem extends ContributionItem {

	/** The lock. */
	private final Lock lock = new ReentrantLock(true);

	/** The Constant DEFAULT_CHAR_WIDTH. */
	private final static int DEFAULT_CHAR_WIDTH = 40;

	/** The Constant CALC_TRUE_WIDTH. */
	public final static int CALC_TRUE_WIDTH = -1;

	/** The label. */
	private CLabel label;

	/** The status line. */
	private Composite statusLine = null;

	/** The text. */
	private String text = Util.ZERO_LENGTH_STRING;

	/** The bg image. */
	private Image bgImage;

	/** The settings item. */
	// private GUIFactory guiFactory;

	/** The settings item. */
	private MenuItem enabledItem, stateItem, settingsItem;

	/**
	 * Instantiates a new status line contribution item.
	 * @param id the id
	 */
	public StatusLineContributionItem(String id) {
		this(id, DEFAULT_CHAR_WIDTH);
	}

	/**
	 * Instantiates a new status line contribution item.
	 * @param id the id
	 * @param charWidth the char width
	 */
	public StatusLineContributionItem(String id, int charWidth) {
		super(id);

		setVisible(false);
		// guiFactory = new GUIFactory();
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#fill(org.eclipse.swt.widgets .Composite)
	 */
	@Override
	public void fill(Composite parent) {
		statusLine = parent;
		label = new CLabel(statusLine, SWT.LEFT_TO_RIGHT | SWT.SHADOW_IN);
		label.setImage(bgImage);
		label.setText(text);

		// label.add

		// label.pack(true);
		label.layout(true, true);
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the speed pop up menu.
	 * @param download the download
	 */
	public void createSpeedPopUpMenu(boolean download) {
		Menu menu = new Menu(label);

		enabledItem = new MenuItem(menu, SWT.CHECK);
		new MenuItem(menu, SWT.SEPARATOR);
		stateItem = new MenuItem(menu, SWT.NONE);
		new MenuItem(menu, SWT.SEPARATOR);

		settingsItem = new MenuItem(menu, SWT.CASCADE);
		settingsItem.setText("Setup");

		// before listener attached
		loadPreferences();

		enabledItem.addListener(SWT.Selection, popupMenuListener);
		stateItem.addListener(SWT.Selection, popupMenuListener);
		settingsItem.addListener(SWT.Selection, popupMenuListener);

		label.setMenu(menu);
	}

	// ---------------------------------------------------------------

	/** The popup menu listener. */
	private final Listener popupMenuListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (event.widget.equals(enabledItem)) {
				boolean selection = enabledItem.getSelection();
				enabledItem.setText(selection ? FTextConstants.SWITCH_ENABLED[0] : FTextConstants.SWITCH_ENABLED[1]);
				stateItem.setEnabled(selection);
				settingsItem.setEnabled(selection);

			} else if (event.widget.equals(stateItem)) {
				stateItem.setText("128");

			} else if (event.widget.equals(settingsItem)) {
				PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(statusLine.getShell(), EView.PREF_ALGORITHMS.ID, null, null);
				if (pref != null) {
					pref.open();
					loadPreferences();
				}
			}
		}
	};

	// ---------------------------------------------------------------

	/**
	 * Load preferences.
	 */
	private void loadPreferences() {
		enabledItem.setSelection(true);
		enabledItem.setText(FTextConstants.SWITCH_ENABLED[0]);

		stateItem.setText(FTextConstants.RATIO_DEF);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the display location.
	 * @return the display location
	 */
	public Point getDisplayLocation() {
		if ((label != null) && (statusLine != null)) {
			return statusLine.toDisplay(label.getLocation());
		}
		return null;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the text.
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the text.
	 * @param text the new text
	 */
	public void setText(String text) {
		Assert.isNotNull(text);

		this.text = LegacyActionTools.escapeMnemonics(text);

		if (label != null && !label.isDisposed()) {

			if (this.text.length() == 0) {
				setVisible(false);
			} else {
				setVisible(true);
				label.setText(this.text);

				if (this.bgImage != null) {
					label.setImage(this.bgImage);
				}

				this.update();
				label.pack(true);
				// statusLine.update();
				statusLine.layout(true, true);
			}
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the image.
	 * @param image the new image
	 */
	public void setImage(Image image) {
		Assert.isNotNull(image);

		this.bgImage = image;

		if (label != null && !label.isDisposed()) {

			if (this.text.length() == 0) {
				setVisible(false);
			} else {
				setVisible(true);
				label.setImage(this.bgImage);
			}
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the status line.
	 * @return the status line
	 */
	public Composite getStatusLine() {
		return statusLine;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the lock.
	 * @return the lock
	 */
	public Lock getLock() {
		return lock;
	}
}
