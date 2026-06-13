# PACKAGE CONTEXT

## Directory: eu.kalafatic.utils/src/eu/kalafatic/utils/shedulers/

## Domain: general

## Components
* `CPUScheduler.java`: package eu.kalafatic.utils.shedulers; import java.lang.management.ManagementFactory; import java.lang.management.ThreadMXBean; import java.util.Timer; import java.util.TimerTask; import java.util.concurrent.locks.Lock; import java.util.concurrent.locks.ReentrantLock; import org.eclipse.swt.widgets.Display; import eu.kalafatic.utils.hack.StatusLineContributionItem; import eu.kalafatic.utils.lib.AppData; public class CPUScheduler { private Timer timer; private RefreshTask task; private long refreshTime; private long time = System.currentTimeMillis(); private double allCPUTime = 0; private int allCPUPercent = -1;
