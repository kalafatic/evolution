package eu.kalafatic.evolution.view;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class PropertiesView extends ViewPart implements ISelectionListener {

    public static final String ID = "eu.kalafatic.evolution.view.propertiesView";
    private TreeViewer viewer;
    private EditingDomain editingDomain;
    private EObject rootObject;
   
    private String[] ollamaModels;

    // Wrapper class to associate an attribute with its owner instance
    class PropertyDescriptor {
        final EObject owner;
        final EAttribute attribute;

        PropertyDescriptor(EObject owner, EAttribute attribute) {
            this.owner = owner;
            this.attribute = attribute;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PropertyDescriptor that = (PropertyDescriptor) obj;
            return Objects.equals(owner, that.owner) &&
                   Objects.equals(attribute, that.attribute);
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner, attribute);
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        viewer.setContentProvider(new PropertiesContentProvider());
        
        // HERE you can use getViewSite()
        IViewSite iViewSite = getViewSite();
        iViewSite.setSelectionProvider(viewer);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);

        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);

        // Property Name Column
        TreeViewerColumn propColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        propColumn.getColumn().setText("Property");
        propColumn.getColumn().setWidth(200);
        propColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof EObject) {
                    return ((EObject) element).eClass().getName();
                }
                if (element instanceof PropertyDescriptor) {
                    return ((PropertyDescriptor) element).attribute.getName();
                }
                return "";
            }
        });

        // Property Value Column
        TreeViewerColumn valueColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        valueColumn.getColumn().setText("Value");
        valueColumn.getColumn().setWidth(300);
        valueColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof PropertyDescriptor) {
                    PropertyDescriptor desc = (PropertyDescriptor) element;
                    Object value = desc.owner.eGet(desc.attribute);
                    return value != null ? value.toString() : "";
                }
                return "";
            }
        });
        valueColumn.setEditingSupport(new PropertiesEditingSupport(viewer));

        // Initialize EMF Edit
        AdapterFactory adapterFactory = new ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
        BasicCommandStack commandStack = new BasicCommandStack();
        editingDomain = new AdapterFactoryEditingDomain(adapterFactory, commandStack);


        Job job = new Job("Fetching Ollama Models") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
             
                IHandlerService hs = iViewSite.getService(IHandlerService.class);
                try {
					hs.executeCommand("eu.kalafatic.evolution.controller.orchestrationCommand", null);
				} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                Display.getDefault().asyncExec(() -> {
                    viewer.refresh();
                });
                return Status.OK_STATUS;
            }
        };
        job.schedule();

        // Load model and create an instance to edit
        ResourceSet resourceSet = editingDomain.getResourceSet();
        URI fileURI = URI.createPlatformPluginURI("eu.kalafatic.evolution.model/model/evolution.ecore", true);
        Resource resource = resourceSet.getResource(fileURI, true);

        try {
            resource.load(Collections.EMPTY_MAP);
            EPackage ePackage = (EPackage) resource.getContents().get(0);

            // Create an instance of the model to be edited
            EFactory factory = ePackage.getEFactoryInstance();
            EClass orchestratorClass = (EClass) ePackage.getEClassifier("Orchestrator");
            rootObject = factory.create(orchestratorClass);

            // Create and set Git
            EClass gitClass = (EClass) ePackage.getEClassifier("Git");
            EObject gitObject = factory.create(gitClass);
            gitObject.eSet(gitClass.getEStructuralFeature("repositoryUrl"), "https://github.com/example/repo.git");
            rootObject.eSet(orchestratorClass.getEStructuralFeature("git"), gitObject);

            // Create and set Maven
			EClass mavenClass = (EClass) ePackage.getEClassifier("Maven");
			EObject mavenObject = factory.create(mavenClass);
			rootObject.eSet(orchestratorClass.getEStructuralFeature("maven"), mavenObject);

			// Create and set LLM
			EClass llmClass = (EClass) ePackage.getEClassifier("LLM");
			EObject llmObject = factory.create(llmClass);
			rootObject.eSet(orchestratorClass.getEStructuralFeature("llm"), llmObject);

			// Create and set Compiler
			EClass compilerClass = (EClass) ePackage.getEClassifier("Compiler");
			EObject compilerObject = factory.create(compilerClass);
			rootObject.eSet(orchestratorClass.getEStructuralFeature("compiler"), compilerObject);

			// Create and set Ollama
			if (rootObject instanceof Orchestrator) {
				((Orchestrator) rootObject).setOllama(OrchestrationFactory.eINSTANCE.createOllama());
				((Orchestrator) rootObject).setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
			}

            viewer.setInput(rootObject);
            viewer.expandAll();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        super.dispose();
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection) selection).getFirstElement();
            if (first instanceof Orchestrator) {
                rootObject = (Orchestrator) first;
                viewer.setInput(rootObject);
                viewer.expandAll();
            }
        }
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    public EObject getRootObject() {
        return rootObject;
    }

    class PropertiesContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof EObject) {
                return ((EObject) inputElement).eContents().toArray();
            }
            return new Object[0];
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof EObject) {
                EObject owner = (EObject) parentElement;
                return owner.eClass().getEAllAttributes().stream()
                    .map(attr -> new PropertyDescriptor(owner, attr))
                    .toArray();
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof PropertyDescriptor) {
                return ((PropertyDescriptor) element).owner;
            }
            if (element instanceof EObject) {
                return ((EObject) element).eContainer();
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            return getChildren(element).length > 0;
        }
    }

    class PropertiesEditingSupport extends EditingSupport {
        public PropertiesEditingSupport(TreeViewer viewer) {
            super(viewer);
        }

        @Override
        protected boolean canEdit(Object element) {
            return element instanceof PropertyDescriptor;
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            if (element instanceof PropertyDescriptor) {
                PropertyDescriptor desc = (PropertyDescriptor) element;
                if (desc.owner.eClass() == OrchestrationPackage.Literals.LLM && desc.attribute == OrchestrationPackage.Literals.LLM__MODEL && ollamaModels != null && ollamaModels.length > 0) {
                    return new ComboBoxCellEditor(viewer.getTree(), ollamaModels, SWT.READ_ONLY);
                }

                EDataType type = desc.attribute.getEAttributeType();
                if (type instanceof EEnum) {
                    EEnum eEnum = (EEnum) type;
                    String[] labels = eEnum.getELiterals().stream().map(l -> l.getLiteral()).toArray(String[]::new);
                    return new ComboBoxCellEditor(viewer.getTree(), labels, SWT.READ_ONLY);
                }
            }
            return new TextCellEditor(viewer.getTree());
        }

        @Override
        protected Object getValue(Object element) {
            PropertyDescriptor desc = (PropertyDescriptor) element;
            if (desc.owner.eClass() == OrchestrationPackage.Literals.LLM && desc.attribute == OrchestrationPackage.Literals.LLM__MODEL && ollamaModels != null && ollamaModels.length > 0) {
                String model = (String) desc.owner.eGet(desc.attribute);
                for (int i = 0; i < ollamaModels.length; i++) {
                    if (ollamaModels[i].equals(model)) {
                        return i;
                    }
                }
                return 0;
            }

            Object value = desc.owner.eGet(desc.attribute);
            EDataType type = desc.attribute.getEAttributeType();
            if (type instanceof EEnum) {
                EEnum eEnum = (EEnum) type;
                if (value instanceof Enumerator) {
                    return eEnum.getELiterals().indexOf(eEnum.getEEnumLiteralByLiteral(((Enumerator) value).getLiteral()));
                }
                return 0;
            }

            return value != null ? value.toString() : "";
        }

        @Override
        protected void setValue(Object element, Object value) {
            PropertyDescriptor desc = (PropertyDescriptor) element;
            if (desc.owner.eClass() == OrchestrationPackage.Literals.LLM && desc.attribute == OrchestrationPackage.Literals.LLM__MODEL && ollamaModels != null && ollamaModels.length > 0) {
                value = ollamaModels[(int) value];
            } else {
                EDataType type = desc.attribute.getEAttributeType();
                if (type instanceof EEnum) {
                    EEnum eEnum = (EEnum) type;
                    value = eEnum.getELiterals().get((int) value).getInstance();
                }
            }

            Command command = SetCommand.create(editingDomain, desc.owner, desc.attribute, value);
            editingDomain.getCommandStack().execute(command);
            viewer.update(element, null);
        }
    }
}
