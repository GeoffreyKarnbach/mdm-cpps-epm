import { Component, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { RoutingService } from '../../services/routing.service';
import { QueryParams } from '../../dtos/NavigationItem';

@Component({
  selector: 'app-routing',
  templateUrl: './routing.component.html',
  styleUrl: './routing.component.scss'
})
export class RoutingComponent implements OnInit {

  defaultComponent: string = 'login-page';
  targetComponent: string = this.defaultComponent;
  targetQueryParams: QueryParams = {};

  private subscription: Subscription = new Subscription();

  constructor(public routingService: RoutingService) {}

  ngOnInit() {
    this.subscription = this.routingService.navigate$.subscribe(component => {
      this.targetComponent = component.component;
      this.targetQueryParams = component.queryParams;
    });
  }
}
