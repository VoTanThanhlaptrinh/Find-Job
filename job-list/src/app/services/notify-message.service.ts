import { isPlatformBrowser } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { ToastrService, IndividualConfig } from 'ngx-toastr';

@Injectable({
  providedIn: 'root'
})
export class NotifyMessageService {


  constructor(
    @Inject(PLATFORM_ID) private _platformId: Object,
    private _toastr: ToastrService) {}

  showMessage(message: string, title?: string, status?: any, options: Partial<IndividualConfig> = {}) {
     if(isPlatformBrowser(this._platformId)){
        switch (status) {
            case 'success':
              this._toastr.success(message, title);
              break;
            case 'error':
              this._toastr.error(message, title);
              break;
            case 'info':
              this._toastr.info(message, title);
              break;
            case 'warning':
              this._toastr.warning(message, title);
              break;
            default:
          }
    }
    
}
}
