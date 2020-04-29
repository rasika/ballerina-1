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

import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.websocket.WebSocketConstants;
import org.ballerinalang.net.http.websocket.WebSocketUtil;
import org.ballerinalang.net.http.websocket.client.listener.ClientConnectorListener;
import org.ballerinalang.net.http.websocket.client.listener.RetryConnectorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the Retry WebSocket Client.
 *
 * @since 1.2.0
 */
public class RetryInitEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RetryInitEndpoint.class);
    private static final String INTERVAL_IN_MILLIS = "intervalInMillis";
    private static final String MAX_WAIT_INTERVAL = "maxWaitIntervalInMillis";
    private static final String MAX_COUNT = "maxCount";
    private static final String BACK_OF_FACTOR = "backOffFactor";

    public static void initEndpoint(ObjectValue retryClient) {
        @SuppressWarnings(WebSocketConstants.UNCHECKED)
        MapValue<String, Object> clientEndpointConfig = (MapValue<String, Object>) retryClient.getMapValue(
                HttpConstants.CLIENT_ENDPOINT_CONFIG);
        @SuppressWarnings(WebSocketConstants.UNCHECKED)
        MapValue<String, Object> retryConfig = (MapValue<String, Object>) clientEndpointConfig.getMapValue(
                WebSocketConstants.RETRY_CONTEXT);
        RetryContext retryConnectorConfig = new RetryContext();
        populateRetryConnectorConfig(retryConfig, retryConnectorConfig);
        retryClient.addNativeData(WebSocketConstants.RETRY_CONTEXT, retryConnectorConfig);
        retryClient.addNativeData(WebSocketConstants.CLIENT_LISTENER, new RetryConnectorListener(
                new ClientConnectorListener()));
        InitEndpoint.initEndpoint(retryClient);
    }

    /**
     * Populate the retry config.
     *
     * @param retryConfig - the retry config
     * @param retryConnectorConfig - the retry connector config
     */
    private static void populateRetryConnectorConfig(MapValue<String, Object> retryConfig,
                                                     RetryContext retryConnectorConfig) {
        retryConnectorConfig.setInterval(WebSocketUtil.getIntValue(retryConfig, INTERVAL_IN_MILLIS, 1000));
        retryConnectorConfig.setBackOfFactor(getDoubleValue(retryConfig));
        retryConnectorConfig.setMaxInterval(WebSocketUtil.getIntValue(retryConfig, MAX_WAIT_INTERVAL, 30000));
        retryConnectorConfig.setMaxAttempts(WebSocketUtil.getIntValue(retryConfig, MAX_COUNT, 0));
    }

    private static Double getDoubleValue(MapValue<String, Object> configs) {
        double value = Math.toRadians(configs.getFloatValue(BACK_OF_FACTOR));
        if (value < 1) {
            logger.warn("The value set for `backOffFactor` needs to be great than than 1. The `backOffFactor`" +
                    " value is set to {}", 1.0);
            value = 1.0;
        }
        return value;
    }

    private RetryInitEndpoint() {
    }
}
