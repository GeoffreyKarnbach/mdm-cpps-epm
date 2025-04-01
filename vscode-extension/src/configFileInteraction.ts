import * as vscode from 'vscode';
import * as fs from 'fs';
import { existsConfigFile } from './configFileGenerator';
import { DomainWorkspace, User } from './types';
import { v4 as uuidv4 } from 'uuid';

export function setProjectDetails(name: string, description: string, version: string) {

    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        config.name = name;
        config.description = description;
        config.version = version;

        fs.writeFileSync(configFilePath, JSON.stringify(config, null, 4));
    }
}

export function getProjectDetails() {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        // I need an array with 3 elements: name, description, version
        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));
        return [config.name, config.description, config.version];
    }
    return null;
}

export function getUserList(): User[] {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        let users: User[] = [];
        
        for (let i = 0; i < config.users.length; i++){
            const newUser = {
                id: config.users[i].id,
                gitUserId: config.users[i].gitUserId,
                gitUsername: config.users[i].gitUsername,
                username: config.users[i].username,
                isReviewer: config.users[i].isReviewer
            };
            users.push(newUser);
        }
        return users;
    }
    return [];
}

export function getUser(id: string): User | null {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        for (let i = 0; i < config.users.length; i++){
            if (config.users[i].id === id){
                return {
                    id: config.users[i].id,
                    gitUserId: config.users[i].gitUserId,
                    gitUsername: config.users[i].gitUsername,
                    username: config.users[i].username,
                    isReviewer: config.users[i].isReviewer
                };
            }
        }
    }
    return null;
}

export function addUser(user: User): boolean {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        user.id = uuidv4();

        config.users.push(user);

        fs.writeFileSync(configFilePath, JSON.stringify(config, null, 4));
        vscode.commands.executeCommand('bachelorarbeit.triggerUpdateUserList');
        return true;
    }

    return false;
}

export function removeUser(id: string): boolean {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        for (let i = 0; i < config.users.length; i++){
            if (config.users[i].id === id){
                config.users.splice(i, 1);
                fs.writeFileSync(configFilePath, JSON.stringify(config, null, 4));
                vscode.commands.executeCommand('bachelorarbeit.triggerUpdateUserList');
                return true;
            }
        }
    }
    return false;
}

export function editUser(user: User, id: string): boolean {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        for (let i = 0; i < config.users.length; i++){
            if (config.users[i].id === id){
                config.users[i] = user;
                fs.writeFileSync(configFilePath, JSON.stringify(config, null, 4));
                vscode.commands.executeCommand('bachelorarbeit.triggerUpdateUserList');
                return true;
            }
        }
    }

    return false;
}

export function getDomainWorkspaces(): DomainWorkspace[] {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        let domainWorkspaces: DomainWorkspace[] = [];
        
        for (let i = 0; i < config.domainWorkspaces.length; i++){
            const newDomainWorkspace = {
                id: config.domainWorkspaces[i].id,
                name: config.domainWorkspaces[i].name,
                users: config.domainWorkspaces[i].users
            };
            domainWorkspaces.push(newDomainWorkspace);
        }
        return domainWorkspaces;
    }
    return [];
}

export function getDomainWorkspace(id: string): DomainWorkspace | null {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        for (let i = 0; i < config.domainWorkspaces.length; i++){
            if (config.domainWorkspaces[i].id === id){
                return {
                    id: config.domainWorkspaces[i].id,
                    name: config.domainWorkspaces[i].name,
                    users: config.domainWorkspaces[i].users
                };
            }
        }
    }
    return null;
}


export function addDomainWorkspace(dw: DomainWorkspace): boolean {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        dw.id = uuidv4();

        config.domainWorkspaces.push(dw);

        fs.writeFileSync(configFilePath, JSON.stringify(config, null, 4));
        vscode.commands.executeCommand('bachelorarbeit.triggerUpdateDomainWorkspaceList');
        return true;
    }

    return false;
}

export function removeDomainWorkspace(id: string): boolean {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        for (let i = 0; i < config.domainWorkspaces.length; i++){
            if (config.domainWorkspaces[i].id === id){
                config.domainWorkspaces.splice(i, 1);
                fs.writeFileSync(configFilePath, JSON.stringify(config, null, 4));
                return true;
            }
        }
    }
    return false;
}

export function editDomainWorkspace(dw: DomainWorkspace, id: string): boolean {
    if (existsConfigFile()){
        const workspaceFolders = vscode.workspace.workspaceFolders;
        const workspacePath = workspaceFolders ? workspaceFolders[0].uri.fsPath : '';
        const configFilePath = workspacePath + "/.mdmcpps/config.json";

        const config = JSON.parse(fs.readFileSync(configFilePath, 'utf8'));

        for (let i = 0; i < config.domainWorkspaces.length; i++){
            if (config.domainWorkspaces[i].id === id){
                config.domainWorkspaces[i] = dw;
                fs.writeFileSync(configFilePath, JSON.stringify(config, null, 4));
                vscode.commands.executeCommand('bachelorarbeit.triggerUpdateDomainWorkspaceList');
                return true;
            }
        }
    }

    return false;
}
