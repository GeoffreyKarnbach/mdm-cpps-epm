import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { User } from '../../../dtos/User';
import { DomainWorkspace } from '../../../dtos/DomainWorkspace';
import { CommonWorkspace } from '../../../dtos/CommonWorkspace';
import { catchError, debounceTime, Observable, Subject, switchMap } from 'rxjs';
import { FeedbackService } from '../../../services/feedback.service';
import { ProjectUser } from '../../../dtos/ProjectUser';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-common-workspace',
  templateUrl: './common-workspace.component.html',
  styleUrl: './common-workspace.component.scss',
})
export class CommonWorkspaceComponent implements OnInit {
  constructor(
    private feedbackService: FeedbackService,
    private userService: UserService
  ) {}

  @Input() availableProjectUsers: ProjectUser[] = [];
  @Input() gitlabHasBeenInitiatedAlready: boolean = false;

  availableUsers: User[] = [];

  @Input() mode: string = 'create';

  @Input() commonWorkspace: CommonWorkspace = {
    id: -1,
    name: '',
    users: [],
  };

  private searchSubject = new Subject<string>();
  matchingUsers: User[] = [];
  currentUser: User = {
    id: -1,
    gitlabId: -1,
    gitlabUsername: '',
    email: '',
    avatarUrl: '',
  };

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe((user) => {
      this.currentUser = user;
      if (
        !this.commonWorkspace.users.find(
          (addedUser) => addedUser.id === user.id
        )
      ) {
        this.commonWorkspace.users.push(user);
      }

      this.availableUsers = this.availableProjectUsers.map((pu) => pu.user);
      console.log(this.availableUsers);
      this.availableUsers = this.availableUsers.filter(
        (user) =>
          !this.commonWorkspace.users.find(
            (addedUser) => addedUser.id === user.id
          )
      );

      console.log(this.availableUsers);
    });

    this.searchSubject
      .pipe(
        debounceTime(100),
        switchMap(
          (value) =>
            new Observable<User[]>((observer) => {
              const users = this.availableUsers.filter((user) =>
                user.gitlabUsername.toString().includes(value)
              );
              observer.next(users);
              observer.complete();
            })
        ),
        catchError((error) => {
          console.log(error);
          return [];
        })
      )
      .subscribe(
        (users) => {
          this.matchingUsers = users;
        },
        (error) => {
          console.log(error);
        }
      );
  }

  @Input() idx: number = -1;
  @Input() isCommonWorkspace: boolean = false;

  @Output() nextStep = new EventEmitter<any>();

  gitlabUsername: string = '';

  onAddUser(): void {
    const gitLabUser = this.availableUsers.find(
      (user) => user.gitlabUsername === this.gitlabUsername
    );

    if (!gitLabUser) {
      this.feedbackService.showError('User not found');
      return;
    }

    const gitLabId = gitLabUser.gitlabId;

    const user = this.availableUsers.find((user) => user.gitlabId == gitLabId);

    // Check if not already added
    if (this.commonWorkspace.users.find((user) => user.gitlabId === gitLabId)) {
      this.feedbackService.showError('User already added');
      return;
    }

    if (user) {
      this.commonWorkspace.users.push(user);
      this.gitlabUsername = '';
      this.availableUsers = this.availableUsers.filter(
        (user) => user.gitlabId != gitLabId
      );
      this.updateRecommendations();
    }
  }

  updateRecommendations() {
    this.matchingUsers = [];
    this.searchSubject.next(this.gitlabUsername.toString());
  }

  removeUser(userId: number): void {
    const user = this.commonWorkspace.users.find((user) => user.id == userId);
    this.commonWorkspace.users = this.commonWorkspace.users.filter(
      (user) => user.id !== userId
    );

    if (!user) {
      return;
    }
    this.availableUsers.push(user);
  }

  getSuggestionComponentId(): string {
    return `suggestions-${this.idx}`;
  }

  save(): void {
    if (this.commonWorkspace.name === '') {
      this.feedbackService.showError(
        'Please enter a name for the common workspace'
      );
      return;
    }
    if (this.commonWorkspace.users.length === 0) {
      this.feedbackService.showError(
        'Please add at least one user to the common workspace'
      );
      return;
    }
    this.nextStep.emit();
  }
}
