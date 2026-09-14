/**
 */
package eu.kalafatic.evolution.model.orchestration.tests;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;

import junit.framework.TestCase;

import junit.textui.TestRunner;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Server Settings</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class ServerSettingsTest extends TestCase {

	/**
	 * The fixture for this Server Settings test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServerSettings fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(ServerSettingsTest.class);
	}

	/**
	 * Constructs a new Server Settings test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ServerSettingsTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this Server Settings test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(ServerSettings fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this Server Settings test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServerSettings getFixture() {
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
		setFixture(OrchestrationFactory.eINSTANCE.createServerSettings());
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

} //ServerSettingsTest
