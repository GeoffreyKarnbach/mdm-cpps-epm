import { Component, Input, OnInit } from '@angular/core';
import { RoutingService } from '../../../services/routing.service';
import { QueryParams } from '../../../dtos/NavigationItem';

@Component({
  selector: 'app-page-1',
  templateUrl: './page-1.component.html',
  styleUrl: './page-1.component.scss'
})
export class Page1Component implements OnInit {

  @Input() public queryParams: QueryParams = {};

  constructor(
    public routingService: RoutingService
  ) { }

  ngOnInit(): void {
      console.log('Page1Component initialized with query params: ', this.queryParams);
  }
}
