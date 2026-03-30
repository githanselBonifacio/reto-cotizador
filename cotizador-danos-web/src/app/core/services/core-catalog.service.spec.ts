import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CoreCatalogService } from './core-catalog.service';
import { environment } from '../../../environments/environment';

describe('CoreCatalogService', () => {
  let service: CoreCatalogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(CoreCatalogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should request agents with default pagination and map items', () => {
    let result: unknown;

    service.getAgents().subscribe((value) => {
      result = value;
    });

    const req = httpMock.expectOne(`${environment.coreApiUrl}/agents?skip=0&limit=100`);
    expect(req.request.method).toBe('GET');

    req.flush({
      total: 1,
      items: [{ _id: 'a1', name: 'Agent One', agent_code: 'AG01', status: 'ACTIVE' }]
    });

    expect(result).toEqual([{ _id: 'a1', name: 'Agent One', agent_code: 'AG01', status: 'ACTIVE' }]);
  });

  it('should request business lines with custom limit', () => {
    let result: unknown;

    service.getBusinessLines(25).subscribe((value) => {
      result = value;
    });

    const req = httpMock.expectOne(`${environment.coreApiUrl}/business-lines?skip=0&limit=25`);
    expect(req.request.method).toBe('GET');

    req.flush({
      total: 1,
      items: [{ _id: 'b1', code: 'BIO', name: 'Biotecnologia', status: 'ACTIVE' }]
    });

    expect(result).toEqual([{ _id: 'b1', code: 'BIO', name: 'Biotecnologia', status: 'ACTIVE' }]);
  });

  it('should map null catalog payload items to empty list', () => {
    let result: unknown;

    service.getGuarantees().subscribe((value) => {
      result = value;
    });

    const req = httpMock.expectOne(`${environment.coreApiUrl}/catalogs/guarantees?skip=0&limit=100`);
    expect(req.request.method).toBe('GET');

    req.flush({ total: 0, items: null });

    expect(result).toEqual([]);
  });

  it('should validate zip code with expected request body', () => {
    let result: unknown;

    service.validateZipCode('11011').subscribe((value) => {
      result = value;
    });

    const req = httpMock.expectOne(`${environment.coreApiUrl}/zip-codes/validate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ zip_code: '11011' });

    req.flush({
      is_valid: true,
      zip_code: '11011',
      city: 'Bogota',
      state: 'Cundinamarca',
      risk_zone: 'A'
    });

    expect(result).toEqual({
      is_valid: true,
      zip_code: '11011',
      city: 'Bogota',
      state: 'Cundinamarca',
      risk_zone: 'A'
    });
  });
});
