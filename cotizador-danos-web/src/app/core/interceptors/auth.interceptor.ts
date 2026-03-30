import { HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { environment } from '../../../environments/environment';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler) {
    const hasApiKeyHeader = req.headers.has('x-api-key') || req.headers.has('Authorization');

    if (hasApiKeyHeader) {
      return next.handle(req);
    }

    const updatedRequest = environment.requireApiKey
      ? req.clone({
          setHeaders: {
            Authorization: `ApiKey ${environment.apiKey}`
          }
        })
      : req.clone({
          setHeaders: {
            'x-api-key': environment.apiKey
          }
        });

    return next.handle(updatedRequest);
  }
}
