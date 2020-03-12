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

import ballerina/system;
import ballerinax/java;

# Struct which represents Kafka Producer configuration.
#
# + bootstrapServers - List of remote server endpoints of Kafka brokers.
# + acks - Number of acknowledgments. This can be either `kafka:ACKS_ALL`, `kafka:ACKS_SINGLE` or `kafka:ACKS_NONE`.
# + compressionType - Compression type to be used for messages.
# + clientId - Identifier to be used for server side logging.
# + metricsRecordingLevel - Metrics recording level.
# + metricReporterClasses - Metrics reporter classes.
# + partitionerClass - Partitioner class to be used to select partition to which the message is sent.
# + interceptorClasses - Interceptor classes to be used before sending records.
# + transactionalId - Transactional ID to be used in transactional delivery.
# + keySerializerType - Serializer used for the Kafka record key. This can be either `kafka:SerializerType` or a
#       user-defined serializer.
# + valueSerializerType - Serializer used for the Kafka record value. This can be either `kafka:SerializerType` or a
#       user-defined serializer.
# + keySerializer - Custom serializer object to serialize kafka keys. This should be implement the `kafka:Serializer`
#       object.
# + valueSerializer - Custom serializer object to serialize kafka values. This should be implement the
#       `kafka:Serializer` object.
# + schemaRegistryUrl - Avro schema registry url. Use this field to specify schema registry url, if Avro serializer
#       is used.
# + bufferMemory - Total bytes of memory the producer can use to buffer records.
# + retryCount - Number of retries to resend a record.
# + batchSize - Number of records to be batched for a single request. Use 0 for no batching.
# + linger - Delay to allow other records to be batched.
# + sendBuffer - Size of the TCP send buffer (SO_SNDBUF).
# + receiveBuffer - Size of the TCP receive buffer (SO_RCVBUF).
# + maxRequestSize - The maximum size of a request in bytes.
# + reconnectBackoffTimeInMillis - Time to wait before attempting to reconnect.
# + reconnectBackoffMaxTimeInMillis - Maximum amount of time in milliseconds to wait when reconnecting.
# + retryBackoffTimeInMillis - Time to wait before attempting to retry a failed request.
# + maxBlock - Maximum block time which the send is blocked, when the buffer is full.
# + requestTimeoutInMillis - Wait time for response of a request.
# + metadataMaxAgeInMillis - Maximum time to force a refresh of metadata.
# + metricsSampleWindowInMillis - Time window for a metrics sample to computed over.
# + metricsNumSamples - Number of samples maintained to compute metrics.
# + maxInFlightRequestsPerConnection - Maximum number of unacknowledged requests on a single connection.
# + connectionsMaxIdleTimeInMillis - Close idle connections after the number of milliseconds.
# + transactionTimeoutInMillis - Timeout for transaction status update from the producer.
# + enableIdempotence - Exactly one copy of each message is written in the stream when enabled.
# + secureSocket - Configurations related to SSL/TLS.
public type ProducerConfiguration record {|
    string bootstrapServers;
    ProducerAcks acks = ACKS_SINGLE;
    CompressionType compressionType = COMPRESSION_NONE;
    string clientId?;
    string metricsRecordingLevel?;
    string metricReporterClasses?;
    string partitionerClass?;
    string interceptorClasses?;
    string transactionalId?;

    SerializerType valueSerializerType = SER_BYTE_ARRAY;
    SerializerType keySerializerType = SER_BYTE_ARRAY;
    Serializer valueSerializer?;
    Serializer keySerializer?;
    string schemaRegistryUrl?;

    int bufferMemory?;
    int retryCount?;
    int batchSize?;
    int linger?;
    int sendBuffer?;
    int receiveBuffer?;
    int maxRequestSize?;
    int reconnectBackoffTimeInMillis?;
    int reconnectBackoffMaxTimeInMillis?;
    int retryBackoffTimeInMillis?;
    int maxBlock?;
    int requestTimeoutInMillis?;
    int metadataMaxAgeInMillis?;
    int metricsSampleWindowInMillis?;
    int metricsNumSamples?;
    int maxInFlightRequestsPerConnection?;
    int connectionsMaxIdleTimeInMillis?;
    int transactionTimeoutInMillis?;

    boolean enableIdempotence = false;

    SecureSocket secureSocket?;
|};

# Kafka producer acknowledgement type.
public type ProducerAcks ACKS_ALL|ACKS_NONE|ACKS_SINGLE;

