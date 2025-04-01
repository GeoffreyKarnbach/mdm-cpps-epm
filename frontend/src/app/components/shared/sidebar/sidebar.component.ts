import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../services/user.service';
import { User } from '../../../dtos/User';
import { RoutingService } from '../../../services/routing.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent implements OnInit {
  constructor(
    private userService: UserService,
    public routingService: RoutingService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe(
      (user) => {
        this.user = user;
      },
      (error) => {
        console.error(error);
        this.logout();
      }
    );
  }

  user: User = {
    id: -1,
    gitlabId: -1,
    gitlabUsername: '',
    email: '',
    avatarUrl: '',
  };

  logout() {
    // remove "jwt" from local storage
    localStorage.removeItem('jwt');
    this.routingService.navigateToComponent('login-page', {});
  }
}
