# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/editors/pages/settings/

## Domain: general

## Components
* `NetworkSettingsGroup.java`: package eu.kalafatic.evolution.view.editors.pages.settings; import org.eclipse.jface.viewers.ArrayContentProvider; import org.eclipse.jface.viewers.ColumnLabelProvider; import org.eclipse.jface.viewers.TableViewer; import org.eclipse.jface.viewers.TableViewerColumn; import org.eclipse.swt.SWT; import org.eclipse.swt.layout.GridData; import org.eclipse.swt.widgets.Button; import org.eclipse.swt.widgets.Composite; import org.eclipse.swt.widgets.Display; import org.eclipse.swt.widgets.Table; import org.eclipse.ui.forms.widgets.FormToolkit; import eu.kalafatic.evolution.controller.discovery.NetworkDiscoveryService; import eu.kalafatic.evolution.model.orchestration.NetworkEntry; import eu.kalafatic.evolution.model.orchestration.Orchestrator; import eu.kalafatic.evolution.view.editors.MultiPageEditor; import eu.kalafatic.evolution.view.editors.pages.AEvoGroup; import eu.kalafatic.evolution.view.editors.pages.SettingsPage; import eu.kalafatic.utils.factories.GUIFactory; public class NetworkSettingsGroup extends AEvoGroup {
