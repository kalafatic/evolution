package eu.kalafatic.utils.model;
import java.util.Map;
import java.util.HashMap;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

public abstract class AModelManager {
    protected EObject model;
    protected Resource resource;
    public void initModel(String path, String name, String modelName) {}
    public abstract void initModel();
    public abstract void createModel();
    public abstract void setUpModel();
    public abstract EObject getModel();
}
