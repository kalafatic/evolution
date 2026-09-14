/**
 */
package eu.kalafatic.evolution.model.orchestration.tests;

import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

import junit.framework.TestCase;

import junit.textui.TestRunner;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Iteration</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class IterationTest extends TestCase {

	/**
	 * The fixture for this Iteration test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected Iteration fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(IterationTest.class);
	}

	/**
	 * Constructs a new Iteration test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public IterationTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this Iteration test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(Iteration fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this Iteration test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected Iteration getFixture() {
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
		setFixture(OrchestrationFactory.eINSTANCE.createIteration());
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

	/**
	 * Tests the fixture creation.
	 */
	public void testFixture() {
		assertNotNull(getFixture());
	}

} //IterationTest
