package eu.kalafatic.evolution.view.editors.pages;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;

import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.utils.model.ObservableList;
import eu.kalafatic.utils.model.ObservableProperty;

public class OllamaViewModel extends BaseViewModel {
    private final Ollama model;

    public final ObservableProperty<String> url = new ObservableProperty<>("url");
    public final ObservableProperty<String> modelName = new ObservableProperty<>("modelName");
    public final ObservableProperty<String> path = new ObservableProperty<>("path");

    // Status properties
    public final ObservableProperty<Boolean> isReachable = new ObservableProperty<>("isReachable", false);
    public final ObservableProperty<String> version = new ObservableProperty<>("version", "Offline");
    public final ObservableList<String> availableModels = new ObservableList<>("availableModels");

    private final Adapter modelAdapter = new AdapterImpl() {
        @Override
        public void notifyChanged(Notification msg) {
            if (msg.getFeatureID(Ollama.class) == OrchestrationPackage.OLLAMA__URL) {
                url.setValue(msg.getNewStringValue());
            } else if (msg.getFeatureID(Ollama.class) == OrchestrationPackage.OLLAMA__MODEL) {
                modelName.setValue(msg.getNewStringValue());
            } else if (msg.getFeatureID(Ollama.class) == OrchestrationPackage.OLLAMA__PATH) {
                path.setValue(msg.getNewStringValue());
            }
        }
    };

    public OllamaViewModel(Ollama model) {
        this.model = model;

        // Initial sync from model to ViewModel
        url.setValue(model.getUrl());
        modelName.setValue(model.getModel());
        path.setValue(model.getPath());

        // Listen to model changes
        model.eAdapters().add(modelAdapter);

        // Listen to ViewModel changes and update model
        url.addChangeListener(e -> {
            if (!e.getNewValue().equals(model.getUrl())) {
                model.setUrl((String) e.getNewValue());
            }
            updateStatus();
        });
        modelName.addChangeListener(e -> {
            if (!e.getNewValue().equals(model.getModel())) {
                model.setModel((String) e.getNewValue());
            }
            updateStatus();
        });
        path.addChangeListener(e -> {
            if (!e.getNewValue().equals(model.getPath())) {
                model.setPath((String) e.getNewValue());
            }
        });

        registerProperty(url);
        registerProperty(modelName);
        registerProperty(path);
        registerProperty(isReachable);
        registerProperty(version);

        updateStatus();
    }

    public void updateStatus() {
        String currentUrl = url.getValue();
        String currentModel = modelName.getValue();

        new Thread(() -> {
            OllamaService service = new OllamaService(currentUrl, currentModel);
            boolean reachable = service.ping();
            String v = reachable ? service.getVersion() : "Offline";
            List<OllamaModel> models = reachable ? service.loadModels() : Collections.emptyList();
            List<String> modelNames = models.stream().map(OllamaModel::getName).collect(Collectors.toList());

            // Update observables (SWTBinding handles asyncExec if needed, but ViewModel should be thread-safe)
            isReachable.setValue(reachable);
            version.setValue(v);

            // Update availableModels list
            synchronized (availableModels) {
                availableModels.clear();
                for (String m : modelNames) {
                    availableModels.add(m);
                }
            }
        }).start();
    }

    @Override
    public void dispose() {
        model.eAdapters().remove(modelAdapter);
    }
}
