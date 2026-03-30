import { ErrorHandler, Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

import { ApiErrorResponse } from '../models/ApiError.model';

@Injectable({
  providedIn: 'root'
})
export class GlobalErrorHandlerService implements ErrorHandler {
  handleError(error: unknown): void {
    if (!(error instanceof HttpErrorResponse)) {
      console.error('Unhandled application error:', error);
      return;
    }

    const apiError = this.mapToApiError(error);

    switch (apiError.status) {
      case 400:
        console.error('Bad Request:', apiError.message);
        break;
      case 404:
        console.error('Resource not found:', apiError.message);
        break;
      case 409:
        console.error('Conflict error:', apiError.message);
        break;
      case 500:
        console.error('Internal server error:', apiError.message);
        break;
      default:
        console.error(`HTTP error ${apiError.status}:`, apiError.message);
    }
  }

  private mapToApiError(error: HttpErrorResponse): ApiErrorResponse {
    const payload = error.error as Partial<ApiErrorResponse> | null;

    return {
      timestamp: payload?.timestamp ?? new Date().toISOString(),
      status: payload?.status ?? error.status,
      error: payload?.error ?? error.statusText ?? 'Unknown error',
      message: payload?.message ?? error.message ?? 'Unexpected API error'
    };
  }
}
