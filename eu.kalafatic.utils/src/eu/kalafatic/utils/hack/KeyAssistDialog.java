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
package eu.kalafatic.utils.hack;

/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.internal.keys.KeyAssistMessages;
import org.eclipse.ui.internal.keys.WorkbenchKeyboard;
import org.eclipse.ui.keys.IBindingService;

/**
 * The Class class KeyAssistDialog.
 *
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings("restriction")
public class KeyAssistDialog extends PopupDialog {

	/** The Constant BINDING_KEY. */
	private static final String BINDING_KEY = "Binding.bindings.jface.eclipse.org"; //$NON-NLS-1$

	/** The Constant NO_REMEMBERED_WIDTH. */
	private static final int NO_REMEMBERED_WIDTH = -1;

	/** The activity manager. */
	private final IActivityManager activityManager;

	/** The binding service. */
	private final IBindingService bindingService;

	/** The binding. */
	private Binding binding = null;

	/** The bindings. */
	private final List<Binding> bindings = new ArrayList<Binding>();

	/** The command service. */
	private final ICommandService commandService;

	/** The completions table. */
	private Table completionsTable = null;

	/** The has remembered state. */
	private boolean hasRememberedState = false;

	/** The key binding state. */
	private final KeyBindingState keyBindingState;

	/** The previous width. */
	private int previousWidth = NO_REMEMBERED_WIDTH;

	/** The workbench keyboard. */
	@SuppressWarnings("unused")
	private final WorkbenchKeyboard workbenchKeyboard;

	/** The conflict matches. */
	private SortedMap<Binding, String> conflictMatches;

	/**
	 * Instantiates a new key assist dialog.
	 *
	 * @param workbench
	 *            the workbench
	 * @param associatedKeyboard
	 *            the associated keyboard
	 * @param associatedState
	 *            the associated state
	 */

	public KeyAssistDialog(final IWorkbench workbench,
			final WorkbenchKeyboard associatedKeyboard,
			final KeyBindingState associatedState) {
		super((Shell) null, PopupDialog.INFOPOPUP_SHELLSTYLE, true, false,
				false, true, true, "", "");

		this.activityManager = workbench.getActivitySupport()
				.getActivityManager();
		this.bindingService = workbench
				.getService(IBindingService.class);
		this.commandService = workbench
				.getService(ICommandService.class);
		this.keyBindingState = associatedState;
		this.workbenchKeyboard = associatedKeyboard;

		this.setInfoText(getKeySequenceString());
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Clear remembered state.
	 */
	final void clearRememberedState() {
		previousWidth = NO_REMEMBERED_WIDTH;
		binding = null;
		hasRememberedState = false;
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.PopupDialog#close()
	 */
	@Override
	public final boolean close() {
		return close(false);
	}

	// ---------------------------------------------------------------

	/**
	 * Close.
	 *
	 * @param rememberState
	 *            the remember state
	 * @return true, if successful
	 */
	public final boolean close(final boolean rememberState) {
		return close(rememberState, true);
	}

	// ---------------------------------------------------------------

	/**
	 * Close.
	 *
	 * @param rememberState
	 *            the remember state
	 * @param resetState
	 *            the reset state
	 * @return true, if successful
	 */
	private final boolean close(final boolean rememberState,
			final boolean resetState) {
		final Shell shell = getShell();
		if (rememberState) {
			// Remember the previous width.
			final int widthToRemember;
			if ((shell != null) && (!shell.isDisposed())) {
				widthToRemember = getShell().getSize().x;
			} else {
				widthToRemember = NO_REMEMBERED_WIDTH;
			}

			// Remember the selected command name and key sequence.
			final Binding bindingToRemember;
			if ((completionsTable != null) && (!completionsTable.isDisposed())) {
				final int selectedIndex = completionsTable.getSelectionIndex();
				if (selectedIndex != -1) {
					final TableItem selectedItem = completionsTable
							.getItem(selectedIndex);
					bindingToRemember = (Binding) selectedItem
							.getData(BINDING_KEY);
				} else {
					bindingToRemember = null;
				}
			} else {
				bindingToRemember = null;
			}

			rememberState(widthToRemember, bindingToRemember);
			completionsTable = null;
		}

		if (resetState) {
			keyBindingState.reset();
		}
		return super.close();
	}

	// ---------------------------------------------------------------

	/**
	 * Configure location.
	 *
	 * @param size
	 *            the size
	 */
	private final void configureLocation(final Point size) {
		final Shell shell = getShell();

		final Shell workbenchWindowShell = keyBindingState
				.getAssociatedWindow().getShell();
		final int xCoord;
		final int yCoord;
		if (workbenchWindowShell != null) {
			/*
			 * Position the shell at the bottom right corner of the workbench
			 * window
			 */
			final Rectangle workbenchWindowBounds = workbenchWindowShell
					.getBounds();
			xCoord = workbenchWindowBounds.x + workbenchWindowBounds.width
					- size.x - 10;
			yCoord = workbenchWindowBounds.y + workbenchWindowBounds.height
					- size.y - 10;

		} else {
			xCoord = 0;
			yCoord = 0;

		}
		final Rectangle bounds = new Rectangle(xCoord, yCoord, size.x, size.y);
		shell.setBounds(getConstrainedShellBounds(bounds));
	}

	// ---------------------------------------------------------------

	/**
	 * Configure size.
	 *
	 * @return the point
	 */
	private final Point configureSize() {
		final Shell shell = getShell();

		// Get the packed size of the shell.
		shell.pack();
		final Point size = shell.getSize();

		// Use the previous width if appropriate.
		if ((previousWidth != NO_REMEMBERED_WIDTH) && (previousWidth > size.x)) {
			size.x = previousWidth;
		}

		// Enforce maximum sizing.
		final Shell workbenchWindowShell = keyBindingState
				.getAssociatedWindow().getShell();
		if (workbenchWindowShell != null) {
			final Point workbenchWindowSize = workbenchWindowShell.getSize();
			final int maxWidth = workbenchWindowSize.x * 2 / 5;
			final int maxHeight = workbenchWindowSize.y / 2;
			if (size.x > maxWidth) {
				size.x = maxWidth;
			}
			if (size.y > maxHeight) {
				size.y = maxHeight;
			}
		}

		// Set the size for the shell.
		shell.setSize(size);
		return size;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the key sequence string.
	 *
	 * @return the key sequence string
	 */
	private String getKeySequenceString() {
		final Command command = commandService
				.getCommand(IWorkbenchCommandConstants.WINDOW_SHOW_KEY_ASSIST);
		final TriggerSequence[] keyBindings = bindingService
				.getActiveBindingsFor(new ParameterizedCommand(command, null));
		final int keyBindingsCount = keyBindings.length;
		final KeySequence currentState = keyBindingState.getCurrentSequence();
		final int prefixSize = currentState.getKeyStrokes().length;

		// Try to find the first possible matching key binding.
		KeySequence keySequence = null;
		for (int i = 0; i < keyBindingsCount; i++) {
			keySequence = (KeySequence) keyBindings[i];

			// Now just double-check to make sure the key is still possible.
			if (prefixSize > 0) {
				if (keySequence.startsWith(currentState, false)) {
					/*
					 * Okay, so we have a partial match. Replace the key binding
					 * with the required suffix completion.
					 */
					final KeyStroke[] oldKeyStrokes = keySequence
							.getKeyStrokes();
					final int newSize = oldKeyStrokes.length - prefixSize;
					final KeyStroke[] newKeyStrokes = new KeyStroke[newSize];
					System.arraycopy(oldKeyStrokes, prefixSize, newKeyStrokes,
							0, newSize);
					keySequence = KeySequence.getInstance(newKeyStrokes);
					break;
				}

				/*
				 * The prefix doesn't match, so null out the key binding and try
				 * again.
				 */
				keySequence = null;
				continue;

			}

			// There is no prefix, so just grab the first.
			break;
		}
		if (keySequence == null) {
			return null; // couldn't find a suitable key binding
		}

		return NLS.bind(KeyAssistMessages.openPreferencePage,
				keySequence.format());
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.dialogs.PopupDialog#createDialogArea(org.eclipse.swt
	 * .widgets.Composite)
	 */
	@Override
	protected final Control createDialogArea(final Composite parent) {
		// First, register the shell type with the context support
		registerShellType();

		// Create a composite for the dialog area.
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout compositeLayout = new GridLayout();
		compositeLayout.marginHeight = 0;
		compositeLayout.marginWidth = 0;
		composite.setLayout(compositeLayout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setBackground(parent.getBackground());

		// Layout the partial matches.
		final SortedMap<Binding, String> partialMatches;
		if (conflictMatches != null) {
			partialMatches = conflictMatches;
			conflictMatches = null;
		} else {
			partialMatches = getPartialMatches();
		}

		if (partialMatches.isEmpty()) {
			createEmptyDialogArea(composite);
		} else {
			createTableDialogArea(composite, partialMatches);
		}
		return composite;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the empty dialog area.
	 *
	 * @param parent
	 *            the parent
	 */
	private final void createEmptyDialogArea(final Composite parent) {
		final Label noMatchesLabel = new Label(parent, SWT.NULL);
//		noMatchesLabel.setText(KeyAssistMessages.NoMatches_Message);
		noMatchesLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
		noMatchesLabel.setBackground(parent.getBackground());
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the table dialog area.
	 *
	 * @param parent
	 *            the parent
	 * @param partialMatches
	 *            the partial matches
	 */
	private final void createTableDialogArea(final Composite parent,
			final SortedMap<Binding, String> partialMatches) {
		// Layout the table.
		completionsTable = new Table(parent, SWT.FULL_SELECTION | SWT.SINGLE);
		final GridData gridData = new GridData(GridData.FILL_BOTH);
		completionsTable.setLayoutData(gridData);
		completionsTable.setBackground(parent.getBackground());
		completionsTable.setLinesVisible(true);

		// Initialize the columns and rows.
		bindings.clear();
		final TableColumn columnCommandName = new TableColumn(completionsTable,
				SWT.LEFT, 0);
		final TableColumn columnKeySequence = new TableColumn(completionsTable,
				SWT.LEFT, 1);
		final Iterator<?> itemsItr = partialMatches.entrySet().iterator();
		while (itemsItr.hasNext()) {
			@SuppressWarnings("rawtypes")
			final Map.Entry entry = (Map.Entry) itemsItr.next();
			final String sequence = (String) entry.getValue();
			final Binding binding = (Binding) entry.getKey();
			final ParameterizedCommand command = binding
					.getParameterizedCommand();
			try {
				final String[] text = { command.getName(), sequence };
				final TableItem item = new TableItem(completionsTable, SWT.NULL);
				item.setText(text);
				item.setData(BINDING_KEY, binding);
				bindings.add(binding);
			} catch (NotDefinedException e) {
				// Not much to do, but this shouldn't really happen.
			}
		}

		Dialog.applyDialogFont(parent);
		columnKeySequence.pack();
		if (previousWidth != NO_REMEMBERED_WIDTH) {
			columnKeySequence.setWidth(previousWidth);
		}
		columnCommandName.pack();
		if (completionsTable.getItems().length > 0) {
			completionsTable.setSelection(0);
		}

		/*
		 * If you double-click on the table, it should execute the selected
		 * command.
		 */
		completionsTable.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public final void handleEvent(final Event event) {
				executeKeyBinding(event);
			}
		});
	}

	// ---------------------------------------------------------------

	/**
	 * Edits the key binding.
	 */
	private final void editKeyBinding() {
		// Create a preference dialog on the keys preference page.
		final String keysPageId = "org.eclipse.ui.preferencePages.Keys"; //$NON-NLS-1$
		final PreferenceDialog dialog = PreferencesUtil
				.createPreferenceDialogOn(getShell(), keysPageId, null, binding);

		/*
		 * Forget the remembered state (so we don't get stuck editing
		 * preferences).
		 */
		clearRememberedState();

		// Open the dialog (blocking).
		dialog.open();
	}

	// ---------------------------------------------------------------

	/**
	 * Execute key binding.
	 *
	 * @param trigger
	 *            the trigger
	 */
	private final void executeKeyBinding(final Event trigger) {
		// Try to execute the corresponding command.
		final int selectionIndex = completionsTable.getSelectionIndex();
		if (selectionIndex >= 0) {
			// final Binding binding = bindings.get(selectionIndex);
			// try {
			// workbenchKeyboard.updateShellKludge(null);
			// workbenchKeyboard.executeCommand(binding, trigger);
			// } catch (final CommandException e) {
			// workbenchKeyboard.logException(e,
			// binding.getParameterizedCommand());
			// }
		}
	}

	/**
	 * Gets the partial matches.
	 *
	 * @return the partial matches
	 */
	private final SortedMap<Binding, String> getPartialMatches() {
		// Put all partial matches into the matches into the map.
		final Map<?, ?> partialMatches = bindingService
				.getPartialMatches(keyBindingState.getCurrentSequence());

		// Create a sorted map that sorts based on lexicographical order.
		final SortedMap<Binding, String> sortedMatches = new TreeMap<Binding, String>();
//		final SortedMap<Binding, String> sortedMatches = new TreeMap<Binding, String>(
//				new Comparator<Object>() {
//					@Override
//					public final int compare(final Object a, final Object b) {
//						final Binding bindingA = (Binding) a;
//						final Binding bindingB = (Binding) b;
//						final ParameterizedCommand commandA = bindingA
//								.getParameterizedCommand();
//						final ParameterizedCommand commandB = bindingB
//								.getParameterizedCommand();
//						try {
//							return commandA.getName().compareTo(
//									commandB.getName());
//						} catch (final NotDefinedException e) {
//							// should not happen
//							return 0;
//						}
//					}
//				});

		/*
		 * Remove those partial matches for which either the command is not
		 * identified or the activity manager believes the command is not
		 * enabled.
		 */
		final Iterator<?> partialMatchItr = partialMatches.entrySet()
				.iterator();
		while (partialMatchItr.hasNext()) {
			@SuppressWarnings("rawtypes")
			final Map.Entry entry = (Map.Entry) partialMatchItr.next();
			final Binding binding = (Binding) entry.getValue();
			final Command command = binding.getParameterizedCommand()
					.getCommand();
			if (command.isDefined()
					&& activityManager.getIdentifier(command.getId())
							.isEnabled()) {
				TriggerSequence bestActiveBindingFor = bindingService
						.getBestActiveBindingFor(binding
								.getParameterizedCommand());
				sortedMatches.put(binding, bestActiveBindingFor == null ? null
						: bestActiveBindingFor.format());
			}
		}

		return sortedMatches;

	}

	/**
	 * Checks for remembered state.
	 *
	 * @return true, if successful
	 */
	private final boolean hasRememberedState() {
		return hasRememberedState;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.PopupDialog#open()
	 */
	@Override
	public final int open() {
		// If there is remember state, open the preference page.
		if (hasRememberedState()) {
			editKeyBinding();
			clearRememberedState();
			return Window.OK;
		}

		// If the dialog is already open, dispose the shell and recreate it.
		final Shell shell = getShell();
		if (shell != null) {
			close(false, false);
		}
		create();

		// Configure the size and location.
		final Point size = configureSize();
		configureLocation(size);

		// Call the super method.
		return super.open();
	}

	/**
	 * Open.
	 *
	 * @param bindings
	 *            the bindings
	 * @return the int
	 */
	public final int open(Collection<?> bindings) {
//		conflictMatches = new TreeMap<Binding, String>(
//				new Comparator<Object>() {
//					@Override
//					public final int compare(final Object a, final Object b) {
//						final Binding bindingA = (Binding) a;
//						final Binding bindingB = (Binding) b;
//						final ParameterizedCommand commandA = bindingA
//								.getParameterizedCommand();
//						final ParameterizedCommand commandB = bindingB
//								.getParameterizedCommand();
//						try {
//							return commandA.getName().compareTo(
//									commandB.getName());
//						} catch (final NotDefinedException e) {
//							// should not happen
//							return 0;
//						}
//					}
//				});
//		Iterator<?> i = bindings.iterator();
//		while (i.hasNext()) {
//			Binding b = (Binding) i.next();
//			TriggerSequence bestActiveBindingFor = bindingService
//					.getBestActiveBindingFor(b.getParameterizedCommand());
//			conflictMatches.put(b, bestActiveBindingFor == null ? null
//					: bestActiveBindingFor.format());
//		}

		// If the dialog is already open, dispose the shell and recreate it.
		final Shell shell = getShell();
		if (shell != null) {
			close(false, false);
		}
		create();

		// Configure the size and location.
		final Point size = configureSize();
		configureLocation(size);

		// Call the super method.
		return super.open();
	}

	/**
	 * Register shell type.
	 */
	private final void registerShellType() {
		final Shell shell = getShell();
		final IContextService contextService = keyBindingState
				.getAssociatedWindow().getWorkbench()
				.getService(IContextService.class);
		contextService.registerShell(shell,
				contextService.getShellType((Shell) shell.getParent()));
	}

	/**
	 * Remember state.
	 *
	 * @param previousWidth
	 *            the previous width
	 * @param binding
	 *            the binding
	 */
	private final void rememberState(final int previousWidth,
			final Binding binding) {
		this.previousWidth = previousWidth;
		this.binding = binding;
		hasRememberedState = true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.window.Window#setParentShell(org.eclipse.swt.widgets
	 * .Shell)
	 */
	@Override
	protected final void setParentShell(final Shell newParentShell) {
		super.setParentShell(newParentShell);
	}
}
