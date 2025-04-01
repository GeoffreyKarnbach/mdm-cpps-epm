import * as vscode from 'vscode';
import { GitlabViewProvider } from './views/gitlabViewProvider';
import { CppsProjectViewProvider } from './views/cppsProjectViewProvider';
import { CppsUserViewProvider } from './views/cppsUserViewProvider';
import { generateConfigFile } from './configFileGenerator';
import { CppsUserCreateEditPanelProvider } from './views/cppsUserCreateEditPanelProvider';
import { CppsDomainWorkspaceViewProvider } from './views/cppsDomainWorkspaceViewProvider';
import { CppsDomainWorkspaceCreateEditPanelProvider } from './views/cppsDomainWorkspaceCreateEditPanelProvider';
import { CppsCommonWorkspaceViewProvider } from './views/cppsCommonWorkspaceViewProvider';
import * as path from 'path';
import * as fs from 'fs';

export function deactivate() {}

export function activate(context: vscode.ExtensionContext) {

	console.log('Congratulations, your extension "bachelorarbeit" is now active!');

	generateConfigFile();

	// For CPPS Project Tab
	const projectViewProvider = new CppsProjectViewProvider(context.extensionUri);
	context.subscriptions.push(vscode.window.registerWebviewViewProvider('projectView', projectViewProvider));

	const userViewProvider = new CppsUserViewProvider(context.extensionUri);
	context.subscriptions.push(vscode.window.registerWebviewViewProvider('userView', userViewProvider));

	const domainWorkspaceViewProvider = new CppsDomainWorkspaceViewProvider(context.extensionUri);
	context.subscriptions.push(vscode.window.registerWebviewViewProvider('domainWorkspaceView', domainWorkspaceViewProvider));

	const commonWorkspaceViewProvider = new CppsCommonWorkspaceViewProvider(context.extensionUri);
	context.subscriptions.push(vscode.window.registerWebviewViewProvider('commonWorkspaceView', commonWorkspaceViewProvider));

	////////////////////////////////////////

	// For Gitlab Tab
	const gitlabViewProvider = new GitlabViewProvider();
    vscode.window.registerTreeDataProvider('gitlabView', gitlabViewProvider);

	// For Jira Tab
	const jiraViewProvider = new GitlabViewProvider();
	vscode.window.registerTreeDataProvider('jiraView', jiraViewProvider);

	// Any commands for my extension
	context.subscriptions.push(
		vscode.commands.registerCommand('bachelorarbeit.helloWorld', () => {
			vscode.window.showInformationMessage('Hello World from BachelorArbeit!');
		})
	);

	const userViewCreateEditProvider = new CppsUserCreateEditPanelProvider(context.extensionUri);
	context.subscriptions.push(
		vscode.commands.registerCommand('bachelorarbeit.showUserCreateEditPanel', (editType?: string, id?: string) => {
			userViewCreateEditProvider.show(editType, id);
	}));

	context.subscriptions.push(
        vscode.commands.registerCommand('bachelorarbeit.triggerUpdateUserList', () => {
            userViewProvider.triggerUpdate();
        })
    );

	const domainWorkspaceViewCreateEditProvider = new CppsDomainWorkspaceCreateEditPanelProvider(context.extensionUri);
	context.subscriptions.push(
		vscode.commands.registerCommand('bachelorarbeit.showDomainWorkspaceCreateEditPanel', (editType?: string, id?: string) => {
			domainWorkspaceViewCreateEditProvider.show(editType, id);
	}));

	context.subscriptions.push(
		vscode.commands.registerCommand('bachelorarbeit.triggerUpdateDomainWorkspaceList', () => {
			domainWorkspaceViewProvider.triggerUpdate();
	}));

	// If a new workspace is opened, generate a new config file
	vscode.workspace.onDidChangeWorkspaceFolders(() => {
		generateConfigFile();
	});

	let disposable = vscode.commands.registerCommand('bachelorarbeit.showWebview', () => {
		const panel = vscode.window.createWebviewPanel(
			'angularWebview',
			'My Angular App',
			vscode.ViewColumn.One,
			{
				enableScripts: true,
				localResourceRoots: [vscode.Uri.file(path.join(context.extensionPath, 'dist', 'bachelorarbeit-frontend', 'browser'))]
			}
		);
	
		// Get the HTML content from the Angular app's index.html file
		const indexPath = path.join(context.extensionPath, 'dist', 'bachelorarbeit-frontend','browser', 'index.html');
		let indexHtml = fs.readFileSync(indexPath, 'utf8');
	
		// Update the paths in index.html to use `vscode-webview` compatible URIs
		indexHtml = indexHtml.replace(/(src|href)=["']([^"']+)["']/g, (match, p1, p2) => {
			// Ignore absolute URLs (http, https, etc.) and data URLs
			if (p2.startsWith('http') || p2.startsWith('data:')) {
				return match;
			}
	
			// Convert local resource paths to vscode-webview URIs
			const resourcePath = vscode.Uri.file(path.join(context.extensionPath, 'dist', 'bachelorarbeit-frontend', 'browser', p2));
			return `${p1}="${panel.webview.asWebviewUri(resourcePath)}"`;
		});
	
		// Set the webview's HTML content
		panel.webview.html = indexHtml;
	});
	
	  context.subscriptions.push(disposable);
}



