/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.jsonutils;

import org.ballerinalang.jvm.BallerinaErrors;
import org.ballerinalang.jvm.XMLFactory;
import org.ballerinalang.jvm.util.exceptions.BLangExceptionHelper;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.XMLValue;

/**
 * Converts a XML to the corresponding JSON representation.
 *
 * @since 1.0
 */
public class FromXML {

    private static final String OPTIONS_ATTRIBUTE_PREFIX = "attributePrefix";
    private static final String OPTIONS_PRESERVE_NS = "preserveNamespaces";

    public static Object fromXML(XMLValue xml, MapValue<?, ?> options) {
        try {
            String attributePrefix = (String) options.get(OPTIONS_ATTRIBUTE_PREFIX);
            boolean preserveNamespaces = ((Boolean) options.get(OPTIONS_PRESERVE_NS));
            return XMLFactory.convertToJSON(xml, attributePrefix, preserveNamespaces);
        } catch (Exception e) {
            try {
                BLangExceptionHelper.handleXMLException("{ballerina/jsonutils}Error", e);
            } catch (Exception ex) {
                return BallerinaErrors.createError("{ballerina/jsonutils}Error", ex.getMessage());
            }
        }
        return null;
    }

    private FromXML() {
    }
}
