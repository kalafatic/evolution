package eu.kalafatic.evolution.model.orchestration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.emf.common.util.Enumerator;

public enum TestStatus implements Enumerator {
	PENDING(0, "PENDING", "PENDING"),
	RUNNING(1, "RUNNING", "RUNNING"),
	PASSED(2, "PASSED", "PASSED"),
	FAILED(3, "FAILED", "FAILED");

	public static final int PENDING_VALUE = 0;
	public static final int RUNNING_VALUE = 1;
	public static final int PASSED_VALUE = 2;
	public static final int FAILED_VALUE = 3;

	private static final TestStatus[] VALUES_ARRAY = new TestStatus[] { PENDING, RUNNING, PASSED, FAILED };
	public static final List<TestStatus> VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

	public static TestStatus get(String literal) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			TestStatus result = VALUES_ARRAY[i];
			if (result.toString().equals(literal)) {
				return result;
			}
		}
		return null;
	}

	public static TestStatus getByName(String name) {
		for (int i = 0; i < VALUES_ARRAY.length; ++i) {
			TestStatus result = VALUES_ARRAY[i];
			if (result.getName().equals(name)) {
				return result;
			}
		}
		return null;
	}

	public static TestStatus get(int value) {
		switch (value) {
			case PENDING_VALUE: return PENDING;
			case RUNNING_VALUE: return RUNNING;
			case PASSED_VALUE: return PASSED;
			case FAILED_VALUE: return FAILED;
		}
		return null;
	}

	private final int value;
	private final String name;
	private final String literal;

	private TestStatus(int value, String name, String literal) {
		this.value = value;
		this.name = name;
		this.literal = literal;
	}

	@Override
	public int getValue() { return value; }
	@Override
	public String getName() { return name; }
	@Override
	public String getLiteral() { return literal; }
	@Override
	public String toString() { return literal; }
}
