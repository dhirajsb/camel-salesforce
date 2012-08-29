package org.fusesource.camel.component.salesforce;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultEndpointConfiguration;
import org.fusesource.camel.component.salesforce.internal.PayloadFormat;
import org.fusesource.camel.component.salesforce.internal.RestClientHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SalesforceEndpointConfig extends DefaultEndpointConfiguration {

    private static final String EXCHANGE_PROPERTY_PREFIX = SalesforceEndpointConfig.class.getName();
    public static final String SOBJECT_NAME = EXCHANGE_PROPERTY_PREFIX + ".sObjectName";
    public static final String SOBJECT_ID = EXCHANGE_PROPERTY_PREFIX + ".sObjectId";
    public static final String SOBJECT_FIELDS = EXCHANGE_PROPERTY_PREFIX + ".sObjectFields";
    public static final String SOBJECT_EXT_ID_NAME = EXCHANGE_PROPERTY_PREFIX + ".sObjectIdName";
    public static final String SOBJECT_EXT_ID_VALUE = EXCHANGE_PROPERTY_PREFIX + ".sObjectIdValue";

    private PayloadFormat format;
    private String apiVersion;
    private RestClientHelper.ApiName apiName;// endpoint properties for APIs

    private String sObjectName;
    private String sObjectId;
    private String sObjectFields;
    private String sObjectIdName;
    private String sObjectIdValue;

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

    public RestClientHelper.ApiName getApiName() {
        return apiName;
    }

    public void setApiName(RestClientHelper.ApiName apiName) {
        this.apiName = apiName;
    }

    public String getsObjectName() {
        return sObjectName;
    }

    public void setsObjectName(String sObjectName) {
        this.sObjectName = sObjectName;
    }

    public String getsObjectId() {
        return sObjectId;
    }

    public void setsObjectId(String sObjectId) {
        this.sObjectId = sObjectId;
    }

    public String getsObjectFields() {
        return sObjectFields;
    }

    public void setsObjectFields(String sObjectFields) {
        this.sObjectFields = sObjectFields;
    }

    public String getsObjectIdName() {
        return sObjectIdName;
    }

    public void setsObjectIdName(String sObjectIdName) {
        this.sObjectIdName = sObjectIdName;
    }

    public String getsObjectIdValue() {
        return sObjectIdValue;
    }

    public void setsObjectIdValue(String sObjectIdValue) {
        this.sObjectIdValue = sObjectIdValue;
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
        valueMap.put("apiName", apiName.toString());

        valueMap.put("sObjectName", sObjectName);
        valueMap.put("sObjectId", sObjectId);
        valueMap.put("sObjectFields", sObjectFields);
        valueMap.put("sObjectIdName", sObjectIdName);
        valueMap.put("sObjectIdValue", sObjectIdValue);

        return Collections.unmodifiableMap(valueMap);
    }

}