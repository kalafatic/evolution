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
package eu.kalafatic.utils.threads;

import java.awt.HeadlessException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The Class class DeadlocksManagerThread.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class DeadlocksManagerThread {

	/** The timer. */
	private Timer timer;

	/** The mbean. */
	private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean();

	/** The Constant DEADLOCK_CHECK_PERIOD. */
	private static final int DEADLOCK_CHECK_PERIOD = 2000;

	/** The deadlocked set. */
	private Set<Long> deadlockedSet = new HashSet<Long>();

	/** The deadlocked array. */
	public static ArrayList<Thread> deadlockedArray = new ArrayList<Thread>();

	/** The all threads array. */
	public static ArrayList<Thread> allThreadsArray = new ArrayList<Thread>();

	/** The Constant INSTANCE. */
	public static final DeadlocksManagerThread INSTANCE = new DeadlocksManagerThread();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Start.
	 */
	public void start() {
		timer = new Timer("Thread Monitor", true);

		timer.schedule(new TimerTask() {

			@Override
			public void run() {

				try {
					long[] ids = mbean.findMonitorDeadlockedThreads();

					if (ids != null && ids.length > 0) {

						System.out.println("deadlock ids.length " + ids.length);

						for (Long l : ids) {

							if (!deadlockedSet.contains(l)) {

								deadlockedSet.add(l);

								Thread deadlockedThread = findMatchingThread(mbean
										.getThreadInfo(l));

								deadlockedArray.add(deadlockedThread);
								System.err.println("DEADLOCK-"
										+ deadlockedThread);
							}

							solveProblem();
						}
					}
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (HeadlessException e) {
					e.printStackTrace();
				} catch (ArrayStoreException e) {
					e.printStackTrace();
				}
			}
		}, 10, DEADLOCK_CHECK_PERIOD);
	}

	// ---------------------------------------------------------------

	/**
	 * Find matching thread.
	 * 
	 * @param inf
	 *            the inf
	 * @return the thread
	 */
	private Thread findMatchingThread(ThreadInfo inf) {
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getId() == inf.getThreadId()) {
				return thread;
			}
		}
		throw new IllegalStateException("Deadlocked Thread not found");
	}

	// ---------------------------------------------------------------

	/**
	 * Solve problem.
	 */
	@SuppressWarnings("static-access")
	public void solveProblem() {
		try {
			for (int i = 0; i < deadlockedArray.size(); i++) {
				deadlockedArray.get(i).wait();
			}
			for (int i = 0; i < deadlockedArray.size(); i++) {

				synchronized (deadlockedArray) {
					deadlockedArray.get(i).run();
					deadlockedArray.get(i).yield();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * List all threads.
	 */
	public synchronized void listAllThreads() {
		ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
		while (root.getParent() != null) {
			root = root.getParent();
		}

		visit(root, 0);
	}

	// ---------------------------------------------------------------

	/**
	 * Visit.
	 * 
	 * @param group
	 *            the group
	 * @param level
	 *            the level
	 */
	public static void visit(ThreadGroup group, int level) {
		int numThreads = group.activeCount();
		Thread[] threads = new Thread[numThreads * 2];
		numThreads = group.enumerate(threads, false);

		for (int i = 0; i < numThreads; i++) {
			Thread thread = threads[i];
			allThreadsArray.add(thread);
		}

		int numGroups = group.activeGroupCount();
		ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
		numGroups = group.enumerate(groups, false);

		for (int i = 0; i < numGroups; i++) {
			visit(groups[i], level + 1);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Stop.
	 */
	public void stop() {
		if (timer != null) {
			timer.cancel();
		}
	}
}
