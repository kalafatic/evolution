package eu.kalafatic.evolution.tests.iterative;

/**
 * Interface for simulation-based tests that update an ITestListener.
 */
public interface ISimulationTest {
    /**
     * Starts the simulation.
     */
    void run();

    /**
     * Stops the simulation.
     */
    void stop();
}
