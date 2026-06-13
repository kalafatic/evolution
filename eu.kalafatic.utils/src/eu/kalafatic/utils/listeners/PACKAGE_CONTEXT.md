# PACKAGE CONTEXT

## Directory: eu.kalafatic.utils/src/eu/kalafatic/utils/listeners/

## Domain: general

## Components
* `OpenFileListener.java`: package eu.kalafatic.utils.listeners; import org.eclipse.core.commands.ExecutionException; import org.eclipse.core.commands.NotEnabledException; import org.eclipse.core.commands.NotHandledException; import org.eclipse.core.commands.common.NotDefinedException; import org.eclipse.swt.events.MouseEvent; import org.eclipse.swt.events.MouseListener; import org.eclipse.swt.events.SelectionEvent; import org.eclipse.swt.events.SelectionListener; import org.eclipse.ui.IWorkbenchPartSite; import org.eclipse.ui.handlers.IHandlerService; import eu.kalafatic.utils.lib.EHandler; public class OpenFileListener implements SelectionListener, MouseListener { private IWorkbenchPartSite site; public OpenFileListener(IWorkbenchPartSite site) { this.site = site; }
* `PerspectiveEventListener.java`: package eu.kalafatic.utils.listeners; import org.eclipse.ui.IPerspectiveDescriptor; import org.eclipse.ui.IPerspectiveListener4; import org.eclipse.ui.IWorkbenchPage; import org.eclipse.ui.IWorkbenchPartReference; import eu.kalafatic.utils.log.Log; import eu.kalafatic.utils.preferences.ECorePreferences; public class PerspectiveEventListener implements IPerspectiveListener4 { @Override public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {} @Override public void perspectiveChanged(final IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) { Log.log(ECorePreferences.MODULE, "PERSPECTVE-CHANGED : " + changeId);
