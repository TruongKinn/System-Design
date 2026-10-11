import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

// Ant Design Modules
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageModule } from 'ng-zorro-antd/message';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { CryptoComponent } from './crypto.component';

const routes: Routes = [
  { path: '', component: CryptoComponent }
];

@NgModule({
  declarations: [
    CryptoComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild(routes),
    NzButtonModule,
    NzIconModule,
    NzInputModule,
    NzMessageModule,
    NzCardModule,
    NzTagModule
  ]
})
export class CryptoModule { }
