/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.http.connector;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXBuilder;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.util.AXIOMUtils;
import org.apache.synapse.util.InlineExpressionUtil;
import org.jaxen.JaxenException;
import org.json.JSONArray;
import org.wso2.carbon.connector.core.AbstractConnector;

import java.io.StringReader;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.wso2.carbon.http.connector.Constants.HEADERS_IDENTIFIER;
import static org.wso2.carbon.http.connector.Constants.JSON_CONTENT_TYPE;
import static org.wso2.carbon.http.connector.Constants.JSON_TYPE;
import static org.wso2.carbon.http.connector.Constants.RELATIVE_PATH_IDENTIFIER;
import static org.wso2.carbon.http.connector.Constants.REQUEST_BODY_JSON_IDENTIFIER;
import static org.wso2.carbon.http.connector.Constants.REQUEST_BODY_TEXT_IDENTIFIER;
import static org.wso2.carbon.http.connector.Constants.REQUEST_BODY_TYPE_IDENTIFIER;
import static org.wso2.carbon.http.connector.Constants.REQUEST_BODY_XML_IDENTIFIER;
import static org.wso2.carbon.http.connector.Constants.SOAP11_CONTENT_TYPE;
import static org.wso2.carbon.http.connector.Constants.SOAP12_CONTENT_TYPE;
import static org.wso2.carbon.http.connector.Constants.TEXT_CONTENT_TYPE;
import static org.wso2.carbon.http.connector.Constants.TEXT_TYPE;
import static org.wso2.carbon.http.connector.Constants.XML_CONTENT_TYPE;
import static org.wso2.carbon.http.connector.Constants.XML_TYPE;

public class RestURLBuilder extends AbstractConnector {

    private final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

