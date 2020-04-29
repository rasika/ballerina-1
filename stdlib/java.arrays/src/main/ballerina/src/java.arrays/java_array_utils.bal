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

# Returns a new Java array instance with the specified element type and dimensions. This function completes abruptly
# with a `panic` if the specified handle refers to a Java null or if zero dimensions have been provided.
# ```ballerina
# handle stringClass = check java:getClass("java.lang.String");
# handle StrArray = java.arrays:newInstance(stringClass, 4);
# ```
#
# + class - The element type of the array
# + dimensions - The dimensions of the array
# + return - The new Java array instance
public function newInstance(public handle class, int ...dimensions) returns handle = @java:Method {
    class: "java.lang.reflect.Array",
    paramTypes: ["java.lang.Class", {class: "int", dimensions:1}]
} external;

# Returns a `handle`, which refers to the element at the specified index in the given Java array. This function
# completes abruptly with a `panic` if the specified handle refers to a Java null or if the handle does not refer
# to a Java array.
# ```ballerina
# handle words = getSortedArray();
# handle firstWord = java.arrays:get(words, 0);
# ```
#
# + array - The `handle`, which refers to the Java array
# + index - The index of the element to be returned
# + return - The `handle`, which refers to the element at the specified position in the Java array
public function get(public handle array, public int index) returns handle = @java:Method {
    class: "java.lang.reflect.Array"
} external;


# Replaces the indexed element at the specified index in the given Java array with the specified element. This
# function completes abruptly with a `panic` if the specified handle refers to a Java null or if the handle does
# not refer to a Java array.
# ```ballerina
# handle strArray = getStringArray();
# java.arrays:set(strArray, 0, java:fromString("Ballerina"));
# ```
#
# + array - The `handle`, which refers to the Java array
# + index - The index of the element to be replaced
# + element - The element to be stored at the specified index
public function set(public handle array, public int index, public handle element) = @java:Method {
    class: "java.lang.reflect.Array"
} external;

# Returns the length of the given Java array.
# ```ballerina
# handle array = getArray();
# int length = java.arrays:getLength(array);
# ```
#
# + array - The `handle`, which refers to the Java array
# + return - The length of the given Java array
public function getLength(public handle array) returns int = @java:Method {
    class: "java.lang.reflect.Array"
} external;
