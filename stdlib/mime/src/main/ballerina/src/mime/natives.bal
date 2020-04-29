// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
import ballerina/java;
import ballerina/lang.'int as langint;

# Key name for `boundary` parameter in MediaType. This is needed for composite type media types.
public const string BOUNDARY = "boundary";

# Key name for `start` parameter in MediaType. This determines which part in the multipart message contains the
# payload.
public const string START = "start";

# Key name for `type` parameter in MediaType. This indicates the MIME media type of the `root` body part.
public const string TYPE = "type";

# Key name for `charset` parameter in MediaType. This indicates the character set of the body text.
public const string CHARSET = "charset";

# Default charset to be used with MIME encoding and decoding.
public const string DEFAULT_CHARSET = "UTF-8";

# Permission to be used with opening a byte channel for overflow data.
const READ_PERMISSION = "r";

# Represents `content-id` header name.
public const string CONTENT_ID = "content-id";

# Represents `content-length` header name.
public const string CONTENT_LENGTH = "content-length";

# Represents `content-type` header name.
public const string CONTENT_TYPE = "content-type";

# Represents `content-disposition` header name.
public const string CONTENT_DISPOSITION = "content-disposition";

# Represents values in `Content-Disposition` header.
#
# + fileName - Default filename for storing the body part if the receiving agent wishes to store it in an external
#              file
# + disposition - Indicates how the body part should be presented (`inline`, `attachment`, or as `form-data`)
# + name - Represents the field name in case of `multipart/form-data`
# + parameters - A set of parameters specified in the `attribute=value` notation
public type ContentDisposition object {

    public string fileName = "";
    public string disposition = "";
    public string name = "";
    public map<string> parameters = {};

# Converts the `ContentDisposition` type to a string suitable to use as the value of a corresponding MIME header.
# ```ballerina
# string contDisposition = contentDisposition.toString();
# ```
#
# + return - The `string` representation of the `ContentDisposition` object
    public function toString() returns string {
        return <string>java:toString(convertContentDispositionToString(self));
    }
};

function convertContentDispositionToString(ContentDisposition contentDisposition) returns handle =
@java:Method {
    class: "org.ballerinalang.mime.nativeimpl.ContentDisposition"
} external;

# Describes the nature of the data in the body of a MIME entity.
#
# + primaryType - Declares the general type of data
# + subType - A specific format of the primary-type data
# + suffix - Identifies the semantics of a specific media type
# + parameters - A set of parameters specified in an `attribute=value` notation
public type MediaType object {

    public string primaryType = "";
    public string subType = "";
    public string suffix = "";
    public map<string> parameters = {};

# Gets the “primaryType/subtype+suffix” combination in a `string` format.
# ```ballerina
# string baseType = mediaType.getBaseType();
# ```
#
# + return - Base type as a `string` from the `MediaType` struct
    public function getBaseType() returns string {
        return self.primaryType + "/" + self.subType;
    }

# Converts the media type to a `string`, which is suitable to be used as the value of a corresponding HTTP header.
# ```ballerina
# string mediaTypeString = mediaType.toString();
# ```
#
# + return - Content type with parameters as a `string`
    public function toString() returns string {
        string contentType = self.getBaseType();
        // map<string> parameters = self.parameters;
        string[] arrKeys = self.parameters.keys();
        int size = arrKeys.length();
        if (size > 0) {
            contentType = contentType + "; ";
        }
        int index = 0;
        while (index < size) {
            string value = self.parameters[arrKeys[index]] ?: "";
            if (index == size - 1) {
                contentType = contentType + arrKeys[index] + "=" + value;
                break;
            } else {
                contentType = contentType + arrKeys[index] + "=" + value + ";";
                index = index + 1;
            }
        }
        return contentType;
    }
};

