package eu.kalafatic.forge.controller.bridge;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import eu.kalafatic.forge.controller.api.*;

public class ForgeFunction extends BrowserFunction {

    private final SessionController sessionController;
    private final ModelController modelController;
    private final DatasetController datasetController;
    private final TrainingController trainingController;
    private final SnapshotController snapshotController;

    public ForgeFunction(Browser browser, String name,
                         SessionController sessionController,
                         ModelController modelController,
                         DatasetController datasetController,
                         TrainingController trainingController,
                         SnapshotController snapshotController) {
        super(browser, name);
        this.sessionController = sessionController;
        this.modelController = modelController;
        this.datasetController = datasetController;
        this.trainingController = trainingController;
        this.snapshotController = snapshotController;
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
            default:
                return null;
        }
    }
}
