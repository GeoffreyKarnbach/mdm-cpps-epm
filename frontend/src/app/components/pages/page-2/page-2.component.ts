import { Component, Input } from '@angular/core';
import { RoutingService } from '../../../services/routing.service';
import { QueryParams } from '../../../dtos/NavigationItem';

@Component({
  selector: 'app-page-2',
  templateUrl: './page-2.component.html',
  styleUrl: './page-2.component.scss'
})
export class Page2Component {

  @Input() public queryParams: QueryParams = {};

  constructor(
    public routingService: RoutingService
  ) { }

  ngOnInit(): void {
      console.log('Page2Component initialized with query params: ', this.queryParams);
  }
}
