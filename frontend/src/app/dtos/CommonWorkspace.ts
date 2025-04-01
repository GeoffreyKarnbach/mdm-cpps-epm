import { User } from './User';

export interface CommonWorkspace {
  id: number;
  name: string;
  users: User[];
}
