package eu.kalafatic.evolution.view;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class HTTPUtils {

	private static final Map<Integer, String> STATUS_MESSAGES = new HashMap<>();

	static {
		// 2xx
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_OK, "OK");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_CREATED, "Created");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_ACCEPTED, "Accepted");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_NO_CONTENT, "No Content");

		// 3xx
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_MOVED_PERM, "Moved Permanently");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_MOVED_TEMP, "Found");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_NOT_MODIFIED, "Not Modified");

		// 4xx
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_BAD_REQUEST, "Bad Request");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_UNAUTHORIZED, "Unauthorized");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_FORBIDDEN, "Forbidden");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_NOT_FOUND, "Not Found");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_CONFLICT, "Conflict");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_GONE, "Gone");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_CLIENT_TIMEOUT, "Request Timeout");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_ENTITY_TOO_LARGE, "Payload Too Large");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, "Unsupported Media Type");

		// 5xx
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal Server Error");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_NOT_IMPLEMENTED, "Not Implemented");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_BAD_GATEWAY, "Bad Gateway");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_UNAVAILABLE, "Service Unavailable");
		STATUS_MESSAGES.put(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, "Gateway Timeout");

		// Common codes not defined by HttpURLConnection
		STATUS_MESSAGES.put(429, "Too Many Requests");
		STATUS_MESSAGES.put(418, "I'm a teapot");
		STATUS_MESSAGES.put(451, "Unavailable For Legal Reasons");
		STATUS_MESSAGES.put(507, "Insufficient Storage");
	}

	private HTTPUtils() {
	    }

	public static String getMessage(int statusCode) {
		return STATUS_MESSAGES.getOrDefault(statusCode, "Unknown Status");
	}

	public static String decode(int statusCode) {
		return statusCode + " " + getMessage(statusCode);
	}

	public static boolean isSuccess(int statusCode) {
		return statusCode >= 200 && statusCode < 300;
	}

	public static boolean isRedirect(int statusCode) {
		return statusCode >= 300 && statusCode < 400;
	}

	public static boolean isClientError(int statusCode) {
		return statusCode >= 400 && statusCode < 500;
	}

	public static boolean isServerError(int statusCode) {
	        return statusCode >= 500 && statusCode < 600;
	    }

	/**
	 * Validates that a response really came from the EVO backend and carries JSON.
	 * An unrelated process occupying an EVO port may answer HTTP 200 with an HTML
	 * page; parsing that as JSON produces cryptic errors. This check turns it into
	 * an explicit, actionable backend-connectivity failure.
	 *
	 * @param endpoint the requested URL (for diagnostics)
	 * @param statusCode the HTTP status
	 * @param contentType the response Content-Type (may be null)
	 * @throws IllegalStateException when the status is not 2xx or the body is not JSON
	 */
	public static void requireEvolutionJson(String endpoint, int statusCode, String contentType) {
		if (!isSuccess(statusCode)) {
			throw new IllegalStateException("EVO backend returned HTTP " + decode(statusCode)
					+ " from " + endpoint + " — is the EVO server running on the configured port?");
		}
		if (contentType == null || !contentType.toLowerCase().contains("json")) {
			throw new IllegalStateException("EVO backend identity check failed at " + endpoint
					+ ": expected a JSON response but received '" + (contentType == null ? "unknown" : contentType)
					+ "'. A different application may be occupying the EVO server port.");
		}
	}
}
