/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.jdbc.query;

import org.ballerinalang.jdbc.utils.SQLDBUtils;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BDecimal;
import org.ballerinalang.model.values.BFloat;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
import org.ballerinalang.test.util.BCompileUtil;
import org.ballerinalang.test.util.BRunUtil;
import org.ballerinalang.test.util.CompileResult;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * This test class handles the complex sql types to ballerina type conversion for query operation.
 *
 * @since 1.2.0
 */
public class ComplexTypesQueryTest {

    private CompileResult result;
    private static final String DB_NAME = "TEST_SQL_COMPLEX_QUERY";
    private static final String JDBC_URL = "jdbc:h2:file:" + SQLDBUtils.DB_DIR + DB_NAME;
    private BValue[] args = {new BString(JDBC_URL), new BString(SQLDBUtils.DB_USER),
            new BString(SQLDBUtils.DB_PASSWORD)};

    @BeforeClass
    public void setup() {
        result = BCompileUtil.compileOffline(SQLDBUtils.getBalFilesDir("query", "complex-query-test.bal"));
        SQLDBUtils.deleteFiles(new File(SQLDBUtils.DB_DIR), DB_NAME);
        SQLDBUtils.initH2Database(SQLDBUtils.DB_DIR, DB_NAME,
                SQLDBUtils.getSQLResourceDir("query", "complex-test-data.sql"));
    }

    @Test(description = "Check retrieving primitive types.")
    public void testGetPrimitiveTypes() {
        BValue[] returnVal = BRunUtil.invokeFunction(result, "testGetPrimitiveTypes", args);
        Assert.assertTrue(returnVal[0] instanceof BMap);
        LinkedHashMap result = ((BMap) returnVal[0]).getMap();
        Assert.assertEquals(result.size(), 6);
        Assert.assertEquals(((BInteger) result.get("INT_TYPE")).intValue(), 1);
        Assert.assertEquals(((BInteger) result.get("LONG_TYPE")).intValue(), 9223372036854774807L);
        Assert.assertEquals(((BFloat) result.get("FLOAT_TYPE")).floatValue(), 123.34);
        Assert.assertEquals(((BFloat) result.get("DOUBLE_TYPE")).floatValue(), 2139095039D);
        Assert.assertTrue(((BBoolean) result.get("BOOLEAN_TYPE")).booleanValue());
        Assert.assertEquals(((BString) result.get("STRING_TYPE")).stringValue(), "Hello");
    }

    @Test(description = "Check record to JSON conversion.")
    public void testToJson() {
        BValue[] returns = BRunUtil.invokeFunction(result, "testToJson", args);
        Assert.assertEquals(returns.length, 1);
        Assert.assertTrue(returns[0] instanceof BMap);
        String expected = "{\"INT_TYPE\":1, \"LONG_TYPE\":9223372036854774807, \"FLOAT_TYPE\":123.34, "
                + "\"DOUBLE_TYPE\":2.139095039E9, \"BOOLEAN_TYPE\":true, \"STRING_TYPE\":\"Hello\"}";
        Assert.assertEquals(returns[0].stringValue(), expected);
    }

    @Test(description = "Check retrieving blob clob binary data.")
    public void testToJsonComplexTypes() {
        BValue[] returns = BRunUtil.invokeFunction(result, "testToJsonComplexTypes", args);
        Assert.assertEquals(returns.length, 1);
        Assert.assertTrue(returns[0] instanceof BMap);
        LinkedHashMap result = ((BMap) returns[0]).getMap();
        String blobString = new String(((BValueArray) result.get("BLOB_TYPE")).getBytes());
        String clobString = ((BString) result.get("CLOB_TYPE")).stringValue();
        String binaryString = new String(((BValueArray) result.get("BINARY_TYPE")).getBytes());
        Assert.assertEquals(blobString, "wso2 ballerina blob test.");
        Assert.assertEquals(clobString, "very long text");
        Assert.assertEquals(binaryString, "wso2 ballerina binary test.");
    }

