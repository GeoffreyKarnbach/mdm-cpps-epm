import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';
import { Observable } from 'rxjs';
import { GitlabGroupListItem } from '../dtos/GitlabGroupListItem';

@Injectable({
  providedIn: 'root',
})
export class GitlabService {
  constructor(private http: HttpClient, private authService: AuthService) {}

  getUserGroups(): Observable<GitlabGroupListItem[]> {
    const jwt = this.authService.getJWTToken();

    const headers = new HttpHeaders({
      Authorization: 'Bearer ' + jwt,
    });

    return this.http.get<GitlabGroupListItem[]>(
      'http://localhost:8080/api/v1/gitlab/groups',
      {
        headers: headers,
      }
    );
  }
}
