import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { EstadoCotizacion } from '../models/Cotizacion.model';
import { Location } from '../models/Location.model';
import { QuoteService } from './quote.service';
import { environment } from '../../../environments/environment';

describe('QuoteService', () => {
  let service: QuoteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(QuoteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize quote and sync current quote state', () => {
    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    service.initializeQuote().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/folios`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});

    req.flush({
      numeroFolio: 'FOL001',
      estadoCotizacion: EstadoCotizacion.PENDIENTE,
      version: 1,
      primaNeta: null,
      primaComercial: null
    });

    expect(currentQuote).toEqual({
      folio: 'FOL001',
      version: 1,
      status: EstadoCotizacion.PENDING
    });
  });

  it('should update general info and sync current quote', () => {
    service.updateGeneralInfo('FOL001', {
      version: 1,
      datosAsegurado: { nombre: 'Cliente', rfc: 'ABC123456XYZ' },
      codigoAgente: 'AG01'
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL001/general-info`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.codigoAgente).toBe('AG01');

    req.flush({
      numeroFolio: 'FOL001',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 2,
      primaNeta: null,
      primaComercial: null
    });

    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    expect(currentQuote).toEqual({
      folio: 'FOL001',
      version: 2,
      status: EstadoCotizacion.PENDING
    });
  });

  it('should map array locations response and keep existing version', () => {
    service.initializeQuote().subscribe();
    httpMock.expectOne(`${environment.apiUrl}/folios`).flush({
      numeroFolio: 'FOL100',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 5,
      primaNeta: null,
      primaComercial: null
    });

    let response: unknown;
    service.getLocations('FOL100').subscribe((value) => {
      response = value;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL100/locations`);
    expect(req.request.method).toBe('GET');

    req.flush([{ locationName: 'Centro' }]);

    expect(response).toEqual({
      locations: [{ locationName: 'Centro' }],
      version: 5
    });
  });

  it('should map object locations response and update version', () => {
    service.initializeQuote().subscribe();
    httpMock.expectOne(`${environment.apiUrl}/folios`).flush({
      numeroFolio: 'FOL200',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 1,
      primaNeta: null,
      primaComercial: null
    });

    service.getLocations('FOL200').subscribe();

    httpMock.expectOne(`${environment.apiUrl}/quotes/FOL200/locations`).flush({
      locations: [{ locationName: 'Norte' }],
      version: 9
    });

    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    expect(currentQuote).toEqual({
      folio: 'FOL200',
      version: 9,
      status: EstadoCotizacion.PENDING
    });
  });

  it('should request patch location by index', () => {
    service.patchLocationByIndex('FOL300', 2, {
      version: 1,
      nombreUbicacion: 'Sucursal'
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL300/locations/2`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.nombreUbicacion).toBe('Sucursal');

    req.flush({
      numeroFolio: 'FOL300',
      estadoCotizacion: EstadoCotizacion.ERROR,
      version: 3,
      primaNeta: null,
      primaComercial: null
    });

    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    expect(currentQuote).toEqual({
      folio: 'FOL300',
      version: 3,
      status: EstadoCotizacion.ERROR
    });
  });

  it('should request calculation endpoint and sync state', () => {
    service.calculateQuote('FOL400', {
      version: 4,
      parametros_calculo: { factorRiesgo: 1, factorComercial: 1 }
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL400/calculate`);
    expect(req.request.method).toBe('POST');

    req.flush({
      numeroFolio: 'FOL400',
      estadoCotizacion: EstadoCotizacion.CALCULADO,
      version: 5,
      primaNeta: 100,
      primaComercial: 120
    });

    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    expect(currentQuote).toEqual({
      folio: 'FOL400',
      version: 5,
      status: EstadoCotizacion.CALCULATED
    });
  });

  it('should map quote state response to current quote', () => {
    let response: unknown;
    service.getQuoteState('FOL500').subscribe((value) => {
      response = value;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL500/state`);
    expect(req.request.method).toBe('GET');

    req.flush({
      numeroFolio: 'FOL500',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 11
    });

    expect(response).toEqual({
      folio: 'FOL500',
      version: 11,
      status: EstadoCotizacion.PENDING
    });
  });

  it('should get general info without failing when current quote is empty', () => {
    let response: unknown;
    service.getGeneralInfo('FOL501').subscribe((value) => {
      response = value;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL501/general-info`);
    expect(req.request.method).toBe('GET');

    req.flush({
      datosAsegurado: { nombre: 'Test', rfc: 'ABC123456XYZ' },
      codigoAgente: 'AG1',
      version: 4
    });

    expect(response).toEqual({
      datosAsegurado: { nombre: 'Test', rfc: 'ABC123456XYZ' },
      codigoAgente: 'AG1',
      version: 4
    });
  });

  it('should sync version when getting general info and current quote exists', () => {
    service.initializeQuote().subscribe();
    httpMock.expectOne(`${environment.apiUrl}/folios`).flush({
      numeroFolio: 'FOL502',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 1,
      primaNeta: null,
      primaComercial: null
    });

    service.getGeneralInfo('FOL502').subscribe();
    httpMock.expectOne(`${environment.apiUrl}/quotes/FOL502/general-info`).flush({
      datosAsegurado: { nombre: 'Cliente', rfc: 'ABC123456XYZ' },
      codigoAgente: 'AG2',
      version: 7
    });

    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    expect(currentQuote).toEqual({
      folio: 'FOL502',
      version: 7,
      status: EstadoCotizacion.PENDING
    });
  });

  it('should update locations and sync current quote', () => {
    service.updateLocations('FOL503', {
      version: 2,
      locations: [{ index: 0, locationName: 'Loc 1' }]
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL503/locations`);
    expect(req.request.method).toBe('PUT');

    req.flush({
      numeroFolio: 'FOL503',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 3,
      primaNeta: null,
      primaComercial: null
    });

    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    expect(currentQuote).toEqual({
      folio: 'FOL503',
      version: 3,
      status: EstadoCotizacion.PENDING
    });
  });

  it('should get locations layout and sync version only when quote exists', () => {
    service.initializeQuote().subscribe();
    httpMock.expectOne(`${environment.apiUrl}/folios`).flush({
      numeroFolio: 'FOL504',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 1,
      primaNeta: null,
      primaComercial: null
    });

    let layoutResponse: unknown;
    service.getLocationsLayout('FOL504').subscribe((value) => {
      layoutResponse = value;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL504/locations/layout`);
    expect(req.request.method).toBe('GET');
    req.flush({ configuracionLayout: { deducible: 10 }, version: 6 });

    expect(layoutResponse).toEqual({ configuracionLayout: { deducible: 10 }, version: 6 });

    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    expect(currentQuote).toEqual({
      folio: 'FOL504',
      version: 6,
      status: EstadoCotizacion.PENDING
    });
  });

  it('should update locations layout and coverage options', () => {
    service.updateLocationsLayout('FOL505', {
      version: 1,
      configuracionLayout: { moneda: 'COP' }
    }).subscribe();

    const layoutReq = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL505/locations/layout`);
    expect(layoutReq.request.method).toBe('PUT');
    layoutReq.flush({
      numeroFolio: 'FOL505',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 2,
      primaNeta: null,
      primaComercial: null
    });

    service.updateCoverageOptions('FOL505', {
      version: 2,
      opcionesCobertura: ['incendio']
    }).subscribe();

    const coverageReq = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL505/coverage-options`);
    expect(coverageReq.request.method).toBe('PUT');
    coverageReq.flush({
      numeroFolio: 'FOL505',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 3,
      primaNeta: null,
      primaComercial: null
    });

    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    expect(currentQuote).toEqual({
      folio: 'FOL505',
      version: 3,
      status: EstadoCotizacion.PENDING
    });
  });

  it('should get coverage options and sync version', () => {
    service.initializeQuote().subscribe();
    httpMock.expectOne(`${environment.apiUrl}/folios`).flush({
      numeroFolio: 'FOL506',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 1,
      primaNeta: null,
      primaComercial: null
    });

    let coverageResponse: unknown;
    service.getCoverageOptions('FOL506').subscribe((value) => {
      coverageResponse = value;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL506/coverage-options`);
    expect(req.request.method).toBe('GET');
    req.flush({ opcionesCobertura: ['incendio', 'terremoto'], version: 5 });

    expect(coverageResponse).toEqual({ opcionesCobertura: ['incendio', 'terremoto'], version: 5 });
  });

  it('should request locations summary and quote summary', () => {
    let locationsSummary: unknown;
    let quoteSummary: unknown;

    service.getLocationsSummary('FOL507').subscribe((value) => {
      locationsSummary = value;
    });
    service.getSummary('FOL507').subscribe((value) => {
      quoteSummary = value;
    });

    const locationsReq = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL507/locations/summary`);
    expect(locationsReq.request.method).toBe('GET');
    locationsReq.flush({
      totalLocations: 2,
      validLocations: 2,
      warningLocations: 0,
      totalBlockingAlerts: 0,
      totalPrimaNeta: 200,
      totalPrimaComercial: 240
    });

    const summaryReq = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL507/summary`);
    expect(summaryReq.request.method).toBe('GET');
    summaryReq.flush({
      numeroFolio: 'FOL507',
      estadoCotizacion: EstadoCotizacion.CALCULATED,
      version: 9,
      primaNeta: 200,
      primaComercial: 240
    });

    expect(locationsSummary).toEqual({
      totalLocations: 2,
      validLocations: 2,
      warningLocations: 0,
      totalBlockingAlerts: 0,
      totalPrimaNeta: 200,
      totalPrimaComercial: 240
    });
    expect(quoteSummary).toEqual({
      numeroFolio: 'FOL507',
      estadoCotizacion: EstadoCotizacion.CALCULATED,
      version: 9,
      primaNeta: 200,
      primaComercial: 240
    });
  });

  it('should return EMPTY when refreshing without current folio', () => {
    let nextCalled = false;
    let completeCalled = false;

    service.refreshCurrentQuoteState().subscribe({
      next: () => {
        nextCalled = true;
      },
      complete: () => {
        completeCalled = true;
      }
    });

    expect(nextCalled).toBe(false);
    expect(completeCalled).toBe(true);
  });

  it('should refresh current quote state when folio exists', () => {
    service.initializeQuote().subscribe();
    httpMock.expectOne(`${environment.apiUrl}/folios`).flush({
      numeroFolio: 'FOL600',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 1,
      primaNeta: null,
      primaComercial: null
    });

    let refreshed: unknown;
    service.refreshCurrentQuoteState().subscribe((value) => {
      refreshed = value;
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL600/state`);
    expect(req.request.method).toBe('GET');

    req.flush({
      numeroFolio: 'FOL600',
      estadoCotizacion: EstadoCotizacion.CALCULATED,
      version: 8
    });

    expect(refreshed).toEqual({
      folio: 'FOL600',
      version: 8,
      status: EstadoCotizacion.CALCULATED
    });
  });

  it('should emit refresh signal when requestQuoteRefresh is called', () => {
    let calls = 0;
    service.refreshRequested$.subscribe(() => {
      calls += 1;
    });

    service.requestQuoteRefresh();

    expect(calls).toBe(1);
  });

  it('should clear current quote', () => {
    service.initializeQuote().subscribe();
    httpMock.expectOne(`${environment.apiUrl}/folios`).flush({
      numeroFolio: 'FOL700',
      estadoCotizacion: EstadoCotizacion.PENDING,
      version: 1,
      primaNeta: null,
      primaComercial: null
    });

    service.clearCurrentQuote();

    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    expect(currentQuote).toBeNull();
  });

  it('should transform locations into update request', () => {
    const locations: Location[] = [
      {
        indice: 9,
        locationName: 'Sede Principal',
        address: 'Calle 1',
        zipCode: '11011',
        fireKey: 'FK1',
        coverages: ['incendio'],
        insuredAmount: 1000,
        contentsValue: 500
      },
      {
        nombreUbicacion: 'Bodega',
        direccion: 'Calle 2',
        codigoPostal: '22022',
        claveIncendio: 'FK2',
        garantias: ['terremoto'],
        buildingValue: 2000,
        contentsValue: 700
      }
    ];

    const request = service.toUpdateLocationsRequest(locations, 3);

    expect(request.version).toBe(3);
    expect(request.locations.length).toBe(2);
    expect(request.locations[0]).toEqual({
      index: 9,
      locationName: 'Sede Principal',
      address: 'Calle 1',
      zipCode: '11011',
      state: undefined,
      municipality: undefined,
      neighborhood: undefined,
      city: undefined,
      constructionType: undefined,
      level: undefined,
      constructionYear: undefined,
      giro: undefined,
      fireKey: 'FK1',
      coverages: ['incendio'],
      insuredAmount: 1000,
      contentsValue: 500
    });
    expect(request.locations[1].index).toBe(1);
    expect(request.locations[1].locationName).toBe('Bodega');
    expect(request.locations[1].fireKey).toBe('FK2');
    expect(request.locations[1].coverages).toEqual(['terremoto']);
    expect(request.locations[1].insuredAmount).toBe(2000);
  });

  it('should transform location into patch-by-index request', () => {
    const request = service.toPatchLocationByIndexRequest(
      {
        locationName: 'Centro',
        address: 'Cra 5',
        zipCode: '11001',
        fireKey: 'INC-1',
        buildingValue: 150,
        contentsValue: 75,
        coverages: ['incendio'],
        alertasBloqueantes: ['A1'],
        estadoValidacion: 'VALID'
      },
      17
    );

    expect(request).toEqual({
      version: 17,
      nombreUbicacion: 'Centro',
      direccion: 'Cra 5',
      codigoPostal: '11001',
      giro: undefined,
      claveIncendio: 'INC-1',
      buildingValue: 150,
      contentsValue: 75,
      garantias: ['incendio'],
      alertasBloqueantes: ['A1'],
      estadoValidacion: 'VALID'
    });
  });

  it('should not sync quote when payload does not contain valid folio/version/status', () => {
    let currentQuote: unknown;
    service.currentQuote$.subscribe((value) => {
      currentQuote = value;
    });

    service.getSummary('FOL900').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/quotes/FOL900/summary`);
    expect(req.request.method).toBe('GET');

    req.flush({
      numeroFolio: 'FOL900',
      estadoCotizacion: 'UNKNOWN',
      version: 3,
      primaNeta: 1,
      primaComercial: 1
    });

    expect(currentQuote).toBeNull();
  });
});
