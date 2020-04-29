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

import ballerina/log;

# Record type to hold the details of an error.
#
# + message - Specific error message of the error
# + cause - Any other error, which causes this error
public type Detail record {
    string message;
    error cause?;
};

# Represents the Auth error reason.
public const AUTH_ERROR = "{ballerina/auth}Error";

# Represents the Auth error type with details. This will be returned if an error occurred while inbound auth providers
# try to authenticate the received credentials and outbound auth providers try to generate the token.
public type Error error<AUTH_ERROR, Detail>;

# Log and prepare `error` as a `Error`.
#
# + message - Error message
# + err - `error` instance
# + return - Prepared `Error` instance
function prepareError(string message, error? err = ()) returns Error {
    log:printError(message, err);
    Error authError;
    if (err is error) {
        authError = error(AUTH_ERROR, message = message, cause = err);
    } else {
        authError = error(AUTH_ERROR, message = message);
    }
    return authError;
}