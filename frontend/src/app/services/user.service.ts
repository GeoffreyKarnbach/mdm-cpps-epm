import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { User } from '../dtos/User';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  constructor(private http: HttpClient, private authService: AuthService) {}

  getCurrentUser(): Observable<User> {
    const jwt = this.authService.getJWTToken();

    const headers = new HttpHeaders({
      Authorization: 'Bearer ' + jwt,
    });

    return this.http.get<User>('http://localhost:8080/api/v1/user', {
      headers: headers,
    });
  }

  registerAndReturnUser(gitlabId: number): Observable<User> {
    const jwt = this.authService.getJWTToken();

    const headers = new HttpHeaders({
      Authorization: 'Bearer ' + jwt,
    });

    return this.http.get<User>(
      'http://localhost:8080/api/v1/user/registration/' + gitlabId,
      { headers: headers }
    );
  }

  getMatchingUsers(usernameSubStr: string): Observable<User[]> {
    const jwt = this.authService.getJWTToken();

    const headers = new HttpHeaders({
      Authorization: 'Bearer ' + jwt,
    });

    return this.http.get<User[]>(
      'http://localhost:8080/api/v1/user/matchingUsers?search=' +
        usernameSubStr,
      { headers: headers }
    );
  }
}
