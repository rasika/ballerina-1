/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ballerina.plugins.idea.stubs.types;

import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import io.ballerina.plugins.idea.psi.BallerinaPackageReference;
import io.ballerina.plugins.idea.psi.impl.BallerinaPackageReferenceImpl;
import io.ballerina.plugins.idea.stubs.BallerinaPackageReferenceStub;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Represents package reference stub element.
 */
public class BallerinaPackageReferenceStubElementType extends
        BallerinaNamedStubElementType<BallerinaPackageReferenceStub, BallerinaPackageReference> {

    public BallerinaPackageReferenceStubElementType(@NotNull String debugName) {
        super(debugName);
    }

    @Override
    public BallerinaPackageReference createPsi(@NotNull BallerinaPackageReferenceStub stub) {
        return new BallerinaPackageReferenceImpl(stub, this);
    }

    @NotNull
    @Override
    public BallerinaPackageReferenceStub createStub(@NotNull BallerinaPackageReference psi, StubElement parentStub) {
        return new BallerinaPackageReferenceStub(parentStub, this, psi.getName(), psi.isPublic());
    }

    @Override
    public void serialize(@NotNull BallerinaPackageReferenceStub stub, @NotNull StubOutputStream dataStream)
            throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isPublic());
    }

    @NotNull
    @Override
    public BallerinaPackageReferenceStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub)
            throws IOException {
        return new BallerinaPackageReferenceStub(parentStub, this, dataStream.readName(), dataStream.readBoolean());
    }
}
