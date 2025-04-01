import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { OAuthUrlResponse } from '../dtos/OAuthUrlResponse';
import { Observable } from 'rxjs';
import { JWTResponse } from '../dtos/JWTResponse';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(
    private http: HttpClient
  ) { }

  private jwtToken: string = '';

  requestOAuthUrl(clientToken: string): Observable<OAuthUrlResponse> {
    return this.http.get<OAuthUrlResponse>('http://localhost:8080/api/v1/auth/oauth/url?client_token=' + clientToken);
  }

  checkJWT(clientToken: string): Observable<JWTResponse> {
    return this.http.get<JWTResponse>('http://localhost:8080/api/v1/auth/jwt?client_token=' + clientToken);
  }

  setJWTToken(jwtToken: string): void {
    this.jwtToken = jwtToken;
  }

  getJWTToken(): string {
    return this.jwtToken;
  }
}