# Represents the headers and body of a message. This can be used to represent both the entity of a top level message
# and an entity(body part) inside of a multipart entity.
#
# + cType - Describes the data contained in the body of the entity
# + cId - Helps one body of an entity to make a reference to another
# + cLength - Represents the size of the entity
# + cDisposition - Represents values related to `Content-Disposition` header
public type Entity object {

    private MediaType? cType = ();
    private string cId = "";
    private int cLength = 0;
    private ContentDisposition? cDisposition = ();

# Sets the content-type to the entity.
# ```ballerina
# mime:InvalidContentTypeError? contentType = mimeEntity.setContentType("application/json");
# ```
#
# + mediaType - Content type, which needs to be set to the entity
# + return - `()` if successful or else an `mime:InvalidContentTypeError` in case of invalid media-type
    public function setContentType(@untainted string mediaType) returns InvalidContentTypeError? {
        self.cType = check getMediaType(mediaType);
        self.setHeader(CONTENT_TYPE, mediaType);
    }

# Gets the content type of the entity.
# ```ballerina
# string contentType = mimeEntity.getContentType();
# ```
#
# + return - Content type as a `string`
    public function getContentType() returns @tainted string {
        string contentTypeHeaderValue = "";
        if (self.hasHeader(CONTENT_TYPE)) {
            contentTypeHeaderValue = self.getHeader(CONTENT_TYPE);
        }
        return contentTypeHeaderValue;
    }

# Sets the content ID of the entity.
# ```ballerina
# mimeEntity.setContentId("test-id");
# ```
#
# + contentId - Content ID, which needs to be set to the entity
    public function setContentId(@untainted string contentId) {
        self.cId = contentId;
        self.setHeader(CONTENT_ID, contentId);
    }

# Gets the content ID of the entity.
# ```ballerina
# string contentId = mimeEntity.getContentId();
# ```
#
# + return - Content ID as a `string`
    public function getContentId() returns @tainted string {
        string contentId = "";
        if (self.hasHeader(CONTENT_ID)) {
            contentId = self.getHeader(CONTENT_ID);
        }
        return contentId;
    }

# Sets the content length of the entity.
# ```ballerina
# mimeEntity.setContentLength(45555);
# ```
#
# + contentLength - Content length, which needs to be set to the entity
    public function setContentLength(@untainted int contentLength) {
        self.cLength = contentLength;
        var contentLengthStr = contentLength.toString();
        self.setHeader(CONTENT_LENGTH, contentLengthStr);
    }

# Gets the content length of the entity.
# ```ballerina
# int|error contentLength = mimeEntity.getContentLength();
# ```
#
# + return - Content length as an `int` or else an error in case of a failure
    public function getContentLength() returns @tainted int|error {
        string contentLength = "";
        if (self.hasHeader(CONTENT_LENGTH)) {
            contentLength = self.getHeader(CONTENT_LENGTH);
        }
        if (contentLength == "") {
            return -1;
        } else {
            return langint:fromString(contentLength);
        }
    }

# Sets the content disposition of the entity.
# ```ballerina
# mimeEntity.setContentDisposition(contentDisposition);
# ```
#
# + contentDisposition - Content disposition, which needs to be set to the entity
    public function setContentDisposition(ContentDisposition contentDisposition) {
        self.cDisposition = contentDisposition;
        self.setHeader(CONTENT_DISPOSITION, contentDisposition.toString());
    }

    # Gets the content disposition of the entity.
    #
    # + return - A `ContentDisposition` object
    public function getContentDisposition() returns ContentDisposition {
        string contentDispositionVal = "";
        if (self.hasHeader(CONTENT_DISPOSITION)) {
            contentDispositionVal = self.getHeader(CONTENT_DISPOSITION);
        }
        return getContentDispositionObject(contentDispositionVal);
    }

# Sets the body of the entity with the given content. Note that any string value is set as `text/plain`. To send a
# JSON-compatible string, set the content-type header to `application/json` or use the `setJsonPayload` method instead.
# ```ballerina
# mimeEntity.setBody("body string");
# ```
#
# + entityBody - Entity body can be of the type `string`,`xml`,`json`,`byte[]`,`io:ReadableByteChannel`, or `Entity[]`.
    public function setBody(@untainted string|xml|json|byte[]|io:ReadableByteChannel|Entity[] entityBody) {
        if (entityBody is string) {
            self.setText(entityBody);
        } else if (entityBody is xml) {
            self.setXml(entityBody);
        }  else if (entityBody is byte[]) {
            self.setByteArray(entityBody);
        } else if (entityBody is json) {
            self.setJson(entityBody);
        } else if(entityBody is io:ReadableByteChannel) {
            self.setByteChannel(entityBody);
        } else {
            self.setBodyParts(entityBody);
        }
    }

# Sets the entity body with a given file. This method overrides any existing `content-type` headers
# with the default content-type, which is `application/octet-stream`. This default value
# can be overridden by passing the content type as an optional parameter.
# ```ballerina
# mimeEntity.setFileAsEntityBody("<file path>");
# ```
#
# + filePath - Path of the file
# + contentType - Content type to be used with the payload. This is an optional parameter.
#                 The default value is `application/octet-stream`
    public function setFileAsEntityBody(@untainted string filePath, public string contentType = "application/octet-stream") {
        io:ReadableByteChannel byteChannel = checkpanic io:openReadableFile(filePath);
        self.setByteChannel(byteChannel, contentType = contentType);
    }

# Sets the entity body with the given `json` content. This method overrides any existing `content-type` headers
# with the default content-type, which is `application/json`. This default value can be overridden
# by passing the content type as an optional parameter.
# ```ballerina
# mimeEntity.setJson({ "Hello": "World" });
# ```
#
# + jsonContent - JSON content, which needs to be set to the entity
# + contentType - Content type to be used with the payload. This is an optional parameter. 
#                The default value is `application/json`
    public function setJson(@untainted json jsonContent, @untainted public string contentType = "application/json") {
        return externSetJson(self, jsonContent, java:fromString(contentType));
    }

    # Extracts the JSON body from the entity.
    #
    # + return - `json` data extracted from the entity body or else an `mime:ParserError` if the entity body is not a JSON
    public function getJson() returns @tainted json|ParserError {
        return externGetJson(self);
    }

# Sets the entity body with the given XML content. This method overrides any existing content-type headers
# with the default content-type, which is `application/xml`. This default value can be overridden
# by passing the content-type as an optional parameter.
# ```ballerina
# mimeEntity.setXml(xml `<hello> world </hello>`);
# ```
#
# + xmlContent - XML content, which needs to be set to the entity
# + contentType - Content type to be used with the payload. This is an optional parameter. 
#               The default value is `application/xml` 
    public function setXml(@untainted xml xmlContent, @untainted public string contentType = "application/xml") {
        return externSetXml(self, xmlContent, java:fromString(contentType));
    }

    # Extracts the `xml` body from the entity.
    #
    # + return - `xml` data extracted from the entity body or else an `mime:ParserError` if the entity body is not an XML
    public function getXml() returns @tainted xml|ParserError {
        return externGetXml(self);
    }

# Sets the entity body with the given text content. This method overrides any existing content-type headers
# with the default content-type, which is `text/plain`. This default value can be overridden
# by passing the content type as an optional parameter.
# ```ballerina
# mimeEntity.setText("Hello World");
# ```
#
# + textContent - Text content, which needs to be set to the entity
# + contentType - Content type to be used with the payload. This is an optional parameter. 
#                The default value is `text/plain` 
    public function setText(@untainted string textContent, @untainted public string contentType = "text/plain") {
        return externSetText(self, java:fromString(textContent), java:fromString(contentType));
    }

    # Extracts the text body from the entity. If the entity body is not text compatible, an error is returned.
    #
    # + return - `string` data extracted from the the entity body or else an `mime:ParserError` if the entity body is not
    #            text compatible
    public function getText() returns @tainted string|ParserError {
        var returnVal = externGetText(self);
        if (returnVal is ParserError) {
            return returnVal;
        } else {
            return <string>java:toString(returnVal);
        }
    }

    # Sets the entity body with the given byte[] content. This method overrides any existing `content-type` headers
    # with the default content-type, which is `application/octet-stream`. This default value 
    # can be overridden by passing the content type as an optional parameter.
    #
    # + blobContent - byte[] content that needs to be set to the entity
    # + contentType - Content type to be used with the payload. This is an optional parameter.
    #                 The default value is `application/octet-stream` 
    public function setByteArray(@untainted byte[] blobContent, @untainted public string contentType =
                                                                "application/octet-stream") {
        return externSetByteArray(self, blobContent, java:fromString(contentType));
    }

    # Gets the entity body as a `byte[]` from a given entity. If the entity size is considerably large, consider
    # using the getByteChannel() method instead.
    #
    # + return - `byte[]` data extracted from the the entity body or else a `mime:ParserError` in case of
    #            errors
    public function getByteArray() returns @tainted byte[]|ParserError {
        return externGetByteArray(self);
    }

    # Sets the entity body with the given byte channel content. This method overrides any existing content-type headers
    # with the default content-type, which is `application/octet-stream`. This default value
    # can be overridden by passing the content-type as an optional parameter.
    #
    # + byteChannel - Byte channel, which needs to be set to the entity
    # + contentType - Content-type to be used with the payload. This is an optional parameter.
    #                 The `application/octet-stream` is the default value
    public function setByteChannel(io:ReadableByteChannel byteChannel, @untainted public string contentType =
                                                                                    "application/octet-stream") {
        return externSetByteChannel(self, byteChannel, java:fromString(contentType));
    }

    # Gets the entity body as a byte channel from a given entity.
    #
    # + return - An `io:ReadableByteChannel` or else a `mime:ParserError` record will be returned in case of errors
    public function getByteChannel() returns @tainted io:ReadableByteChannel|ParserError {
        return externGetByteChannel(self);
    }

    # Gets the body parts from a given entity.
    #
    # + return - An array of body parts(`Entity[]`) extracted from the entity body or else a `mime:ParserError` if the
    #            entity body is not a set of the body parts
    public function getBodyParts() returns Entity[]|ParserError {
        return externGetBodyParts(self);
    }

    # Gets the body parts as a byte channel from a given entity.
    #
    # + return - Body parts as a byte channel
    public function getBodyPartsAsChannel() returns @tainted io:ReadableByteChannel|ParserError {
        return externGetBodyPartsAsChannel(self);
    }

    # Sets body parts to entity. This method overrides any existing `content-type` headers
    # with the default `multipart/form-data` content-type. The default `multipart/form-data` value can be overridden
    # by passing the content type as an optional parameter.
    #
    # + bodyParts - Body parts, which needs to be set to the entity
    # + contentType - Content-type to be used with the payload. This is an optional parameter.
    #                The default value is `multipart/form-data`.
    public function setBodyParts(@untainted Entity[] bodyParts, @untainted public string contentType =
                                                                                "multipart/form-data") {
        return externSetBodyParts(self, bodyParts, java:fromString(contentType));
    }

# Gets the header value associated with the given header name.
# ```ballerina
# string headerName = mimeEntity.getHeader(mime:CONTENT_LENGTH);
# ```
#
# + headerName - Header name
# + position - Position of the header as an optional parameter
# + return - Header value associated with the given header name as a `string`. If multiple header values are
#            present, then the first value is returned. An exception is thrown if no header is found. Use
#            `Entity.hasHeader()` beforehand to check the existence of a header
    public function getHeader(@untainted string headerName, public HeaderPosition position = LEADING)
                                                                                returns @tainted string {
        return <string>java:toString(externGetHeader(self, java:fromString(headerName), position));
    }

    # Gets all the header values associated with the given header name.
    #
    # + headerName - Header name
    # + position - Position of the header as an optional parameter. If the position is `mime:TRAILING`,
    #              the body of the `Entity` must be accessed initially
    # + return - All the header values associated with the given header name as a `string[]`. Panics if no header is
    #            found. Use the `Entity.hasHeader()` beforehand to check the existence of a header
    public function getHeaders(@untainted string headerName, public HeaderPosition position = LEADING)
                                                                                returns @tainted string[] {
        handle[] headerValues = externGetHeaders(self, java:fromString(headerName), position);
        string[] headers = [];
        int index = 0;
        foreach var headerValue in headerValues {
            headers[index] = <string>java:toString(headerValue);
            index = index + 1;
        }
        return headers;
    }

# Gets all the header names.
# ```ballerina
# string[] headerNames = mimeEntity.getHeaderNames();
# ```
#
# + position - Position of the header as an optional parameter
# + return - All header names as a `string[]`
    public function getHeaderNames(public HeaderPosition position = LEADING) returns @tainted string[] {
        handle[] headerNames = externGetHeaderNames(self, position);
        string[] headers = [];
        int index = 0;
        foreach var headerName in headerNames {
            headers[index] = <string>java:toString(headerName);
            index = index + 1;
        }
        return headers;
    }

# Adds the given header value against the given header.
# ```ballerina
# mimeEntity.addHeader("custom-header", "header-value");
# ```
#
# + headerName - Header name
# + headerValue - The header value to be added
# + position - Position of the header as an optional parameter
    public function addHeader(@untainted string headerName, string headerValue, public HeaderPosition position = LEADING) {
        return externAddHeader(self, java:fromString(headerName), java:fromString(headerValue), position);
    }

# Sets the given header value against the existing header. If a header already exists, its value is replaced
# with the given header value.
# ```ballerina
# mimeEntity.setHeader("custom-header", "header-value");
# ```
#
# + headerName - Header name
# + headerValue - Header value
# + position - Position of the header as an optional parameter
    public function setHeader(@untainted string headerName, string headerValue, public HeaderPosition position = LEADING) {
        return externSetHeader(self, java:fromString(headerName), java:fromString(headerValue), position);
    }

    # Removes the given header from the entity.
    #
    # + headerName - Header name
    # + position - Position of the header as an optional parameter. If the position is `mime:TRAILING`,
    #              the body of the `Entity` must be accessed initially.
    public function removeHeader(@untainted string headerName, public HeaderPosition position = LEADING) {
        return externRemoveHeader(self, java:fromString(headerName), position);
    }

    # Removes all headers associated with the entity.
    #
    # + position - Position of the header as an optional parameter. If the position is `mime:TRAILING`,
    #              the body of the `Entity` must be accessed initially.
    public function removeAllHeaders(public HeaderPosition position = LEADING) {
        return externRemoveAllHeaders(self, position);
    }

    # Checks whether the requested header key exists in the header map.
    #
    # + headerName - Header name
    # + position - Position of the header as an optional parameter. If the position is `mime:TRAILING`,
    #              the body of the `Entity` must be accessed initially.
    # + return - `true` if the specified header key exists
    public function hasHeader(@untainted string headerName, public HeaderPosition position = LEADING) returns boolean {
        return externHasHeader(self, java:fromString(headerName), position);
    }
};

