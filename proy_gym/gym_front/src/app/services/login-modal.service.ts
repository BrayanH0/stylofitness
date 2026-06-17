import { Subject } from 'rxjs';

export class LoginModalService {
  private openSource = new Subject<void>();
  open$ = this.openSource.asObservable();

  open() {
    this.openSource.next();
  }
}