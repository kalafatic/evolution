# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.evolution.view/src/eu/kalafatic/evolution/view/editors/compare/

## Domain: general

## Components
* `ResourceCompareInput.java`: package eu.kalafatic.evolution.view.editors.compare; import java.io.ByteArrayInputStream; import java.io.InputStream; import org.eclipse.compare.CompareConfiguration; import org.eclipse.compare.CompareEditorInput; import org.eclipse.compare.ITypedElement; import org.eclipse.compare.ResourceNode; import org.eclipse.compare.structuremergeviewer.DiffNode; import org.eclipse.core.resources.IFile; import org.eclipse.core.runtime.IProgressMonitor; import org.eclipse.swt.graphics.Image; public class ResourceCompareInput extends CompareEditorInput { private Object left; private Object right; public ResourceCompareInput(CompareConfiguration config, Object left, Object right) { super(config); this.left = left; this.right = right; } @Override
