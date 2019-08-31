/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.test.error;

import org.ballerinalang.model.types.TypeTags;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BError;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
import org.ballerinalang.test.util.BAssertUtil;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.ballerinalang.util.exceptions.BLangRuntimeException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test cases for Error.
 *
 * @since 0.985.0
 */
public class ErrorTest {

    private CompileResult errorTestResult;
    private CompileResult negativeCompileResult;

    private static final String ERROR1 = "error1";
    private static final String ERROR2 = "error2";
    private static final String ERROR3 = "error3";
    private static final String EMPTY_CURLY_BRACE = "{}";
    private static final String CONST_ERROR_REASON = "reason one";

    @BeforeClass
    public void setup() {
        errorTestResult = BCompileUtil.compile("test-src/error/error_test.bal");
        negativeCompileResult = BCompileUtil.compile("test-src/error/error_test_negative.bal");
    }

    @Test
    public void testIndirectErrorCtor() {
        BValue[] errors = BRunUtil.invoke(errorTestResult, "testIndirectErrorConstructor");
        Assert.assertEquals(errors.length, 4);
        Assert.assertEquals(errors[0].stringValue(), "ErrNo-1 {message:\"arg\", data:{}}");
        Assert.assertEquals(errors[1].stringValue(), "ErrNo-1 {message:\"arg\", data:{}}");
        Assert.assertEquals(errors[2], errors[0]);
        Assert.assertEquals(errors[3], errors[1]);
    }

