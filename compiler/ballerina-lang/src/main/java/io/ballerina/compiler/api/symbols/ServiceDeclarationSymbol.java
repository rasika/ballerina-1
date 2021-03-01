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

import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;

import java.util.Optional;

/**
 * Represents a service declaration. Unlike other module level symbols, service declaration symbols aren't associated
 * with a name. Hence, they cannot be looked up using {@link io.ballerina.compiler.api.SemanticModel#symbol(Document,
 * LinePosition)} nor are they considered as visible symbols in
 * {@link io.ballerina.compiler.api.SemanticModel#visibleSymbols(Document,
 * LinePosition)}.
 *
 * @since 2.0.0
 */
public interface ServiceDeclarationSymbol extends Symbol, Annotatable, Documentable, Qualifiable {

    /**
     * Returns the type of the service. The type can be either user specified or inferred by the compiler.
     *
     * @return The type of the service
     */
    TypeSymbol typeDescriptor();

    /**
     * Returns the specified attach point if there is one. If an attach point is returned, it can be one of the
     * following types: {@link LiteralAttachPoint} or {@link AbsResourcePathAttachPoint}.
     *
     * @return The attach point if it is specified, returns empty otherwise
     */
    Optional<ServiceAttachPoint> attachPoint();
}