function externSetJson(Entity entity, json jsonContent, handle contentType) = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "setJson"
} external;

function externGetJson(Entity entity) returns @tainted json|ParserError = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeDataSourceBuilder",
    name: "getJson"
} external;

function externSetXml(Entity entity, xml xmlContent, handle contentType) = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "setXml"
} external;

function externGetXml(Entity entity) returns @tainted xml|ParserError = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeDataSourceBuilder",
    name: "getXml"
} external;

function externSetText(Entity entity, handle textContent, handle contentType) = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "setText"
} external;

function externGetText(Entity entity) returns @tainted handle|ParserError = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeDataSourceBuilder",
    name: "getText"
} external;

function externSetByteArray(Entity entity, byte[] byteArray, handle contentType) = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "setByteArray"
} external;

function externGetByteArray(Entity entity) returns @tainted byte[]|ParserError = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeDataSourceBuilder",
    name: "getByteArray"
} external;

function externSetByteChannel(Entity entity, io:ReadableByteChannel byteChannel, handle contentType) = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "setByteChannel"
} external;

function externGetByteChannel(Entity entity) returns @tainted io:ReadableByteChannel|ParserError = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "getByteChannel"
} external;

function externSetBodyParts(Entity entity, Entity[] bodyParts, handle contentType) = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "setBodyParts"
} external;

