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
# + message - Specific error message of the error
# + cause - Any other error, which causes this error
public type Detail record {
    string message;
    error cause?;
};

# Represents the error code for invalid operations.
public const INVALID_OPERATION_ERROR = "{ballerina/system}InvalidOperationError";

# Represents an `InvalidOperationError` with a detailed message.
public type InvalidOperationError error<INVALID_OPERATION_ERROR, Detail>;

# Represents the error code for permission errors.
public const PERMISSION_ERROR = "{ballerina/system}PermissionError";

# Represents a `PermissionError` with a detailed message.
public type PermissionError error<PERMISSION_ERROR, Detail>;

# Represents the error code for file system errors.
public const FILE_SYSTEM_ERROR = "{ballerina/system}FileSystemError";

# Represents a `FileSystemError` with a detailed message.
public type FileSystemError error<FILE_SYSTEM_ERROR, Detail>;

# Represents the error code for file not found.
public const FILE_NOT_FOUND_ERROR = "{ballerina/system}FileNotFoundError";

# Represents a `FileNotFoundError` with a detailed message.
public type FileNotFoundError error<FILE_NOT_FOUND_ERROR, Detail>;

# Represents the error code for process execute error.
public const PROCESS_EXEC_ERROR = "{ballerina/system}ProcessExecError";

# Represents an `ProcessExecError` with a detailed message.
public type ProcessExecError error<PROCESS_EXEC_ERROR, Detail>;

# Represents System related errors.
public type Error InvalidOperationError|PermissionError|FileSystemError|FileNotFoundError|ProcessExecError;
