# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.utils/src/eu/kalafatic/utils/

## Domain: general

## Components
* `Activator.java`: package eu.kalafatic.utils; import java.io.File; import java.io.IOException; import java.net.URL; import java.text.MessageFormat; import java.util.Collections; import java.util.PropertyResourceBundle; import org.eclipse.core.runtime.FileLocator; import org.eclipse.core.runtime.Path; import org.eclipse.equinox.p2.ui.Policy; import org.eclipse.jface.resource.ImageDescriptor; import org.eclipse.swt.SWT; import org.eclipse.swt.widgets.Display; import org.eclipse.swt.widgets.Shell; import org.eclipse.swt.widgets.ToolTip; import org.eclipse.swt.widgets.TrayItem; import org.eclipse.ui.plugin.AbstractUIPlugin; import org.osgi.framework.Bundle; import org.osgi.framework.BundleContext; import org.osgi.framework.ServiceRegistration;
* `PreferenceInitializer.java`: package eu.kalafatic.utils; import java.io.File; import java.io.IOException; import java.net.InetAddress; import java.net.NetworkInterface; import java.util.Enumeration; import org.eclipse.core.runtime.Platform; import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer; import org.eclipse.equinox.p2.ui.Policy; import org.eclipse.swt.SWT; import org.osgi.service.prefs.BackingStoreException; import org.osgi.service.prefs.Preferences; import eu.kalafatic.utils.application.JavaUtils; import eu.kalafatic.utils.constants.FCMDConstants; import eu.kalafatic.utils.constants.FConstants; import eu.kalafatic.utils.convert.ConvertUtils; import eu.kalafatic.utils.log.Log; import eu.kalafatic.utils.model.NetInterface; import eu.kalafatic.utils.os.OSUtils; import eu.kalafatic.utils.p2.PreferenceConstants;
* `Evo.java`: package eu.kalafatic.utils; import java.lang.annotation.Retention; import java.lang.annotation.RetentionPolicy; @Retention(RetentionPolicy.SOURCE) public @interface Evo { int iteration(); String variant(); String reason(); }
