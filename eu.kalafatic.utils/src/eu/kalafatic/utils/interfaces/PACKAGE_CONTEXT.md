# PACKAGE CONTEXT

## Directory: eu.kalafatic.utils/src/eu/kalafatic/utils/interfaces/

## Domain: general

## Components
* `ESyncType.java`: package eu.kalafatic.utils.interfaces; public enum ESyncType { DEF(0, "Default"),
* `ESync.java`: package eu.kalafatic.utils.interfaces; public enum ESync { SYNC_DB("Sync DB", true),
* `ISWizard.java`: package eu.kalafatic.utils.interfaces; import org.eclipse.swt.widgets.Table; public interface ISWizard extends ISWizardSettings {
* `ATreeViewer.java`: package eu.kalafatic.utils.interfaces; import org.eclipse.jface.action.Action; import org.eclipse.jface.action.IMenuManager;
* `IWizardHandler.java`: package eu.kalafatic.utils.interfaces; import org.eclipse.core.commands.ExecutionEvent; import org.eclipse.ui.wizards.IWizardRegistry;
* `PACKAGE_CONTEXT.md`: 
* `ISWizardSettings.java`: package eu.kalafatic.utils.interfaces; public interface ISWizardSettings { public final String ENABLED = "Enabled";
* `ASync.java`: package eu.kalafatic.utils.interfaces; public final class ASync { public static final int ORIGINAL = 1 << 0;
* `IServerThread.java`: package eu.kalafatic.utils.interfaces; import java.io.IOException; import java.net.ServerSocket;
* `IFormInput.java`: package eu.kalafatic.utils.interfaces; public interface IFormInput { boolean setInput();
* `ISplashUser.java`: package eu.kalafatic.utils.interfaces; import eu.kalafatic.utils.dialogs.GeminiSplashHandler; public interface ISplashUser {
* `ALog.java`: package eu.kalafatic.utils.interfaces; import static eu.kalafatic.utils.constants.FConstants.PREFERENCES; import java.util.logging.Logger;
* `IPreference.java`: package eu.kalafatic.utils.interfaces; public interface IPreference { String getName();
* `AServerThread.java`: package eu.kalafatic.utils.interfaces; import java.io.IOException; import java.net.InetSocketAddress;
* `AViewer.java`: package eu.kalafatic.utils.interfaces; import java.util.Collection; import org.eclipse.jface.action.IMenuListener;
* `ILog.java`: package eu.kalafatic.utils.interfaces; import static eu.kalafatic.utils.constants.FConstants.PREFERENCES; import eu.kalafatic.utils.preferences.ECorePreferences;
* `IViewer.java`: package eu.kalafatic.utils.interfaces; import java.util.Collection; import org.eclipse.ui.IPerspectiveListener;
