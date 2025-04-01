import * as vscode from 'vscode';
import { DomainWorkspace } from '../types';
import { removeDomainWorkspace, getDomainWorkspaces } from '../configFileInteraction';

export class CppsDomainWorkspaceViewProvider implements vscode.WebviewViewProvider {

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
                    vscode.commands.executeCommand('bachelorarbeit.showDomainWorkspaceCreateEditPanel', 'create');
                    break;
                case 'removeDomainWorkspace':
                    removeDomainWorkspace(message.dwID);
                    this._updateWebviewContent();
                    break;
                case 'editDomainWorkspace':
                    vscode.commands.executeCommand('bachelorarbeit.showDomainWorkspaceCreateEditPanel', 'edit', message.dwID);
                    break;
                case 'refresh':
                    this._updateWebviewContent();
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

        const domainWorkspaces: DomainWorkspace[] = getDomainWorkspaces();

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
                    <h3><u>Domain Workspaces</u></h3>
                    <button id="refreshButton" style="width: auto !important; margin-left: auto;">
                        <img src="${refreshIconUri}" alt="Refresh" style="width:16px;height:16px;">
                    </button>
                </div>
                <br>
                <table>
                    <tr>
                        <th>Name</th>
                        <th>User count</th>
                        <th>Actions</th>
                    </tr>`;
            
        let htmlStringMiddle = '';

        const editIconUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'assets', 'edit.svg')
        );

        const removeIconUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'assets', 'remove.svg')
        );

        for (let i = 0; i < domainWorkspaces.length; i++){

            const userCount = domainWorkspaces[i].users.length;

            htmlStringMiddle += `
                <tr>
                    <td>${domainWorkspaces[i].name}</td>
                    <td>${userCount}</td>
                    <td>
                        <div class="actions">
                            <button class="editButton" data-domain-workspace-id="${domainWorkspaces[i].id}">
                                <img src="${editIconUri}" alt="Edit" style="width:16px;height:16px;">
                            </button>
                            <button class="removeButton" data-domain-workspace-id="${domainWorkspaces[i].id}">
                                <img src="${removeIconUri}" alt="Remove" style="width:16px;height:16px;">
                            </button>
                        </div>
                    </td>
                </tr>`;
        }

        const htmlStringEnd = 
        `
            </table>
            <br>
            <button id="addDomainWorkspaceButton">
                Add a new domain workspace
            </button>
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