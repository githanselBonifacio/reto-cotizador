import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { QuoteService } from '../services/quote.service';

@Injectable()
export class OptimisticLockInterceptor implements HttpInterceptor {
  private readonly snackBar = inject(MatSnackBar);
  private readonly quoteService = inject(QuoteService);

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((error: unknown) => {
        const isConflict = error instanceof HttpErrorResponse && error.status === 409;
        const isWriteMethod = req.method === 'PUT' || req.method === 'PATCH';

        if (isConflict && isWriteMethod) {
          const snackBarRef = this.snackBar.open(
            'Concurrent update detected. Reload latest version?',
            'Refresh',
            {
              duration: 10000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            }
          );

          snackBarRef.onAction().subscribe(() => {
            this.quoteService.refreshCurrentQuoteState().subscribe();
            this.quoteService.requestQuoteRefresh();
          });
        }

        return throwError(() => error);
      })
    );
  }
}
