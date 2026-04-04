package eu.kalafatic.utils.application;
import org.eclipse.jface.dialogs.IInputValidator;
public class ValidationUtils {
    public static final ValidationUtils INSTANCE = new ValidationUtils();
    public class LengthValidator implements IInputValidator {
        public LengthValidator(int min, int max) {}
        @Override public String isValid(String newText) { return null; }
    }
}
