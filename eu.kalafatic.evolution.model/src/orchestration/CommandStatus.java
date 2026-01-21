package orchestration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum CommandStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
;

/**
	 * The '<em><b>PENDING</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #PENDING
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int PENDING_VALUE = 0;
/**
	 * The '<em><b>RUNNING</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RUNNING
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int RUNNING_VALUE = 0;
/**
	 * The '<em><b>COMPLETED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #COMPLETED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int COMPLETED_VALUE = 0;
/**
	 * The '<em><b>FAILED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #FAILED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int FAILED_VALUE = 0;
/**
	 * An array of all the '<em><b>Command Status</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final CommandStatus[] VALUES_ARRAY =
		new CommandStatus[] {
			PENDING,
			RUNNING,
			COMPLETED,
			FAILED,
		};
/**
	 * A public read-only list of all the '<em><b>Command Status</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<CommandStatus> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

/**
	 * Returns the '<em><b>Command Status</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static CommandStatus get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			CommandStatus result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

/**
	 * Returns the '<em><b>Command Status</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static CommandStatus getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			CommandStatus result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

/**
	 * Returns the '<em><b>Command Status</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static CommandStatus get(int value) {
		switch (value) {
			case PENDING_VALUE: return PENDING;
		}
		return null;
	}

/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final int value;
/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String name;
/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private final String literal;

/**
	 * Only this class can construct instances.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private CommandStatus(int value, String name, String literal) {
		this.value = value;
		this.name = name;
		this.literal = literal;
	}

/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public int getValue() {
	  return value;
	}

/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
	  return name;
	}

/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getLiteral() {
	  return literal;
	}

/**
	 * Returns the literal value of the enumerator, which is its string representation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		return literal;
	}}
