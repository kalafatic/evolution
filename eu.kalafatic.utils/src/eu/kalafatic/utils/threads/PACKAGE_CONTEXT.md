# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.utils/src/eu/kalafatic/utils/threads/

## Domain: general

## Components
* `DeadlocksManagerThread.java`: package eu.kalafatic.utils.threads; import java.awt.HeadlessException; import java.lang.management.ManagementFactory; import java.lang.management.ThreadInfo; import java.lang.management.ThreadMXBean; import java.util.ArrayList; import java.util.HashSet; import java.util.Set; import java.util.Timer; import java.util.TimerTask; public class DeadlocksManagerThread { private Timer timer; private final ThreadMXBean mbean = ManagementFactory.getThreadMXBean(); private static final int DEADLOCK_CHECK_PERIOD = 2000; private Set<Long> deadlockedSet = new HashSet<Long>(); public static ArrayList<Thread> deadlockedArray = new ArrayList<Thread>(); public static ArrayList<Thread> allThreadsArray = new ArrayList<Thread>();
