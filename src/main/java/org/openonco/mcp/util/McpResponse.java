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

package org.openonco.mcp.util;

import io.quarkiverse.mcp.server.ToolCallException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class McpResponse {
    /**
     * Returning ToolCallException so Quarkus sets isError=true and uses the message as content
     */
    public static ToolCallException error(Throwable t) {
        if (t instanceof ToolCallException te) return te;
        String fallback = t.getMessage() != null ? t.getMessage() : "An unexpected processing error occurred.";
        return new ToolCallException(fallback);
    }
}