function externGetBodyParts(Entity entity) returns Entity[]|ParserError = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "getBodyParts"
} external;

function externGetBodyPartsAsChannel(Entity entity) returns @tainted io:ReadableByteChannel|ParserError = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "getBodyPartsAsChannel"
} external;

function externGetHeader(Entity entity, handle headerName, HeaderPosition position) returns @tainted handle =
@java:Method {
    class: "org.ballerinalang.mime.nativeimpl.EntityHeaders",
    name: "getHeader"
} external;

function externGetHeaders(Entity entity, handle headerName, HeaderPosition position) returns @tainted handle[] =
@java:Method {
    class: "org.ballerinalang.mime.nativeimpl.EntityHeaders",
    name: "getHeaders"
} external;

function externGetHeaderNames(Entity entity, HeaderPosition position) returns @tainted handle[] = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.EntityHeaders",
    name: "getHeaderNames"
} external;

function externAddHeader(Entity entity, handle headerName, handle headerValue, HeaderPosition position) =
@java:Method {
    class: "org.ballerinalang.mime.nativeimpl.EntityHeaders",
    name: "addHeader"
} external;

function externSetHeader(Entity entity, handle headerName, handle headerValue, HeaderPosition position) =
@java:Method {
    class: "org.ballerinalang.mime.nativeimpl.EntityHeaders",
    name: "setHeader"
} external;

