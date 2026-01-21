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
package eu.kalafatic.evolution.view;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.splash.SplashHandlerFactory;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.MarkerTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.osgi.framework.Version;

import eu.kalafatic.utils.constants.FCoreImageConstants;
import eu.kalafatic.utils.dialogs.GeminiSplashHandler;
import eu.kalafatic.utils.dialogs.GeminiSplashHandler.GSHf;
import eu.kalafatic.utils.hack.EclipseSplashHandler;
import eu.kalafatic.utils.interfaces.ISplashUser;
import eu.kalafatic.utils.lib.AppData;
import eu.kalafatic.utils.log.Log;
import eu.kalafatic.utils.preferences.ECorePreferences;



/**
 * The Class class ApplicationWorkbenchWindowAdvisor.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor implements ISplashUser {

	/** The tray item. */
	private TrayItem trayItem;

	/** The tray image. */
	private Image trayImage;

	/** The tip. */
	private ToolTip tip;

	/** The display. */
	private Display display;

	/** The splash. */
	private static GeminiSplashHandler splash;

	/** The action bar advisor. */
	private ApplicationActionBarAdvisor actionBarAdvisor;

	/** The window. */
	private IWorkbenchWindow window;

	private IPreferenceStore apiStore;

	/**
	 * Instantiates a new application workbench window advisor.
	 * @param configurer the configurer
	 */
	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#createActionBarAdvisor (org.eclipse.ui.application.IActionBarConfigurer)
	 */
	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		actionBarAdvisor = new ApplicationActionBarAdvisor(configurer);
		return actionBarAdvisor;
	}
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#preWindowOpen()
	 */
	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(800, 600));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowPerspectiveBar(true);
		//configurer.setShowFastViewBars(true);
		configurer.setShowMenuBar(true);
		configurer.setShowProgressIndicator(true);

		// add the drag and drop support for the editor area
		configurer.addEditorAreaTransfer(ResourceTransfer.getInstance());
		configurer.addEditorAreaTransfer(FileTransfer.getInstance());
		configurer.addEditorAreaTransfer(MarkerTransfer.getInstance());
		configurer.addEditorAreaTransfer(LocalSelectionTransfer.getTransfer());
		configurer.addEditorAreaTransfer(EditorInputTransfer.getInstance());
		// configurer
		// .configureEditorAreaDropListener(new
		// EditorAreaDropAdapter(configurer.getWindow()));

		final String product = Platform.getProduct().getName();
		Version version = Version.parseVersion(Platform.getProduct().getDefiningBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION));
		configurer.setTitle(product + " v" + version.getMajor() + "." + version.getMinor() + "." + version.getMicro());

		apiStore = PlatformUI.getPreferenceStore();
		apiStore.setValue(IWorkbenchPreferenceConstants.DOCK_PERSPECTIVE_BAR, "TOP_LEFT");

	}

	// ---------------------------------------------------------------

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

			window.getShell().setImage(FCoreImageConstants.GEMINI_IMG);
		}
		// Activator.processMessages();
//		CMDUtils.getInstance().runAfterStart();
//		AppUtils.getInstance().createProject(Platform.getProduct().getName());
		initListeners();
