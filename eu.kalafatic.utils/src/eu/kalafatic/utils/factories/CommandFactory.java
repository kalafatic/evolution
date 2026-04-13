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
package eu.kalafatic.utils.factories;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * A factory for creating Command objects.
 */
public class CommandFactory {

	/** The Constant INSTANCE. */
	public static final CommandFactory INSTANCE = new CommandFactory();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Execute command.
	 * 
	 * @param commandID
	 *            the command id
	 * @return the object
	 */
	public Object executeCommand(String commandID) {
		try {
			ICommandService cmdService = (ICommandService) PlatformUI
					.getWorkbench().getService(ICommandService.class);

			Command command = cmdService.getCommand(commandID);

			if (command.isDefined()) {
				IHandlerService handlerService = (IHandlerService) PlatformUI
						.getWorkbench().getService(IHandlerService.class);

				return handlerService.executeCommand(commandID, null);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// ---------------------------------------------------------------

	/**
	 * Execute command.
	 * 
	 * @param commandID
	 *            the command id
	 * @param args
	 *            the args
	 * @return true, if successful
	 */
	public boolean executeCommand(String commandID, String... args) {
		Command command = null;
		try {
			IHandlerService handlerService = (IHandlerService) PlatformUI
					.getWorkbench().getService(IHandlerService.class);

			ICommandService commandService = (ICommandService) PlatformUI
					.getWorkbench().getService(ICommandService.class);

			command = commandService.getCommand(commandID);

			ParameterizedCommand parameterizedCommand = createParametrizedCommand(
					command, args);

			ExecutionEvent execEvent = handlerService.createExecutionEvent(
					parameterizedCommand, null);

			command.executeWithChecks(execEvent);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates a new Command object.
	 * 
	 * @param command
	 *            the command
	 * @param args
	 *            the args
	 * @return the parameterized command
	 * @throws NotDefinedException
	 *             the not defined exception
	 */
	private ParameterizedCommand createParametrizedCommand(Command command,
			String... args) throws NotDefinedException {

		IParameter[] parameters = command.getParameters();

		Map<String, Object> params = new HashMap<String, Object>();

		for (int i = 0; i < parameters.length; i++) {
			params.put(parameters[i].getId(), args[i]);
		}

		ParameterizedCommand parameterizedCommand = ParameterizedCommand
				.generateCommand(command, params);
		return parameterizedCommand;
	}
}
