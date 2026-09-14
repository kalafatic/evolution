/**
 */
package eu.kalafatic.evolution.model.orchestration.tests;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;

import junit.framework.TestCase;

import junit.textui.TestRunner;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Self Dev Session</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class SelfDevSessionTest extends TestCase {

	/**
	 * The fixture for this Self Dev Session test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SelfDevSession fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(SelfDevSessionTest.class);
	}

	/**
	 * Constructs a new Self Dev Session test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SelfDevSessionTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this Self Dev Session test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(SelfDevSession fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this Self Dev Session test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SelfDevSession getFixture() {
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
		setFixture(OrchestrationFactory.eINSTANCE.createSelfDevSession());
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

} //SelfDevSessionTest
