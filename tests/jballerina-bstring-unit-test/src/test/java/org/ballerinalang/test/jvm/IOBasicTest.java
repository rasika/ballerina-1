/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.ballerinalang.test.jvm;

import org.ballerinalang.model.values.BValueType;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Test cases to cover io println related tests on jBallerina.
 *
 * @since 0.995.0
 */
public class IOBasicTest {

    private CompileResult compileResult;
    private PrintStream original;
    private static final String EXPECTED_OUTPUT = "Hello\nBallerina\n";

    @BeforeClass(alwaysRun = true)
    public void setup() {
        original = System.out;
        compileResult = BCompileUtil.compile("test-src/jvm/io-basics.bal");
    }

    @AfterClass(alwaysRun = true)
    public void cleanup() {
        System.setOut(original);
    }

    @Test
    public void testPrint1() throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(outputStream));
            BRunUtil.invoke(compileResult, "testPrint1", new BValueType[0]);
            Assert.assertEquals(outputStream.toString().replace("\r", ""), EXPECTED_OUTPUT);
        } finally {
            System.setOut(original);
        }
    }
}
