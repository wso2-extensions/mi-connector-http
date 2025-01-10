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
import org.apache.axiom.om.OMFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class Utils {

    /**
     * Creates an OMElement with the specified content as its text.
     *
     * @param content the text content to be set in the OMElement. If null, an empty string is used.
     * @return the created OMElement with the specified text content.
     */
    public static OMElement getTextElement(String content) {

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement textElement = factory.createOMElement(Constants.TEXT_ELEMENT);
        if (content == null) {
            content = "";
        }

        textElement.setText(content);
        return textElement;
    }

    /**
     * Gets the transport headers from the message context.
     *
     * @param messageContext the message context
     * @return the transport headers
     */
    public static Object getTransportHeaders(MessageContext messageContext) {

        Axis2MessageContext axis2smc = (Axis2MessageContext) messageContext;
        org.apache.axis2.context.MessageContext axis2MessageContext =
                axis2smc.getAxis2MessageContext();
        return axis2MessageContext.getProperty("TRANSPORT_HEADERS");
    }

    /**
     * Initializes the transport headers in the message context.
     *
     * @param messageContext the message context
     */
    public static void initializeTransportHeaders(MessageContext messageContext) {

        Axis2MessageContext axis2smc = (Axis2MessageContext) messageContext;
        org.apache.axis2.context.MessageContext axis2MessageContext =
                axis2smc.getAxis2MessageContext();
        if (axis2MessageContext.getProperty("TRANSPORT_HEADERS") == null) {
            Map<String, Object> headersMap = new TreeMap(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return o1.compareToIgnoreCase(o2);
                }
            });
            axis2MessageContext.setProperty("TRANSPORT_HEADERS", headersMap);
        }
    }
}
