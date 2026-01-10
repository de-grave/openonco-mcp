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
 * Holds a parameterized SQL query and its parameter values.
 * Used by {@link QueryBuilder} to return safe, parameterized queries.
 *
 * @param sql    The SQL query string with ? placeholders for parameters
 * @param params The parameter values to bind to the query placeholders
 */
public record QueryResult(String sql, Object[] params) {

    /**
     * Creates a QueryResult with no parameters.
     *
     * @param sql The SQL query string
     * @return A QueryResult with empty params array
     */
    public static QueryResult of(String sql) {
        return new QueryResult(sql, new Object[0]);
    }

    /**
     * Creates a QueryResult with the given parameters.
     *
     * @param sql    The SQL query string with ? placeholders
     * @param params The parameter values
     * @return A QueryResult with the specified params
     */
    public static QueryResult of(String sql, Object... params) {
        return new QueryResult(sql, params != null ? params : new Object[0]);
    }
}
