/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.messaging.kafka.nativeimpl.producer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.api.BArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ballerinalang.messaging.kafka.utils.KafkaConstants.ALIAS_PARTITION;
import static org.ballerinalang.messaging.kafka.utils.KafkaUtils.getIntValue;
import static org.ballerinalang.messaging.kafka.utils.KafkaUtils.getLongValue;

/**
 * Native methods to send {@code any} values and with different types of keys to Kafka broker from ballerina kafka
 * producer.
 */
public class SendCustomValues extends Send {
    /* *********************************************************************** *
     *          Send records with value of type any (custom)                   *
     *       The value is considered first since key can be null               *
     ************************************************************************* */

    private static final Logger logger = LoggerFactory.getLogger(SendCustomValues.class);

    // ballerina anydata
    public static Object sendCustomValuesNilKeys(ObjectValue producer, Object value, String topic, Object partition,
                                                 Object timestamp) {
        Integer partitionValue = getIntValue(partition, ALIAS_PARTITION, logger);
        Long timestampValue = getLongValue(timestamp);
        ProducerRecord<?, Object> kafkaRecord = new ProducerRecord<>(topic, partitionValue, timestampValue, null,
                                                                     value);
        return sendKafkaRecord(kafkaRecord, producer);
    }

    // ballerina anydata and String
    public static Object sendCustomValuesStringKeys(ObjectValue producer, Object value, String topic, String key,
                                                    Object partition, Object timestamp) {
        Integer partitionValue = getIntValue(partition, ALIAS_PARTITION, logger);
        Long timestampValue = getLongValue(timestamp);
        ProducerRecord<String, Object> kafkaRecord = new ProducerRecord<>(topic, partitionValue, timestampValue, key,
                                                                          value);
        return sendKafkaRecord(kafkaRecord, producer);
    }

    // ballerina anydata and ballerina int
    public static Object sendCustomValuesIntKeys(ObjectValue producer, Object value, String topic, long key,
                                                 Object partition, Object timestamp) {
        Integer partitionValue = getIntValue(partition, ALIAS_PARTITION, logger);
        Long timestampValue = getLongValue(timestamp);
        ProducerRecord<Long, Object> kafkaRecord = new ProducerRecord<>(topic, partitionValue, timestampValue, key,
                                                                        value);
        return sendKafkaRecord(kafkaRecord, producer);
    }

    // ballerina anydata and ballerina float
    public static Object sendCustomValuesFloatKeys(ObjectValue producer, Object value, String topic, double key,
                                                   Object partition, Object timestamp) {
        Integer partitionValue = getIntValue(partition, ALIAS_PARTITION, logger);
        Long timestampValue = getLongValue(timestamp);
        ProducerRecord<Double, Object> kafkaRecord = new ProducerRecord<>(topic, partitionValue, timestampValue, key,
                                                                          value);
        return sendKafkaRecord(kafkaRecord, producer);
    }

    // ballerina anydata and ballerina byte[]
    public static Object sendCustomValuesByteArrayKeys(ObjectValue producer, Object value, String topic, BArray key,
                                                       Object partition, Object timestamp) {
        Integer partitionValue = getIntValue(partition, ALIAS_PARTITION, logger);
        Long timestampValue = getLongValue(timestamp);
        ProducerRecord<byte[], Object> kafkaRecord = new ProducerRecord<>(topic, partitionValue, timestampValue,
                                                                          key.getBytes(), value);
        return sendKafkaRecord(kafkaRecord, producer);
    }

    // ballerina anydata and ballerina anydata
    public static Object sendCustomValuesCustomKeys(ObjectValue producer, Object value, String topic, Object key,
                                                    Object partition, Object timestamp) {
        Integer partitionValue = getIntValue(partition, ALIAS_PARTITION, logger);
        Long timestampValue = getLongValue(timestamp);
        ProducerRecord<Object, Object> kafkaRecord = new ProducerRecord<>(topic, partitionValue, timestampValue, key,
                                                                          value);
        return sendKafkaRecord(kafkaRecord, producer);
    }
}
