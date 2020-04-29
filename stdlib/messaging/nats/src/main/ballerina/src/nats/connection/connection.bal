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

import ballerina/java;

# Represents a single network connection to the NATS server.
public type Connection object {

    private ConnectionConfig config = {};
    private string[] url;

    # Initializes a connection with the NATS server.
    #
    # + url - The NATS Broker URL. For a clustered use case, pass the URLs as a string array
    # + config - Configurations associated with the NATS client to establish a connection with the server
    public function __init(public string[] url = [DEFAULT_URL], public ConnectionConfig? config = ()) {
        self.config = config ?: {};
        self.url = url;
        externInit(self, self.url, self.config);
    }

    # Closes a given connection.
    #
    # + forceful - The graceful shutdown flag. If `true`, the connection closes immediately.
    #              The default value is `false`.
    # + return - `()` or else a `nats:Error` if unable to complete the close operation.
    public function close(boolean forceful = false) returns Error? {
        return externClose(self, forceful);
    }
};

function externInit(Connection connection, string[] url, ConnectionConfig config) =
@java:Method {
    class: "org.ballerinalang.nats.connection.Init"
} external;

function externClose(Connection connection, boolean forceful = false) returns Error? =
@java:Method {
    class: "org.ballerinalang.nats.connection.Close"
} external;