    @Test
    public void errorConstructReasonTest() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "errorConstructReasonTest");

        Assert.assertTrue(returns[0] instanceof BError);
        Assert.assertEquals(((BError) returns[0]).getReason(), ERROR1);
        Assert.assertEquals(((BError) returns[0]).getDetails().stringValue(), EMPTY_CURLY_BRACE);
        Assert.assertTrue(returns[1] instanceof BError);
        Assert.assertEquals(((BError) returns[1]).getReason(), ERROR2);
        Assert.assertEquals(((BError) returns[1]).getDetails().stringValue(), EMPTY_CURLY_BRACE);
        Assert.assertTrue(returns[2] instanceof BError);
        Assert.assertEquals(((BError) returns[2]).getReason(), ERROR3);
        Assert.assertEquals(((BError) returns[2]).getDetails().stringValue(), EMPTY_CURLY_BRACE);
        Assert.assertEquals(returns[3].stringValue(), ERROR1);
        Assert.assertEquals(returns[4].stringValue(), ERROR2);
        Assert.assertEquals(returns[5].stringValue(), ERROR3);
    }

    @Test
    public void errorConstructDetailTest() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "errorConstructDetailTest");
        String detail1 = "{message:\"msg1\"}";
        String detail2 = "{message:\"msg2\"}";
        String detail3 = "{message:\"msg3\"}";
        Assert.assertTrue(returns[0] instanceof BError);
        Assert.assertEquals(((BError) returns[0]).getReason(), ERROR1);
        Assert.assertEquals(((BError) returns[0]).getDetails().stringValue().trim(), detail1);
        Assert.assertTrue(returns[1] instanceof BError);
        Assert.assertEquals(((BError) returns[1]).getReason(), ERROR2);
        Assert.assertEquals(((BError) returns[1]).getDetails().stringValue().trim(), detail2);
        Assert.assertTrue(returns[2] instanceof BError);
        Assert.assertEquals(((BError) returns[2]).getReason(), ERROR3);
        Assert.assertEquals(((BError) returns[2]).getDetails().stringValue().trim(), detail3);
        Assert.assertEquals(returns[3].stringValue().trim(), detail1);
        Assert.assertEquals(returns[4].stringValue().trim(), detail2);
        Assert.assertEquals(returns[5].stringValue().trim(), detail3);
    }

    @Test
    public void errorPanicTest() {
        // Case without panic
        BValue[] args = new BValue[] { new BInteger(10) };
        BValue[] returns = BRunUtil.invoke(errorTestResult, "errorPanicTest", args);
        Assert.assertEquals(returns[0].stringValue(), "done");

        // Now panic
        args = new BValue[] { new BInteger(15) };
        Exception expectedException = null;
        try {
            BRunUtil.invoke(errorTestResult, "errorPanicTest", args);
        } catch (Exception e) {
            expectedException = e;
        }
        Assert.assertNotNull(expectedException);
        String message = ((BLangRuntimeException) expectedException).getMessage();

        Assert.assertEquals(message,
                "error: largeNumber message=large number\n\t" +
                        "at error_test:errorPanicCallee(error_test.bal:37)\n\t" +
                        "   error_test:errorPanicTest(error_test.bal:31)");
    }

    @Test
    public void errorTrapTest() {
        // Case without panic
        BValue[] args = new BValue[] { new BInteger(10) };
        BValue[] returns = BRunUtil.invoke(errorTestResult, "errorTrapTest", args);
        Assert.assertEquals(returns[0].stringValue(), "done");

        // Now panic
        args = new BValue[] { new BInteger(15) };
        returns = BRunUtil.invoke(errorTestResult, "errorTrapTest", args);
        String result = "largeNumber {message:\"large number\"}";
        Assert.assertEquals(returns[0].stringValue(), result.trim());
    }

    @Test
    public void customErrorDetailsTest() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testCustomErrorDetails");
        Assert.assertEquals(returns[0].stringValue(), "trxErr {message:\"\", data:\"test\"}");
        Assert.assertEquals(((BError) returns[0]).getDetails().getType().getTag(), TypeTags.RECORD_TYPE_TAG);
        Assert.assertEquals(((BError) returns[0]).getDetails().getType().getName(), "TrxErrorData");
    }

    @Test
    public void testCustomErrorDetails2() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testCustomErrorDetails2");
        Assert.assertEquals(returns[0].stringValue(), "test");
    }

    @Test
    public void testErrorWithErrorConstructor() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testErrorWithErrorConstructor");
        Assert.assertEquals(returns[0].stringValue(), "test");
    }

    @Test
    public void testGetCallStack() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "getCallStackTest");
        Assert.assertEquals(returns[0].stringValue(), "{callableName:\"getCallStack\", " +
                                                      "moduleName:\"ballerina.runtime.errors\"," +
                                                      " fileName:\"errors.bal\", lineNumber:33}");
    }

    @Test
    public void testConsecutiveTraps() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testConsecutiveTraps");
        Assert.assertEquals(returns[0].stringValue(), "Error");
        Assert.assertEquals(returns[1].stringValue(), "Error");
    }

    @Test
    public void testOneLinePanic() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testOneLinePanic");
        Assert.assertTrue(returns[0] instanceof BValueArray);
        BValueArray array = (BValueArray) returns[0];
        Assert.assertEquals(array.getString(0), "Error1");
        Assert.assertEquals(array.getString(1), "Error2");
        Assert.assertEquals(array.getString(2), "Something Went Wrong");
        Assert.assertEquals(array.getString(3), "Error3");
        Assert.assertEquals(array.getString(4), "Something Went Wrong");
        Assert.assertEquals(array.getString(5), "1");
    }

    @Test
    public void testGenericErrorWithDetailRecord() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testGenericErrorWithDetailRecord");
        Assert.assertTrue(returns[0] instanceof BBoolean);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testTrapSuccessScenario() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testTrapWithSuccessScenario");
        Assert.assertTrue(returns[0] instanceof BInteger);
        Assert.assertEquals(((BInteger) returns[0]).intValue(), 1);
    }

    @Test(dataProvider = "userDefTypeAsReasonTests")
    public void testErrorWithUserDefinedReasonType(String testFunction) {
        BValue[] returns = BRunUtil.invoke(errorTestResult, testFunction);
        Assert.assertTrue(returns[0] instanceof BError);
        Assert.assertEquals(((BError) returns[0]).getReason(), CONST_ERROR_REASON);
    }

    @Test(dataProvider = "constAsReasonTests")
    public void testErrorWithConstantAsReason(String testFunction) {
        BValue[] returns = BRunUtil.invoke(errorTestResult, testFunction);
        Assert.assertTrue(returns[0] instanceof BError);
        Assert.assertEquals(((BError) returns[0]).getReason(), CONST_ERROR_REASON);
        Assert.assertEquals(((BMap) ((BError) returns[0]).getDetails()).get("message").stringValue(),
                            "error detail message");
    }

