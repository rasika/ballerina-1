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
package org.ballerinalang.sql.utils;

import org.ballerinalang.jvm.types.BArrayType;
import org.ballerinalang.jvm.types.BField;
import org.ballerinalang.jvm.types.BRecordType;
import org.ballerinalang.jvm.types.BStructureType;
import org.ballerinalang.jvm.types.BType;
import org.ballerinalang.jvm.types.BTypes;
import org.ballerinalang.jvm.types.BUnionType;
import org.ballerinalang.jvm.types.TypeTags;
import org.ballerinalang.jvm.values.ArrayValue;
import org.ballerinalang.jvm.values.ArrayValueImpl;
import org.ballerinalang.jvm.values.DecimalValue;
import org.ballerinalang.jvm.values.MapValue;
import org.ballerinalang.jvm.values.MapValueImpl;
import org.ballerinalang.sql.Constants;
import org.ballerinalang.sql.exception.ApplicationError;
import org.ballerinalang.stdlib.time.util.TimeUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * This class has the utility methods to process and convert the SQL types into ballerina types,
 * and other shared utility methods.
 *
 * @since 1.2.0
 */
class Utils {

    public static void closeResources(ResultSet resultSet, Statement statement, Connection connection) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException ignored) {
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    static ArrayValue convert(Array array, int sqlType, BType bType) throws SQLException, ApplicationError {
        if (array != null) {
            validatedInvalidFieldAssignment(sqlType, bType, "SQL Array");
            Object[] dataArray = (Object[]) array.getArray();
            if (dataArray == null || dataArray.length == 0) {
                return null;
            }

            Object[] result = validateNullable(dataArray);
            Object firstNonNullElement = result[0];
            boolean containsNull = (boolean) result[1];

            if (containsNull) {
                // If there are some null elements, return a union-type element array
                return createAndPopulateRefValueArray(firstNonNullElement, dataArray, bType);
            } else {
                // If there are no null elements, return a ballerina primitive-type array
                return createAndPopulatePrimitiveValueArray(firstNonNullElement, dataArray);
            }

        } else {
            return null;
        }
    }

    private static ArrayValue createAndPopulatePrimitiveValueArray(Object firstNonNullElement, Object[] dataArray) {
        int length = dataArray.length;
        if (firstNonNullElement instanceof String) {
            ArrayValue stringDataArray = new ArrayValueImpl(new BArrayType(BTypes.typeString));
            for (int i = 0; i < length; i++) {
                stringDataArray.add(i, (String) dataArray[i]);
            }
            return stringDataArray;
        } else if (firstNonNullElement instanceof Boolean) {
            ArrayValue boolDataArray = new ArrayValueImpl(new BArrayType(BTypes.typeBoolean));
            for (int i = 0; i < length; i++) {
                boolDataArray.add(i, ((Boolean) dataArray[i]).booleanValue());
            }
            return boolDataArray;
        } else if (firstNonNullElement instanceof Integer) {
            ArrayValue intDataArray = new ArrayValueImpl(new BArrayType(BTypes.typeInt));
            for (int i = 0; i < length; i++) {
                intDataArray.add(i, ((Integer) dataArray[i]).intValue());
            }
            return intDataArray;
        } else if (firstNonNullElement instanceof Long) {
            ArrayValue longDataArray = new ArrayValueImpl(new BArrayType(BTypes.typeInt));
            for (int i = 0; i < length; i++) {
                longDataArray.add(i, ((Long) dataArray[i]).longValue());
            }
            return longDataArray;
        } else if (firstNonNullElement instanceof Float) {
            ArrayValue floatDataArray = new ArrayValueImpl(new BArrayType(BTypes.typeFloat));
            for (int i = 0; i < length; i++) {
                floatDataArray.add(i, ((Float) dataArray[i]).floatValue());
            }
            return floatDataArray;
        } else if (firstNonNullElement instanceof Double) {
            ArrayValue doubleDataArray = new ArrayValueImpl(new BArrayType(BTypes.typeFloat));
            for (int i = 0; i < dataArray.length; i++) {
                doubleDataArray.add(i, ((Double) dataArray[i]).doubleValue());
            }
            return doubleDataArray;
        } else if ((firstNonNullElement instanceof BigDecimal)) {
            ArrayValue decimalDataArray = new ArrayValueImpl(new BArrayType(BTypes.typeDecimal));
            for (int i = 0; i < dataArray.length; i++) {
                decimalDataArray.add(i, new DecimalValue((BigDecimal) dataArray[i]));
            }
            return decimalDataArray;
        } else {
            return null;
        }
    }

    static String getString(Clob data) throws IOException, SQLException {
        if (data == null) {
            return null;
        }
        try (Reader r = new BufferedReader(data.getCharacterStream())) {
            StringBuilder sb = new StringBuilder();
            int pos;
            while ((pos = r.read()) != -1) {
                sb.append((char) pos);
            }
            return sb.toString();
        }
    }

    private static ArrayValue createAndPopulateRefValueArray(Object firstNonNullElement, Object[] dataArray,
                                                             BType bType) {
        ArrayValue refValueArray = null;
        int length = dataArray.length;
        if (firstNonNullElement instanceof String) {
            refValueArray = createEmptyRefValueArray(BTypes.typeString);
            for (int i = 0; i < length; i++) {
                refValueArray.add(i, dataArray[i]);
            }
        } else if (firstNonNullElement instanceof Boolean) {
            refValueArray = createEmptyRefValueArray(BTypes.typeBoolean);
            for (int i = 0; i < length; i++) {
                refValueArray.add(i, dataArray[i]);
            }
        } else if (firstNonNullElement instanceof Integer) {
            refValueArray = createEmptyRefValueArray(BTypes.typeInt);
            for (int i = 0; i < length; i++) {
                refValueArray.add(i, dataArray[i]);
            }
        } else if (firstNonNullElement instanceof Long) {
            refValueArray = createEmptyRefValueArray(BTypes.typeInt);
            for (int i = 0; i < length; i++) {
                refValueArray.add(i, dataArray[i]);
            }
        } else if (firstNonNullElement instanceof Float) {
            refValueArray = createEmptyRefValueArray(BTypes.typeFloat);
            for (int i = 0; i < length; i++) {
                refValueArray.add(i, dataArray[i]);
            }
        } else if (firstNonNullElement instanceof Double) {
            refValueArray = createEmptyRefValueArray(BTypes.typeFloat);
            for (int i = 0; i < length; i++) {
                refValueArray.add(i, dataArray[i]);
            }
        } else if (firstNonNullElement instanceof BigDecimal) {
            refValueArray = createEmptyRefValueArray(BTypes.typeDecimal);
            for (int i = 0; i < length; i++) {
                refValueArray.add(i, dataArray[i] != null ? new DecimalValue((BigDecimal) dataArray[i]) : null);
            }
        } else if (firstNonNullElement == null) {
            refValueArray = createEmptyRefValueArray(bType);
            for (int i = 0; i < length; i++) {
                refValueArray.add(i, firstNonNullElement);
            }
        }
        return refValueArray;
    }

    private static ArrayValue createEmptyRefValueArray(BType type) {
        List<BType> memberTypes = new ArrayList<>(2);
        memberTypes.add(type);
        memberTypes.add(BTypes.typeNull);
        BUnionType unionType = new BUnionType(memberTypes);
        return new ArrayValueImpl(new BArrayType(unionType));
    }

    private static Object[] validateNullable(Object[] objects) {
        Object[] returnResult = new Object[2];
        boolean foundNull = false;
        Object nonNullObject = null;
        for (Object object : objects) {
            if (object != null) {
                if (nonNullObject == null) {
                    nonNullObject = object;
                }
                if (foundNull) {
                    break;
                }
            } else {
                foundNull = true;
                if (nonNullObject != null) {
                    break;
                }
            }
        }
        returnResult[0] = nonNullObject;
        returnResult[1] = foundNull;
        return returnResult;
    }

    static Object convert(String value, int sqlType, BType bType) throws ApplicationError {
        validatedInvalidFieldAssignment(sqlType, bType, "SQL String");
        return value;
    }

    static Object convert(byte[] value, int sqlType, BType bType) throws ApplicationError {
        validatedInvalidFieldAssignment(sqlType, bType, "SQL Binary");
        if (value != null) {
            return new ArrayValueImpl(value);
        } else {
            return null;
        }
    }

    static Object convert(long value, int sqlType, BType bType, boolean isNull) throws ApplicationError {
        validatedInvalidFieldAssignment(sqlType, bType, "SQL long or integer");
        if (isNull) {
            return null;
        } else {
            if (bType.getTag() == TypeTags.STRING_TAG) {
                return String.valueOf(value);
            }
            return value;
        }
    }

    static Object convert(double value, int sqlType, BType bType, boolean isNull) throws ApplicationError {
        validatedInvalidFieldAssignment(sqlType, bType, "SQL double or float");
        if (isNull) {
            return null;
        } else {
            if (bType.getTag() == TypeTags.STRING_TAG) {
                return String.valueOf(value);
            }
            return value;
        }
    }

    static Object convert(BigDecimal value, int sqlType, BType bType, boolean isNull) throws ApplicationError {
        validatedInvalidFieldAssignment(sqlType, bType, "SQL decimal or real");
        if (isNull) {
            return null;
        } else {
            if (bType.getTag() == TypeTags.STRING_TAG) {
                return String.valueOf(value);
            }
            return new DecimalValue(value);
        }
    }

    static Object convert(Blob value, int sqlType, BType bType) throws ApplicationError, SQLException {
        validatedInvalidFieldAssignment(sqlType, bType, "SQL Blob");
        if (value != null) {
            return new ArrayValueImpl(value.getBytes(1L, (int) value.length()));
        } else {
            return null;
        }
    }

    static Object convert(java.util.Date date, int sqlType, BType bType) throws ApplicationError {
        validatedInvalidFieldAssignment(sqlType, bType, "SQL Date/Time");
        if (date != null) {
            switch (bType.getTag()) {
                case TypeTags.STRING_TAG:
                    return getString(date);
                case TypeTags.OBJECT_TYPE_TAG:
                case TypeTags.RECORD_TYPE_TAG:
                    return createTimeStruct(date.getTime());
                case TypeTags.INT_TAG:
                    return date.getTime();
            }
        }
        return null;
    }

    static Object convert(boolean value, int sqlType, BType bType, boolean isNull) throws ApplicationError {
        validatedInvalidFieldAssignment(sqlType, bType, "SQL Boolean");
        if (!isNull) {
            switch (bType.getTag()) {
                case TypeTags.BOOLEAN_TAG:
                    return value;
                case TypeTags.INT_TAG:
                    if (value) {
                        return 1L;
                    } else {
                        return 0L;
                    }
                case TypeTags.STRING_TAG:
                    return String.valueOf(value);
            }
        }
        return null;
    }

    static Object convert(Struct value, int sqlType, BType bType) throws ApplicationError {
        validatedInvalidFieldAssignment(sqlType, bType, "SQL Struct");
        if (value != null) {
            if (bType instanceof BRecordType) {
                return createUserDefinedType(value, (BRecordType) bType);
            } else {
                throw new ApplicationError("The ballerina type that can be used for SQL struct should be record type," +
                        " but found " + bType.getName() + " .");
            }
        } else {
            return null;
        }
    }

    private static MapValue<String, Object> createUserDefinedType(Struct structValue, BStructureType structType)
            throws ApplicationError {
        if (structValue == null) {
            return null;
        }
        BField[] internalStructFields = structType.getFields().values().toArray(new BField[0]);
        MapValue<String, Object> struct = new MapValueImpl<>(structType);
        try {
            Object[] dataArray = structValue.getAttributes();
            if (dataArray != null) {
                if (dataArray.length != internalStructFields.length) {
                    throw new ApplicationError("specified record and the returned SQL Struct field counts " +
                            "are different, and hence not compatible");
                }
                int index = 0;
                for (BField internalField : internalStructFields) {
                    int type = internalField.getFieldType().getTag();
                    String fieldName = internalField.getFieldName();
                    Object value = dataArray[index];
                    switch (type) {
                        case TypeTags.INT_TAG:
                            if (value instanceof BigDecimal) {
                                struct.put(fieldName, ((BigDecimal) value).intValue());
                            } else {
                                struct.put(fieldName, value);
                            }
                            break;
                        case TypeTags.FLOAT_TAG:
                            if (value instanceof BigDecimal) {
                                struct.put(fieldName, ((BigDecimal) value).doubleValue());
                            } else {
                                struct.put(fieldName, value);
                            }
                            break;
                        case TypeTags.DECIMAL_TAG:
                            if (value instanceof BigDecimal) {
                                struct.put(fieldName, value);
                            } else {
                                struct.put(fieldName, new DecimalValue((BigDecimal) value));
                            }
                            break;
                        case TypeTags.STRING_TAG:
                            struct.put(fieldName, value);
                            break;
                        case TypeTags.BOOLEAN_TAG:
                            struct.put(fieldName, ((int) value) == 1);
                            break;
                        case TypeTags.OBJECT_TYPE_TAG:
                        case TypeTags.RECORD_TYPE_TAG:
                            struct.put(fieldName,
                                    createUserDefinedType((Struct) value,
                                            (BStructureType) internalField.getFieldType()));
                            break;
                        default:
                            throw new ApplicationError("Error while retrieving data for unsupported type " +
                                    internalField.getFieldType().getName() + " to create "
                                    + structType.getName() + " record.");
                    }
                    ++index;
                }
            }
        } catch (SQLException e) {
            throw new ApplicationError("Error while retrieving data to create " + structType.getName()
                    + " record. ", e);
        }
        return struct;
    }


    private static MapValue<String, Object> createTimeStruct(long millis) {
        return TimeUtils.createTimeRecord(TimeUtils.getTimeZoneRecord(), TimeUtils.getTimeRecord(), millis,
                Constants.TIMEZONE_UTC);
    }

    private static String getString(java.util.Date value) {
        if (value == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTime(value);

        if (!calendar.isSet(Calendar.ZONE_OFFSET)) {
            calendar.setTimeZone(TimeZone.getDefault());
        }
        StringBuffer datetimeString = new StringBuffer(28);
        if (value instanceof Date) {
            //'-'? yyyy '-' mm '-' dd zzzzzz?
            calendar.setTime(value);
            appendDate(datetimeString, calendar);
            appendTimeZone(calendar, datetimeString);
        } else if (value instanceof Time) {
            //hh ':' mm ':' ss ('.' s+)? (zzzzzz)?
            calendar.setTimeInMillis(value.getTime());
            appendTime(calendar, datetimeString);
            appendTimeZone(calendar, datetimeString);
        } else if (value instanceof Timestamp) {
            calendar.setTimeInMillis(value.getTime());
            appendDate(datetimeString, calendar);
            datetimeString.append("T");
            appendTime(calendar, datetimeString);
            appendTimeZone(calendar, datetimeString);
        } else {
            calendar.setTime(value);
            appendTime(calendar, datetimeString);
            appendTimeZone(calendar, datetimeString);
        }
        return datetimeString.toString();
    }

    private static void appendTimeZone(Calendar calendar, StringBuffer dateString) {
        int timezoneOffSet = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
        int timezoneOffSetInMinits = timezoneOffSet / 60000;
        if (timezoneOffSetInMinits < 0) {
            dateString.append("-");
            timezoneOffSetInMinits = timezoneOffSetInMinits * -1;
        } else {
            dateString.append("+");
        }
        int hours = timezoneOffSetInMinits / 60;
        int minits = timezoneOffSetInMinits % 60;
        if (hours < 10) {
            dateString.append("0");
        }
        dateString.append(hours).append(":");
        if (minits < 10) {
            dateString.append("0");
        }
        dateString.append(minits);
    }

    private static void appendTime(Calendar value, StringBuffer dateString) {
        if (value.get(Calendar.HOUR_OF_DAY) < 10) {
            dateString.append("0");
        }
        dateString.append(value.get(Calendar.HOUR_OF_DAY)).append(":");
        if (value.get(Calendar.MINUTE) < 10) {
            dateString.append("0");
        }
        dateString.append(value.get(Calendar.MINUTE)).append(":");
        if (value.get(Calendar.SECOND) < 10) {
            dateString.append("0");
        }
        dateString.append(value.get(Calendar.SECOND)).append(".");
        if (value.get(Calendar.MILLISECOND) < 10) {
            dateString.append("0");
        }
        if (value.get(Calendar.MILLISECOND) < 100) {
            dateString.append("0");
        }
        dateString.append(value.get(Calendar.MILLISECOND));
    }

    private static void appendDate(StringBuffer dateString, Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        if (year < 1000) {
            dateString.append("0");
        }
        if (year < 100) {
            dateString.append("0");
        }
        if (year < 10) {
            dateString.append("0");
        }
        dateString.append(year).append("-");
        // sql date month is started from 1 and calendar month is
        // started from 0. so have to add one
        int month = calendar.get(Calendar.MONTH) + 1;
        if (month < 10) {
            dateString.append("0");
        }
        dateString.append(month).append("-");
        if (calendar.get(Calendar.DAY_OF_MONTH) < 10) {
            dateString.append("0");
        }
        dateString.append(calendar.get(Calendar.DAY_OF_MONTH));
    }

    private static void validatedInvalidFieldAssignment(int sqlType, BType bType, String sqlTypeName)
            throws ApplicationError {
        if (!isValidFieldConstraint(sqlType, bType)) {
            throw new ApplicationError(sqlTypeName + " field cannot be converted to ballerina type : "
                    + bType.getName());
        }
    }

    public static BType validFieldConstraint(int sqlType, BType bType) {
        if (bType.getTag() == TypeTags.UNION_TAG && bType instanceof BUnionType) {
            BUnionType bUnionType = (BUnionType) bType;
            for (BType memberType : bUnionType.getMemberTypes()) {
                //In case if the member type is another union type, check recursively.
                if (isValidFieldConstraint(sqlType, memberType)) {
                    return memberType;
                }
            }
        } else {
            if (isValidPrimitiveConstraint(sqlType, bType)) {
                return bType;
            }
        }
        return null;
    }

    public static boolean isValidFieldConstraint(int sqlType, BType bType) {
        if (bType.getTag() == TypeTags.UNION_TAG && bType instanceof BUnionType) {
            BUnionType bUnionType = (BUnionType) bType;
            for (BType memberType : bUnionType.getMemberTypes()) {
                //In case if the member type is another union type, check recursively.
                if (isValidFieldConstraint(sqlType, memberType)) {
                    return true;
                }
            }
            return false;
        } else {
            return isValidPrimitiveConstraint(sqlType, bType);
        }
    }

    private static boolean isValidPrimitiveConstraint(int sqlType, BType bType) {
        switch (sqlType) {
            case Types.ARRAY:
                return bType.getTag() == TypeTags.ARRAY_TAG;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
            case Types.ROWID:
                return bType.getTag() == TypeTags.STRING_TAG ||
                        bType.getTag() == TypeTags.JSON_TAG;
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case Types.TIME_WITH_TIMEZONE:
                return bType.getTag() == TypeTags.STRING_TAG ||
                        bType.getTag() == TypeTags.OBJECT_TYPE_TAG ||
                        bType.getTag() == TypeTags.RECORD_TYPE_TAG ||
                        bType.getTag() == TypeTags.INT_TAG;
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                return bType.getTag() == TypeTags.INT_TAG ||
                        bType.getTag() == TypeTags.STRING_TAG;
            case Types.BIT:
            case Types.BOOLEAN:
                return bType.getTag() == TypeTags.BOOLEAN_TAG ||
                        bType.getTag() == TypeTags.INT_TAG ||
                        bType.getTag() == TypeTags.STRING_TAG;
            case Types.NUMERIC:
            case Types.DECIMAL:
                return bType.getTag() == TypeTags.DECIMAL_TAG ||
                        bType.getTag() == TypeTags.INT_TAG ||
                        bType.getTag() == TypeTags.STRING_TAG;
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
                return bType.getTag() == TypeTags.FLOAT_TAG ||
                        bType.getTag() == TypeTags.STRING_TAG;
            case Types.BLOB:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                if (bType.getTag() == TypeTags.ARRAY_TAG) {
                    int elementTypeTag = ((BArrayType) bType).getElementType().getTag();
                    return elementTypeTag == TypeTags.BYTE_TAG;
                }
                return bType.getTag() == TypeTags.BYTE_ARRAY_TAG;
            case Types.STRUCT:
                return bType.getTag() == TypeTags.RECORD_TYPE_TAG;
            default:
                return bType.getTag() == TypeTags.ANY_TAG;
        }
    }
}
