/**
 */
package orchestration.tests;

import junit.framework.TestCase;

import junit.textui.TestRunner;

import orchestration.Maven;
import orchestration.OrchestrationFactory;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Maven</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class MavenTest extends TestCase {

	/**
	 * The fixture for this Maven test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected Maven fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(MavenTest.class);
	}

	/**
	 * Constructs a new Maven test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MavenTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this Maven test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(Maven fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this Maven test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected Maven getFixture() {
		return fixture;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see junit.framework.TestCase#setUp()
	 * @generated
	 */
	@Override
	protected void setUp() throws Exception {
		setFixture(OrchestrationFactory.eINSTANCE.createMaven());
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

} //MavenTest
