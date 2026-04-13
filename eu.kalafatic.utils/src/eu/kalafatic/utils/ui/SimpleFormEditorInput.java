package eu.kalafatic.utils.ui;

public class SimpleFormEditorInput extends FormEditorInput {

	private SimpleModel model;

	public SimpleFormEditorInput(String name) {
		super(name);
		model = new SimpleModel();
	}

	public SimpleModel getModel() {
		return model;
	}
}
