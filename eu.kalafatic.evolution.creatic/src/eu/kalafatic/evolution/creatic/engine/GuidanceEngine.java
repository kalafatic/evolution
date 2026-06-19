package eu.kalafatic.evolution.creatic.engine;

import java.util.Map;
import eu.kalafatic.evolution.creatic.model.*;

public class GuidanceEngine {

    public GuidanceResponse evaluate(ContextGraph context) {
        GuidanceResponse response = new GuidanceResponse();
        String pageId = context.getPageId();

        if (pageId.contains("forge")) {
            evaluateForge(context, response);
        } else if (pageId.contains("chat")) {
            evaluateChat(context, response);
        } else if (pageId.contains("architecture")) {
            evaluateArchitecture(context, response);
        } else {
            evaluateGeneral(context, response);
        }

        if (response.getActions().isEmpty()) {
            response.getActions().add(new GuidanceAction("Continue Workflow", "CONTINUE", "Keep moving forward with your current task."));
        }

        return response;
    }

    private void evaluateForge(ContextGraph context, GuidanceResponse response) {
        response.setSummary("Forge Guided Workflow");

        String status = (String) context.get("forge.workflow.status");
        if (status == null) status = "START";

        switch (status) {
            case "START":
                response.setSummary("Step 1: Create Architecture");
                response.getTips().add(new Tip("Drag a high-level component (MLP, CNN, Transformer...) from the palette to generate an architecture."));
                break;
            case "ARCH_GENERATED":
                response.setSummary("Step 2: Review Properties");
                response.getTips().add(new Tip("Your architecture has been created. Review and customize the model properties before training."));
                response.getActions().add(new GuidanceAction("Confirm Properties", "CONFIRM_PROPERTIES", "Proceed to training configuration."));
                break;
            case "PROPERTIES_CONFIRMED":
                response.setSummary("Step 3: Select Dataset");
                response.getTips().add(new Tip("Choose a dataset appropriate for your architecture."));
                response.getActions().add(new GuidanceAction("Select Dataset", "SELECT_DATASET", "Open the dataset selector."));
                break;
            case "DATASET_SELECTED":
                response.setSummary("Step 4: Configure Training");
                response.getTips().add(new Tip("Adjust optimizer, epochs, and learning rate."));
                response.getActions().add(new GuidanceAction("Start Training", "TRAIN_MODEL", "Begin the training process."));
                break;
            case "TRAINING_ACTIVE":
                response.setSummary("Step 5: Training in Progress");
                response.getInsights().add(new Insight("Model is learning. Monitor loss and accuracy in the observability panel."));
                break;
            case "TRAINING_COMPLETED":
                response.setSummary("Step 6: Evaluate Model");
                response.getTips().add(new Tip("Inspect metrics and verify model performance before exporting."));
                response.getActions().add(new GuidanceAction("Evaluate Now", "EVALUATE_MODEL", "Run evaluation on validation set."));
                response.getActions().add(new GuidanceAction("Export Model", "EXPORT_MODEL", "Proceed to final model export."));
                break;
            case "EVALUATED":
                response.setSummary("Step 7: Export & Finish");
                response.getTips().add(new Tip("Generate a deterministic EVO model package for deployment."));
                response.getActions().add(new GuidanceAction("Export Model", "EXPORT_MODEL", "Save model to forge-lab repository."));
                break;
            case "EXPORTED":
                response.setSummary("Workflow Completed");
                response.getInsights().add(new Insight("Your model is ready for deployment in the demo gallery."));
                break;
        }

        Boolean modelExists = (Boolean) context.get("model.exists");
        if (!Boolean.TRUE.equals(modelExists) && "START".equals(status)) {
             response.getTips().add(new Tip("You can also load a pre-configured demo template from the sidebar."));
        }

        response.getInsights().add(new Insight("Ensure your architecture is saved before starting long training runs."));

        if (Boolean.TRUE.equals(context.get("training.active"))) {
             response.getInsights().add(new Insight("Training in progress. Model performance is being tracked in real-time."));
        }

        String modelType = (String) context.get("model.type");
        if ("TRANSFORMER".equals(modelType)) {
            response.getTips().add(new Tip("Transformers require significant memory. Consider using smaller batch sizes if training fails."));
        } else if ("CNN".equals(modelType)) {
            response.getTips().add(new Tip("CNNs perform best with data augmentation. Check if your dataset supports it."));
        }
    }

    private void evaluateChat(ContextGraph context, GuidanceResponse response) {
        response.setSummary("AI Chat Intelligence");

        Boolean darwinActive = (Boolean) context.get("darwin.active");
        String topic = (String) context.get("conversation.topic");
        Boolean chatEmpty = (Boolean) context.get("chat.empty");

        if (Boolean.TRUE.equals(chatEmpty)) {
            response.getTips().add(new Tip("Start a conversation by asking for a task implementation or code analysis."));
        }

        if (Boolean.TRUE.equals(darwinActive)) {
            response.getTips().add(new Tip("Darwin is active. You can ask for evolutionary improvements to your code."));
            response.getActions().add(new GuidanceAction("Explore Darwin Branches", "DARWIN_EXPLORE", "Analyze the different proposed variants in the chat."));
        }

        if ("training".equals(topic)) {
            response.getInsights().add(new Insight("You are discussing training. Check the Forge page for detailed metrics."));
        }

        if (Boolean.TRUE.equals(context.get("system.busy"))) {
            response.getWarnings().add(new Warning("System is under high load. AI responses might be delayed."));
        }

        if (Boolean.TRUE.equals(context.get("system.errors"))) {
            response.getWarnings().add(new Warning("Evolution Kernel reported internal errors. Check the logs for details."));
        }
    }

    private void evaluateArchitecture(ContextGraph context, GuidanceResponse response) {
        response.setSummary("Architecture Explorer");

        String selectedNode = (String) context.get("node.selected");
        if (selectedNode != null) {
            response.getInsights().add(new Insight("Selected Node: " + selectedNode + ". You can analyze its dependencies and impact."));
            response.getActions().add(new GuidanceAction("Analyze Dependencies", "DEPS_ANALYZE", "Identify which components depend on this node."));
        } else {
            response.getTips().add(new Tip("Select a component in the graph to see deep architectural insights."));

            if (Boolean.TRUE.equals(context.get("graph.empty"))) {
                response.getActions().add(new GuidanceAction("Run Discovery", "ARCH_DISCOVER", "Execute architecture discovery to map the project structure."));
            }
        }

        if (Boolean.TRUE.equals(context.get("graph.complex"))) {
            response.getTips().add(new Tip("The architecture is complex. Use filtering to focus on specific subsystems."));
        }
    }

    private void evaluateGeneral(ContextGraph context, GuidanceResponse response) {
        Long idleTime = (Long) context.get("user.idle_time");
        if (idleTime != null && idleTime > 300000) { // 5 minutes
            response.getTips().add(new Tip("You've been idle for a while. Need help continuing your workflow?"));
        }

        if (response.getActions().isEmpty()) {
            response.getActions().add(new GuidanceAction("Explore Documentation", "HELP_DOCS", "Read the Evolution Forge Lab documentation to learn more about advanced features."));
        }
    }
}
