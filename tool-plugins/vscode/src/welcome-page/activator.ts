import * as _ from 'lodash';

import { BallerinaExtension } from 'src/core';
import { ExtensionContext, commands, window, ViewColumn, WebviewPanel, extensions } from 'vscode';

import { render } from './renderer';
import { getCommonWebViewOptions } from '../utils';

let welcomePanel: WebviewPanel | undefined;

export function activate(ballerinaExtInstance: BallerinaExtension) {
    let context = <ExtensionContext>ballerinaExtInstance.context;
    const welcomeDisposable = commands.registerCommand('ballerina.showWelcomePage', () => {
        return ballerinaExtInstance.onReady()
            .then(() => {
                if (welcomePanel) {
                    welcomePanel.reveal();
                } else {
                    const pluginVersion = extensions.getExtension('ballerina.ballerina')!.packageJSON.version.split('-')[0];
                    openWebView(context, pluginVersion);
                }
            });
    });
    context.subscriptions.push(welcomeDisposable);
}

function openWebView(context: ExtensionContext, pluginVersion: string) {
    if (!window.activeTextEditor) {
        return;
    }

    if (!welcomePanel) {
        welcomePanel = window.createWebviewPanel(
            'welcomePage',
            'Welcome to Ballerina',
            { viewColumn: ViewColumn.One, preserveFocus: true },
            getCommonWebViewOptions()
        );
    }

    const editor = window.activeTextEditor;
    if (!editor) {
        return;
    }

    const html = render(context, pluginVersion);
    if (welcomePanel && html) {
        welcomePanel.webview.html = html;
    }

    welcomePanel.onDidDispose(() => {
        welcomePanel = undefined;
    });
}