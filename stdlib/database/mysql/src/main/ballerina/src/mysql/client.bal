// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/crypto;
import ballerina/sql;
import ballerina/java;

# Represents a MySQL database client.
public type Client client object {
    *sql:Client;
    private boolean clientActive = true;

    # Initialize MySQL Client.
    #
    # + host - Hostname of the mysql server to be connected
    # + user - If the mysql server is secured, the username to be used to connect to the mysql server
    # + password - The password of provided username of the database
    # + database - The name fo the database to be connected
    # + port - Port number of the mysql server to be connected
    # + options - The Database specific JDBC client properties
    # + connectionPool - The `sql:ConnectionPool` object to be used within the jdbc client.
    #                   If there is no connectionPool is provided, the global connection pool will be used and it will
    #                   be shared by other clients which has same properties
    public function __init(public string host = "localhost",
        public string? user = (), public string? password = (), public string? database = (),
        public int port = 3306, public Options? options = (),
        public sql:ConnectionPool? connectionPool = ()) returns sql:Error? {
        ClientConfiguration clientConfig = {
            host: host,
            port: port,
            user: user,
            password: password,
            database: database,
            options: options,
            connectionPool: connectionPool
        };
        return createClient(self, clientConfig, sql:getGlobalConnectionPool());
    }

    # Queries the database with the query provided by the user, and returns the result as stream.
    #
    # + sqlQuery - The query which needs to be executed as `string` or `ParameterizedString` when the SQL query has
    #              params to be passed in
    # + rowType - The `typedesc` of the record that should be returned as a result. If this is not provided the default
    #             column names of the query result set be used for the record attributes
    # + return - Stream of records in the type of `rowType`
    public remote function query(@untainted string|sql:ParameterizedString sqlQuery, typedesc<record {}>? rowType = ())
    returns @tainted stream<record{}, sql:Error> {
        if (self.clientActive) {
            sql:ParameterizedString sqlParamString;
            if (sqlQuery is string) {
                sqlParamString = {
                    parts : [sqlQuery],
                    insertions: []
                };
            } else {
                sqlParamString = sqlQuery;
            }
            return nativeQuery(self, sqlParamString, rowType);
        } else {
            return sql:generateApplicationErrorStream("MySQL Client is already closed,"
                + "hence further operations are not allowed");
        }
    }

    # Executes the DDL or DML sql queries provided by the user, and returns summary of the execution.
    #
    # + sqlQuery - The DDL or DML query such as INSERT, DELETE, UPDATE, etc
    # + return - Summary of the sql update query as `sql:ExecuteResult` or returns `sql:Error`
    #           if any error occured when executing the query
    public remote function execute(@untainted string sqlQuery) returns sql:ExecuteResult|sql:Error? {
        if (self.clientActive) {
            return nativeExecute(self, java:fromString(sqlQuery));
        } else {
            return sql:ApplicationError(message = "JDBC Client is already closed,"
                + " hence further operations are not allowed");
        }
    }

    # Close the SQL client.
    #
    # + return - Possible error during closing the client
    public function close() returns sql:Error? {
        self.clientActive = false;
        return close(self);
    }
};

# Provides a set of configurations for the mysql client to be passed internally within the module.
#
# + host - URL of the database to connect
# + port - Port of the database to connect
# + user - Username for the database connection
# + password - Password for the database connection
# + database - Name of the database
# + options - Mysql datasource `Options` to be configured
# + connectionPool - Properties for the connection pool configuration. Refer `sql:ConnectionPool` for more details
type ClientConfiguration record {|
    string host;
    int port;
    string? user;
    string? password;
    string? database;
    Options? options;
    sql:ConnectionPool? connectionPool;
|};

# MySQL database options.
#
# + ssl - SSL Configuration to be used
# + useXADatasource - Boolean value to enable XADatasource
# + connectTimeoutInSeconds - Timeout to be used when connecting to the mysql server
# + socketTimeoutInSeconds - Socket timeout during the read/write operations with mysql server,
#                            0 means no socket timeout
public type Options record {|
    SSLConfig? ssl = {};
    boolean useXADatasource = false;
    decimal connectTimeoutInSeconds = 30;
    decimal socketTimeoutInSeconds = 0;
|};

# Possible options for SSL Mode.
public const SSL_PREFERRED = "PREFERRED";
public const SSL_REQUIRED = "REQUIRED";
public const SSL_VERIFY_CERT = "VERIFY_CERT";
public const SSL_VERIFY_IDENTITY = "VERIFY_IDENTITY";

# SSLMode as a union of available ssl modes.
public type SSLMode SSL_PREFERRED|SSL_REQUIRED|SSL_VERIFY_CERT|SSL_VERIFY_IDENTITY;

# SSL Configuration to be used when connecting to mysql server.
#
# + mode - `SSLMode` to be usedduring the connection
# + clientCertKeystore - Keystore configuration of the client certificates
# + trustCertKeystore - Keystore configurtion of the trust certificates
#
public type SSLConfig record {|
    SSLMode mode = SSL_PREFERRED;
    crypto:KeyStore clientCertKeystore?;
    crypto:KeyStore trustCertKeystore?;
|};

function createClient(Client mysqlClient, ClientConfiguration clientConf,
    sql:ConnectionPool globalConnPool) returns sql:Error? = @java:Method {
    class: "org.ballerinalang.mysql.NativeImpl"
} external;

function nativeQuery(Client sqlClient, sql:ParameterizedString sqlQuery, typedesc<record {}>? rowtype)
returns stream<record{}, sql:Error> = @java:Method {
    class: "org.ballerinalang.sql.utils.QueryUtils"
} external;

function nativeExecute(Client sqlClient,@untainted handle sqlQuery)
returns sql:ExecuteResult|sql:Error? = @java:Method {
    class: "org.ballerinalang.sql.utils.ExecuteUtils"
} external;

function close(Client mysqlClient) returns sql:Error? = @java:Method {
    class: "org.ballerinalang.mysql.NativeImpl"
} external;
