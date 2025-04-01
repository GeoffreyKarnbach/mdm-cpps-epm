import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DomainWorkspace } from '../../../dtos/DomainWorkspace';
import { User } from '../../../dtos/User';
import {
  catchError,
  debounceTime,
  filter,
  Observable,
  Subject,
  switchMap,
} from 'rxjs';
import { FeedbackService } from '../../../services/feedback.service';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-domain-workspace',
  templateUrl: './domain-workspace.component.html',
  styleUrl: './domain-workspace.component.scss',
})
export class DomainWorkspaceComponent implements OnInit {
  private searchSubject = new Subject<string>();
  matchingUsers: User[] = [];

  @Input() gitlabHasBeenInitiatedAlready: boolean = false;
  @Input() isNewWorkspace: boolean = false;
  loaded = false;

  constructor(
    private feedbackService: FeedbackService,
    private userService: UserService
  ) {}

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
      this.domainWorkspace.users.forEach((user) => {
        this.availableUsers = this.availableUsers.filter(
          (availableUser) => availableUser.id !== user.id
        );
      });
      if (
        this.domainWorkspace.users.find((addedUser) => addedUser.id === user.id)
      ) {
        return;
      }
      this.domainWorkspace.users.push(user);
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

  @Input() availableUsers: User[] = [];

  @Input() domainWorkspace: DomainWorkspace = {
    id: 0,
    name: '',
    users: [],
  };

  @Input() mode: string = 'edit';

  @Input() idx: number = -1;

  @Output() delete = new EventEmitter<number>();

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

    if (this.domainWorkspace.users.find((user) => user.gitlabId === gitLabId)) {
      this.feedbackService.showError('User already added');
      return;
    }

    if (user) {
      this.domainWorkspace.users.push(user);
      this.gitlabUsername = '';
      this.availableUsers = this.availableUsers.filter(
        (user) => user.gitlabId != gitLabId
      );
      this.updateRecommendations();
    } else {
      this.feedbackService.showError('User not found');
    }
  }

  updateRecommendations() {
    this.matchingUsers = [];
    this.searchSubject.next(this.gitlabUsername.toString());
  }

  deleteWorkspace(): void {
    if (!this.isNewWorkspace) {
      return;
    }

    this.delete.emit(this.idx);
  }

  removeUser(userId: number): void {
    const user = this.domainWorkspace.users.find((user) => user.id == userId);
    this.domainWorkspace.users = this.domainWorkspace.users.filter(
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
}
