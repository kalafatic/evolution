/**
 */
package orchestration.tests;

import junit.framework.TestCase;

import junit.textui.TestRunner;

import orchestration.EvoProject;
import orchestration.OrchestrationFactory;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Evo Project</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class EvoProjectTest extends TestCase {

	/**
	 * The fixture for this Evo Project test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EvoProject fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(EvoProjectTest.class);
	}

	/**
	 * Constructs a new Evo Project test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EvoProjectTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this Evo Project test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(EvoProject fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this Evo Project test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EvoProject getFixture() {
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
		setFixture(OrchestrationFactory.eINSTANCE.createEvoProject());
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

} //EvoProjectTest
