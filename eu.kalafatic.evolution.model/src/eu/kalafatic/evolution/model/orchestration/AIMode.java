/**
 */
package eu.kalafatic.evolution.model.orchestration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>AI Mode</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIMode()
 * @model instanceClass="eu.kalafatic.evolution.model.orchestration.AIMode"
 * @generated
 */
public enum AIMode implements Enumerator {
	/**
	 * The '<em><b>LOCAL</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #LOCAL_VALUE
	 * @generated
	 * @ordered
	 */
	LOCAL(0, "LOCAL", "Local"),

	/**
	 * The '<em><b>HYBRID</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #HYBRID_VALUE
	 * @generated
	 * @ordered
	 */
	HYBRID(1, "HYBRID", "Hybrid"),

	/**
	 * The '<em><b>REMOTE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REMOTE_VALUE
	 * @generated
	 * @ordered
	 */
	REMOTE(2, "REMOTE", "Remote");

	/**
	 * The '<em><b>LOCAL</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #LOCAL
	 * @model literal="Local"
	 * @generated
	 * @ordered
	 */
	public static final int LOCAL_VALUE = 0;

	/**
	 * The '<em><b>HYBRID</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #HYBRID
	 * @model literal="Hybrid"
	 * @generated
	 * @ordered
	 */
	public static final int HYBRID_VALUE = 1;

	/**
	 * The '<em><b>REMOTE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REMOTE
	 * @model literal="Remote"
	 * @generated
	 * @ordered
	 */
	public static final int REMOTE_VALUE = 2;

	/**
	 * An array of all the '<em><b>AI Mode</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final AIMode[] VALUES_ARRAY =
		new AIMode[] {
			LOCAL,
			HYBRID,
			REMOTE,
		};

	/**
	 * A public read-only list of all the '<em><b>AI Mode</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<AIMode> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>AI Mode</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static AIMode get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			AIMode result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>AI Mode</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static AIMode getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			AIMode result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>AI Mode</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static AIMode get(int value) {
		switch (value) {
			case LOCAL_VALUE: return LOCAL;
			case HYBRID_VALUE: return HYBRID;
			case REMOTE_VALUE: return REMOTE;
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
	private AIMode(int value, String name, String literal) {
		this.value = value;
		this.name = name;
		this.literal = literal;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getValue() {
	  return value;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName() {
	  return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
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
	}
	
} //AIMode
