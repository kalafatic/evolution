package eu.kalafatic.evolution.forge.controller.bridge;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import eu.kalafatic.evolution.forge.controller.api.*;

public class ForgeFunction extends BrowserFunction {

    private final SessionController sessionController;
    private final ModelController modelController;
    private final DatasetController datasetController;
    private final TrainingController trainingController;
    private final SnapshotController snapshotController;
    private final ForgeEvolutionController evolutionController;
    private final LLMController llmController;
    private final ExporterController exporterController;
    private final RuntimeController runtimeController;
    private final ObservabilityController observabilityController;

    public ForgeFunction(Browser browser, String name,
                         SessionController sessionController,
                         ModelController modelController,
                         DatasetController datasetController,
                         TrainingController trainingController,
                         SnapshotController snapshotController,
                         ForgeEvolutionController evolutionController,
                         LLMController llmController,
                         ExporterController exporterController,
                         RuntimeController runtimeController,
                         ObservabilityController observabilityController) {
        super(browser, name);
        this.sessionController = sessionController;
        this.modelController = modelController;
        this.datasetController = datasetController;
        this.trainingController = trainingController;
        this.snapshotController = snapshotController;
        this.evolutionController = evolutionController;
        this.llmController = llmController;
        this.exporterController = exporterController;
        this.runtimeController = runtimeController;
        this.observabilityController = observabilityController;
    }

    @Override
    public Object function(Object[] arguments) {
        if (arguments.length == 0) return null;
        String action = String.valueOf(arguments[0]);

        switch (action) {
            case "createSession":
                if (arguments.length > 1) {
                    return sessionController.createSession(String.valueOf(arguments[1]));
                }
                return null;
            case "deleteSession":
                if (arguments.length > 1) {
                    sessionController.deleteSession(String.valueOf(arguments[1]));
                    return true;
                }
                return false;
            case "startTraining":
                if (arguments.length > 1) {
                    trainingController.startTraining(String.valueOf(arguments[1]));
                    return true;
                }
                return false;
            case "stopTraining":
                if (arguments.length > 1) {
                    trainingController.stopTraining(String.valueOf(arguments[1]));
                    return true;
                }
                return false;
            case "saveSnapshot":
                if (arguments.length > 1) {
                    snapshotController.saveSnapshot(String.valueOf(arguments[1]));
                    return true;
                }
                return false;
            case "selectSession":
                if (arguments.length > 1) {
                    sessionController.selectSession(String.valueOf(arguments[1]));
                    return true;
                }
                return false;
            case "saveSession":
                if (arguments.length > 1) {
                    sessionController.saveSession(String.valueOf(arguments[1]));
                    return true;
                }
                return false;
            case "addSubModel":
                if (arguments.length > 4) {
                    return evolutionController.addSubModel(
                        String.valueOf(arguments[1]),
                        String.valueOf(arguments[2]),
                        String.valueOf(arguments[3]),
                        String.valueOf(arguments[4])
                    );
                }
                return null;
            case "connectSubModels":
                if (arguments.length > 5) {
                    evolutionController.connectSubModels(
                        String.valueOf(arguments[1]),
                        String.valueOf(arguments[2]),
                        String.valueOf(arguments[3]),
                        String.valueOf(arguments[4]),
                        String.valueOf(arguments[5])
                    );
                    return true;
                }
                return false;
            case "evolve":
                if (arguments.length > 2) {
                    evolutionController.evolve(
                        String.valueOf(arguments[1]),
                        String.valueOf(arguments[2])
                    );
                    return true;
                }
                return false;
            case "generate":
                if (arguments.length > 1) {
                    try {
                        return llmController.generate(String.valueOf(arguments[1]));
                    } catch (Exception e) {
                        return "Error: " + e.getMessage();
                    }
                }
                return null;
            case "exportModel":
                if (arguments.length > 3) {
                    try {
                        return exporterController.exportModel(String.valueOf(arguments[1]), String.valueOf(arguments[2]), String.valueOf(arguments[3]));
                    } catch (Exception e) {
                        return "Error: " + e.getMessage();
                    }
                }
                return null;
            case "deployModel":
                if (arguments.length > 1) {
                    try {
                        runtimeController.deployModel(String.valueOf(arguments[1]));
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                return false;
            case "chat":
                if (arguments.length > 1) {
                    try {
                        return runtimeController.chat(String.valueOf(arguments[1]));
                    } catch (Exception e) {
                        return "Error: " + e.getMessage();
                    }
                }
                return null;
            case "trackEvent":
                if (arguments.length > 3) {
                    observabilityController.trackEvent(String.valueOf(arguments[1]), String.valueOf(arguments[2]), String.valueOf(arguments[3]));
                    return true;
                }
                return false;
            default:
                return null;
        }
    }
}
