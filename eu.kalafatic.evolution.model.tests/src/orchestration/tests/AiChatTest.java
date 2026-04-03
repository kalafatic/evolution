/**
 */
package orchestration.tests;

import junit.framework.TestCase;

import junit.textui.TestRunner;

import orchestration.AiChat;
import orchestration.OrchestrationFactory;

/**
 * <!-- begin-user-doc -->
 * A test case for the model object '<em><b>Ai Chat</b></em>'.
 * <!-- end-user-doc -->
 * @generated
 */
public class AiChatTest extends TestCase {

	/**
	 * The fixture for this Ai Chat test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AiChat fixture = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(AiChatTest.class);
	}

	/**
	 * Constructs a new Ai Chat test case with the given name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public AiChatTest(String name) {
		super(name);
	}

	/**
	 * Sets the fixture for this Ai Chat test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void setFixture(AiChat fixture) {
		this.fixture = fixture;
	}

	/**
	 * Returns the fixture for this Ai Chat test case.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AiChat getFixture() {
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
		setFixture(OrchestrationFactory.eINSTANCE.createAiChat());
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

} //AiChatTest
