import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { User } from '../../../dtos/User';
import { UserService } from '../../../services/user.service';
import { FeedbackService } from '../../../services/feedback.service';
import { ProjectUser } from '../../../dtos/ProjectUser';
import { CppsProject } from '../../../dtos/CppsProject';
import { catchError, debounceTime, of, Subject, switchMap } from 'rxjs';

@Component({
  selector: 'app-user-add-list',
  templateUrl: './user-add-list.component.html',
  styleUrl: './user-add-list.component.scss',
})
export class UserAddListComponent implements OnInit {
  public currentUserId: number = -1;
  public currentUser: User = {
    id: -1,
    gitlabId: -1,
    gitlabUsername: '',
    email: '',
    avatarUrl: '',
  };

  private searchSubject = new Subject<string>();
  matchingUsers: User[] = [];

  updateRecommendations() {
    this.matchingUsers = [];
    this.gitlabIdToAdd = '';
    this.searchSubject.next(this.gitlabUsernameSubstr);
  }

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe((user) => {
      // Check if user is already added
      this.currentUser = user;
      this.currentUserId = user.id;
      if (
        this.addedUsersList.find((addedUser) => addedUser.user.id === user.id)
      ) {
        return;
      }
      this.addedUsersList.push({
        user: user,
        isAdmin: true,
        isReviewer: true,
      });
    });

    this.searchSubject
      .pipe(
        debounceTime(750),
        switchMap((value) => this.userService.getMatchingUsers(value)),
        catchError((error) => {
          console.log(error);
          return of([]);
        })
      )
      .subscribe(
        (users) => {
          this.matchingUsers = users.filter(
            (user) =>
              !this.addedUsersList.find(
                (addedUser) => addedUser.user.gitlabId === user.gitlabId
              )
          );
          if (
            this.matchingUsers.find(
              (user) => user.gitlabUsername === this.gitlabUsernameSubstr
            )
          ) {
            this.onAddUserByUsername(this.gitlabUsernameSubstr);
          }
        },
        (error) => {
          console.log(error);
        }
      );
  }

  constructor(
    private userService: UserService,
    private feedbackService: FeedbackService
  ) {}

  @Output() stepperEvent = new EventEmitter<any>();
  @Output() nextStep = new EventEmitter<any>();

  @Input() mode: string = 'create';
  @Input() addedUsersList: ProjectUser[] = [];
  @Input() cppsProject: CppsProject = {
    id: -1,
    details: {
      name: '',
      description: '',
      version: '',
      demo: false,
    },
    users: [],
    domainWorkspaces: [],
    commonWorkspace: {
      id: -1,
      name: '',
      users: [],
    },
    externalToolsDetails: {
      rootGroupId: 0,
      creatorGitlabUserId: 0,
      gitlabProjectId: 0,
      mergeRequestTemplates: [],
      issueTemplates: [],
      gitlabHasBeenGenerated: false,
    },
  };

  gitlabIdToAdd: string = '';
  gitlabUsernameSubstr: string = '';

  onAddUserByUsername(username: string) {
    const user = this.matchingUsers.find(
      (user) => user.gitlabUsername === username
    );
    if (!user) {
      this.feedbackService.showError('User not found');
      return;
    }
    this.gitlabIdToAdd = user.gitlabId.toString();
  }

  onAddUser() {
    if (!this.gitlabIdToAdd) {
      this.feedbackService.showError('Please enter a GitLab ID');
      return;
    }

    const gitLabId: number = parseInt(this.gitlabIdToAdd);

    // Check if not already added
    if (this.addedUsersList.find((user) => user.user.gitlabId === gitLabId)) {
      this.feedbackService.showError('User already added');
      return;
    }

    this.userService.registerAndReturnUser(gitLabId).subscribe(
      (user: User) => {
        this.addedUsersList.push({
          user: user,
          isAdmin: false,
          isReviewer: false,
        });
        this.gitlabIdToAdd = '';
        this.gitlabUsernameSubstr = '';
        this.matchingUsers = [];
      },
      (error) => {
        this.feedbackService.showError('Error adding user');
      }
    );
  }

  onSubmit() {
    if (this.addedUsersList.length === 0) {
      this.feedbackService.showError('Please add at least one user');
      return;
    }

    this.nextStep.emit();
  }

  removeUser(userId: number) {
    if (userId === this.currentUserId) {
      this.feedbackService.showError('Cannot remove creator user');
      return;
    }

    // Check if user is used in common or domain workspace
    if (
      this.cppsProject.commonWorkspace.users.find(
        (user) => user.id === userId
      ) ||
      this.cppsProject.domainWorkspaces.find((workspace) =>
        workspace.users.find((user) => user.id === userId)
      )
    ) {
      this.feedbackService.showError(
        'Cannot remove user (already in workspace), remove from workspace first'
      );
      return;
    }

    this.addedUsersList = this.addedUsersList.filter(
      (addedUser) => addedUser.user.id != userId
    );

    this.stepperEvent.emit({ type: 'userList', data: this.addedUsersList });
  }

  isAdmin(): boolean {
    return (
      this.addedUsersList.find(
        (addedUser) => addedUser.user.id === this.currentUserId
      )?.isAdmin || false
    );
  }
}
