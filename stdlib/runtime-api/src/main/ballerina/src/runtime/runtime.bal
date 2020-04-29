// Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Halts the current worker for a predefined amount of time.
# ```ballerina
# runtime:sleep(1000);
# ```
#
# + millis - Amount of time to sleep in milliseconds
public function sleep(int millis) = @java:Method {
    class: "org.ballerinalang.stdlib.runtime.nativeimpl.Sleep"
} external;

# Returns the system property value associated with the specified property name.
# ```ballerina
# string userHome = runtime:getProperty("user.home");
# ```
#
# + name - Name of the property
# + return - Value of the property if the property exists or else an empty string otherwise
public function getProperty(@untainted string name) returns string {
    return externGetProperty(java:fromString(name)).toString();
}

function externGetProperty(@untainted handle name) returns handle = @java:Method {
    name: "getProperty",
    class: "org.ballerinalang.stdlib.runtime.nativeimpl.GetProperty"
} external;

# Gives a timeout to the current worker for a predefined amount of time.
# ```ballerina
# future<()> f1 = runtime:timeout(2000);
# ```
#
# + millis - Amount of time needed for the timeout in milliseconds
# + return - Future to be invoked after the timeout
public function timeout(int millis) returns future<()> {
    return start sleep(millis);
}
