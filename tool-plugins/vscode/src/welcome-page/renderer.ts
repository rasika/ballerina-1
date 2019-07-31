/**
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
import {ExtensionContext} from 'vscode';
import {getComposerWebViewOptions, getLibraryWebViewContent} from '../utils';

export function render(context: ExtensionContext, pluginVersion: string): string {
    return renderPage(context, pluginVersion);
}

function renderPage(context: ExtensionContext, pluginVersion: string): string {
    const body = `
    <!DOCTYPE html>
    <html>
    
    <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>
    <body>
    <div class="ui container">
        <p>
        <h1 class="ui header huge">Ballerina</h1>
        In the cloud, applications interact with their environment over the network. Ballerina is a general-purpose programming language with features optimized for these network distributed applications. It is easy to write and modify and is suitable for application programmers. 
        </p>
        <p>
        <div class="ui vertically divided grid">
            <div class="two column row">
                <div class="twelve wide column">
                    <div class="ui header huge">
                        What's New in <div class="ui header violet horizontal huge label">Ballerina ${pluginVersion}</div>
                    </div>
                    </p>
                    <p>See the <a>release note</a> for the full set of changes</p>
                    <p>
                    <div class="ui divided selection list">
                    <a class="item">
                        <div class="ui blue horizontal label">FIXED</div>
                        Fix NPE of hover operation for the variable definitions
                    </a>
                    <a class="item">
                        <div class="ui blue horizontal label">IMPROVED</div>
                        Improve error handling in language server services
                    </a>
                    <a class="item">
                        <div class="ui green horizontal label">NEW</div>
                        Introduce Ballerina http error types
                    </a>
                    <a class="item">
                        <div class="ui blue horizontal label">IMPROVED</div>
                        Improve Encoding APIs with error types
                    </a>
                    </div>
                    </p>
                </div>
                <div class="four wide column">
                  <div role="list" class="ui divided middle aligned list">
                      <div role="listitem" class="item">
                        <div class="content"><a class="header">Write a Review</a></div>
                      </div>
                      <div role="listitem" class="item">
                        <div class="content"><a class="header">Star me on Github</a></div>
                      </div>
                      <div role="listitem" class="item">
                        <div class="content"><a class="header">Follow me on Twitter</a></div>
                      </div>
                    <h2 class="ui header">Help</h2>
                    <div role="listitem" class="item">
                        <div class="content"><a class="header">Documentation</a></div>
                      </div>
                      <div role="listitem" class="item">
                        <div class="content"><a class="header">Questions &amp; Issues</a></div>
                      </div>
                      <div role="listitem" class="item">
                        <div class="content"><a class="header">Slack</a></div>
                      </div>
                    <h2 class="ui header">Resources</h2>
                    <div role="listitem" class="item">
                        <div class="content"><a class="header">Website</a></div>
                      </div>
                      <div role="listitem" class="item">
                        <div class="content"><a class="header">Changelog</a></div>
                      </div>
                      <div role="listitem" class="item">
                        <div class="content"><a class="header">Marketplace</a></div>
                      </div>
                       <div role="listitem" class="item">
                        <div class="content"><a class="header">Github</a></div>
                      </div>
                       <div role="listitem" class="item">
                        <div class="content"><a class="header">License</a></div>
                      </div>
                    </div>
                </div>
            </div>
    </div>
    </body>
    </html>
    `;

    const bodyCss = "diagram";

    const styles = `
        body {
            background: #f1f1f1;
        }
        .overlay {
            display: none;
        }
        .drop-zone.rect {
            fill-opacity: 0;
        }
        #diagram {
            height : 100%;
        }
        #errors {
            display: table;
            width: 100%;
            height: 100%;
        }
        #errors span { 
            display: table-cell;
            vertical-align: middle;
            text-align: center;
        }
        #warning {
            position: absolute;
            top: 15px;
            position: absolute;
            overflow: hidden;
            height: 25px;
            vertical-align: bottom;
            text-align: center;
            color: rgb(255, 90, 30);
            width: 100%;
        }
        #warning p {
            line-height: 25px;
        }
    `;

    const scripts = ``;

    return getLibraryWebViewContent({ ...getComposerWebViewOptions(), body, scripts, styles, bodyCss });
}

export function renderError() {
    return `
    <!DOCTYPE html>
    <html>
    
    <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
    </head>
    <body>
    <div>
        Error while rendering. Please try again after restarting vscode.
        <a href="command:workbench.action.reloadWindow">Restart</a>
    </div>
    </body>
    </html>
    `;
}
