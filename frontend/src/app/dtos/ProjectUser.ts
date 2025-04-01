import { User } from './User';

export interface ProjectUser {
  user: User;
  isReviewer: boolean;
  isAdmin: boolean;
}
