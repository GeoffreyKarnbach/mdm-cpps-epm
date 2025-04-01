export interface User {
    id: string;
    gitUserId: string;
    gitUsername: string;
    username: string;
    isReviewer: boolean;
}

export interface DomainWorkspace {
    id: string;
    name: string;
    users: string[];
}

export interface CommonWorkspace {
    name: string;
    users: string[];
}