/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.compiler.api.symbols;

import java.util.List;

/**
 * Represents an absolute resource path attach point. An absolute resource path takes the following form:
 * '/path/segments'. If there are no path segments, then it will just be the root resource path ('/').
 *
 * @since 2.0.0
 */
public interface AbsResourcePathAttachPoint extends ServiceAttachPoint {

    /**
     * Gets the absolute resource path represented by this attach point. If the path is just the root resource path, an
     * empty list is returned.
     *
     * @return The resource path segments as a list
     */
    List<String> get();
}
