package org.example.fms.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class to ensure all API responses follow the standard JSON envelope
 * format requested.
 */
public class ResponseUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void sendResponse(HttpServletResponse response, int statusCode, boolean success, Object data,
            Object error) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> responseEnvelope = new HashMap<>();
        responseEnvelope.put("success", success);
        responseEnvelope.put("data", data);
        responseEnvelope.put("error", error);

        Map<String, String> meta = new HashMap<>();
        meta.put("timestamp", java.time.Instant.now().toString());
        meta.put("requestId", UUID.randomUUID().toString()); // In a real app, this should come from a request filter
        responseEnvelope.put("meta", meta);

        try (PrintWriter out = response.getWriter()) {
            mapper.writeValue(out, responseEnvelope);
        }
    }

    public static void sendOk(HttpServletResponse response, Object data) throws IOException {
        sendResponse(response, HttpServletResponse.SC_OK, true, data, null);
    }

    public static void sendCreated(HttpServletResponse response, Object data) throws IOException {
        sendResponse(response, HttpServletResponse.SC_CREATED, true, data, null);
    }

    public static void sendError(HttpServletResponse response, int statusCode, String errorCode, String errorMessage)
            throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("code", errorCode);
        error.put("message", errorMessage);
        sendResponse(response, statusCode, false, null, error);
    }

    public static void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static void sendForbidden(HttpServletResponse response, String message) throws IOException {
        sendError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", message);
    }
}
