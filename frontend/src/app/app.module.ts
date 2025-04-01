import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { Page1Component } from './components/pages/page-1/page-1.component';
import { Page2Component } from './components/pages/page-2/page-2.component';
import { HomePageComponent } from './components/pages/home-page/home-page.component';
import { RoutingComponent } from './components/routing/routing.component';
import { HttpClientModule } from '@angular/common/http';
import { LoginComponent } from './components/pages/login/login.component';
import { SidebarComponent } from './components/shared/sidebar/sidebar.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ProjectListComponent } from './components/pages/project-list/project-list.component';
import { CreateEditProjectComponent } from './components/pages/create-edit-project/create-edit-project.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ErrorBannerComponent } from './components/shared/error-banner/error-banner.component';
import { DatePipe } from '@angular/common';
import { StepperComponent } from './components/shared/stepper/stepper.component';
import { ProjectDetailsComponent } from './components/shared/project-details/project-details.component';
import { UserAddListComponent } from './components/shared/user-add-list/user-add-list.component';
import { CommonWorkspaceComponent } from './components/shared/common-workspace/common-workspace.component';
import { DomainWorkspaceComponent } from './components/shared/domain-workspace/domain-workspace.component';
import { DomainWorkspaceListComponent } from './components/shared/domain-workspace-list/domain-workspace-list.component';
import { ProjectViewComponent } from './components/pages/project-view/project-view.component';
import { ExternalToolsDetailsComponent } from './components/shared/external-tools-details/external-tools-details.component';
import { TabGroupComponent } from './components/shared/tab-group/tab-group.component';
import { ProjectTypeSelectionComponent } from './components/shared/project-type-selection/project-type-selection.component';
import { CreateDemoProjectComponent } from './components/pages/create-demo-project/create-demo-project.component';
import { DemoProjectConfigComponent } from './components/shared/demo-project-config/demo-project-config.component';
import { ImportProjectComponent } from './components/pages/import-project/import-project.component';

@NgModule({
  declarations: [
    AppComponent,
    Page1Component,
    Page2Component,
    HomePageComponent,
    RoutingComponent,
    LoginComponent,
    SidebarComponent,
    ProjectListComponent,
    CreateEditProjectComponent,
    ErrorBannerComponent,
    StepperComponent,
    ProjectDetailsComponent,
    UserAddListComponent,
    CommonWorkspaceComponent,
    DomainWorkspaceComponent,
    DomainWorkspaceListComponent,
    ProjectViewComponent,
    ExternalToolsDetailsComponent,
    TabGroupComponent,
    ProjectTypeSelectionComponent,
    CreateDemoProjectComponent,
    DemoProjectConfigComponent,
    ImportProjectComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    NgbModule,
    ReactiveFormsModule,
    FormsModule,
  ],
  providers: [DatePipe],
  bootstrap: [AppComponent],
})
export class AppModule {}
