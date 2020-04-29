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

# Details of an error.
#
# + message - Specific error message of the error
# + cause - Any other error, which causes this error
public type Detail record {
    string message;
    error cause?;
};

# Specifies the time error, which occurs in the module.
public const TIME_ERROR_REASON = "{ballerina/time}TimeError";
# Represents the Time module related error.
public type Error error<TIME_ERROR_REASON, Detail>;

function getInvalidStringError() returns Error {
    error e = error(TIME_ERROR_REASON, message = "Invalid string returned from the format function");
    return <Error>e;
}