    @Test(description = "Check retrieving nill values.")
    public void testComplexTypesNil() {
        BValue[] returns = BRunUtil.invokeFunction(result, "testComplexTypesNil", args);
        Assert.assertEquals(returns.length, 1);
        LinkedHashMap result = ((BMap) returns[0]).getMap();
        Assert.assertEquals(result.size(), 3);
        Assert.assertEquals(result.get("BLOB_TYPE"), null);
        Assert.assertEquals(result.get("CLOB_TYPE"), null);
        Assert.assertEquals(result.get("BINARY_TYPE"), null);
    }

    @Test(description = "Check array retrieval.")
    public void testArrayRetrieval() {
        BValue[] returns = BRunUtil.invokeFunction(result, "testArrayRetrieval", args);
        Assert.assertEquals(returns.length, 1);
        LinkedHashMap result = ((BMap) returns[0]).getMap();
        Assert.assertEquals(result.size(), 13);
        Assert.assertEquals(((BInteger) result.get("INT_TYPE")).intValue(), 1);

        BValueArray intArray = (BValueArray) result.get("INT_ARRAY");
        Assert.assertEquals(((BInteger) intArray.getBValue(0)).intValue(), 1);
        Assert.assertEquals(((BInteger) intArray.getBValue(1)).intValue(), 2);
        Assert.assertEquals(((BInteger) intArray.getBValue(2)).intValue(), 3);

        Assert.assertEquals(((BInteger) result.get("LONG_TYPE")).stringValue(), "9223372036854774807");

        BValueArray longArray = (BValueArray) result.get("LONG_ARRAY");
        Assert.assertEquals(((BInteger) longArray.getBValue(0)).intValue(), 100000000);
        Assert.assertEquals(((BInteger) longArray.getBValue(1)).intValue(), 200000000);
        Assert.assertEquals(((BInteger) longArray.getBValue(2)).intValue(), 300000000);

        Assert.assertEquals(((BFloat) result.get("FLOAT_TYPE")).floatValue(), 123.34);

        BValueArray floatArray = (BValueArray) result.get("FLOAT_ARRAY");
        Assert.assertEquals(((BDecimal) floatArray.getBValue(0)).floatValue(), 245.23);
        Assert.assertEquals(((BDecimal) floatArray.getBValue(1)).floatValue(), 5559.49);
        Assert.assertEquals(((BDecimal) floatArray.getBValue(2)).floatValue(), 8796.123);

        Assert.assertEquals(((BFloat) result.get("DOUBLE_TYPE")).floatValue(), 2.139095039E9);
        Assert.assertEquals(((BBoolean) result.get("BOOLEAN_TYPE")).booleanValue(), true);
        Assert.assertEquals(((BString) result.get("STRING_TYPE")).stringValue(), "Hello");
        Assert.assertEquals(((BDecimal) result.get("DECIMAL_TYPE")).stringValue(), "342452151425.4556");

        BValueArray doubleArray = (BValueArray) result.get("DOUBLE_ARRAY");
        Assert.assertEquals(((BDecimal) doubleArray.getBValue(0)).stringValue(), "245.23");
        Assert.assertEquals(((BDecimal) doubleArray.getBValue(1)).stringValue(), "5559.49");
        Assert.assertEquals(((BDecimal) doubleArray.getBValue(2)).stringValue(), "8796.123");

        BValueArray booleanArray = (BValueArray) result.get("BOOLEAN_ARRAY");
        Assert.assertEquals(((BBoolean) booleanArray.getBValue(0)).booleanValue(), true);
        Assert.assertEquals(((BBoolean) booleanArray.getBValue(1)).booleanValue(), false);
        Assert.assertEquals(((BBoolean) booleanArray.getBValue(2)).booleanValue(), true);

        BValueArray stringArray = (BValueArray) result.get("STRING_ARRAY");
        Assert.assertEquals(((BString) stringArray.getBValue(0)).stringValue(), "Hello");
        Assert.assertEquals(((BString) stringArray.getBValue(1)).stringValue(), "Ballerina");
    }

