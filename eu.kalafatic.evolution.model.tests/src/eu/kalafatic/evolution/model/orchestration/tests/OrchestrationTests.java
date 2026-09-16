/**
 */
package eu.kalafatic.evolution.model.orchestration.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import junit.textui.TestRunner;

/**
 * <!-- begin-user-doc -->
 * A test suite for the '<em><b>orchestration</b></em>' package.
 * <!-- end-user-doc -->
 * @generated
 */
public class OrchestrationTests extends TestSuite {

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(suite());
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static Test suite() {
		TestSuite suite = new OrchestrationTests("orchestration Tests");
		suite.addTestSuite(AIProviderTest.class);
		suite.addTestSuite(AgentTest.class);
		suite.addTestSuite(AiChatTest.class);
		suite.addTestSuite(ChangeSetTest.class);
		suite.addTestSuite(ChatMessageTest.class);
		suite.addTestSuite(ChatSessionTest.class);
		suite.addTestSuite(CommandTest.class);
		suite.addTestSuite(CommentTest.class);
		suite.addTestSuite(CompilerTest.class);
		suite.addTestSuite(DatabaseTest.class);
		suite.addTestSuite(DiffHunkTest.class);
		suite.addTestSuite(EclipseTest.class);
		suite.addTestSuite(EvaluationResultTest.class);
		suite.addTestSuite(EvoProjectTest.class);
		suite.addTestSuite(FileChangeTest.class);
		suite.addTestSuite(FileConfigTest.class);
		suite.addTestSuite(ForgeSessionTest.class);
		suite.addTestSuite(GenomeSnapshotTest.class);
		suite.addTestSuite(GitTest.class);
		suite.addTestSuite(IterationTest.class);
		suite.addTestSuite(LLMTest.class);
		suite.addTestSuite(MavenTest.class);
		suite.addTestSuite(MonitoringDataTest.class);
		suite.addTestSuite(NetworkEntryTest.class);
		suite.addTestSuite(NeuronAITest.class);
		suite.addTestSuite(OllamaTest.class);
		suite.addTestSuite(OrchestratorTest.class);
		suite.addTestSuite(PromptInstructionsTest.class);
		suite.addTestSuite(ReviewSessionTest.class);
		suite.addTestSuite(SelfDevSessionTest.class);
		suite.addTestSuite(ServerSessionTest.class);
		suite.addTestSuite(ServerSettingsTest.class);
		suite.addTestSuite(SessionExperimentTest.class);
		suite.addTestSuite(SessionModelStateTest.class);
		suite.addTestSuite(SessionSnapshotTest.class);
		suite.addTestSuite(SupervisorSettingsTest.class);
		suite.addTestSuite(TaskTest.class);
		suite.addTestSuite(TestTest.class);
		return suite;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OrchestrationTests(String name) {
		super(name);
	}

} //OrchestrationTests
