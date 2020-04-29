/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.net.http.websocket.client;

import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.net.http.websocket.WebSocketConstants;
import org.ballerinalang.net.http.websocket.WebSocketException;
import org.ballerinalang.net.http.websocket.WebSocketUtil;
import org.ballerinalang.net.http.websocket.client.listener.ClientConnectorListener;
import org.ballerinalang.net.http.websocket.client.listener.FailoverConnectorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Initializes the Failover WebSocket Client.
 *
 * @since 1.2.0
 */
public class FailoverInitEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(FailoverInitEndpoint.class);
    private static final String FAILOVER_INTERVAL = "failoverIntervalInMillis";

    public static void initEndpoint(ObjectValue failoverClient) {
        @SuppressWarnings(WebSocketConstants.UNCHECKED)
        MapValue<String, Object> clientEndpointConfig = (MapValue<String, Object>) failoverClient.getMapValue(
                WebSocketConstants.CLIENT_ENDPOINT_CONFIG);
        List<String> newTargetUrls = getValidUrls(clientEndpointConfig.getArrayValue(WebSocketConstants.TARGET_URLS));
        // Sets the failover config values.
        failoverClient.set(WebSocketConstants.CLIENT_URL_CONFIG, newTargetUrls.get(0));
        FailoverContext failoverContext = new FailoverContext();
        populateFailoverContext(clientEndpointConfig, failoverContext, newTargetUrls);
        failoverClient.addNativeData(WebSocketConstants.FAILOVER_CONTEXT, failoverContext);
        failoverClient.addNativeData(WebSocketConstants.CLIENT_LISTENER, new FailoverConnectorListener(
                new ClientConnectorListener()));
        InitEndpoint.initEndpoint(failoverClient);
    }

    /**
     * Populates the failover config.
     *
     * @param failoverConfig - a failover config
     * @param failoverClientConnectorConfig - a failover client connector config
     * @param targetUrls - target URLs
     */
    private static void populateFailoverContext(MapValue<String, Object> failoverConfig,
                                                FailoverContext failoverClientConnectorConfig,
                                                List<String> targetUrls) {
        failoverClientConnectorConfig.setFailoverInterval(WebSocketUtil.getIntValue(failoverConfig, FAILOVER_INTERVAL,
                1000));
        failoverClientConnectorConfig.setTargetUrls(targetUrls);
    }

    /**
     * Checks whether the URL has a valid format or not. If it isn't in the valid format, removes that from the URL set.
     *
     * @param targets - target URLs array
     * @return - validated target URLs array
     */
    private static List<String> getValidUrls(ArrayValue targets) {
        List<String> newTargetUrls = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < targets.size(); i++) {
            String url = targets.get(i).toString();
            try {
                URI uri = new URI(url);
                String scheme = uri.getScheme();
                if (!WebSocketConstants.WS_SCHEME.equalsIgnoreCase(scheme) && !WebSocketConstants.WSS_SCHEME.
                        equalsIgnoreCase(scheme)) {
                    String name = targets.get(i).toString();
                    logger.error("{} drop from the targets url because webSocket client supports only WS(S) scheme.",
                            name);
                } else {
                    newTargetUrls.add(index, url);
                    index++;
                }
            } catch (URISyntaxException e) {
                logger.error("Error occurred when constructing a hierarchical URI from the " +
                        "given url[" + url + "].");
            }
        }
        if (newTargetUrls.isEmpty()) {
            throw new WebSocketException("TargetUrls should have at least one valid URL.");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("New targetUrls: {}", newTargetUrls);
        }
        return newTargetUrls;
    }

    private FailoverInitEndpoint() {
    }
}
