import * as vscode from 'vscode';


export class GitlabViewProvider implements vscode.TreeDataProvider<GitlabItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<GitlabItem | undefined | void> = new vscode.EventEmitter<GitlabItem | undefined | void>();
    readonly onDidChangeTreeData: vscode.Event<GitlabItem | undefined | void> = this._onDidChangeTreeData.event;

    getTreeItem(element: GitlabItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: GitlabItem): Thenable<GitlabItem[]> {
        if (element) {
            return Promise.resolve([]);
        } else {
            return Promise.resolve([new GitlabItem('Sample Gitlab Item', vscode.TreeItemCollapsibleState.None)]);
        }
    }

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }
}

class GitlabItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState
    ) {
        super(label, collapsibleState);
    }
}