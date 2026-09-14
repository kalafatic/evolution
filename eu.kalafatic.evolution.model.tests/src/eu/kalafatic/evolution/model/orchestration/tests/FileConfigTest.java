/**
 */
package eu.kalafatic.evolution.model.orchestration.tests;

import eu.kalafatic.evolution.model.orchestration.FileConfig;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

import junit.framework.TestCase;

import junit.textui.TestRunner;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>File Config</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class FileConfigTest extends TestCase {

	/**
	 * The fixture for this File Config test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected FileConfig fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(FileConfigTest.class);
	}

	/**
	 * Constructs a new File Config test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public FileConfigTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this File Config test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(FileConfig fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this File Config test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected FileConfig getFixture() {
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
		setFixture(OrchestrationFactory.eINSTANCE.createFileConfig());
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

} //FileConfigTest
