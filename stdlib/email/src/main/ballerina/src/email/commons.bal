// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/mime;

# Email message properties.
#
# + to - TO address list
# + cc - CC address list
# + bcc - BCC address list
# + subject - Subject of email
# + body - Body of the email message
# + from - From address
# + sender - Sender's address
# + replyTo - Reply To addresses
# + attachments - Email attachements
public type Email record {|
    string[] to;
    string[] cc?;
    string[] bcc?;
    string subject;
    string body;
    string 'from;
    string sender?;
    string[] replyTo?;
    mime:Entity[] attachments?;
|};

# Default folder to read emails.
public const DEFAULT_FOLDER = "INBOX";
