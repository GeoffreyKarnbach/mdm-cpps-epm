import * as vscode from 'vscode';
import { DomainWorkspace, User } from '../types';
import { addDomainWorkspace, editDomainWorkspace, getDomainWorkspace, getUserList } from '../configFileInteraction';

export class CppsDomainWorkspaceCreateEditPanelProvider {

    constructor(private readonly _extensionUri: vscode.Uri) { }
    _view?: vscode.WebviewView;
    editType: string = '';
    id: string = '';
    isCommonWorkspace: boolean = false;

    domainWorkspace: DomainWorkspace = {
        id: '',
        name: '',
        users: []
    };

    public show(editType?: string, id?: string, isCommonWorkspace?: boolean) {

        this.editType = editType ? editType : '';
        this.id = id ? id : '';
        this.isCommonWorkspace = isCommonWorkspace ? isCommonWorkspace : false;

        const panel = vscode.window.createWebviewPanel(
            'domainWorkspaceCreateEdit',
            'Domain Workspace Create / Edit',
            vscode.ViewColumn.One,
            {
                enableScripts: true,
                localResourceRoots: [this._extensionUri]
            }
        );

        if (this.editType === 'edit') {
            const dw : DomainWorkspace | null = getDomainWorkspace(this.id);
            if (dw){
                this.domainWorkspace = dw;
            } else {
                vscode.window.showErrorMessage('Domain Workspace not found!');
                panel.dispose();
            }
        } else {
            this.domainWorkspace = {
                id: '',
                name: '',
                users: []
            };
        }

        panel.webview.html = this._getHtmlForWebview(panel.webview);

        panel.webview.onDidReceiveMessage(message => {
            switch (message.command) {
                case 'submit':
                    if (this.editType === 'create') {
                        addDomainWorkspace({
                            id: '',
                            name: message.name,
                            users: message.users
                        });
                    } else if (this.editType === 'edit') {
                        editDomainWorkspace({
                            id: this.id,
                            name: message.name,
                            users: message.users
                        }, this.id);
                    }

                    panel.dispose();
                    break;
                case 'removeUser':
                    let index = this.domainWorkspace.users.indexOf(message.id);
                    if (index > -1) {
                        this.domainWorkspace.users.splice(index, 1);
                    }
                    this.domainWorkspace.name = message.name;
                    console.log(this.domainWorkspace);
                    panel.webview.html = this._getHtmlForWebview(panel.webview);
                    break;
                case 'addUser':
                    this.domainWorkspace.users.push(message.id);
                    this.domainWorkspace.name = message.name;
                    console.log(this.domainWorkspace);
                    panel.webview.html = this._getHtmlForWebview(panel.webview);
                    break;
            }
        });
    }

    private _getHtmlForWebview(webview: vscode.Webview) {
        const styleVSCodeUri = webview.asWebviewUri(
            vscode.Uri.joinPath(this._extensionUri, "media", "vscode.css")
          );

        const editMode: string = this.editType === 'create' ? 'Create' : 'Edit';

        const { id, name, users } = this.domainWorkspace;

        const userList: User[] = getUserList();

        let usersInDomainWorkspace: User[] = [];
        let usersNotInDomainWorkspace: User[] = [];

        userList.forEach(user => {
            if (users.includes(user.id)) {
                usersInDomainWorkspace.push(user);
            } else {
                usersNotInDomainWorkspace.push(user);
            }
        });


        const htmlBeginning = `<!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link href="${styleVSCodeUri}" rel="stylesheet">
            </head>
            <body>
                <h1><u>${editMode} domain workspace</u></h1>
                <br>
                <form id="userForm">
                    <label for="description">Domain Workspace Name</label>
                    <br>
                    <input type="text" id="domainWorkspaceName" name="domainWorkspaceName" placeholder="Enter Domain Workspace Name" value="${name}" required></input>
                    <br>
                    <p>Users in Domain Workspace</p>
                `;
            
        let htmlMiddle = `
            <div id="userButtons">`;

        usersInDomainWorkspace.forEach(user => {
            htmlMiddle += `<button type="button" id="${user.id}" class="remove-user-button" style="margin-bottom: 0.5em;">${user.username}</button><br>`;
        });

        htmlMiddle += `<hr>`;

        usersNotInDomainWorkspace.forEach(user => {
            htmlMiddle += `<button type="button" id="${user.id}" class="add-user-button" style="margin-bottom: 0.5em;">${user.username}</button><br>`;
        });
        
        htmlMiddle += `</div>`;
        
        const htmlEnd = `
                <br><br>
                    <button type="button" id="submit">${editMode} domain workspace</button>
                </form>
                <script>
                    const vscode = acquireVsCodeApi();

                    document.getElementById('submit').addEventListener('click', () => {
                        const name = document.getElementById('domainWorkspaceName').value;

                        vscode.postMessage({
                            command: 'submit',
                            id: '${this.id}',
                            name: name,
                            users: ${JSON.stringify(users)},
                            mode: '${this.editType}'
                        });
                    });

                    document.querySelectorAll('.remove-user-button').forEach(button => {

                        button.addEventListener('click', () => {
                            const name_ = document.getElementById('domainWorkspaceName').value;
                            vscode.postMessage({
                                command: 'removeUser',
                                id: button.id,
                                name: name_
                            });
                        });
                    });

                    document.querySelectorAll('.add-user-button').forEach(button => {

                        button.addEventListener('click', () => {
                            const name_ = document.getElementById('domainWorkspaceName').value;
                            vscode.postMessage({
                                command: 'addUser',
                                id: button.id,
                                name: name_
                            });
                        });
                    });
                </script>
            </body>
            </html>`;

        return htmlBeginning + htmlMiddle + htmlEnd;
    }

}