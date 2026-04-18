package eu.kalafatic.evolution.model.orchestration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Platform Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPlatformType()
 * @model
 * @generated
 */
public enum PlatformType implements Enumerator {
	/**
	 * The '<em><b>SIMPLE CHAT</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SIMPLE_CHAT_VALUE
	 * @generated
	 * @ordered
	 */
	SIMPLE_CHAT(0, "SIMPLE_CHAT", "SIMPLE_CHAT"),

	/**
	 * The '<em><b>ASSISTED CODING</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ASSISTED_CODING_VALUE
	 * @generated
	 * @ordered
	 */
	ASSISTED_CODING(1, "ASSISTED_CODING", "ASSISTED_CODING"),

	/**
	 * The '<em><b>DARWIN MODE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DARWIN_MODE_VALUE
	 * @generated
	 * @ordered
	 */
	DARWIN_MODE(2, "DARWIN_MODE", "DARWIN_MODE"),

	/**
	 * The '<em><b>SELF DEV MODE</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SELF_DEV_MODE_VALUE
	 * @generated
	 * @ordered
	 */
	SELF_DEV_MODE(3, "SELF_DEV_MODE", "SELF_DEV_MODE");

	/**
	 * The '<em><b>SIMPLE CHAT</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SIMPLE_CHAT
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int SIMPLE_CHAT_VALUE = 0;

	/**
	 * The '<em><b>ASSISTED CODING</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #ASSISTED_CODING
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int ASSISTED_CODING_VALUE = 1;

	/**
	 * The '<em><b>DARWIN MODE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #DARWIN_MODE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int DARWIN_MODE_VALUE = 2;

	/**
	 * The '<em><b>SELF DEV MODE</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #SELF_DEV_MODE
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int SELF_DEV_MODE_VALUE = 3;

	/**
	 * An array of all the literals of the {@link PlatformType} enumeration.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final PlatformType[] VALUES_ARRAY =
		new PlatformType[] {
			SIMPLE_CHAT,
			ASSISTED_CODING,
			DARWIN_MODE,
			SELF_DEV_MODE,
		};

	/**
	 * A public read-only list of all the literals of the {@link PlatformType} enumeration.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<PlatformType> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Platform Type</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static PlatformType get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			PlatformType result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Platform Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static PlatformType getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			PlatformType result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Platform Type</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static PlatformType get(int value) {
		switch (value) {
			case SIMPLE_CHAT_VALUE: return SIMPLE_CHAT;
			case ASSISTED_CODING_VALUE: return ASSISTED_CODING;
			case DARWIN_MODE_VALUE: return DARWIN_MODE;
			case SELF_DEV_MODE_VALUE: return SELF_DEV_MODE;
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
	private PlatformType(int value, String name, String literal) {
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

} //PlatformType
