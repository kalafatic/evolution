/**
 */
package eu.kalafatic.evolution.model.orchestration.tests;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.ServerSession;

import junit.framework.TestCase;

import junit.textui.TestRunner;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Server Session</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class ServerSessionTest extends TestCase {

	/**
	 * The fixture for this Server Session test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServerSession fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(ServerSessionTest.class);
	}

	/**
	 * Constructs a new Server Session test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServerSessionTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this Server Session test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(ServerSession fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this Server Session test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServerSession getFixture() {
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
		setFixture(OrchestrationFactory.eINSTANCE.createServerSession());
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

} //ServerSessionTest
