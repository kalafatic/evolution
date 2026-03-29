/**
 */
package orchestration.tests;

import junit.textui.TestRunner;

import orchestration.MemoryRule;
import orchestration.OrchestrationFactory;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Memory Rule</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class MemoryRuleTest extends RuleTest {

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(MemoryRuleTest.class);
	}

	/**
	 * Constructs a new Memory Rule test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MemoryRuleTest(String name) {
		super(name);
	}

	/**
	 * Returns the fixture for this Memory Rule test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected MemoryRule getFixture() {
		return (MemoryRule)fixture;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see junit.framework.TestCase#setUp()
	 * @generated
	 */
	@Override
	protected void setUp() throws Exception {
		setFixture(OrchestrationFactory.eINSTANCE.createMemoryRule());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see junit.framework.TestCase#tearDown()
	 * @generated
	 */
	@Override
	protected void tearDown() throws Exception {
		setFixture(null);
	}

} //MemoryRuleTest
