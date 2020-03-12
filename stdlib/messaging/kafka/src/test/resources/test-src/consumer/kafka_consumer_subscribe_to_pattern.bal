// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/kafka;

kafka:ConsumerConfiguration consumerConfigs = {
    bootstrapServers: "localhost:14106",
    groupId: "test-group",
    clientId: "subscribe-to-pattern-consumer",
    metadataMaxAgeInMillis: 100,
    defaultApiTimeoutInMillis: 100
};

kafka:Consumer kafkaConsumer = new(consumerConfigs);

function funcKafkaTestSubscribeToPattern() {
    var result = kafkaConsumer->subscribeToPattern("test.*");
}

function funcKafkaTestUnsubscribe() returns kafka:ConsumerError? {
    return kafkaConsumer->unsubscribe();
}

function funcKafkaTestGetSubscribedTopicCount() returns int|error {
    var result = kafkaConsumer->getSubscription();
    if (result is error) {
        return result;
    } else {
        return result.length();
    }
}

function funcKafkaGetAvailableTopicsCount() returns int|error {
    var result = kafkaConsumer->getAvailableTopics(duration = 100);
    if (result is error) {
        return result;
    } else {
        return result.length();
    }
}
