/**
 */
package eu.kalafatic.evolution.model.orchestration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Review Decision</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getReviewDecision()
 * @model
 * @generated
 */
public enum ReviewDecision implements Enumerator {
	/**
	 * The '<em><b>OPEN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #OPEN_VALUE
	 * @generated
	 * @ordered
	 */
	OPEN(0, "OPEN", "OPEN"),

	/**
	 * The '<em><b>IN REVIEW</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #IN_REVIEW_VALUE
	 * @generated
	 * @ordered
	 */
	IN_REVIEW(1, "IN_REVIEW", "IN_REVIEW"),

	/**
	 * The '<em><b>APPROVED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #APPROVED_VALUE
	 * @generated
	 * @ordered
	 */
	APPROVED(2, "APPROVED", "APPROVED"),

	/**
	 * The '<em><b>REJECTED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REJECTED_VALUE
	 * @generated
	 * @ordered
	 */
	REJECTED(3, "REJECTED", "REJECTED"),

	/**
	 * The '<em><b>CHANGES REQUESTED</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #CHANGES_REQUESTED_VALUE
	 * @generated
	 * @ordered
	 */
	CHANGES_REQUESTED(4, "CHANGES_REQUESTED", "CHANGES_REQUESTED");

	/**
	 * The '<em><b>OPEN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #OPEN
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int OPEN_VALUE = 0;

	/**
	 * The '<em><b>IN REVIEW</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #IN_REVIEW
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int IN_REVIEW_VALUE = 1;

	/**
	 * The '<em><b>APPROVED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #APPROVED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int APPROVED_VALUE = 2;

	/**
	 * The '<em><b>REJECTED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #REJECTED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int REJECTED_VALUE = 3;

	/**
	 * The '<em><b>CHANGES REQUESTED</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #CHANGES_REQUESTED
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int CHANGES_REQUESTED_VALUE = 4;

	/**
	 * An array of all the '<em><b>Review Decision</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final ReviewDecision[] VALUES_ARRAY =
		new ReviewDecision[] {
			OPEN,
			IN_REVIEW,
			APPROVED,
			REJECTED,
			CHANGES_REQUESTED,
		};

	/**
	 * A public read-only list of all the '<em><b>Review Decision</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<ReviewDecision> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Review Decision</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReviewDecision get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ReviewDecision result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Review Decision</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReviewDecision getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ReviewDecision result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Review Decision</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static ReviewDecision get(int value) {
		switch (value) {
			case OPEN_VALUE: return OPEN;
			case IN_REVIEW_VALUE: return IN_REVIEW;
			case APPROVED_VALUE: return APPROVED;
			case REJECTED_VALUE: return REJECTED;
			case CHANGES_REQUESTED_VALUE: return CHANGES_REQUESTED;
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
	private ReviewDecision(int value, String name, String literal) {
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
	
} //ReviewDecision