function externRemoveHeader(Entity entity, handle headerName, HeaderPosition position) = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.EntityHeaders",
    name: "removeHeader"
} external;

function externRemoveAllHeaders(Entity entity, HeaderPosition position) = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.EntityHeaders",
    name: "removeAllHeaders"
} external;

function externHasHeader(Entity entity, handle headerName, HeaderPosition position) returns boolean = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.EntityHeaders",
    name: "hasHeader"
} external;

# **Deprecated API**. Encodes a given input with MIME specific Base64 encoding scheme.
#
# + contentToBeEncoded - Content that needs to be encoded can be of type `string`, `byte[]` or `io:ReadableByteChannel`
# + charset - Charset to be used. This is used only with the string input
# + return - An encoded `string` if the given input is of type string, an encoded `byte[]` if the given input is of
#            type byte[], an encoded `io:ReadableByteChannel` if the given input is of type `io:ReadableByteChannel`,
#            or else a `mime:EncodeError` record in case of errors
public function base64Encode((string|byte[]|io:ReadableByteChannel) contentToBeEncoded, string charset = "utf-8")
                returns (string|byte[]|io:ReadableByteChannel|EncodeError) {
    return externBase64Encode(contentToBeEncoded, java:fromString(charset));
}

function externBase64Encode((string|byte[]|io:ReadableByteChannel) contentToBeEncoded, handle charset) returns
                                                                (string|byte[]|io:ReadableByteChannel|EncodeError) =
