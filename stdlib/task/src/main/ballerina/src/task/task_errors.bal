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

# Record type to hold the details of an error.
#
# + message - Error message describing the error occurred
# + cause - Any other error, which causes this error
public type Detail record {
    string message;
    error cause?;
};

# Used as the error reason for the `task:SchedulerError` type.
public const SCHEDULER_ERROR_REASON = "{ballerina/task}SchedulerError";

# Error type specific to the `task:Scheduler` object functions.
public type SchedulerError error<SCHEDULER_ERROR_REASON, Detail>;

# Used as the error reason for the `task:ListenerError` type.
public const LISTENER_ERROR_REASON = "{ballerina/task}ListenerError";

# Error type specific to the `task:Listener` object functions.
public type ListenerError error<LISTENER_ERROR_REASON, Detail>;

# Represents the Union error type of the ballerina/task module. This error type represents any error that can occur during the
# execution of task APIs.
public type Error SchedulerError|ListenerError;
