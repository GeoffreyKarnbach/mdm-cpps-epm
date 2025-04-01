import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';
import { CppsProject } from '../dtos/CppsProject';
import { Observable } from 'rxjs';
import { CppsProjectListItem } from '../dtos/CppsProjectListItem';
import { BuildResponse } from '../dtos/BuildResponse';
import { DemoProject } from '../dtos/DemoProject';
import { CppsProjectJsonExport } from '../dtos/CppsProjectJsonExport';

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  constructor(private http: HttpClient, private authService: AuthService) {}

  getHttpHeaders(): HttpHeaders {
    const jwt = this.authService.getJWTToken();

    return new HttpHeaders({
      Authorization: 'Bearer ' + jwt,
    });
  }

  createProject(project: CppsProject): Observable<CppsProject> {
    const headers = this.getHttpHeaders();

    return this.http.post<CppsProject>(
      'http://localhost:8080/api/v1/project',
      project,
      { headers: headers }
    );
  }

  getProjectsForUser(): Observable<CppsProjectListItem[]> {
    const headers = this.getHttpHeaders();

    return this.http.get<CppsProjectListItem[]>(
      'http://localhost:8080/api/v1/project',
      { headers: headers }
    );
  }

  getProjectById(id: number): Observable<CppsProject> {
    const headers = this.getHttpHeaders();

    return this.http.get<CppsProject>(
      'http://localhost:8080/api/v1/project/' + id,
      { headers: headers }
    );
  }

  exportProjectUrl(id: number): string {
    const jwt = this.authService.getJWTToken();

    return 'http://localhost:8080/api/v1/project/' + id + '/export?jwt=' + jwt;
  }

  deleteProject(id: number): Observable<any> {
    const headers = this.getHttpHeaders();

    return this.http.delete<any>('http://localhost:8080/api/v1/project/' + id, {
      headers: headers,
    });
  }

  updateProject(project: CppsProject): Observable<CppsProject> {
    const headers = this.getHttpHeaders();

    return this.http.put<CppsProject>(
      'http://localhost:8080/api/v1/project/' + project.id,
      project,
      { headers: headers }
    );
  }

  buildProjectInfrastructure(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' + projectId + '/build/project',
      {},
      { headers: headers }
    );
  }

  buildProjectSubgroups(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' + projectId + '/build/subgroups',
      {},
      { headers: headers }
    );
  }

  addProjectUsers(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' +
        projectId +
        '/build/project_users',
      {},
      { headers: headers }
    );
  }

  addSubgroupUsers(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' +
        projectId +
        '/build/subgroup_users',
      {},
      { headers: headers }
    );
  }

  addDeployKey(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' + projectId + '/build/deploy_key',
      {},
      { headers: headers }
    );
  }

  generateRepositoryFiles(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' +
        projectId +
        '/build/repository_files',
      {},
      { headers: headers }
    );
  }

  resetGitlabRootGroup(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' +
        projectId +
        '/build/reset_gitlab',
      {},
      { headers: headers }
    );
  }

  getGitlabUrl(projectId: number): Observable<string> {
    const headers = this.getHttpHeaders();
    headers.append('Content-Type', 'text/plain');

    return this.http.get<string>(
      'http://localhost:8080/api/v1/project/' + projectId + '/gitlab_url',
      { headers: headers, responseType: 'text' as 'json' }
    );
  }

  generateGitlabLabels(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' + projectId + '/build/labels',
      {},
      { headers: headers }
    );
  }

  editProjectSubgroups(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' + projectId + '/edit/subgroups',
      {},
      { headers: headers }
    );
  }

  editProjectUsers(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' +
        projectId +
        '/edit/project_users',
      {},
      { headers: headers }
    );
  }

  editSubgroupUsers(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' +
        projectId +
        '/edit/subgroup_users',
      {},
      { headers: headers }
    );
  }

  editRepositoryFiles(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' +
        projectId +
        '/edit/repository_files',
      {},
      { headers: headers }
    );
  }

  createDemoProject(project: DemoProject): Observable<DemoProject> {
    const headers = this.getHttpHeaders();

    return this.http.post<DemoProject>(
      'http://localhost:8080/api/v1/project/demo',
      project,
      { headers: headers }
    );
  }

  exportAnonymizedProjectUrl(id: number): string {
    const jwt = this.authService.getJWTToken();

    return (
      'http://localhost:8080/api/v1/project/' +
      id +
      '/export/anonymized?jwt=' +
      jwt
    );
  }

  importProject(project: CppsProjectJsonExport): Observable<CppsProject> {
    const headers = this.getHttpHeaders();

    return this.http.post<CppsProject>(
      'http://localhost:8080/api/v1/project/import',
      project,
      { headers: headers }
    );
  }

  performFileConsistencyCheck(projectId: number): Observable<BuildResponse> {
    const headers = this.getHttpHeaders();

    return this.http.post<BuildResponse>(
      'http://localhost:8080/api/v1/project/' +
        projectId +
        '/file_consistency_check',
      {},
      { headers: headers }
    );
  }
}
