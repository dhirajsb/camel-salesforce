/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.camel.component.salesforce.api.dto;

import java.util.List;

public class RestError {
    private String errorCode;
    private String message;
    private List<String> fields;

    // default ctor for unmarshalling
    public RestError() {
        super();
    }

    public RestError(String errorCode, String message, List<String> fields) {
        this(errorCode, message);
        this.fields = fields;
    }

    public RestError(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(String.format("{ code: %s, description: \"%s\"", errorCode, message));
        if ((null != fields) && !fields.isEmpty()) {
            builder.append(", fields: [");
            for (String field : fields) {
                builder.append(field);
                builder.append(", ");
            }
            builder.append("]");
        }
        builder.append(" }");
        return builder.toString();
    }

}
