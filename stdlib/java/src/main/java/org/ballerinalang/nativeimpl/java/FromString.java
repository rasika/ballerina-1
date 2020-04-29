/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.nativeimpl.java;

import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.values.HandleValue;
import org.ballerinalang.jvm.values.api.BString;
import org.ballerinalang.natives.annotations.BallerinaFunction;

/**
 * This class contains the implementation of the "fromString" ballerina function in ballerina/java module.
 *
 * @since 1.0.0
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "java",
        functionName = "fromString"
)
public class FromString {

    public static HandleValue fromString(Strand strand, String strValue) {
        return new HandleValue(strValue);
    }
    public static HandleValue fromString_bstring(Strand strand, BString strValue) {
        return new HandleValue(strValue.getValue());
    }
}
