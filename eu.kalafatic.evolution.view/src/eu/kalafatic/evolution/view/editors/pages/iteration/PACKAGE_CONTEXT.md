# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/editors/pages/iteration/

## Domain: general

## Components
* `SelfDevEditDialog.java`: package eu.kalafatic.evolution.view.editors.pages.iteration; import java.util.LinkedHashMap; import org.eclipse.swt.widgets.Shell; import eu.kalafatic.evolution.model.orchestration.SelfDevSession; import eu.kalafatic.evolution.view.editors.pages.DevelopmentPage; import eu.kalafatic.utils.dialogs.DynamicField; import eu.kalafatic.utils.dialogs.DynamicMapDialog; public class SelfDevEditDialog extends DynamicMapDialog { private SelfDevSession session; private DevelopmentPage page; private static final String INITIAL_REQUEST = "initialRequest"; private static final String MAX_ITERATIONS = "maxIterations"; private static final String RATIONALE = "rationale"; public SelfDevEditDialog(Shell parentShell, SelfDevSession session, DevelopmentPage page) { super(parentShell, createFields(session)); this.session = session; this.page = page; setTitle("Edit Self-Dev Session"); setContainerWidth(600); }
