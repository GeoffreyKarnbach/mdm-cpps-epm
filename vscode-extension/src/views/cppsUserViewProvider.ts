import * as vscode from 'vscode';
import { getUserList, removeUser } from '../configFileInteraction';
import { User } from '../types';

export class CppsUserViewProvider implements vscode.WebviewViewProvider {

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
                case 'addUser':
                    vscode.commands.executeCommand('bachelorarbeit.showUserCreateEditPanel', 'create');
                    break;
                case 'removeUser':
                    removeUser(message.userId);
                    this._updateWebviewContent();
                    break;
                case 'editUser':
                    vscode.commands.executeCommand('bachelorarbeit.showUserCreateEditPanel', 'edit', message.userId);
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

        const userList: User[] = getUserList();

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
                    <h3><u>User List</u></h3>
                    <button id="refreshButton" style="width: auto !important; margin-left: auto;">
                        <img src="${refreshIconUri}" alt="Refresh" style="width:16px;height:16px;">
                    </button>
                </div>
                <br>
                <table>
                    <tr>
                        <th>Git ID</th>
                        <th>Git Name</th>
                        <th>Username</th>
                        <th>Reviewer</th>
                        <th>Actions</th>
                    </tr>`;
            
        let htmlStringMiddle = '';

        const editIconUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'assets', 'edit.svg')
        );

        const removeIconUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'assets', 'remove.svg')
        );

        const trueIconUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'assets', 'true.svg')
        );

        const falseIconUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, 'assets', 'false.svg')
        );

        for (let i = 0; i < userList.length; i++){

            const isReviewerIconUri = userList[i].isReviewer ? trueIconUri : falseIconUri;

            htmlStringMiddle += `
                <tr>
                    <td>${userList[i].gitUserId}</td>
                    <td>${userList[i].gitUsername}</td>
                    <td>${userList[i].username}</td>
                    <td>
                        <img src="${isReviewerIconUri}" alt="Reviewer" style="width:16px;height:16px; display: block; margin-left: auto; margin-right: auto;">
                    </td>
                    <td>
                        <div class="actions">
                            <button class="editButton" data-user-id="${userList[i].id}">
                                <img src="${editIconUri}" alt="Edit" style="width:16px;height:16px;">
                            </button>
                            <button class="removeButton" data-user-id="${userList[i].id}">
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
            <button id="addUserButton">
                Add a new user
            </button>
            <br>
            <script>
                const vscode = acquireVsCodeApi();
                document.getElementById('addUserButton').addEventListener('click', () => {
                    vscode.postMessage({ command: 'addUser' });
                });

                document.querySelectorAll('.editButton').forEach(button => {
                    button.addEventListener('click', (event) => {
                        const userId = event.currentTarget.getAttribute('data-user-id');
                        vscode.postMessage({ command: 'editUser', userId });
                    });
                });

                document.querySelectorAll('.removeButton').forEach(button => {
                    button.addEventListener('click', (event) => {
                        const userId = event.currentTarget.getAttribute('data-user-id');
                        vscode.postMessage({ command: 'removeUser', userId });
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