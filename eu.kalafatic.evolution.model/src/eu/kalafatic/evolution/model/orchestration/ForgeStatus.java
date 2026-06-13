package eu.kalafatic.evolution.model.orchestration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.Enumerator;

public enum ForgeStatus implements Enumerator {
	IDLE(0, "IDLE", "IDLE"),
	TRAINING(1, "TRAINING", "TRAINING"),
	RUNNING(2, "RUNNING", "RUNNING"),
	ERROR(3, "ERROR", "ERROR");

	public static final int IDLE_VALUE = 0;
	public static final int TRAINING_VALUE = 1;
	public static final int RUNNING_VALUE = 2;
	public static final int ERROR_VALUE = 3;

	private static final ForgeStatus[] VALUES_ARRAY =
		new ForgeStatus[] {
			IDLE,
			TRAINING,
			RUNNING,
			ERROR,
		};

	public static final List<ForgeStatus> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	public static ForgeStatus get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ForgeStatus result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	public static ForgeStatus getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			ForgeStatus result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	public static ForgeStatus get(int value) {
		switch (value) {
			case IDLE_VALUE: return IDLE;
			case TRAINING_VALUE: return TRAINING;
			case RUNNING_VALUE: return RUNNING;
			case ERROR_VALUE: return ERROR;
		}
		return null;
	}

	private final int value;
	private final String name;
	private final String literal;

	private ForgeStatus(int value, String name, String literal) {
		this.value = value;
		this.name = name;
		this.literal = literal;
	}

	@Override
	public int getValue() {
	  return value;
	}

	@Override
	public String getName() {
	  return name;
	}

	@Override
	public String getLiteral() {
	  return literal;
	}

	@Override
	public String toString() {
		return literal;
	}

}
