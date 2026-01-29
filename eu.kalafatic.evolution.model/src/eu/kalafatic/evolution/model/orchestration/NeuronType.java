/**
 */
package eu.kalafatic.evolution.model.orchestration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>Neuron Type</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNeuronType()
 * @model
 * @generated
 */
public enum NeuronType implements Enumerator {
	/**
	 * The '<em><b>MLP</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #MLP_VALUE
	 * @generated
	 * @ordered
	 */
	MLP(0, "MLP", "MLP"),

	/**
	 * The '<em><b>CNN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #CNN_VALUE
	 * @generated
	 * @ordered
	 */
	CNN(1, "CNN", "CNN"),

	/**
	 * The '<em><b>RNN</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RNN_VALUE
	 * @generated
	 * @ordered
	 */
	RNN(2, "RNN", "RNN"),

	/**
	 * The '<em><b>LSTM</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #LSTM_VALUE
	 * @generated
	 * @ordered
	 */
	LSTM(3, "LSTM", "LSTM"),

	/**
	 * The '<em><b>TRANSFORMER</b></em>' literal object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TRANSFORMER_VALUE
	 * @generated
	 * @ordered
	 */
	TRANSFORMER(4, "TRANSFORMER", "TRANSFORMER");

	/**
	 * The '<em><b>MLP</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #MLP
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int MLP_VALUE = 0;

	/**
	 * The '<em><b>CNN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #CNN
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int CNN_VALUE = 1;

	/**
	 * The '<em><b>RNN</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #RNN
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int RNN_VALUE = 2;

	/**
	 * The '<em><b>LSTM</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #LSTM
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int LSTM_VALUE = 3;

	/**
	 * The '<em><b>TRANSFORMER</b></em>' literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #TRANSFORMER
	 * @model
	 * @generated
	 * @ordered
	 */
	public static final int TRANSFORMER_VALUE = 4;

	/**
	 * An array of all the '<em><b>Neuron Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static final NeuronType[] VALUES_ARRAY =
		new NeuronType[] {
			MLP,
			CNN,
			RNN,
			LSTM,
			TRANSFORMER,
		};

	/**
	 * A public read-only list of all the '<em><b>Neuron Type</b></em>' enumerators.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static final List<NeuronType> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	/**
	 * Returns the '<em><b>Neuron Type</b></em>' literal with the specified literal value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param literal the literal.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static NeuronType get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			NeuronType result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Neuron Type</b></em>' literal with the specified name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param name the name.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static NeuronType getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			NeuronType result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Returns the '<em><b>Neuron Type</b></em>' literal with the specified integer value.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the integer value.
	 * @return the matching enumerator or <code>null</code>.
	 * @generated
	 */
	public static NeuronType get(int value) {
		switch (value) {
			case MLP_VALUE: return MLP;
			case CNN_VALUE: return CNN;
			case RNN_VALUE: return RNN;
			case LSTM_VALUE: return LSTM;
			case TRANSFORMER_VALUE: return TRANSFORMER;
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
	private NeuronType(int value, String name, String literal) {
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

} //NeuronType