    @Test(description = "Check complex data retrieval as a record.")
    public void testComplexWithStructDef() {
        BValue[] returns = BRunUtil.invokeFunction(result, "testComplexWithStructDef", args);
        Assert.assertEquals(returns.length, 1);
        LinkedHashMap result = ((BMap) returns[0]).getMap();
        Assert.assertEquals(result.size(), 12);
        Assert.assertEquals(((BInteger) result.get("int_type")).intValue(), 1);

        BValueArray intArray = (BValueArray) result.get("int_array");
        Assert.assertEquals(((BInteger) intArray.getBValue(0)).intValue(), 1);
        Assert.assertEquals(((BInteger) intArray.getBValue(1)).intValue(), 2);
        Assert.assertEquals(((BInteger) intArray.getBValue(2)).intValue(), 3);

        Assert.assertEquals(((BInteger) result.get("long_type")).stringValue(), "9223372036854774807");

        BValueArray longArray = (BValueArray) result.get("long_array");
        Assert.assertEquals(((BInteger) longArray.getBValue(0)).intValue(), 100000000);
        Assert.assertEquals(((BInteger) longArray.getBValue(1)).intValue(), 200000000);
        Assert.assertEquals(((BInteger) longArray.getBValue(2)).intValue(), 300000000);

        Assert.assertEquals(((BFloat) result.get("float_type")).floatValue(), 123.34);

        BValueArray floatArray = (BValueArray) result.get("float_array");
        Assert.assertEquals(((BDecimal) floatArray.getBValue(0)).floatValue(), 245.23);
        Assert.assertEquals(((BDecimal) floatArray.getBValue(1)).floatValue(), 5559.49);
        Assert.assertEquals(((BDecimal) floatArray.getBValue(2)).floatValue(), 8796.123);

        Assert.assertEquals(((BFloat) result.get("double_type")).floatValue(), 2.139095039E9);
        Assert.assertEquals(((BBoolean) result.get("boolean_type")).booleanValue(), true);
        Assert.assertEquals(((BString) result.get("string_type")).stringValue(), "Hello");

        BValueArray doubleArray = (BValueArray) result.get("double_array");
        Assert.assertEquals(((BDecimal) doubleArray.getBValue(0)).stringValue(), "245.23");
        Assert.assertEquals(((BDecimal) doubleArray.getBValue(1)).stringValue(), "5559.49");
        Assert.assertEquals(((BDecimal) doubleArray.getBValue(2)).stringValue(), "8796.123");

        BValueArray booleanArray = (BValueArray) result.get("boolean_array");
        Assert.assertEquals(((BBoolean) booleanArray.getBValue(0)).booleanValue(), true);
        Assert.assertEquals(((BBoolean) booleanArray.getBValue(1)).booleanValue(), false);
        Assert.assertEquals(((BBoolean) booleanArray.getBValue(2)).booleanValue(), true);

        BValueArray stringArray = (BValueArray) result.get("string_array");
        Assert.assertEquals(((BString) stringArray.getBValue(0)).stringValue(), "Hello");
        Assert.assertEquals(((BString) stringArray.getBValue(1)).stringValue(), "Ballerina");
    }

