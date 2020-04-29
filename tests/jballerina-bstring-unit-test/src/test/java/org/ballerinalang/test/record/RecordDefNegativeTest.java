/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.test.record;

import org.ballerinalang.test.util.BAssertUtil;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test cases for user record definition negative test.
 */
public class RecordDefNegativeTest {

    @Test(description = "Test usage of duplicate keys in record definition")
    public void duplicateKeyTest() {
        CompileResult compileResult = BCompileUtil.compile("test-src/record/negative/duplicate-key-negative.bal");
        int errorIndex = 0;
        BAssertUtil.validateError(compileResult, errorIndex++, "invalid usage of record literal: duplicate key 'name'",
                26, 10);
        BAssertUtil.validateError(compileResult, errorIndex++, "invalid usage of record literal: duplicate key 'name'",
                35, 9);
        BAssertUtil.validateError(compileResult, errorIndex, "invalid usage of record literal: duplicate key 'name'",
                43, 10);
    }

    @Test
    public void testLocalVarRefInLocalRecordDef() {
        CompileResult compileResult = BCompileUtil.compile("test-src/record/negative/local_var_ref_in_record.bal");
        BAssertUtil.validateError(compileResult, 0, "undefined symbol 'x'", 22, 19);
        BAssertUtil.validateError(compileResult, 1, "undefined symbol 'x'", 27, 19);
        BAssertUtil.validateError(compileResult, 2, "undefined symbol 'x'", 31, 57);
    }

    @Test
    public void testFieldRefFromWithinARecordDef() {
        CompileResult compileResult = BCompileUtil.compile("test-src/record/negative/field_ref_in_own_record.bal");
        int indx = 0;
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'a'", 19, 13);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'a'", 21, 17);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'a'", 25, 17);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'fname'", 33, 27);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'lname'", 33, 41);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'a'", 38, 17);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'a'", 41, 21);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'a'", 45, 21);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'x'", 52, 57);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'p'", 53, 17);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'a'", 59, 21);
        BAssertUtil.validateError(compileResult, indx++, "undefined symbol 'a'", 61, 25);
        BAssertUtil.validateError(compileResult, indx++, "incompatible types: expected 'string', found 'int'", 71, 16);
        assertEquals(compileResult.getErrorCount(), indx);
    }
}