@java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeBase64",
    name: "base64Encode"
} external;

# **Deprecated API**. Decodes a given input with MIME specific Base64 encoding scheme.
#
# + contentToBeDecoded - Content that needs to be decoded can be of type `string`, `byte[]` or `io:ReadableByteChannel`
# + charset - Charset to be used. This is used only with the string input
# + return - A decoded `string` if the given input is of type string, a decoded `byte[]` if the given input is of
#            type byte[], a decoded `io:ReadableByteChannel` if the given input is of type io:ReadableByteChannel
#            or else a `mime:DecodeError` in case of errors
public function base64Decode((string|byte[]|io:ReadableByteChannel) contentToBeDecoded, string charset = "utf-8")
    returns (string|byte[]|io:ReadableByteChannel|DecodeError) {
    return externBase64Decode(contentToBeDecoded, java:fromString(charset));
}

function externBase64Decode((string|byte[]|io:ReadableByteChannel) contentToBeDecoded, handle charset) returns
                                                                (string|byte[]|io:ReadableByteChannel|DecodeError) =
@java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeBase64",
    name: "base64Decode"
} external;

# **Deprecated API**. Encodes a given byte[] using the Base64 encoding scheme.
#
# + valueToBeEncoded - Content, which needs to be encoded
# + return - An encoded byte[] or else a `mime:EncodeError` record in case of errors
public function base64EncodeBlob(byte[] valueToBeEncoded) returns byte[]|EncodeError {
    var result = base64Encode(valueToBeEncoded);
    if (result is byte[]|EncodeError) {
        return result;
    } else {
        return prepareEncodingErrorWithDetail("Error occurred while encoding byte[]");
    }
}

# **Deprecated API**. Decodes a given byte[] using the Base64 encoding scheme.
#
# + valueToBeDecoded - Content, which needs to be decoded
# + return - A decoded `byte[]` or else a `mime:DecodeError` record in case of errors
public function base64DecodeBlob(byte[] valueToBeDecoded) returns byte[]|DecodeError {
    var result = base64Decode(valueToBeDecoded);
    if (result is byte[]|DecodeError) {
        return result;
    } else {
        return prepareDecodingErrorWithDetail("Error occurred while decoding byte[]");
    }
}

# Gets the encoding value from a given MediaType.
#
# + contentType - A MediaType struct
# + return - The encoding value as a `string`
function getEncoding(MediaType contentType) returns (string?) {
    return contentType.parameters[CHARSET];
}

# Gets the `MediaType` object populated with it when the `Content-Type` is in string.
#
# + contentType - Content-Type in string
# + return - `MediaType` object or else a `mime:InvalidContentTypeError` in case of an invalid content-type
public function getMediaType(string contentType) returns MediaType|InvalidContentTypeError {
    return externGetMediaType(java:fromString(contentType));
}

function externGetMediaType(handle contentType) returns MediaType|InvalidContentTypeError = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.MimeEntityBody",
    name: "getMediaType"
} external;

# Given the Content-Disposition as a string, gets the ContentDisposition object with it.
#
# + contentDisposition - Content disposition string
# + return - A `ContentDisposition` object
public function getContentDispositionObject(string contentDisposition) returns ContentDisposition {
    return externGetContentDispositionObject(java:fromString(contentDisposition));
}

function externGetContentDispositionObject(handle contentType) returns ContentDisposition = @java:Method {
    class: "org.ballerinalang.mime.nativeimpl.ContentDisposition",
    name: "getContentDispositionObject"
} external;
