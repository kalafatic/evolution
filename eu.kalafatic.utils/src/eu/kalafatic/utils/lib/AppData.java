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
package eu.kalafatic.utils.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.TrayItem;

import eu.kalafatic.utils.hack.EclipseSplashHandler;
import eu.kalafatic.utils.hack.StatusLineContributionItem;
import eu.kalafatic.utils.interfaces.ISplashUser;

/**
 * The Class class AppData.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public final class AppData {

	/** The tray item. */
	private TrayItem trayItem;

	/** The status line manager. */
	private IStatusLineManager statusLineManager;

	/** The speed down item. */
	private StatusLineContributionItem msgItem, cpuItem, speedUpItem, speedDownItem;

	/** The all down speed. */
	private float allUpSpeed, allDownSpeed;

	/** The splash handler. */
	private EclipseSplashHandler splashHandler;

	/** The splash users. */
	private final List<ISplashUser> splashUsers = new ArrayList<ISplashUser>();

	/** The shared models. */
	private final Map<String, Object> sharedModels = new HashMap<String, Object>();

	/** The INSTANCE. */
	private volatile static AppData INSTANCE;

	/**
	 * Gets the single instance of AppData.
	 * @return single instance of AppData
	 */
	public static AppData getInstance() {
		if (INSTANCE == null) {
			synchronized (AppData.class) {
				INSTANCE = new AppData();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the tray item.
	 * @return the tray item
	 */
	public TrayItem getTrayItem() {
		return trayItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the tray item.
	 * @param trayItem the new tray item
	 */
	public void setTrayItem(TrayItem trayItem) {
		this.trayItem = trayItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the status line manager.
	 * @return the status line manager
	 */
	public IStatusLineManager getStatusLineManager() {
		return statusLineManager;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the status line manager.
	 * @param statusLineManager the new status line manager
	 */
	public void setStatusLineManager(IStatusLineManager statusLineManager) {
		this.statusLineManager = statusLineManager;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the cpu item.
	 * @return the cpu item
	 */
	public StatusLineContributionItem getCpuItem() {
		return cpuItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the cpu item.
	 * @param cpuItem the new cpu item
	 */
	public void setCpuItem(StatusLineContributionItem cpuItem) {
		this.cpuItem = cpuItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the speed down item.
	 * @param speedDownItem the new speed down item
	 */
	public void setSpeedDownItem(StatusLineContributionItem speedDownItem) {
		this.speedDownItem = speedDownItem;
	}

	// ---------------------------------------------------------------
	/**
	 * Sets the speed up item.
	 * @param speedUpItem the new speed up item
	 */
	public void setSpeedUpItem(StatusLineContributionItem speedUpItem) {
		this.speedUpItem = speedUpItem;

	}

	/**
	 * Gets the speed up item.
	 * @return the speed up item
	 */
	public StatusLineContributionItem getSpeedUpItem() {
		return speedUpItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the speed down item.
	 * @return the speed down item
	 */
	public StatusLineContributionItem getSpeedDownItem() {
		return speedDownItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the msg item.
	 * @param msgItem the new msg item
	 */
	public void setMsgItem(StatusLineContributionItem msgItem) {
		this.msgItem = msgItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the msg item.
	 * @return the msg item
	 */
	public StatusLineContributionItem getMsgItem() {
		return msgItem;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the splash handler.
	 * @return the splash handler
	 */
	public EclipseSplashHandler getSplashHandler() {
		return splashHandler;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the splash handler.
	 * @param splashHandler the new splash handler
	 */
	public void setSplashHandler(EclipseSplashHandler splashHandler) {
		this.splashHandler = splashHandler;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the splash users users.
	 * @return the splash users users
	 */
	public List<ISplashUser> getSplashUsersUsers() {
		return splashUsers;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the all up speed.
	 * @return the all up speed
	 */
	public float getAllUpSpeed() {
		return allUpSpeed;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the all up speed.
	 * @param allUpSpeed the new all up speed
	 */
	public void setAllUpSpeed(float allUpSpeed) {
		this.allUpSpeed = allUpSpeed;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the all down speed.
	 * @return the all down speed
	 */
	public float getAllDownSpeed() {
		return allDownSpeed;
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the all down speed.
	 * @param allDownSpeed the new all down speed
	 */
	public void setAllDownSpeed(float allDownSpeed) {
		this.allDownSpeed = allDownSpeed;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the shared models.
	 * @return the shared models
	 */
	public Map<String, Object> getSharedModels() {
		return sharedModels;
	}
}
