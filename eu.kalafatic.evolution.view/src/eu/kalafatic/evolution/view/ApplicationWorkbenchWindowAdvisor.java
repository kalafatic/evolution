package eu.kalafatic.evolution.view;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.MarkerTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.osgi.framework.Version;



public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private IWorkbenchWindow window;

	/** The action bar advisor. */
	private ApplicationActionBarAdvisor actionBarAdvisor;

	/** The tray item. */
	private TrayItem trayItem;

	/** The tray image. */
	private Image trayImage;

	/** The tip. */
	private ToolTip tip;
	
	
    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }
    
    @Override
    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }
    
    @Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(800, 600));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowPerspectiveBar(true);
//		configurer.setShowFastViewBars(true);
		configurer.setShowMenuBar(true);
		configurer.setShowProgressIndicator(true);

		// add the drag and drop support for the editor area
		configurer.addEditorAreaTransfer(ResourceTransfer.getInstance());
		configurer.addEditorAreaTransfer(FileTransfer.getInstance());
		configurer.addEditorAreaTransfer(MarkerTransfer.getInstance());
		configurer.addEditorAreaTransfer(LocalSelectionTransfer.getTransfer());
		configurer.addEditorAreaTransfer(EditorInputTransfer.getInstance());
		// configurer.configureEditorAreaDropListener(new EditorAreaDropAdapter(
		// configurer.getWindow()));

		final String product = Platform.getProduct().getName();
		Version version = Version.parseVersion(Platform.getProduct().getDefiningBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION));
		configurer.setTitle(product + " v" + version.getMajor() + "." + version.getMinor() + "." + version.getMicro());
	}
    @Override
	public void postWindowOpen() {
		super.postWindowOpen();

		window = getWindowConfigurer().getWindow();

		// test
		// openUpdatePopup();

		trayItem = initTrayItem(window);

		if (trayItem != null) {
			createMinimize();
			hookPopupMenu();
		}

			//window.getShell().setImage(FCoreImageConstants.MAINTAIN_IMG);
		}
		
		
		/**
		 * Hook popup menu.
		 */
		private void hookPopupMenu() {
			trayItem.addListener(SWT.MenuDetect, new Listener() {
				@Override
				public void handleEvent(Event event) {
					MenuManager trayMenu = new MenuManager();
					Menu menu = trayMenu.createContextMenu(window.getShell());
					//actionBarAdvisor.fillTrayItem(trayMenu);
					menu.setVisible(true);
				}
			});
		}

		
		/**
		 * Inits the tray item.
		 * @param window the window
		 * @return the tray item
		 */
		private TrayItem initTrayItem(IWorkbenchWindow window) {
			try {
				final Tray tray = window.getShell().getDisplay().getSystemTray();
				trayItem = new TrayItem(tray, SWT.NONE);
				//trayItem.setImage(FCoreImageConstants.MAINTAIN_IMG);
				trayItem.setToolTipText("Gemini");

				tip = new ToolTip(window.getShell(), SWT.BALLOON | SWT.ICON_INFORMATION);
				tip.setAutoHide(true);
				trayItem.setToolTip(tip);

				trayItem.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent e) {
						if (e.getSource() instanceof TrayItem) {
							TrayItem item = (TrayItem) e.getSource();
							item.dispose();
							item = null;
						}
					}
				});
			} catch (Exception e) {

			}
			return trayItem;
		}

		/**
		 * Creates the minimize.
		 */
		private void createMinimize() {
			window.getShell().addShellListener(new ShellAdapter() {
				@Override
				public void shellIconified(ShellEvent e) {
					window.getShell().setVisible(false);
				}
			});

			trayItem.addListener(SWT.DefaultSelection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					Shell shell = window.getShell();
					if (!shell.isVisible()) {
						shell.setVisible(true);
						window.getShell().setMinimized(false);
					}
				}
			});
		}

}
