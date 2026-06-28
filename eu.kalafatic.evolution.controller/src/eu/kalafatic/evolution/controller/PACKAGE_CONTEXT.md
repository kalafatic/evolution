# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/

## Domain: general

## Components
* `AppData.java`: package eu.kalafatic.evolution.controller; import java.util.ArrayList; import java.util.HashMap; import java.util.List; import java.util.Map; import org.eclipse.jface.action.IStatusLineManager; import org.eclipse.swt.widgets.TrayItem; import eu.kalafatic.evolution.controller.splashHandlers.ISplashUser; import eu.kalafatic.utils.hack.EclipseSplashHandler; import eu.kalafatic.utils.hack.StatusLineContributionItem; public final class AppData { private TrayItem trayItem; private IStatusLineManager statusLineManager; private StatusLineContributionItem msgItem, cpuItem, speedUpItem, speedDownItem; private float allUpSpeed, allDownSpeed; private EclipseSplashHandler splashHandler; private final List<ISplashUser> splashUsers = new ArrayList<ISplashUser>();