//    @Test
//    public void testCustomErrorWithMappingOfSelf() {
//        BValue[] returns = BRunUtil.invoke(errorTestResult, "testCustomErrorWithMappingOfSelf");
//        Assert.assertTrue(returns[0] instanceof BBoolean);
//        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
//    }

    @Test
    public void testUnspecifiedErrorDetailFrozenness() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testUnspecifiedErrorDetailFrozenness");
        Assert.assertTrue(returns[0] instanceof BBoolean);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testErrorNegative() {
        Assert.assertEquals(negativeCompileResult.getErrorCount(), 17);
        int i = 0;
        BAssertUtil.validateError(negativeCompileResult, i++,
                                  "incompatible types: expected 'reason one|reason two', found 'string'", 26, 31);
        BAssertUtil.validateError(negativeCompileResult, i++,
                                  "incompatible types: expected 'reason one', found 'reason two'", 31, 31);
        BAssertUtil.validateError(negativeCompileResult, i++,
                                  "invalid error reason type 'int', expected a subtype of 'string'", 41, 28);
        BAssertUtil.validateError(negativeCompileResult, i++, "invalid error detail type 'map', expected a subtype of "
                + "'record {| string message?; $error0 cause?; (anydata|error)...; |}'", 41, 33);
        BAssertUtil.validateError(negativeCompileResult, i++, "invalid error detail type 'boolean', expected a subtype "
                + "of 'record {| string message?; $error0 cause?; (anydata|error)...; |}'", 42, 36);
        BAssertUtil.validateError(negativeCompileResult, i++,
                                  "invalid error reason type '1.0f', expected a subtype of 'string'", 45, 7);
        BAssertUtil.validateError(negativeCompileResult, i++,
                                  "invalid error reason type 'boolean', expected a subtype of 'string'", 48, 11);
        BAssertUtil.validateError(negativeCompileResult, i++, "self referenced variable 'e3'", 54, 22);
        BAssertUtil.validateError(negativeCompileResult, i++, "self referenced variable 'e3'", 54, 43);
        BAssertUtil.validateError(negativeCompileResult, i++, "self referenced variable 'e4'", 55, 42);
        BAssertUtil.validateError(negativeCompileResult, i++,
                "cannot infer reason from error constructor: 'UserDefErrorOne'", 56, 27);
        BAssertUtil.validateError(negativeCompileResult, i++,
                "cannot infer reason from error constructor: 'MyError'", 57, 19);
        BAssertUtil.validateError(negativeCompileResult, i++,
                "cannot infer type of the error from '(UserDefErrorOne|UserDefErrorTwo)'", 75, 12);
        BAssertUtil.validateError(negativeCompileResult, i++,
                "cannot infer reason from error constructor: 'RNError'", 96, 18);
        BAssertUtil.validateError(negativeCompileResult, i++,
                "cannot infer reason from error constructor: 'RNStrError'", 97, 21);
        BAssertUtil.validateError(negativeCompileResult, i++,
                "error reason is mandatory for direct error constructor", 112, 28);
        BAssertUtil.validateError(negativeCompileResult, i++,
                "incompatible types: expected 'error', found '(error|int)'", 118, 11);
    }
    @DataProvider(name = "userDefTypeAsReasonTests")
    public Object[][] userDefTypeAsReasonTests() {
        return new Object[][] {
                { "testErrorConstrWithConstForUserDefinedReasonType" },
                { "testErrorConstrWithLiteralForUserDefinedReasonType" }
        };
    }

    @DataProvider(name = "constAsReasonTests")
    public Object[][] constAsReasonTests() {
        return new Object[][] {
                { "testErrorConstrWithConstForConstReason" },
                { "testErrorConstrWithConstLiteralForConstReason" }
        };
    }

    @Test()
    public void errorReasonSubtypeTest() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "errorReasonSubType");
        Assert.assertEquals(((BError) returns[0]).getReason(), "ErrNo-1");
        Assert.assertEquals(((BError) returns[1]).getReason(), "ErrorNo-2");
        Assert.assertEquals(((BError) returns[2]).getReason(), "ErrNo-1");
        Assert.assertEquals(((BError) returns[3]).getReason(), "ErrorNo-2");
    }

    @Test()
    public void indirectErrorCtorTest() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "indirectErrorCtor");
        Assert.assertEquals(((BString) returns[0]).stringValue(), "foo");
        Assert.assertEquals(((BBoolean) returns[1]).booleanValue(), true);
        Assert.assertEquals(((BError) returns[2]).stringValue(), "foo {code:3456}");
    }

    @Test()
    public void testUnionLhsWithIndirectErrorRhs() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testUnionLhsWithIndirectErrorRhs");
        Assert.assertEquals(((BError) returns[0]).getReason(), "Foo");
    }

    @Test()
    public void testOptionalErrorReturn() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testOptionalErrorReturn");
        Assert.assertEquals(((BError) returns[0]).stringValue(), "this is broken {message:\"too bad\"}");
    }

    @Test()
    public void testIndirectErrorReturn() {
        BValue[] returns = BRunUtil.invoke(errorTestResult, "testIndirectErrorReturn");
        Assert.assertEquals(((BError) returns[0]).stringValue(), "Foo {message:\"error msg\"}");
    }

    @Test
    public void testStackTraceInNative() {
        Exception expectedException = null;
        try {
            BRunUtil.invoke(errorTestResult, "testStackTraceInNative");
        } catch (Exception e) {
            expectedException = e;
        }

        Assert.assertNotNull(expectedException);
        String message = ((BLangRuntimeException) expectedException).getMessage();
        Assert.assertEquals(message,
                "error: array index out of range: index: 4, size: 2 \n\t" +
                        "at ballerina.lang_array:slice(array.bal:105)\n\t" +
                        "   error_test:testStackTraceInNative(error_test.bal:279)");
    }

    @Test
    public void testPanicOnErrorUnion() {
        BValue[] args = new BValue[] { new BInteger(0) };
        BValue[] result = BRunUtil.invoke(errorTestResult, "testPanicOnErrorUnion", args);
        Assert.assertEquals(result[0].stringValue(), "str");
    }

    @Test(expectedExceptions = BLangRuntimeException.class, expectedExceptionsMessageRegExp = "error: x.*")
    public void testPanicOnErrorUnionCustomError() {
        BValue[] args = new BValue[] { new BInteger(1) };
        BRunUtil.invoke(errorTestResult, "testPanicOnErrorUnion", args);
    }

    @Test(expectedExceptions = BLangRuntimeException.class, expectedExceptionsMessageRegExp = "error: y code=4.*")
    public void testPanicOnErrorUnionCustomError2() {
        BValue[] args = new BValue[] { new BInteger(2) };
        BRunUtil.invoke(errorTestResult, "testPanicOnErrorUnion", args);
    }

    @Test
    public void testErrorUnionPassedToErrorParam() {
        BValue[] result = BRunUtil.invoke(errorTestResult, "testErrorUnionPassedToErrorParam");
        Assert.assertEquals(result[0].stringValue(), "a1");
    }

    @Test
    public void testNonModuleQualifiedReasons() {
        CompileResult compileResult = BCompileUtil.compile(
                "test-src/error/non_module_qualified_error_reasons_negative.bal");
        Assert.assertEquals(compileResult.getWarnCount(), 3);

        int index = 0;
        BAssertUtil.validateWarning(compileResult, index++, "error reason '{test string 1' is not module qualified",
                                    22, 21);
        BAssertUtil.validateWarning(compileResult, index++, "error reason '{test string 1' is not module qualified",
                                    23, 21);
        BAssertUtil.validateWarning(compileResult, index, "error reason '{test/string}identifier' is not module " +
                "qualified", 23, 21);
    }

    @Test
    public void testNonModuleQualifiedReasonsInProject() {
        CompileResult compileResult = BCompileUtil.compile("test-src/error/error_project", "err_module");
        Assert.assertEquals(compileResult.getWarnCount(), 3);

        int index = 0;
        BAssertUtil.validateWarning(compileResult, index++, "error reason '{test string 1' is not module qualified",
                                    22, 21);
        BAssertUtil.validateWarning(compileResult, index++, "error reason '{test string 1' is not module qualified",
                                    23, 21);
        BAssertUtil.validateWarning(compileResult, index, "error reason '{test/string}identifier' is not module " +
                "qualified", 23, 21);
    }
}
