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
package eu.kalafatic.utils.application;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.utils.constants.FConstants;

/**
 * The Class class ValidationUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class ValidationUtils {

	/** The Constant INSTANCE. */
	public static final ValidationUtils INSTANCE = new ValidationUtils();

	/** The decorator map. */
	public final Map<Control, ControlDecoration> decoratorMap = new HashMap<Control, ControlDecoration>();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the discover port validator.
	 * @param control the control
	 * @return the discover port validator
	 */
	public IValidator getDiscoverPortValidator(final Text control) {
		getControlDecorator(control);

		IValidator iValidator = new IValidator() {
			@Override
			public IStatus validate(Object value) {
				IStatus status = ValidationStatus.error("Some error!");
				try {
					String text = (String) value;
					if (text.trim().length() != 0) {
						int parseInt = Integer.parseInt(text);

						if (parseInt < (Integer) FConstants.WELL_KNOWN_PORTS[2]) {
							status = ValidationStatus.warning((String) FConstants.WELL_KNOWN_PORTS[0]);
						} else if (parseInt < (Integer) FConstants.REGISTERED_PORTS[2]) {
							status = ValidationStatus.info((String) FConstants.REGISTERED_PORTS[0]);
						} else if (parseInt < (Integer) FConstants.DYNAMIC_PORTS[2]) {
							status = ValidationStatus.cancel((String) FConstants.DYNAMIC_PORTS[0]);
						}
					}
				} catch (NumberFormatException e) {
					status = ValidationStatus.error("Type number!");
				} catch (Exception e) {
					e.printStackTrace();
				}
				showDecoration(control, status);
				return status;
			}
		};
		return iValidator;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the control decorator.
	 * @param control the control
	 * @return the control decorator
	 */
	public ControlDecoration getControlDecorator(Control control) {
		ControlDecoration decoration = null;
		if ((decoration = decoratorMap.get(control)) == null) {
			decoration = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
			decoratorMap.put(control, decoration);
		}
		return decoration;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the control decorator.
	 * @param control the control
	 * @param status the status
	 * @param message the message
	 * @return the control decoration
	 */
	public ControlDecoration createControlDecorator(Control control, int status, String message) {
		ControlDecoration decoration = getControlDecorator(control);

		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);

		switch (status) {
		case IStatus.OK:
			break;
		case IStatus.INFO:
			fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
			break;
		case IStatus.WARNING:
			fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING);
			break;
		case IStatus.ERROR:
			fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
			break;
		case IStatus.CANCEL:
			fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_REQUIRED);
			break;
		default:
			break;
		}
		decoration.setDescriptionText(message);
		decoration.setImage(fieldDecoration.getImage());

		return decoration;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the req control decorator.
	 * @param control the control
	 * @return the control decoration
	 */
	public ControlDecoration createReqControlDecorator(Control control) {
		return createControlDecorator(control, IStatus.INFO, "Required");
	}

	// ---------------------------------------------------------------

	/**
	 * Show dirty decoration.
	 * @param control the control
	 */
	public void showDirtyDecoration(Control control) {
		showDecoration(control, IStatus.CANCEL, "Dirty");
	}

	// ---------------------------------------------------------------

	/**
	 * Show decoration.
	 * @param control the control
	 * @param status the status
	 */
	public void showDecoration(Control control, IStatus status) {
		showDecoration(control, status.getSeverity(), status.getMessage());
	}

	// ---------------------------------------------------------------

	/**
	 * Show decoration.
	 * @param control the control
	 * @param status the status
	 * @param message the message
	 */
	public void showDecoration(Control control, int status, String message) {
		ControlDecoration decoration = decoratorMap.get(control);

		FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);

		switch (status) {
		case IStatus.OK:
		case IStatus.INFO:
			break;
		case IStatus.WARNING:
			fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING);
			break;
		case IStatus.ERROR:
			fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
			break;
		case IStatus.CANCEL:
			fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_REQUIRED);
			break;
		default:
			break;
		}
		decoration.setDescriptionText(message);
		decoration.setImage(fieldDecoration.getImage());
		decoration.show();
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * The Class class LengthValidator.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	public class LengthValidator implements IInputValidator {

		/** The to. */
		int from, to;

		/**
		 * Instantiates a new length validator.
		 * @param from the from
		 * @param to the to
		 */
		public LengthValidator(int from, int to) {
			this.from = from;
			this.to = to;
		}

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.IInputValidator#isValid(java.lang.String)
		 */
		@Override
		public String isValid(String newText) {
			int len = newText.length();

			// Determine if input is too short or too long
			if (len < from) {
				return "Too short";
			}
			if (len > to) {
				return "Too long";
			}
			// Input must be OK
			return null;
		}
	}

}