//		setUpPreferences();

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				createProject(Platform.getProduct().getName());

			}
		});
	}

	// ---------------------------------------------------------------

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#dispose()
		 */
		@Override
		public void dispose() {
			if (trayImage != null) {
				trayImage.dispose();
				trayItem.dispose();
			}
		}

		// ---------------------------------------------------------------

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

	// ---------------------------------------------------------------

	public IProject createProject(String projectName) {
		try {
//			ResourceSet rs = new ResourceSetImpl();
//			rs.getResourceFactoryRegistry()
//			  .getExtensionToFactoryMap()
//			  .put("xmi", new XMIResourceFactoryImpl());
//
//			rs.getPackageRegistry()
//			  .put(MyPackage.eNS_URI, MyPackage.eINSTANCE);
//
//			Resource r = rs.createResource(URI.createFileURI("example.xmi"));
//
//			MyRoot root = MyFactory.eINSTANCE.createMyRoot();
//			root.setName("test");
//
//			r.getContents().add(root);
//			r.save(null);
			
			
			Assert.isNotNull(projectName);
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot root = workspace.getRoot();
			IProject project = root.getProject(projectName);

			IFolder folder = project.getFolder("Core");
			IFile file = folder.getFile(projectName + ".xml");
			// at this point, no resources have been created
			if (!project.exists()) {
				project.create(null);
			}
			if (!project.isOpen()) {
				project.open(null);
			}
			if (!folder.exists()) {
				folder.create(IResource.NONE, true, null);
			}
			if (!file.exists()) {
				byte[] bytes = ("<!-- " + projectName + "� -->").getBytes();
				InputStream source = new ByteArrayInputStream(bytes);
				file.create(source, IResource.NONE, null);
			}
			return project;
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	// ---------------------------------------------------------------

		/**
		 * Gets the splash.
		 * @return the splash
		 */
		@SuppressWarnings({ "unused" })
		private EclipseSplashHandler getSplash() {
			if (splash == null) {

				IProduct product = Platform.getProduct();
				if (product != null) {
					splash = (GeminiSplashHandler) SplashHandlerFactory.findSplashHandlerFor(product);
				}
				if (splash == null) {
					splash = (GeminiSplashHandler) new EclipseSplashHandler();
				}
			}
			return splash;
		}

		// ---------------------------------------------------------------

		/**
		 * Hook popup menu.
		 */
		private void hookPopupMenu() {
			trayItem.addListener(SWT.MenuDetect, new Listener() {
				@Override
				public void handleEvent(Event event) {
					MenuManager trayMenu = new MenuManager();
					Menu menu = trayMenu.createContextMenu(window.getShell());
					actionBarAdvisor.fillTrayItem(trayMenu);
					menu.setVisible(true);
				}
			});
		}

		// ---------------------------------------------------------------

		/**
		 * Inits the tray item.
		 * @param window the window
		 * @return the tray item
		 */
		private TrayItem initTrayItem(IWorkbenchWindow window) {
			try {
				final Tray tray = window.getShell().getDisplay().getSystemTray();
				trayItem = new TrayItem(tray, SWT.NONE);
				trayItem.setImage(FCoreImageConstants.GEMINI_IMG);
				trayItem.setToolTipText("Gemini");

				tip = new ToolTip(window.getShell(), SWT.BALLOON | SWT.ICON_INFORMATION);
				tip.setAutoHide(true);
				trayItem.setToolTip(tip);

				AppData.getInstance().setTrayItem(trayItem);

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
				Log.log(ECorePreferences.MODULE, e);
			}
			return trayItem;
		}

		// ---------------------------------------------------------------

		/**
		 * Inits the listeners.
		 */
		private void initListeners() {
			PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {
				@Override
				public void windowOpened(IWorkbenchWindow window) {}

				@Override
				public void windowDeactivated(IWorkbenchWindow window) {}

				@Override
				public void windowClosed(IWorkbenchWindow window) {
					endPlugin();
				}

				@Override
				public void windowActivated(IWorkbenchWindow window) {}
			});

			window.addPerspectiveListener(new IPerspectiveListener() {

				@Override
				public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
					// if (page.isEditorAreaVisible()) {
					// IEditorReference[] editores = page.getEditorReferences();
					// if (editores.length <= 0) {
					// page.setEditorAreaVisible(false);
					// }
					// }
				}

				@Override
				public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {}
			});
		}

		// ---------------------------------------------------------------




		/*
		 * (non-Javadoc)
		 * @see eu.kalafatic.gemini.core.interfaces.ISplashUser#endWithProgress()
		 */
		@Override
		public void endWithProgress(final GeminiSplashHandler splashHandler) {
			try {
				splashHandler.startSubTask("Stopping modules ...", (GSHf.FLAG & ~GSHf.VISIBLE));
				Thread.sleep(500);

			} catch (Exception e) {
				Log.log(ECorePreferences.MODULE, e);
			}
		}

		// ---------------------------------------------------------------


		/**
		 * End plugin.
		 */
		private void endPlugin() {
			display = getCurrentDisplay(Display.getCurrent().getThread());

			display.asyncExec(new Runnable() {

				Thread finishThread = new Thread() {
					@Override
					public void run() {
						finishApplications(splash);
					};
				};

				Thread pendingThread = new Thread() {
					@Override
					public void run() {
						splash.runPending();
					};
				};

				@Override
				public void run() {
					try {
						splash = (GeminiSplashHandler) AppData.getInstance().getSplashHandler();
						splash.setEndSplash(true);
						Shell shell = splash.createUI(display);
						splash.init(shell);
						splash.setMonitor();

						splash.getBundleProgressMonitor();

						shell.open();

						pendingThread.start();
						finishThread.start();

						while (((GSHf.FLAG & GSHf.DONE) == 0)) {
							if (display.getThread() == Thread.currentThread()) {
								splash.update();
								Thread.sleep(250);
							}
						}
					} catch (Exception e) {
						Log.log(ECorePreferences.MODULE, e);
					} finally {
						if (splash != null) {
							splash.done();
						}
					}
				}

				// --------------------------------------------------------------

				private void finishApplications(GeminiSplashHandler splashHandler) {
					try {
						final List<ISplashUser> splashUsers = AppData.getInstance().getSplashUsersUsers();

						final int users = splashUsers.size() + 1;

						int oneAlpha = (users > 0) ? (200 / (users * 4)) : 0;

						int alpha = splashHandler.getAlpha();

						splashHandler.startTask("Closing Gemini ", users * 100);

						for (ISplashUser iSplashUser : splashUsers) {
							splashHandler.setAlpha(alpha -= oneAlpha / 2);

							iSplashUser.endWithProgress(splashHandler);
							Thread.sleep(500);

							splashHandler.setAlpha(alpha -= oneAlpha / 2);
							Thread.sleep(500);

							GSHf.FLAG |= GSHf.TASK_END;
						}
						GSHf.FLAG |= GSHf.TASK_END;
						Thread.sleep(500);

					} catch (Exception e) {
						Log.log(ECorePreferences.MODULE, e);
					} finally {
						GSHf.FLAG = GSHf.DONE;
					}
				}
			});
		}

		// ---------------------------------------------------------------

		/**
		 * Gets the current display.
		 * @param currentThread the current thread
		 * @return the current display
		 */
		private Display getCurrentDisplay(Thread currentThread) {
			Display display = null;
			if ((display = Display.getDefault()) != null) {
				return display;
			} else if ((display = Display.getCurrent()) != null) {
				return display;
			} else if ((display = Display.findDisplay(currentThread)) != null) {
				return display;
			}
			return display;
		}
}
