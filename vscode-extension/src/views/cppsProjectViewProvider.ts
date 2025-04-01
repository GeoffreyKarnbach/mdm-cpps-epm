import * as vscode from 'vscode';
import { getProjectDetails, setProjectDetails } from '../configFileInteraction';

export class CppsProjectViewProvider implements vscode.WebviewViewProvider {

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
                case 'submit':
                    vscode.window.showInformationMessage(`Name: ${message.name}, Description: ${message.description}, Version: ${message.version}`);
                    setProjectDetails(message.name, message.description, message.version);
                    this._updateWebviewContent();
                    break;
            }
        });
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

        const projectDetails: string[] | null = getProjectDetails();
        const name = projectDetails ? projectDetails[0] : '';
        const description = projectDetails ? projectDetails[1] : '';
        const version = projectDetails ? projectDetails[2] : '';

        return `<!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link href="${styleVSCodeUri}" rel="stylesheet">
            </head>
            <body>
                <h3><u>CPPS Project Details</u></h3>
                <form id="projectForm">
                    <br>
                    <label for="name">Project Name</label>
                    <br>
                    <input type="text" id="name" name="name" placeholder="Enter project name" value="${name}" required>
                    <br>
                    <label for="description">Project Description</label>
                    <br>
                    <input type="text" id="description" name="description" placeholder="Enter project description" value="${description}" required>
                    <br>
                    <label for="version">Project Version</label>
                    <br>
                    <input type="text" id="version" name="version" placeholder="Enter project version" value="${version}" required>
                    <br>
                    <button type="button" id="submit">Update project details</button>
                    <br>
                </form>

                <script>
                    const vscode = acquireVsCodeApi();

                    document.getElementById('submit').addEventListener('click', () => {
                        const name = document.getElementById('name').value;
                        const description = document.getElementById('description').value;
                        const version = document.getElementById('version').value;

                        vscode.postMessage({
                            command: 'submit',
                            name,
                            description,
                            version
                        });
                    });
                </script>
            </body>
            </html>`;
    }

}