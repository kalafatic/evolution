package eu.kalafatic.evolution.controller.orchestration.capability;

public class CapabilityException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public CapabilityException(String message) {
        super(message);
    }
    public CapabilityException(String message,String cause) {
        super(message, new Throwable(cause));
    }
    public CapabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
