import * as vscode from 'vscode';
import { CommonWorkspace } from '../types';

export class CppsCommonWorkspaceViewProvider implements vscode.WebviewViewProvider {

    constructor(private readonly _extensionUri: vscode.Uri) { }
    _view?: vscode.WebviewView;

    public resolveWebviewView(webviewView: vscode.WebviewView) {

        this._view = webviewView;

        webviewView.webview.options = {
            enableScripts: true,
            localResourceRoots: [this._extensionUri],
        };

        this._updateWebviewContent();

        webviewView.webview.onDidReceiveMessage(message => {
            switch (message.command) {
                case 'addDomainWorkspace':
                    //vscode.commands.executeCommand('bachelorarbeit.showDomainWorkspaceCreateEditPanel', 'create');
                    break;
                case 'removeDomainWorkspace':
                    this._updateWebviewContent();
                    break;
                case 'editDomainWorkspace':
                    //vscode.commands.executeCommand('bachelorarbeit.showDomainWorkspaceCreateEditPanel', 'edit', message.dwID);
                    break;
            }
        });
    }

    public triggerUpdate() {
        this._updateWebviewContent();
    }

    private _updateWebviewContent() {
        if (this._view) {
            this._view.webview.html = this._getHtmlForWebview(this._view.webview);
        }
    }

    private _getHtmlForWebview(webview: vscode.Webview) {
        const styleVSCodeUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, "media", "vscode.css")
        );

        const styleTableUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, "media", "table.css")
        );

        const commonWorkspace: CommonWorkspace = {
            name: 'Common Workspace',
            users: ["1", "2", "3"]
        };

        const refreshIconUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'assets', 'refresh.svg')
        );
        
        const htmlStringBeging = `<!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link href="${styleVSCodeUri}" rel="stylesheet">
                <link href="${styleTableUri}" rel="stylesheet">
            </head>
            <body>
                <div style="display: flex; align-items: center;">
                    <h3><u>Common Workspace</u></h3>
                </div>
                <br>`;
            
        let htmlStringMiddle = '';

        const editIconUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'assets', 'edit.svg')
        );

        const removeIconUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'assets', 'remove.svg')
        );



        htmlStringMiddle += `
            <div class="actions">
                <button class="editButton">
                    <img src="${editIconUri}" alt="Edit" style="width:16px;height:16px;">
                </button>
                <button class="removeButton">
                    <img src="${removeIconUri}" alt="Remove" style="width:16px;height:16px;">
                </button>
            </div>`;

        const htmlStringEnd = 
        `
            <br>
            <script>
                const vscode = acquireVsCodeApi();

                document.getElementById('addDomainWorkspaceButton').addEventListener('click', () => {
                    console.log('Add Domain Workspace Button clicked');
                    vscode.postMessage({ command: 'addDomainWorkspace' });
                });

                document.querySelectorAll('.editButton').forEach(button => {
                    button.addEventListener('click', (event) => {
                        const dwID = event.currentTarget.getAttribute('data-domain-workspace-id');
                        vscode.postMessage({ command: 'editDomainWorkspace', dwID });
                    });
                });

                document.querySelectorAll('.removeButton').forEach(button => {
                    button.addEventListener('click', (event) => {
                        const dwID = event.currentTarget.getAttribute('data-domain-workspace-id');
                        vscode.postMessage({ command: 'removeDomainWorkspace', dwID });
                    });
                });
                
                document.getElementById('refreshButton').addEventListener('click', () => {
                    vscode.postMessage({ command: 'refresh' });
                });
                
            </script>
        </body>
        </html>`;

        return htmlStringBeging + htmlStringMiddle + htmlStringEnd;
    }

}