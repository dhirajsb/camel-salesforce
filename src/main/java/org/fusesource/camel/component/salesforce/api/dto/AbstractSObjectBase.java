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

import org.codehaus.jackson.annotate.JsonProperty;

public class AbstractSObjectBase extends AbstractDTOBase {

    private Attributes attributes;

    private String Id;

    private String OwnerId;

    private boolean IsDeleted;

    private String Name;

    private String CreatedDate;

    private String CreatedById;

    private String LastModifiedDate;

    private String LastModifiedById;

    private String SystemModstamp;

    private String LastActivityDate;

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    @JsonProperty("Id")
    public String getId() {
        return Id;
    }

    @JsonProperty("Id")
    public void setId(String id) {
        Id = id;
    }

    @JsonProperty("OwnerId")
    public String getOwnerId() {
        return OwnerId;
    }

    @JsonProperty("OwnerId")
    public void setOwnerId(String ownerId) {
        OwnerId = ownerId;
    }

    @JsonProperty("IsDeleted")
    public boolean isIsDeleted() {
        return IsDeleted;
    }

    @JsonProperty("IsDeleted")
    public void setIsDeleted(boolean isDeleted) {
        IsDeleted = isDeleted;
    }

    @JsonProperty("Name")
    public String getName() {
        return Name;
    }

    @JsonProperty("Name")
    public void setName(String name) {
        Name = name;
    }

    @JsonProperty("CreatedDate")
    public String getCreatedDate() {
        return CreatedDate;
    }

    @JsonProperty("CreatedDate")
    public void setCreatedDate(String createdDate) {
        CreatedDate = createdDate;
    }

    @JsonProperty("CreatedById")
    public String getCreatedById() {
        return CreatedById;
    }

    @JsonProperty("CreatedById")
    public void setCreatedById(String createdById) {
        CreatedById = createdById;
    }

    @JsonProperty("LastModifiedDate")
    public String getLastModifiedDate() {
        return LastModifiedDate;
    }

    @JsonProperty("LastModifiedDate")
    public void setLastModifiedDate(String lastModifiedDate) {
        LastModifiedDate = lastModifiedDate;
    }

    @JsonProperty("LastModifiedById")
    public String getLastModifiedById() {
        return LastModifiedById;
    }

    @JsonProperty("LastModifiedById")
    public void setLastModifiedById(String lastModifiedById) {
        LastModifiedById = lastModifiedById;
    }

    @JsonProperty("SystemModstamp")
    public String getSystemModstamp() {
        return SystemModstamp;
    }

    @JsonProperty("SystemModstamp")
    public void setSystemModstamp(String systemModstamp) {
        SystemModstamp = systemModstamp;
    }

    @JsonProperty("LastActivityDate")
    public String getLastActivityDate() {
        return LastActivityDate;
    }

    @JsonProperty("LastActivityDate")
    public void setLastActivityDate(String lastActivityDate) {
        LastActivityDate = lastActivityDate;
    }

}
