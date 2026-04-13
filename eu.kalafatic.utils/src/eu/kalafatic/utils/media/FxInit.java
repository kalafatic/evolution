package eu.kalafatic.utils.media;


import javafx.application.Platform;

public class FxInit {
	 private static boolean initialized = false;

	    public static synchronized void init() {
	        if (!initialized) {
	            Platform.startup(() -> {});
	            initialized = true;
	        }
	    }
}
