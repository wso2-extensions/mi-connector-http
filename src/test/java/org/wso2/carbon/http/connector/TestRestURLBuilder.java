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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestRestURLBuilder {

    @Test
    public void testProcessUrlPath() throws AxisFault {

        String relativePath = "/books/${var.id}/author/${var.name}";
        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.RELATIVE_PATH_IDENTIFIER, relativePath);
        messageContext.setVariable("id", 1);
        messageContext.setVariable("name", "John");
        restURLBuilder.connect(messageContext);
        String expectedUrlPath = (String) messageContext.getProperty(Constants.URL_PATH);
        assertEquals(expectedUrlPath, "/books/1/author/John");
    }

    @Test
    public void testUrlWithoutPathParameters() throws AxisFault {

        String relativePath = "/books/${var.id}";
        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.RELATIVE_PATH_IDENTIFIER, relativePath);
        restURLBuilder.connect(messageContext);
        String expectedUrlPath = (String) messageContext.getProperty(Constants.URL_PATH);
        assertEquals(expectedUrlPath, "/books/");
    }

    @Test
    public void testAddHeaders() throws AxisFault {

        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.HEADERS_IDENTIFIER, "[[\"Content-Type\", \"application/xml\"], [\"Accept\", \"application/xml\"]]");
        restURLBuilder.connect(messageContext);
        Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
        Object transportHeaders = axis2MessageContext.getAxis2MessageContext().getProperty("TRANSPORT_HEADERS");
        assertNotNull(transportHeaders);
        Map transportHeadersMap = (Map) transportHeaders;
        String actualHeader1 = (String) transportHeadersMap.get("Content-Type");
        String actualHeader2 = (String) transportHeadersMap.get("Accept");
        assertEquals("application/xml", actualHeader1);
        assertEquals("application/xml", actualHeader2);
    }

    @Test
    public void testAddNoHeaders() throws AxisFault {

        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        restURLBuilder.connect(messageContext);
        Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
        Object transportHeaders = axis2MessageContext.getAxis2MessageContext().getProperty("TRANSPORT_HEADERS");
        assertTrue(transportHeaders == null);
    }

    @Test
    public void testAddEmptyHeaders() throws AxisFault {

        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.HEADERS_IDENTIFIER, "");
        restURLBuilder.connect(messageContext);
        Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
        Object transportHeaders = axis2MessageContext.getAxis2MessageContext().getProperty("TRANSPORT_HEADERS");
        assertTrue(transportHeaders == null);
    }

    @Test
    public void testProcessJsonRequestBody() throws AxisFault {

        String expectedBody = "<jsonObject><id>7</id><name>Peoples</name></jsonObject>";
        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.HEADERS_IDENTIFIER, "[[\"Content-Type\", \"application/json\"]]");
        messageContext.setProperty(Constants.REQUEST_BODY_TYPE_IDENTIFIER, "json");
        messageContext.setProperty(Constants.REQUEST_BODY_IDENTIFIER, "{\"id\": 7, \"name\": \"Peoples\"}");
        restURLBuilder.connect(messageContext);
        Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
        String actualBody =
                axis2MessageContext.getAxis2MessageContext().getEnvelope().getBody().getFirstElement().toString();
        assertEquals(expectedBody, actualBody);
    }

    @Test
    public void testProcessXmlRequestBody() throws AxisFault {

        String expectedBody = "<bank><id>7</id><name>Peoples</name></bank>";
        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.HEADERS_IDENTIFIER, "[[\"Content-Type\", \"application/xml\"]]");
        messageContext.setProperty(Constants.REQUEST_BODY_TYPE_IDENTIFIER, "xml");
        messageContext.setProperty(Constants.REQUEST_BODY_IDENTIFIER, "<bank><id>7</id><name>Peoples</name></bank>");
        restURLBuilder.connect(messageContext);
        Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
        String actualBody =
                axis2MessageContext.getAxis2MessageContext().getEnvelope().getBody().getFirstElement().toString();
        assertEquals(expectedBody, actualBody);
    }

    @Test
    public void testProcessTextRequestBody() throws AxisFault {

        String expectedBody = "<axis2ns1:text xmlns:axis2ns1=\"http://ws.apache.org/commons/ns/payload\">7</axis2ns1:text>";
        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.HEADERS_IDENTIFIER, "[[\"Content-Type\", \"text/plain\"]]");
        messageContext.setProperty(Constants.REQUEST_BODY_TYPE_IDENTIFIER, "text");
        messageContext.setProperty(Constants.REQUEST_BODY_IDENTIFIER, "7");
        restURLBuilder.connect(messageContext);
        Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
        String actualBody =
                axis2MessageContext.getAxis2MessageContext().getEnvelope().getBody().getFirstElement().toString();
        assertEquals(expectedBody, actualBody);
    }

    @Test
    public void testProcessNoRequestBody() throws AxisFault {

        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.HEADERS_IDENTIFIER, "[[\"Content-Type\", \"text/plain\"]]");
        messageContext.setProperty(Constants.REQUEST_BODY_TYPE_IDENTIFIER, "text");
        messageContext.setProperty(Constants.REQUEST_BODY_IDENTIFIER, "7");
        restURLBuilder.connect(messageContext);
        Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
        SOAPEnvelope initialEnvelope = axis2MessageContext.getEnvelope();

        RestURLBuilder newRestURLBuilder = new RestURLBuilder();
        MessageContext newMessageContext = createMessageContext();
        newMessageContext.setProperty(Constants.HEADERS_IDENTIFIER, "[[\"Content-Type\", \"text/plain\"]]");
        newMessageContext.setProperty(Constants.REQUEST_BODY_TYPE_IDENTIFIER, "text");
        newMessageContext.setEnvelope(initialEnvelope);
        newRestURLBuilder.connect(newMessageContext);
        Axis2MessageContext newAxis2MessageContext = (Axis2MessageContext) newMessageContext;
        String actualBody =
                newAxis2MessageContext.getAxis2MessageContext().getEnvelope().getBody().getFirstElement().toString();
        assertEquals(initialEnvelope.getBody().getFirstElement().toString(), actualBody);
    }

    @Test
    public void testProcessEmptyRequestBody() throws AxisFault {

        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.HEADERS_IDENTIFIER, "[[\"Content-Type\", \"application/json\"]]");
        messageContext.setProperty(Constants.REQUEST_BODY_TYPE_IDENTIFIER, "json");
        messageContext.setProperty(Constants.REQUEST_BODY_IDENTIFIER, "{ \"age\": 24 }");
        restURLBuilder.connect(messageContext);
        Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
        SOAPEnvelope initialEnvelope = axis2MessageContext.getEnvelope();

        RestURLBuilder newRestURLBuilder = new RestURLBuilder();
        MessageContext newMessageContext = createMessageContext();
        newMessageContext.setProperty(Constants.HEADERS_IDENTIFIER, "[[\"Content-Type\", \"application/json\"]]");
        newMessageContext.setProperty(Constants.REQUEST_BODY_TYPE_IDENTIFIER, "json");
        newMessageContext.setProperty(Constants.REQUEST_BODY_IDENTIFIER, "");
        newMessageContext.setEnvelope(initialEnvelope);
        newRestURLBuilder.connect(newMessageContext);
        Axis2MessageContext newAxis2MessageContext = (Axis2MessageContext) newMessageContext;
        String actualBody =
                newAxis2MessageContext.getAxis2MessageContext().getEnvelope().getBody().getFirstElement().toString();
        assertEquals(initialEnvelope.getBody().getFirstElement().toString(), actualBody);
    }

    @Test
    public void testProcessNoRequestBodyType() throws AxisFault {

        RestURLBuilder restURLBuilder = new RestURLBuilder();
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty(Constants.REQUEST_BODY_IDENTIFIER, "7");
        restURLBuilder.connect(messageContext);
        Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
        OMElement bodyElement = axis2MessageContext.getAxis2MessageContext().getEnvelope().getBody().getFirstElement();
        assertEquals(null, bodyElement);
    }

    @Test
    public void testProcessInvalidRequestBodyAndType() throws AxisFault {

        String expectedError = "Error creating JSON Payload from source <bank><id>7</id><name>Peoples</name></bank>";
        SynapseException synapseException = assertThrows(SynapseException.class, () -> {
            RestURLBuilder restURLBuilder = new RestURLBuilder();
            MessageContext messageContext = createMessageContext();
            messageContext.setProperty(Constants.HEADERS_IDENTIFIER, "[[\"Content-Type\", \"application/xml\"]]");
            messageContext.setProperty(Constants.REQUEST_BODY_TYPE_IDENTIFIER, "json");
            messageContext.setProperty(Constants.REQUEST_BODY_IDENTIFIER, "<bank><id>7</id><name>Peoples</name></bank>");
            restURLBuilder.connect(messageContext);
        });
        assertEquals(expectedError, synapseException.getMessage());
    }

    /**
     * Create a empty message context
     *
     * @return A context with empty message
     * @throws AxisFault on an error creating a context
     */
    private MessageContext createMessageContext() throws AxisFault {

        Axis2SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(new SynapseConfiguration());
        org.apache.axis2.context.MessageContext axis2MC
                = new org.apache.axis2.context.MessageContext();
        axis2MC.setConfigurationContext(new ConfigurationContext(new AxisConfiguration()));

        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MC.setServiceContext(svcCtx);
        axis2MC.setOperationContext(opCtx);
        axis2MC.setTransportIn(new TransportInDescription("http"));
        MessageContext mc = new Axis2MessageContext(axis2MC, new SynapseConfiguration(), synapseEnvironment);
        mc.setMessageID(UIDGenerator.generateURNString());
        mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
        mc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        return mc;
    }
}
