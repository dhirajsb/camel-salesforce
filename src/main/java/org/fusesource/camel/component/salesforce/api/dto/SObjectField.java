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

public class SObjectField {

    private int length;
    private String name;
    private String type;
    private String defaultValue;
    private String label;
    private boolean updateable;
    private boolean calculated;
    private boolean caseSensitive;
    private String controllerName;
    private boolean unique;
    private boolean nillable;
    private int precision;
    private int scale;
    private int byteLength;
    private boolean nameField;
    private boolean sortable;
    private boolean filterable;
    private boolean writeRequiresMasterRead;
    private boolean externalId;
    private boolean idLookup;
    private String inlineHelpText;
    private boolean createable;
    private String soapType;
    private boolean autoNumber;
    private boolean restrictedPicklist;
    private boolean namePointing;
    private boolean custom;
    private boolean defaultedOnCreate;
    private boolean deprecatedAndHidden;
    private boolean htmlFormatted;
    private String defaultValueFormula;
    private String calculatedFormula;
    private List<String> picklistValues;
    private boolean dependentPicklist;
    private List<String> referenceTo;
    private String relationshipName;
    private String relationshipOrder;
    private boolean cascadeDelete;
    private boolean restrictedDelete;
    private String digits;
    private boolean groupable;
    private boolean permissionable;
    private boolean displayLocationInDecimal;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isUpdateable() {
        return updateable;
    }

    public void setUpdateable(boolean updateable) {
        this.updateable = updateable;
    }

    public boolean isCalculated() {
        return calculated;
    }

    public void setCalculated(boolean calculated) {
        this.calculated = calculated;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isNillable() {
        return nillable;
    }

    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getByteLength() {
        return byteLength;
    }

    public void setByteLength(int byteLength) {
        this.byteLength = byteLength;
    }

    public boolean isNameField() {
        return nameField;
    }

    public void setNameField(boolean nameField) {
        this.nameField = nameField;
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public boolean isWriteRequiresMasterRead() {
        return writeRequiresMasterRead;
    }

    public void setWriteRequiresMasterRead(boolean writeRequiresMasterRead) {
        this.writeRequiresMasterRead = writeRequiresMasterRead;
    }

    public boolean isExternalId() {
        return externalId;
    }

    public void setExternalId(boolean externalId) {
        this.externalId = externalId;
    }

    public boolean isIdLookup() {
        return idLookup;
    }

    public void setIdLookup(boolean idLookup) {
        this.idLookup = idLookup;
    }

    public String getInlineHelpText() {
        return inlineHelpText;
    }

    public void setInlineHelpText(String inlineHelpText) {
        this.inlineHelpText = inlineHelpText;
    }

    public boolean isCreateable() {
        return createable;
    }

    public void setCreateable(boolean createable) {
        this.createable = createable;
    }

    public String getSoapType() {
        return soapType;
    }

    public void setSoapType(String soapType) {
        this.soapType = soapType;
    }

    public boolean isAutoNumber() {
        return autoNumber;
    }

    public void setAutoNumber(boolean autoNumber) {
        this.autoNumber = autoNumber;
    }

    public boolean isRestrictedPicklist() {
        return restrictedPicklist;
    }

    public void setRestrictedPicklist(boolean restrictedPicklist) {
        this.restrictedPicklist = restrictedPicklist;
    }

    public boolean isNamePointing() {
        return namePointing;
    }

    public void setNamePointing(boolean namePointing) {
        this.namePointing = namePointing;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public boolean isDefaultedOnCreate() {
        return defaultedOnCreate;
    }

    public void setDefaultedOnCreate(boolean defaultedOnCreate) {
        this.defaultedOnCreate = defaultedOnCreate;
    }

    public boolean isDeprecatedAndHidden() {
        return deprecatedAndHidden;
    }

    public void setDeprecatedAndHidden(boolean deprecatedAndHidden) {
        this.deprecatedAndHidden = deprecatedAndHidden;
    }

    public boolean isHtmlFormatted() {
        return htmlFormatted;
    }

    public void setHtmlFormatted(boolean htmlFormatted) {
        this.htmlFormatted = htmlFormatted;
    }

    public String getDefaultValueFormula() {
        return defaultValueFormula;
    }

    public void setDefaultValueFormula(String defaultValueFormula) {
        this.defaultValueFormula = defaultValueFormula;
    }

    public String getCalculatedFormula() {
        return calculatedFormula;
    }

    public void setCalculatedFormula(String calculatedFormula) {
        this.calculatedFormula = calculatedFormula;
    }

    public List<String> getPicklistValues() {
        return picklistValues;
    }

    public void setPicklistValues(List<String> picklistValues) {
        this.picklistValues = picklistValues;
    }

    public boolean isDependentPicklist() {
        return dependentPicklist;
    }

    public void setDependentPicklist(boolean dependentPicklist) {
        this.dependentPicklist = dependentPicklist;
    }

    public List<String> getReferenceTo() {
        return referenceTo;
    }

    public void setReferenceTo(List<String> referenceTo) {
        this.referenceTo = referenceTo;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public String getRelationshipOrder() {
        return relationshipOrder;
    }

    public void setRelationshipOrder(String relationshipOrder) {
        this.relationshipOrder = relationshipOrder;
    }

    public boolean isCascadeDelete() {
        return cascadeDelete;
    }

    public void setCascadeDelete(boolean cascadeDelete) {
        this.cascadeDelete = cascadeDelete;
    }

    public boolean isRestrictedDelete() {
        return restrictedDelete;
    }

    public void setRestrictedDelete(boolean restrictedDelete) {
        this.restrictedDelete = restrictedDelete;
    }

    public String getDigits() {
        return digits;
    }

    public void setDigits(String digits) {
        this.digits = digits;
    }

    public boolean isGroupable() {
        return groupable;
    }

    public void setGroupable(boolean groupable) {
        this.groupable = groupable;
    }

    public boolean isPermissionable() {
        return permissionable;
    }

    public void setPermissionable(boolean permissionable) {
        this.permissionable = permissionable;
    }

    public boolean isDisplayLocationInDecimal() {
        return displayLocationInDecimal;
    }

    public void setDisplayLocationInDecimal(boolean displayLocationInDecimal) {
        this.displayLocationInDecimal = displayLocationInDecimal;
    }
}
