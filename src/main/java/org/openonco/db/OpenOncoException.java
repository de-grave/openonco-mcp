/*
 * Copyright © 2026 OpenOnco
 * Copyright © 2026 Dmitry Degrave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openonco.db;

/**
 * Custom exception for OpenOnco application errors.
 * Provides error codes and suggestions for MCP tool error responses.
 */
public class OpenOncoException extends RuntimeException {

    /**
     * Error codes for categorizing exceptions.
     */
    public enum ErrorCode {
        NOT_FOUND("Requested resource not found"),
        INVALID_PARAMETER("Invalid parameter value"),
        MISSING_PARAMETER("Required parameter missing"),
        QUERY_ERROR("Database query failed"),
        INITIALIZATION_ERROR("Database initialization failed"),
        DATA_VALIDATION_ERROR("Data validation failed");

        private final String description;

        ErrorCode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ErrorCode errorCode;
    private final String suggestion;

    /**
     * Creates an exception with an error code and message.
     *
     * @param errorCode The error code
     * @param message   The error message
     */
    public OpenOncoException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.suggestion = null;
    }

    /**
     * Creates an exception with an error code, message, and suggestion.
     *
     * @param errorCode  The error code
     * @param message    The error message
     * @param suggestion A suggestion for resolving the error
     */
    public OpenOncoException(ErrorCode errorCode, String message, String suggestion) {
        super(message);
        this.errorCode = errorCode;
        this.suggestion = suggestion;
    }

    /**
     * Creates an exception with an error code, message, and cause.
     *
     * @param errorCode The error code
     * @param message   The error message
     * @param cause     The underlying cause
     */
    public OpenOncoException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.suggestion = null;
    }

    /**
     * Creates an exception with all fields.
     *
     * @param errorCode  The error code
     * @param message    The error message
     * @param suggestion A suggestion for resolving the error
     * @param cause      The underlying cause
     */
    public OpenOncoException(ErrorCode errorCode, String message, String suggestion, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.suggestion = suggestion;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getSuggestion() {
        return suggestion;
    }

    /**
     * Converts this exception to an error response JSON string.
     *
     * @return JSON string representation of the error
     */
    public String toJsonResponse() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"error\": true,");
        json.append("\"code\": \"").append(errorCode.name()).append("\",");
        json.append("\"message\": \"").append(escapeJson(getMessage())).append("\"");
        if (suggestion != null) {
            json.append(",\"suggestion\": \"").append(escapeJson(suggestion)).append("\"");
        }
        json.append("}");
        return json.toString();
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
