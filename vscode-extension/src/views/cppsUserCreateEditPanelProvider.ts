import * as vscode from 'vscode';
import { User } from '../types';
import { getUser, editUser, addUser } from '../configFileInteraction';

export class CppsUserCreateEditPanelProvider{

    constructor(private readonly _extensionUri: vscode.Uri) { }
    _view?: vscode.WebviewView;
    editType: string = '';
    id: string = '';

    user: User = {
        id: '',
        gitUserId: '',
        gitUsername: '',
        username: '',
        isReviewer: false
    };

    public show(editType?: string, id?: string) {

        this.editType = editType ? editType : '';
        this.id = id ? id : '';

        const panel = vscode.window.createWebviewPanel(
            'userCreateEdit',
            'User Create / Edit',
            vscode.ViewColumn.One,
            {
                enableScripts: true,
                localResourceRoots: [this._extensionUri]
            }
        );

        if (this.editType === 'edit') {
            const user = getUser(this.id);
            if (user) {
                this.user = user;
            } else {
                vscode.window.showErrorMessage('User not found!');
                return;
            }
        } else {
            this.user = {
                id: '',
                gitUserId: '',
                gitUsername: '',
                username: '',
                isReviewer: false
            };
        }

        panel.webview.html = this._getHtmlForWebview(panel.webview);

        panel.webview.onDidReceiveMessage(message => {
            switch (message.command) {
                case 'submit':
                    if (this.editType === 'create') {
                        addUser({
                            id: '',
                            gitUserId: message.gitUserId,
                            gitUsername: message.gitUsername,
                            username: message.username,
                            isReviewer: message.isReviewer
                        });
                    } else if (this.editType === 'edit') {
                        editUser({
                            id: this.id,
                            gitUserId: message.gitUserId,
                            gitUsername: message.gitUsername,
                            username: message.username,
                            isReviewer: message.isReviewer
                        }, this.id);
                    }

                    panel.dispose();
                    break;
            }
        });
    }

    private _getHtmlForWebview(webview: vscode.Webview) {
        const styleVSCodeUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, "media", "vscode.css")
          );

        const editMode: string = this.editType === 'create' ? 'Create' : 'Edit';

        const { gitUserId, gitUsername, username, isReviewer } = this.user;

        return `<!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link href="${styleVSCodeUri}" rel="stylesheet">
            </head>
            <body>
                <h1><u>${editMode} user</u></h1>
                <br>
                <form id="userForm">
                    <label for="name">Git User ID</label>
                    <br>
                    <input type="number" id="gitUserId" name="gitUserId" placeholder="Enter the Git User ID" value="${gitUserId}" required>
                    <br>
                    <label for="description">Git Username</label>
                    <br>
                    <input type="text" id="gitUsername" name="gitUsername" placeholder="Enter Git Username" value="${gitUsername}" required></input>
                    <br>
                    <label for="description">Username</label>
                    <br>
                    <input type="text" id="username" name="username" placeholder="Enter Username" value="${username}" required></input>
                    <br>
                    <label for="version">Is Reviewer?</label>
                    <input type="checkbox" id="isReviewer" name="isReviewer" ${isReviewer ? 'checked' : ''}>
                    <br><br>
                    <button type="button" id="submit">${editMode} user</button>
                </form>

                <script>
                    const vscode = acquireVsCodeApi();

                    document.getElementById('submit').addEventListener('click', () => {
                        const gitUserId = document.getElementById('gitUserId').value;
                        const gitUsername = document.getElementById('gitUsername').value;
                        const username = document.getElementById('username').value;
                        const isReviewer = document.getElementById('isReviewer').checked;

                        console.log(gitUserId, gitUsername, username, isReviewer);

                        vscode.postMessage({
                            command: 'submit',
                            gitUserId: gitUserId,
                            gitUsername: gitUsername,
                            username: username,
                            isReviewer: isReviewer,
                            mode: '${this.editType}'
                        });
                    });
                </script>
            </body>
            </html>`;
    }

}