# Kafka in-built serializer type.
public type SerializerType SER_BYTE_ARRAY|SER_STRING|SER_INT|SER_FLOAT|SER_AVRO|SER_CUSTOM;

# kafka compression type to compress the messages
public type CompressionType COMPRESSION_NONE|COMPRESSION_GZIP|COMPRESSION_SNAPPY|COMPRESSION_LZ4|COMPRESSION_ZSTD;

# Represent a Kafka producer endpoint.
#
# + connectorId - Unique ID for a particular connector.
# + producerConfig - Used to store configurations related to a Kafka connection.
public type Producer client object {

    public ProducerConfiguration? producerConfig = ();
    private string keySerializerType;
    private string valueSerializerType;
    private Serializer? keySerializer = ();
    private Serializer? valueSerializer = ();

    # Creates a new Kafka `Producer`.
    #
    # + config - Configurations related to initializing a Kafka `Producer`.
    public function __init(ProducerConfiguration config) {
        self.producerConfig = config;
        self.keySerializerType = config.keySerializerType;
        self.valueSerializerType = config.valueSerializerType;

        if (self.keySerializerType == SER_CUSTOM) {
            var keySerializerObject = config?.keySerializer;
            if (keySerializerObject is ()) {
                panic error(PRODUCER_ERROR, message = "Invalid keySerializer config: Please Provide a " +
                            "valid custom serializer for the keySerializer");
            } else {
                self.keySerializer = keySerializerObject;
            }
        }
        if (self.keySerializerType == SER_AVRO) {
            panic error(PRODUCER_ERROR, message = "Key serialization using Avro is not yet supported.");
            //var schemaRegistryUrl = config.schemaRegistryUrl;
            //if (schemaRegistryUrl is ()) {
            //    panic error(PRODUCER_ERROR, message = "Missing schema registry URL for the Avro serializer. Please " +
            //                "provide 'schemaRegistryUrl' configuration in 'kafka:ProducerConfiguration'.");
            //}
        }

        if (self.valueSerializerType == SER_CUSTOM) {
            var valueSerializerObject = config?.valueSerializer;
            if (valueSerializerObject is ()) {
                panic error(PRODUCER_ERROR, message = "Invalid valueSerializer config: Please Provide a " +
                            "valid custom serializer for the valueSerializer");
            } else {
                self.valueSerializer = valueSerializerObject;
            }
        }
        if (self.valueSerializerType == SER_AVRO) {
            var schemaRegistryUrl = config?.schemaRegistryUrl;
            if (schemaRegistryUrl is ()) {
                panic error(PRODUCER_ERROR, message = "Missing schema registry URL for the Avro serializer. Please " +
                            "provide 'schemaRegistryUrl' configuration in 'kafka:ProducerConfiguration'.");
            }
        }
        checkpanic producerInit(self);
    }

    public string connectorId = system:uuid();

    # Closes producer connection to the external Kafka broker.
    #
    # + return - `kafka:ProducerError` if closing the producer failed, nil otherwise.
    public remote function close() returns ProducerError? {
        return producerClose(self);
    }

    # Commits consumer action which commits consumer consumed offsets to offset topic.
    #
    # + consumer - Consumer which needs offsets to be committed.
    # + return - `kafka:ProducerError` if committing the consumer failed, nil otherwise.
    public remote function commitConsumer(Consumer consumer) returns ProducerError? {
        return producerCommitConsumer(self, consumer);
    }

    # CommitConsumerOffsets action which commits consumer offsets in given transaction.
    #
    # + offsets - Consumer offsets to commit for given transaction.
    # + groupID - Consumer group id.
    # + return - `kafka:ProducerError` if committing consumer offsets failed, nil otherwise.
    public remote function commitConsumerOffsets(PartitionOffset[] offsets, string groupID) returns ProducerError? {
        return producerCommitConsumerOffsets(self, offsets, java:fromString(groupID));
    }

    # Flush action which flush batch of records.
    #
    # + return - `kafka:ProducerError` if records couldn't be flushed, nil otherwise.
    public remote function flushRecords() returns ProducerError? {
        return producerFlushRecords(self);
    }

    # GetTopicPartitions action which returns given topic partition information.
    #
    # + topic - Topic which the partition information is given.
    # + return - `kafka:TopicPartition` array for the given topic, returns `kafka:ProducerError` if operation fails.
    public remote function getTopicPartitions(string topic) returns TopicPartition[]|ProducerError {
        return producerGetTopicPartitions(self, java:fromString(topic));
    }

    # Simple Send action which produce records to Kafka server.
    #
    # + value - Record contents.
    # + topic - Topic to which the record will be appended to.
    # + key - Key that will be included in the record.
    # + partition - Partition to which the record should be sent.
    # + timestamp - Timestamp of the record, in milliseconds since epoch.
    # + return - Returns `kafka:ProducerError` if send action fails to send data, nil otherwise.
    public remote function send(anydata value, string topic, public anydata? key = (), public int? partition = (),
        public int? timestamp = ()) returns ProducerError? {
        handle topicHandle = java:fromString(topic);
        // Handle string values
        if (self.valueSerializerType == SER_STRING) {
            if (value is string) {
                handle valueHandle = java:fromString(value);
                return sendStringValues(self, valueHandle, topicHandle, key, partition, timestamp,
                                        self.keySerializerType);
            }
            panic getValueTypeMismatchError(STRING);
        }
        // Handle int values
        if (self.valueSerializerType == SER_INT) {
            if (value is int) {
                return sendIntValues(self, value, topicHandle, key, partition, timestamp, self.keySerializerType);
            }
            panic getValueTypeMismatchError(INT);
        }
        // Handle float values
        if (self.valueSerializerType == SER_FLOAT) {
            if (value is float) {
                return sendFloatValues(self, value, topicHandle, key, partition, timestamp, self.keySerializerType);
            }
            panic getValueTypeMismatchError(FLOAT);
        }
        // Handle byte[] values
        if (self.valueSerializerType == SER_BYTE_ARRAY) {
            if (value is byte[]) {
                return sendByteArrayValues(self, value, topicHandle, key, partition, timestamp, self.keySerializerType);
            }
            panic getValueTypeMismatchError(BYTE_ARRAY);
        }
        // Handle Avro serializer
        if (self.valueSerializerType == SER_AVRO) {
            return sendAvroValues(self, value, topicHandle, key, partition, timestamp, self.keySerializerType);
        }
        // Handle custom values
        if (self.valueSerializerType == SER_CUSTOM) {
            return sendCustomValues(self, value, topicHandle, key, partition, timestamp, self.keySerializerType);
        }
        panic createProducerError("Invalid value serializer configuration");
    }
};

