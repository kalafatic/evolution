/**
 */
package orchestration.tests;

import junit.textui.TestRunner;

import orchestration.NetworkRule;
import orchestration.OrchestrationFactory;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Network Rule</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class NetworkRuleTest extends RuleTest {

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(NetworkRuleTest.class);
	}

	/**
	 * Constructs a new Network Rule test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NetworkRuleTest(String name) {
		super(name);
	}

	/**
	 * Returns the fixture for this Network Rule test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected NetworkRule getFixture() {
		return (NetworkRule)fixture;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see junit.framework.TestCase#setUp()
	 * @generated
	 */
	@Override
	protected void setUp() throws Exception {
		setFixture(OrchestrationFactory.eINSTANCE.createNetworkRule());
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

} //NetworkRuleTest
