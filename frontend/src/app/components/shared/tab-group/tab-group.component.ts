import { Component, Input } from '@angular/core';
import { GitlabIssuePrTemplate } from '../../../dtos/GitlabIssuePrTemplate';

@Component({
  selector: 'app-tab-group',
  templateUrl: './tab-group.component.html',
  styleUrl: './tab-group.component.scss',
})
export class TabGroupComponent {
  @Input() templateItem: GitlabIssuePrTemplate[] = [];
  @Input() isReadOnly: boolean = false;

  currentTab: number = 0;

  setTab(tabId: number) {
    this.currentTab = tabId;
  }

  deleteTab(index: number) {
    this.templateItem.splice(index, 1);
    if (this.currentTab >= this.templateItem.length) {
      this.currentTab = this.templateItem.length - 1;
    }
  }

  addTab() {
    this.templateItem.push({
      name: '',
      content: '',
    });

    this.currentTab = this.templateItem.length - 1;
  }
}