function sendStringValues(Producer producer, handle value, handle topic, anydata? key, int? partition, int? timestamp,
    string keySerializerType) returns ProducerError? {
    if (key is ()) {
        return producerSendString(producer, value, topic, partition, timestamp);
    }
    if (keySerializerType == SER_STRING) {
        if (key is string) {
            handle keyHandle = java:fromString(key);
            return producerSendStringString(producer, value, topic, keyHandle, partition, timestamp);
        }
        panic getKeyTypeMismatchError(STRING);
    }
    if (keySerializerType == SER_INT) {
        if (key is int) {
            return producerSendStringInt(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(INT);
    }
    if (keySerializerType == SER_FLOAT) {
        if (key is float) {
            return producerSendStringFloat(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(FLOAT);
    }
    if (keySerializerType == SER_BYTE_ARRAY) {
        if (key is byte[]) {
            return producerSendStringByteArray(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(BYTE_ARRAY);
    }
    if (keySerializerType == SER_CUSTOM) {
        return producerSendStringAny(producer, value, topic, key, partition, timestamp);
    }
}

function sendIntValues(Producer producer, int value, handle topic, anydata? key, int? partition, int? timestamp,
    string keySerializerType) returns ProducerError? {
    if (key is ()) {
        return producerSendInt(producer, value, topic, partition, timestamp);
    }
    if (keySerializerType == SER_STRING) {
        if (key is string) {
            handle keyHandle = java:fromString(key);
            return producerSendIntString(producer, value, topic, keyHandle, partition, timestamp);
        }
        panic getKeyTypeMismatchError(STRING);
    }
    if (keySerializerType == SER_INT) {
        if (key is int) {
            return producerSendIntInt(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(INT);
    }
    if (keySerializerType == SER_FLOAT) {
        if (key is float) {
            return producerSendIntFloat(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(FLOAT);
    }
    if (keySerializerType == SER_BYTE_ARRAY) {
        if (key is byte[]) {
            return producerSendIntByteArray(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(BYTE_ARRAY);
    }
    if (keySerializerType == SER_CUSTOM) {
        return producerSendIntAny(producer, value, topic, key, partition, timestamp);
    }
}

function sendFloatValues(Producer producer, float value, handle topic, anydata? key, int? partition, int? timestamp,
    string keySerializerType) returns ProducerError? {
    if (key is ()) {
        return producerSendFloat(producer, value, topic, partition, timestamp);
    }
    if (keySerializerType == SER_STRING) {
        if (key is string) {
            handle keyHandle = java:fromString(key);
            return producerSendFloatString(producer, value, topic, keyHandle, partition, timestamp);
        }
        panic getKeyTypeMismatchError(STRING);
    }
    if (keySerializerType == SER_INT) {
        if (key is int) {
            return producerSendFloatInt(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(INT);
    }
    if (keySerializerType == SER_FLOAT) {
        if (key is float) {
            return producerSendFloatFloat(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(FLOAT);
    }
    if (keySerializerType == SER_BYTE_ARRAY) {
        if (key is byte[]) {
            return producerSendFloatByteArray(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(BYTE_ARRAY);
    }
    if (keySerializerType == SER_CUSTOM) {
        return producerSendFloatAny(producer, value, topic, key, partition, timestamp);
    }
}

function sendByteArrayValues(Producer producer, byte[] value, handle topic, anydata? key, int? partition,
    int? timestamp, string keySerializerType) returns ProducerError? {
    if (key is ()) {
        return producerSendByteArray(producer, value, topic, partition, timestamp);
    }
    if (keySerializerType == SER_STRING) {
        if (key is string) {
            handle keyHandle = java:fromString(key);
            return producerSendByteArrayString(producer, value, topic, keyHandle, partition, timestamp);
        }
        panic getKeyTypeMismatchError(STRING);
    }
    if (keySerializerType == SER_INT) {
        if (key is int) {
            return producerSendByteArrayInt(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(INT);
    }
    if (keySerializerType == SER_FLOAT) {
        if (key is float) {
            return producerSendByteArrayFloat(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(FLOAT);
    }
    if (keySerializerType == SER_BYTE_ARRAY) {
        if (key is byte[]) {
            return producerSendByteArrayByteArray(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(BYTE_ARRAY);
    }
    if (keySerializerType == SER_CUSTOM) {
        return producerSendByteArrayAny(producer, value, topic, key, partition, timestamp);
    }
}

function sendAvroValues(Producer producer, anydata value, handle topic, anydata? key, int? partition, int? timestamp,
    string keySerializerType) returns ProducerError? {
    if (key is ()) {
        return producerSendAvro(producer, value, topic, partition, timestamp);
    }
    if (keySerializerType == SER_STRING) {
        if (key is string) {
            handle keyHandle = java:fromString(key);
            return producerSendAvroString(producer, value, topic, keyHandle, partition, timestamp);
        }
        panic getKeyTypeMismatchError(STRING);
    }
    if (keySerializerType == SER_INT) {
        if (key is int) {
            return producerSendAvroInt(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(INT);
    }
    if (keySerializerType == SER_FLOAT) {
        if (key is float) {
            return producerSendAvroFloat(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(FLOAT);
    }
    if (keySerializerType == SER_BYTE_ARRAY) {
        if (key is byte[]) {
            return producerSendAvroByteArray(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(BYTE_ARRAY);
    }
    if (keySerializerType == SER_CUSTOM) {
        return producerSendAvroAny(producer, value, topic, key, partition, timestamp);
    }
}

function sendCustomValues(Producer producer, anydata value, handle topic, anydata? key, int? partition, int? timestamp,
    string keySerializerType) returns ProducerError? {
    if (key is ()) {
        return producerSendAny(producer, value, topic, partition, timestamp);
    }
    if (keySerializerType == SER_STRING) {
        if (key is string) {
            handle keyHandle = java:fromString(key);
            return producerSendAnyString(producer, value, topic, keyHandle, partition, timestamp);
        }
        panic getKeyTypeMismatchError(STRING);
    }
    if (keySerializerType == SER_INT) {
        if (key is int) {
            return producerSendAnyInt(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(INT);
    }
    if (keySerializerType == SER_FLOAT) {
        if (key is float) {
            return producerSendAnyFloat(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(FLOAT);
    }
    if (keySerializerType == SER_BYTE_ARRAY) {
        if (key is byte[]) {
            return producerSendAnyByteArray(producer, value, topic, key, partition, timestamp);
        }
        panic getKeyTypeMismatchError(BYTE_ARRAY);
    }
    if (keySerializerType == SER_CUSTOM) {
        return producerSendAnyAny(producer, value, topic, key, partition, timestamp);
    }
}

function producerInit(Producer producer) returns error? =
@java:Method {
    name: "init",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.ProducerActions"
} external;

function producerClose(Producer producer) returns ProducerError? =
@java:Method {
    name: "close",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.ProducerActions"
} external;

function producerCommitConsumer(Producer producer, Consumer consumer) returns ProducerError? =
@java:Method {
    name: "commitConsumer",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.ProducerActions"
} external;

function producerCommitConsumerOffsets(Producer producer, PartitionOffset[] offsets, handle groupID)
returns ProducerError? =
@java:Method {
    name: "commitConsumerOffsets",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.ProducerActions"
} external;

function producerFlushRecords(Producer producer) returns ProducerError? =
@java:Method {
    name: "flushRecords",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.ProducerActions"
} external;

function producerGetTopicPartitions(Producer producer, handle topic) returns TopicPartition[]|ProducerError =
@java:Method {
    name: "getTopicPartitions",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.ProducerActions"
} external;

//////////////////////////////////////////////////////////////////////////////////////
//              Different send functions to send different types of data            //
//                  Naming convention: producerSend<ValueType><KeyType>             //
//   Reason for this naming convention is that the key can be nil but value cannot  //
//////////////////////////////////////////////////////////////////////////////////////

// Send string values with different types of keys
function producerSendString(Producer producer, handle value, handle topic, int? partition = (), int? timestamp = ())
returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.String", "java.lang.String", "java.lang.Object",
                "java.lang.Object"]
} external;

function producerSendStringString(Producer producer, handle value, handle topic, handle key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.String", "java.lang.String", "java.lang.String",
                "java.lang.Object", "java.lang.Object"]
} external;

function producerSendStringInt(Producer producer, handle value, handle topic, int key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.String", "java.lang.String", "long",
                "java.lang.Object", "java.lang.Object"]
} external;

function producerSendStringFloat(Producer producer, handle value, handle topic, float key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.String", "java.lang.String", "double",
                "java.lang.Object", "java.lang.Object"]
} external;

function producerSendStringByteArray(Producer producer, handle value, handle topic, byte[] key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.String", "java.lang.String",
                "org.ballerinalang.jvm.values.ArrayValue", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendStringAny(Producer producer, handle value, handle topic, anydata key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyKeys",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.String", "java.lang.String", "java.lang.Object",
                 "java.lang.Object", "java.lang.Object"]
} external;

// Send int values with different types of keys
function producerSendInt(Producer producer, int value, handle topic, int? partition = (), int? timestamp = ())
returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "long", "java.lang.String", "java.lang.Object",
                "java.lang.Object"]
} external;

function producerSendIntString(Producer producer, int value, handle topic, handle key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "long", "java.lang.String", "java.lang.String",
                "java.lang.Object", "java.lang.Object"]
} external;

function producerSendIntInt(Producer producer, int value, handle topic, int key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "long", "java.lang.String", "long",
                "java.lang.Object", "java.lang.Object"]
} external;

function producerSendIntFloat(Producer producer, int value, handle topic, float key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "long", "java.lang.String", "double",
                "java.lang.Object", "java.lang.Object"]
} external;

function producerSendIntByteArray(Producer producer, int value, handle topic, byte[] key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "long", "java.lang.String",
                "org.ballerinalang.jvm.values.ArrayValue", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendIntAny(Producer producer, int value, handle topic, anydata key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyKeys",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "long", "java.lang.String", "java.lang.Object",
                 "java.lang.Object", "java.lang.Object"]
} external;

// Send float values with different types of keys
function producerSendFloat(Producer producer, float value, handle topic, int? partition = (), int? timestamp = ())
returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "double", "java.lang.String", "java.lang.Object",
                "java.lang.Object"]
} external;

function producerSendFloatString(Producer producer, float value, handle topic, handle key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "double", "java.lang.String", "java.lang.String",
                "java.lang.Object", "java.lang.Object"]
} external;

function producerSendFloatInt(Producer producer, float value, handle topic, int key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "double", "java.lang.String", "long",
                "java.lang.Object", "java.lang.Object"]
} external;

function producerSendFloatFloat(Producer producer, float value, handle topic, float key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "double", "java.lang.String", "double",
                "java.lang.Object", "java.lang.Object"]
} external;

function producerSendFloatByteArray(Producer producer, float value, handle topic, byte[] key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "double", "java.lang.String",
                "org.ballerinalang.jvm.values.ArrayValue", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendFloatAny(Producer producer, float value, handle topic, anydata key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyKeys",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "double", "java.lang.String", "java.lang.Object",
                 "java.lang.Object", "java.lang.Object"]
} external;

// Send byte[] values with different types of keys
function producerSendByteArray(Producer producer, byte[] value, handle topic, int? partition = (), int? timestamp = ())
returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "org.ballerinalang.jvm.values.ArrayValue",
                "java.lang.String", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendByteArrayString(Producer producer, byte[] value, handle topic, handle key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "org.ballerinalang.jvm.values.ArrayValue",
                "java.lang.String", "java.lang.String", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendByteArrayInt(Producer producer, byte[] value, handle topic, int key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "org.ballerinalang.jvm.values.ArrayValue",
                "java.lang.String", "long", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendByteArrayFloat(Producer producer, byte[] value, handle topic, float key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "org.ballerinalang.jvm.values.ArrayValue",
                "java.lang.String", "double", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendByteArrayByteArray(Producer producer, byte[] value, handle topic, byte[] key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.Send",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "org.ballerinalang.jvm.values.ArrayValue",
                "java.lang.String", "org.ballerinalang.jvm.values.ArrayValue", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendByteArrayAny(Producer producer, byte[] value, handle topic, anydata key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyKeys",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "org.ballerinalang.jvm.values.ArrayValue",
                "java.lang.String", "java.lang.Object", "java.lang.Object", "java.lang.Object"]
} external;

// Send avro values with different types of keys
function producerSendAvro(Producer producer, anydata value, handle topic, int? partition = (), int? timestamp = ())
returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAvroValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "java.lang.Object",
                 "java.lang.Object"]
} external;

function producerSendAvroString(Producer producer, anydata value, handle topic, handle key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAvroValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "java.lang.String",
                 "java.lang.Object", "java.lang.Object"]
} external;

function producerSendAvroInt(Producer producer, anydata value, handle topic, int key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAvroValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "long",
                 "java.lang.Object", "java.lang.Object"]
} external;

function producerSendAvroFloat(Producer producer, anydata value, handle topic, float key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAvroValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "double",
                 "java.lang.Object", "java.lang.Object"]
} external;

function producerSendAvroByteArray(Producer producer, anydata value, handle topic, byte[] key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAvroValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String",
                 "org.ballerinalang.jvm.values.ArrayValue", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendAvroAny(Producer producer, anydata value, handle topic, any key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "sendAvroAny",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAvroValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "java.lang.Object",
                 "java.lang.Object", "java.lang.Object"]
} external;

// Send custom type values with different types of keys
function producerSendAny(Producer producer, anydata value, handle topic, int? partition = (), int? timestamp = ())
returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "java.lang.Object",
                 "java.lang.Object"]
} external;

function producerSendAnyString(Producer producer, anydata value, handle topic, handle key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "java.lang.String",
                 "java.lang.Object", "java.lang.Object"]
} external;

function producerSendAnyInt(Producer producer, anydata value, handle topic, int key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "long",
                 "java.lang.Object", "java.lang.Object"]
} external;

function producerSendAnyFloat(Producer producer, anydata value, handle topic, float key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "double",
                 "java.lang.Object", "java.lang.Object"]
} external;

function producerSendAnyByteArray(Producer producer, anydata value, handle topic, byte[] key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "send",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyValues",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String",
                 "org.ballerinalang.jvm.values.ArrayValue", "java.lang.Object", "java.lang.Object"]
} external;

function producerSendAnyAny(Producer producer, anydata value, handle topic, any key, int? partition = (),
    int? timestamp = ()) returns ProducerError? =
@java:Method {
    name: "sendAnyAny",
    class: "org.ballerinalang.messaging.kafka.nativeimpl.producer.SendAnyKeys",
    paramTypes: ["org.ballerinalang.jvm.values.ObjectValue", "java.lang.Object", "java.lang.String", "java.lang.Object",
                 "java.lang.Object", "java.lang.Object"]
} external;