    public RestURLBuilder() {

        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        Map props = StAXUtils.loadFactoryProperties("XMLInputFactory.properties");
        if (props != null) {
            for (Object o : props.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                xmlInputFactory.setProperty((String) entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void connect(MessageContext messageContext) {

        String relativePath = messageContext.getProperty(RELATIVE_PATH_IDENTIFIER) != null ?
                (String) messageContext.getProperty(RELATIVE_PATH_IDENTIFIER) : StringUtils.EMPTY;
        String headers = messageContext.getProperty(HEADERS_IDENTIFIER) != null ? (String) messageContext.getProperty(
                HEADERS_IDENTIFIER) : Constants.EMPTY_LIST;
        String requestBodyType = messageContext.getProperty(REQUEST_BODY_TYPE_IDENTIFIER) != null ?
                (String) messageContext.getProperty(REQUEST_BODY_TYPE_IDENTIFIER) : StringUtils.EMPTY;
        String requestBody = getRequestBody(messageContext);

        handleInputHeaders(messageContext, headers);
        handlePayload(messageContext, requestBodyType, requestBody);
        resolveRelativePath(messageContext, relativePath);
    }

    /**
     * Gets the request body from input request bodies.
     *
     * @param messageContext the message context
     * @return the request body
     */
    private String getRequestBody(MessageContext messageContext) {

        String requestBodyJson = (String) messageContext.getProperty(REQUEST_BODY_JSON_IDENTIFIER);
        String requestBodyXml = (String) messageContext.getProperty(REQUEST_BODY_XML_IDENTIFIER);
        String requestBodyText = (String) messageContext.getProperty(REQUEST_BODY_TEXT_IDENTIFIER);

        if (StringUtils.isNotEmpty(requestBodyJson)) {
            return getProcessedJsonRequestBody(requestBodyJson);
        } else if (StringUtils.isNotEmpty(requestBodyXml)) {
            return requestBodyXml;
        } else if (StringUtils.isNotEmpty(requestBodyText)) {
            return requestBodyText;
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Resolves the relative path using inline expression templates.
     *
     * @param messageContext the message context
     * @param relativePath the relative path to be resolved
     */
    private void resolveRelativePath(MessageContext messageContext, String relativePath) {

        try {
            relativePath = InlineExpressionUtil.processInLineSynapseExpressionTemplate(messageContext, relativePath);
            messageContext.setProperty(Constants.URL_PATH, relativePath);
        } catch (JaxenException e) {
            handleException("Error while processing relative path: " + relativePath, e, messageContext);
        }
    }

    /**
     * Handles the payload of the message context based on the request body type.
     * It processes the request body and sets the appropriate content type.
     *
     * @param messageContext the message context
     * @param requestBodyType the type of the request body
     * @param requestBody the request body
     */
    private void handlePayload(MessageContext messageContext, String requestBodyType, String requestBody) {

        if (StringUtils.isNotEmpty(requestBodyType)) {
            org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext)messageContext).getAxis2MessageContext();
            try {
                requestBody = InlineExpressionUtil.processInLineSynapseExpressionTemplate(messageContext, requestBody);
            } catch (JaxenException e) {
                handleException("Error while processing request body", messageContext);
            }
            if (requestBodyType.equals(XML_TYPE)) {
                try {
                    requestBody = "<pfPadding>" + requestBody + "</pfPadding>";
                    JsonUtil.removeJsonPayload(axis2MessageContext);
                    XMLStreamReader xmlReader = xmlInputFactory.createXMLStreamReader(new StringReader(requestBody));
                    StAXBuilder builder = new StAXOMBuilder(xmlReader);
                    OMElement omXML = builder.getDocumentElement();
                    if (!checkAndReplaceEnvelope(omXML, messageContext)) {
                        axis2MessageContext.getEnvelope().getBody().addChild(omXML.getFirstElement());
                    }
                } catch (XMLStreamException var9) {
                    handleException("Error creating SOAP Envelope from source " + requestBody, messageContext);
                }
            } else if (requestBodyType.equals(JSON_TYPE)) {
                try {
                    JsonUtil.getNewJsonPayload(axis2MessageContext, requestBody, true, true);
                } catch (AxisFault var8) {
                    handleException("Error creating JSON Payload from source " + requestBody, messageContext);
                }
            } else if (requestBodyType.equals(TEXT_TYPE)) {
                JsonUtil.removeJsonPayload(axis2MessageContext);
                axis2MessageContext.getEnvelope().getBody().addChild(Utils.getTextElement(requestBody));
            }
            setContentType(messageContext, requestBodyType);
        }
    }

    /**
     * Processes the json request body by removing surrounding single quotes
     *
     * @param requestBody
     * @return
     */
    private String getProcessedJsonRequestBody(String requestBody) {

        if (requestBody.startsWith(Constants.SINGLE_QUOTE) && requestBody.endsWith(Constants.SINGLE_QUOTE)) {
            return requestBody.substring(1, requestBody.length() - 1);
        }
        return requestBody;
    }

    /**
     * Sets the content type of the message context based on the request body type.
     *
     * @param synCtx the message context
     * @param requestBodyType the type of the request body
     */
    private void setContentType(MessageContext synCtx, String requestBodyType) {

        org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        if (requestBodyType.equals(XML_TYPE)) {
            if (!XML_CONTENT_TYPE.equals(a2mc.getProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE)) &&
                    !SOAP11_CONTENT_TYPE.equals(a2mc.getProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE)) &&
                    !SOAP12_CONTENT_TYPE.equals(a2mc.getProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE))) {
                a2mc.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, XML_CONTENT_TYPE);
                a2mc.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, XML_CONTENT_TYPE);
                handleSpecialProperties(XML_CONTENT_TYPE, a2mc);
            }
        } else if (requestBodyType.equals(JSON_TYPE)) {
            a2mc.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, JSON_CONTENT_TYPE);
            a2mc.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, JSON_CONTENT_TYPE);
            handleSpecialProperties(JSON_CONTENT_TYPE, a2mc);
        } else if (requestBodyType.equals(TEXT_TYPE)) {
            a2mc.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, TEXT_CONTENT_TYPE);
            a2mc.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, TEXT_CONTENT_TYPE);
            handleSpecialProperties(TEXT_CONTENT_TYPE, a2mc);
        }
        a2mc.removeProperty("NO_ENTITY_BODY");
    }

    /**
     * Used to change the content type.
     *
     * @param resultValue the content type
     * @param axis2MessageCtx the Axis2 message context
     */
    private void handleSpecialProperties(Object resultValue,
                                         org.apache.axis2.context.MessageContext axis2MessageCtx) {

        axis2MessageCtx.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, resultValue);
        Object o = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        Map headers = (Map) o;
        if (headers != null) {
            headers.remove(HTTP.CONTENT_TYPE);
            headers.put(HTTP.CONTENT_TYPE, resultValue);
        }
    }

    /**
     * Checks and replaces the SOAP envelope if the result element is a valid SOAP envelope.
     *
     * @param resultElement the result element
     * @param synCtx the message context
     * @return true if the envelope was replaced, false otherwise
     */
    private boolean checkAndReplaceEnvelope(OMElement resultElement, MessageContext synCtx) {
        OMElement firstChild = resultElement.getFirstElement();

        if (firstChild == null) {
            handleException("Generated content is not a valid XML payload", synCtx);
        }

        QName resultQName = firstChild.getQName();
        if (resultQName.getLocalPart().equals("Envelope") && (
                resultQName.getNamespaceURI().equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI) ||
                        resultQName.getNamespaceURI().
                                equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI))) {
            SOAPEnvelope soapEnvelope = AXIOMUtils.getSOAPEnvFromOM(resultElement.getFirstElement());
            if (soapEnvelope != null) {
                try {
                    soapEnvelope.buildWithAttachments();
                    synCtx.setEnvelope(soapEnvelope);
                } catch (AxisFault axisFault) {
                    handleException("Unable to attach SOAPEnvelope", axisFault, synCtx);
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Handles input headers by processing inline expression templates and setting transport headers.
     * This method processes the headers provided as a string in the JSON array format,
     * evaluates any inline expressions, and sets the transport headers in the message context.
     *
     * @param messageContext the message context
     * @param headers the headers in JSON array format
     */
    private void handleInputHeaders(MessageContext messageContext, String headers) {

        try {
            if (StringUtils.isNotEmpty(headers)) {
                headers = InlineExpressionUtil.processInLineSynapseExpressionTemplate(messageContext, headers);
                JSONArray headersArray = new JSONArray(headers);
                if (headersArray.length() > 0) {
                    Utils.initializeTransportHeaders(messageContext);
                    Object transportHeaders = Utils.getTransportHeaders(messageContext);
                    for (int i = 0; i < headersArray.length(); i++) {
                        JSONArray headersItem = headersArray.getJSONArray(i);
                        if (headersItem.length() == 2) {
                            String headerName = headersItem.getString(0).trim();
                            String headerValue = headersItem.getString(1).trim();
                            if (transportHeaders instanceof Map) {
                                Map transportHeadersMap = (Map) transportHeaders;
                                transportHeadersMap.put(headerName, headerValue);
                            }
                        }
                    }
                }
            }
        } catch (JaxenException e) {
            handleException("Error while processing headers", messageContext);
        }
    }
}
