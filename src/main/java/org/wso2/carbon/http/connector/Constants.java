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

import javax.xml.namespace.QName;

public class Constants {

    public static final String RELATIVE_PATH_IDENTIFIER = "HTTP_CONN_RELATIVE_PATH";
    public static final String HEADERS_IDENTIFIER = "HTTP_CONN_HEADERS";
    public static final String REQUEST_BODY_TYPE_IDENTIFIER = "HTTP_CONN_REQUEST_BODY_TYPE";
    public static final String REQUEST_BODY_IDENTIFIER = "HTTP_CONN_REQUEST_BODY";
    public static final QName TEXT_ELEMENT = new QName("http://ws.apache.org/commons/ns/payload", "text");
    public static final String JSON_TYPE = "json";
    public static final String XML_TYPE = "xml";
    public static final String TEXT_TYPE = "text";
    public static final String URL_PATH = "uri.var.path";
    public static final String SINGLE_QUOTE = "\'";
    public static final String XML_CONTENT_TYPE = "application/xml";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String TEXT_CONTENT_TYPE = "text/plain";
    public static final String SOAP11_CONTENT_TYPE = "text/xml";
    public static final String SOAP12_CONTENT_TYPE = "application/soap+xml";
}
