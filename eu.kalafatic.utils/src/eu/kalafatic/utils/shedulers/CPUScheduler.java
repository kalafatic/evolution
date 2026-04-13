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
package eu.kalafatic.utils.shedulers;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.widgets.Display;

import eu.kalafatic.utils.hack.StatusLineContributionItem;
import eu.kalafatic.utils.lib.AppData;

/**
 * The Class class CPUScheduler.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class CPUScheduler {

	/** The timer. */
	private Timer timer;

	/** The task. */
	private RefreshTask task;

	/** The refresh time. */
	private long refreshTime;

	/** The time. */
	private long time = System.currentTimeMillis();

	/** The all cpu time. */
	private double allCPUTime = 0;

	/** The all cpu percent. */
	private int allCPUPercent = -1;

	/** The TMXB. */
	private ThreadMXBean TMXB = ManagementFactory.getThreadMXBean();

	/** The PROCESSORS. */
	final int PROCESSORS = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();

	/** The lock. */
	private final Lock lock = new ReentrantLock(true);

	/** The Constant INSTANCE. */
	public static final CPUScheduler INSTANCE = new CPUScheduler();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Start.
	 */
	public void start() {
		refreshTime = 2000;

		if (!TMXB.isThreadCpuTimeEnabled()) {
			TMXB.setThreadCpuTimeEnabled(true);
		}
		schedule();
	}

	// ---------------------------------------------------------------

	/**
	 * Schedule.
	 */
	public void schedule() {
		timer = new Timer(false);
		task = new RefreshTask();
		time = System.currentTimeMillis();
		allCPUTime = getAllCpuTime();

		timer.scheduleAtFixedRate(task, refreshTime, refreshTime);
	}

	// ---------------------------------------------------------------

	/**
	 * The Class class RefreshTask.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	class RefreshTask extends TimerTask {
		/*
		 * (non-Javadoc)
		 * @see java.util.TimerTask#run()
		 */
		@Override
		public void run() {
			Display.getDefault().asyncExec(worker);

		}
	}

	// ---------------------------------------------------------------

	/** The worker. */
	private final Runnable worker = new Runnable() {
		@Override
		public void run() {
			if (lock.tryLock()) {
				try {
					allCPUPercent = getAllSystemTimePercent();

					// Reset once per second
					time = System.currentTimeMillis();
					allCPUTime = getAllCpuTime();

					StatusLineContributionItem cpuItem = AppData.getInstance().getCpuItem();
					if (cpuItem.getLock().tryLock()) {
						try {
							if (!cpuItem.getText().equals("CPU: " + allCPUPercent + " %")) {
								cpuItem.setText("CPU: " + allCPUPercent + " %");
							}
							// AppDataUtil.getInstance().getStatusLineManager()
							// .markDirty();
							// Thread.sleep(1000);
						} finally {
							cpuItem.getLock().unlock();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
			}
		}
	};

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the all system time percent.
	 * @return the all system time percent
	 */
	public int getAllSystemTimePercent() {
		// milliseconds
		long deltaTime = System.currentTimeMillis() - time;
		// nanoseconds (*10 = 1000/100
		long onePercentTime = deltaTime * 1000 * 10;
		// nanoseconds
		double deltaCPU = getAllCpuTime() - allCPUTime;

		double allCPUPercent = deltaCPU / onePercentTime;

		return (int) (allCPUPercent / PROCESSORS);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the all cpu time.
	 * @return the all cpu time
	 */
	private double getAllCpuTime() {
		double cpu = 0;
		long[] ids = TMXB.getAllThreadIds();

		for (int j = 0; j < ids.length; j++) {
			cpu += TMXB.getThreadCpuTime(ids[j]);
		}
		return cpu;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the all cpu percent.
	 * @return the all cpu percent
	 */
	public float getAllCPUPercent() {
		return allCPUPercent;
	}

	// ---------------------------------------------------------------

	/**
	 * Stop.
	 */
	public void stop() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
}