    @Test(description = "Check complex data retrieval as a record.")
    public void testMultipleRecoredRetrieval() {
        BValue[] returns = BRunUtil.invokeFunction(result, "testMultipleRecoredRetrieval", args);
        Assert.assertEquals(returns.length, 1);
        BValueArray rowSet = ((BValueArray) returns[0]);
        Assert.assertEquals(rowSet.size(), 4);
        LinkedHashMap resultRow1 = ((BMap) rowSet.getBValue(0)).getMap();
        LinkedHashMap resultRow2 = ((BMap) rowSet.getBValue(1)).getMap();
        LinkedHashMap resultRow3 = ((BMap) rowSet.getBValue(2)).getMap();
        LinkedHashMap resultRow4 = ((BMap) rowSet.getBValue(3)).getMap();

        BValueArray row1IntArray = (BValueArray) resultRow1.get("INT_ARRAY");
        Assert.assertEquals(((BInteger) row1IntArray.getBValue(0)).intValue(), 1);
        Assert.assertEquals(((BInteger) row1IntArray.getBValue(1)).intValue(), 2);
        Assert.assertEquals(((BInteger) row1IntArray.getBValue(2)).intValue(), 3);

        BValueArray row2IntArray = (BValueArray) resultRow2.get("INT_ARRAY");
        Assert.assertEquals(row2IntArray.getBValue(0), null);
        Assert.assertEquals((row2IntArray.getBValue(1)).stringValue(), "2");
        Assert.assertEquals((row2IntArray.getBValue(2)).stringValue(), "3");

        BValueArray row3IntArray = (BValueArray) resultRow3.get("INT_ARRAY");
        Assert.assertEquals(row3IntArray, null);

        BValueArray row4IntArray = (BValueArray) resultRow4.get("INT_ARRAY");
        Assert.assertEquals(row4IntArray.getBValue(0), null);
        Assert.assertEquals(row4IntArray.getBValue(1), null);
        Assert.assertEquals(row4IntArray.getBValue(2), null);
    }

    @Test(description = "Check date time operation", enabled = false)
    public void testDateTime() throws ParseException {
        DateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd");
        Date dateType = dfDate.parse("2017-5-23");

        //time added in UTC
        DateFormat dfTime = new SimpleDateFormat("hh:mm:ss");
        Date timeType = dfTime.parse("19:45:23");

        DateFormat dfDateTime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date dateTimeType = dfDateTime.parse("2017-01-25 22:03:55");

        BValue[] returns = BRunUtil.invoke(result, "testDateTime", args);
        Assert.assertEquals(returns.length, 1);
        LinkedHashMap result = ((BMap) returns[0]).getMap();
        Assert.assertEquals(result.size(), 4);
        assertDateStringValues(result, dateType, timeType, dateTimeType);
    }

    @Test(description = "Check values retrieved with column alias.")
    public void testColumnAlias() {
        BValue[] returns = BRunUtil.invoke(result, "testColumnAlias", args);
        Assert.assertEquals(returns.length, 1);
        LinkedHashMap result = ((BMap) returns[0]).getMap();
        Assert.assertEquals(result.size(), 7);
        Assert.assertEquals(((BInteger) result.get("INT_TYPE")).intValue(), 1);
        Assert.assertEquals(((BFloat) result.get("FLOAT_TYPE")).floatValue(), 123.34);
        Assert.assertEquals(((BInteger) result.get("DT2INT_TYPE")).intValue(), 100);
    }

    private void assertDateStringValues(LinkedHashMap returns, Date dateInserted, Date timeInserted,
                                        Date timestampInserted) {
        try {
            DateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd");
            String dateReturned = returns.get("DATE_TYPE").toString();
            long dateReturnedEpoch = dfDate.parse(dateReturned).getTime();
            Assert.assertEquals(dateReturnedEpoch, dateInserted.getTime());

            DateFormat dfTime = new SimpleDateFormat("HH:mm:ss");
            String timeReturned = returns.get("TIME_TYPE").toString();
            long timeReturnedEpoch = dfTime.parse(timeReturned).getTime();
            Assert.assertEquals(timeReturnedEpoch, timeInserted.getTime());

            DateFormat dfTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            String timestampReturned = returns.get("TIMESTAMP_TYPE").toString();
            long timestampReturnedEpoch = dfTimestamp.parse(timestampReturned).getTime();
            Assert.assertEquals(timestampReturnedEpoch, timestampInserted.getTime());

            String datetimeReturned = returns.get("DATETIME_TYPE").toString();
            long datetimeReturnedEpoch = dfTimestamp.parse(datetimeReturned).getTime();
            Assert.assertEquals(datetimeReturnedEpoch, timestampInserted.getTime());
        } catch (ParseException e) {
            Assert.fail("Parsing the returned date/time/timestamp value has failed", e);
        }
    }
}
