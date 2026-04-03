/**
 */
package orchestration.tests;

import junit.textui.TestRunner;

import orchestration.OrchestrationFactory;
import orchestration.SecretRule;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Secret Rule</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class SecretRuleTest extends RuleTest {

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(SecretRuleTest.class);
	}

	/**
	 * Constructs a new Secret Rule test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SecretRuleTest(String name) {
		super(name);
	}

	/**
	 * Returns the fixture for this Secret Rule test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected SecretRule getFixture() {
		return (SecretRule)fixture;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see junit.framework.TestCase#setUp()
	 * @generated
	 */
	@Override
	protected void setUp() throws Exception {
		setFixture(OrchestrationFactory.eINSTANCE.createSecretRule());
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

} //SecretRuleTest
