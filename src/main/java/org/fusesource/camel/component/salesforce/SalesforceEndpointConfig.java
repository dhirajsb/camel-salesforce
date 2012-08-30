package org.fusesource.camel.component.salesforce;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultEndpointConfiguration;
import org.fusesource.camel.component.salesforce.internal.PayloadFormat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SalesforceEndpointConfig extends DefaultEndpointConfiguration {

    public static final String SOBJECT_NAME = "sObjectName";
    public static final String SOBJECT_ID = "sObjectId";
    public static final String SOBJECT_FIELDS = "sObjectFields";
    public static final String SOBJECT_EXT_ID_NAME = "sObjectIdName";
    public static final String SOBJECT_EXT_ID_VALUE = "sObjectIdValue";
    public static final String SOBJECT_CLASS = "sObjectClass";

    private PayloadFormat format;
    private String apiVersion;

    private String sObjectName;
    private String sObjectId;
    private String sObjectFields;
    private String sObjectIdName;
    private String sObjectIdValue;
    private String sObjectClass;

    public SalesforceEndpointConfig(CamelContext camelContext) {
        super(camelContext);
    }

    public SalesforceEndpointConfig(CamelContext camelContext, String uri) {
        super(camelContext, uri);
    }

    public PayloadFormat getPayloadFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = PayloadFormat.valueOf(format.toUpperCase());
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getSObjectName() {
        return sObjectName;
    }

    public void setSObjectName(String sObjectName) {
        this.sObjectName = sObjectName;
    }

    public String getSObjectId() {
        return sObjectId;
    }

    public void setSObjectId(String sObjectId) {
        this.sObjectId = sObjectId;
    }

    public String getSObjectFields() {
        return sObjectFields;
    }

    public void setSObjectFields(String sObjectFields) {
        this.sObjectFields = sObjectFields;
    }

    public String getSObjectIdName() {
        return sObjectIdName;
    }

    public void setSObjectIdName(String sObjectIdName) {
        this.sObjectIdName = sObjectIdName;
    }

    public String getSObjectIdValue() {
        return sObjectIdValue;
    }

    public void setSObjectIdValue(String sObjectIdValue) {
        this.sObjectIdValue = sObjectIdValue;
    }

    public String getSObjectClass() {
        return sObjectClass;
    }

    public void setSObjectClass(String sObjectClass) {
        this.sObjectClass = sObjectClass;
    }

    @Override
    public String toUriString(UriFormat format) {
        // ignore format, what is this used for anyway???
        return getURI().toString();
    }

    public Map<String, String> toValueMap() {

        final Map<String, String> valueMap = new HashMap<String, String>();
        valueMap.put("format", format.toString().toLowerCase());
        valueMap.put("apiVersion", apiVersion);

        valueMap.put("sObjectName", sObjectName);
        valueMap.put("sObjectId", sObjectId);
        valueMap.put("sObjectFields", sObjectFields);
        valueMap.put("sObjectIdName", sObjectIdName);
        valueMap.put("sObjectIdValue", sObjectIdValue);
        valueMap.put("sObjectClass", sObjectClass);

        return Collections.unmodifiableMap(valueMap);
    }

}