import { Component, Input } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { RoutingService } from '../../../services/routing.service';
import { QueryParams } from '../../../dtos/NavigationItem';
import { v4 as uuidv4 } from 'uuid'; // Import the uuidv4 function from the uuid library

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  @Input() public queryParams: QueryParams = {};

  public oauthUrlCalled: boolean = false;
  private clientToken: string = uuidv4(); // Generate a random client token
  public oauthUrl: string = '';

  constructor(
    public routingService: RoutingService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // try to get JWT from local storage
    const jwt = this.getJWTTokenFromLocalStorage();
    if (jwt) {
      const isTokenExpired = this.isJWTTokenExpired(jwt);
      if (!isTokenExpired) {
        this.authService.setJWTToken(jwt);
        this.routingService.navigateToComponent('project-list', {});
      } else {
        console.log('JWT token is expired');
        this.removeJWTTokenFromLocalStorage();
      }
    }
  }

  request_oauth_url() {
    this.authService.requestOAuthUrl(this.clientToken).subscribe((response) => {
      // Copy oauth url to clipboard
      navigator.clipboard.writeText(response.url);
      this.oauthUrl = response.url;
      this.oauthUrlCalled = true;
    });
  }

  check_login_status() {
    this.authService.checkJWT(this.clientToken).subscribe((response) => {
      this.authService.setJWTToken(response.token);
      localStorage.setItem('jwt', response.token);
      this.routingService.navigateToComponent('project-list', {});
    });
  }

  private getJWTTokenFromLocalStorage() {
    return localStorage.getItem('jwt');
  }

  private isJWTTokenExpired(token: string): boolean {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const expiry = payload.exp;
    const now = Math.floor(new Date().getTime() / 1000);
    return now >= expiry;
  }

  private removeJWTTokenFromLocalStorage(): void {
    localStorage.removeItem('jwtToken');
  }
}
