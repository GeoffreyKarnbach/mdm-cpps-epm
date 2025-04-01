import { User } from "./User";

export interface DomainWorkspace {
    id: number;
    name: string;
    users: User[]
}