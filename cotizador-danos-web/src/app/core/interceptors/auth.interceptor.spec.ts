import { HttpHandler, HttpHeaders, HttpRequest } from '@angular/common/http';
import { EMPTY } from 'rxjs';

import { AuthInterceptor } from './auth.interceptor';
import { environment } from '../../../environments/environment';

describe('AuthInterceptor', () => {
  let interceptor: AuthInterceptor;

  const createNext = (onRequest: (request: HttpRequest<unknown>) => void): HttpHandler => ({
    handle: (req: HttpRequest<unknown>) => {
      onRequest(req);
      return EMPTY;
    }
  });

  beforeEach(() => {
    interceptor = new AuthInterceptor();
  });

  it('should keep request unchanged when x-api-key header already exists', () => {
    const request = new HttpRequest('GET', '/test', null, {
      headers: new HttpHeaders({ 'x-api-key': 'existing-key' })
    });

    const handledRequests: HttpRequest<unknown>[] = [];
    const next = createNext((req) => {
      handledRequests.push(req);
    });

    interceptor.intercept(request, next);

    expect(handledRequests.length).toBe(1);
    expect(handledRequests[0]?.headers.get('x-api-key')).toBe('existing-key');
    expect(handledRequests[0]?.headers.has('Authorization')).toBe(false);
  });

  it('should keep request unchanged when Authorization header already exists', () => {
    const request = new HttpRequest('GET', '/test', null, {
      headers: new HttpHeaders({ Authorization: 'ApiKey preloaded' })
    });

    const handledRequests: HttpRequest<unknown>[] = [];
    const next = createNext((req) => {
      handledRequests.push(req);
    });

    interceptor.intercept(request, next);

    expect(handledRequests[0]?.headers.get('Authorization')).toBe('ApiKey preloaded');
    expect(handledRequests[0]?.headers.has('x-api-key')).toBe(false);
  });

  it('should set Authorization header when requireApiKey is true', () => {
    const request = new HttpRequest('GET', '/test');
    const previousRequireApiKey = environment.requireApiKey;

    environment.requireApiKey = true;

    const handledRequests: HttpRequest<unknown>[] = [];
    const next = createNext((req) => {
      handledRequests.push(req);
    });

    interceptor.intercept(request, next);

    expect(handledRequests[0]?.headers.get('Authorization')).toBe(`ApiKey ${environment.apiKey}`);
    expect(handledRequests[0]?.headers.has('x-api-key')).toBe(false);

    environment.requireApiKey = previousRequireApiKey;
  });

  it('should set x-api-key header when requireApiKey is false', () => {
    const request = new HttpRequest('GET', '/test');
    const previousRequireApiKey = environment.requireApiKey;

    environment.requireApiKey = false;

    const handledRequests: HttpRequest<unknown>[] = [];
    const next = createNext((req) => {
      handledRequests.push(req);
    });

    interceptor.intercept(request, next);

    expect(handledRequests[0]?.headers.get('x-api-key')).toBe(environment.apiKey);
    expect(handledRequests[0]?.headers.has('Authorization')).toBe(false);

    environment.requireApiKey = previousRequireApiKey;
  });
});
