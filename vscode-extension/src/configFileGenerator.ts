// Check if in the current project directory there is a file named "config.json"
// Check if a folder is open in the workspace
// If not, print out an error message
// If yes and not file exists, create it (copy the file "assets/configuration.json" to the current project directory)
// If the file exists, skip 

import * as vscode from 'vscode';
import * as fs from 'fs';

export function generateConfigFile() {
    if (vscode.workspace.workspaceFolders) {
        const workspacePath = vscode.workspace.workspaceFolders[0].uri.fsPath;
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        if (!fs.existsSync(configFilePath)) {
            fs.mkdirSync(workspacePath + "/.mdmcpps");

            fs.copyFileSync(__dirname + "/../assets/configuration.json", configFilePath);
        }
    }
}

export function existsConfigFile() {
    if (vscode.workspace.workspaceFolders) {
        const workspacePath = vscode.workspace.workspaceFolders[0].uri.fsPath;
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        return fs.existsSync(configFilePath);
    } else {
        return false;
    }
}