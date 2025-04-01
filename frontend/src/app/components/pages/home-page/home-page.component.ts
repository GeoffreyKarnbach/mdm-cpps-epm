import { Component, Input } from '@angular/core';
import { RoutingService } from '../../../services/routing.service';
import { QueryParams } from '../../../dtos/NavigationItem';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-home-page',
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss',
})
export class HomePageComponent {
  @Input() public queryParams: QueryParams = {};

  constructor(public routingService: RoutingService) {}

  ngOnInit(): void {}
}
