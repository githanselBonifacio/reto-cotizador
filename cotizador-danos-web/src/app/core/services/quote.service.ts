import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, EMPTY, map, Observable, Subject, tap } from 'rxjs';

import {
  Cotizacion,
  CurrentQuote,
  EstadoCotizacion,
  QuoteStateResponse,
  QuoteSummary
} from '../models/Cotizacion.model';
import {
  EstadoValidacionUbicacion,
  Location,
  LocationsResponse,
  PatchLocationsRequest
} from '../models/Location.model';
import { environment } from '../../../environments/environment';

export interface GeneralInfoRequest {
  version: number;
  datosAsegurado: {
    nombre: string;
    rfc: string;
  };
  codigoAgente: string;
}

export interface GeneralInfoResponse {
  datosAsegurado: {
    nombre: string;
    rfc: string;
  };
  codigoAgente: string;
  version: number;
}

export interface LocationRequest {
  index: number;
  locationName: string;
  address?: string;
  zipCode?: string;
  state?: string;
  municipality?: string;
  neighborhood?: string;
  city?: string;
  constructionType?: string;
  level?: string;
  constructionYear?: number | null;
  giro?: string;
  fireKey?: string;
  coverages?: string[];
  insuredAmount?: number | null;
  contentsValue?: number | null;
}

export interface UpdateLocationsRequest {
  version: number;
  locations: LocationRequest[];
}

export interface LocationsEnvelope {
  locations: Location[];
  version: number;
}

export interface PatchLocationByIndexRequest extends PatchLocationsRequest {}

export interface LocationsLayoutResponse {
  configuracionLayout: Record<string, unknown>;
  version: number;
}

export interface UpdateLocationsLayoutRequest {
  version: number;
  configuracionLayout: Record<string, unknown>;
}

export interface CoverageOptionsResponse {
  opcionesCobertura: string[];
  version: number;
}

export interface UpdateCoverageOptionsRequest {
  version: number;
  opcionesCobertura: string[];
}

export interface CalculateQuoteRequest {
  version: number;
  parametros_calculo: Record<string, number>;
}

export interface LocationsSummaryResponse {
  totalLocations: number;
  validLocations: number;
  warningLocations: number;
  totalBlockingAlerts: number;
  totalPrimaNeta: number;
  totalPrimaComercial: number;
}

@Injectable({
  providedIn: 'root'
})
export class QuoteService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = environment.apiUrl;
  private readonly rootApiUrl = this.apiBaseUrl.replace(/\/v1\/?$/, '');

  private readonly currentQuoteSubject = new BehaviorSubject<CurrentQuote | null>(null);
  private readonly refreshRequestedSubject = new Subject<void>();
  readonly currentQuote$ = this.currentQuoteSubject.asObservable();
  readonly refreshRequested$ = this.refreshRequestedSubject.asObservable();



  initializeQuote(): Observable<Cotizacion> {
    return this.http.post<Cotizacion>(`${this.apiBaseUrl}/folios`, {}).pipe(
      tap((quote) => this.syncCurrentQuote(quote))
    );
  }

  getGeneralInfo(folio: string): Observable<GeneralInfoResponse> {
    return this.http.get<GeneralInfoResponse>(this.quotePath(folio, '/general-info')).pipe(
      tap((response) => {
        const currentQuote = this.currentQuoteSubject.value;

        if (!currentQuote) {
          return;
        }

        this.currentQuoteSubject.next({
          ...currentQuote,
          version: response.version
        });
      })
    );
  }

  updateGeneralInfo(folio: string, payload: GeneralInfoRequest): Observable<Cotizacion> {
    return this.http.put<Cotizacion>(this.quotePath(folio, '/general-info'), payload).pipe(
      tap((quote) => this.syncCurrentQuote(quote))
    );
  }

  getLocations(folio: string): Observable<LocationsEnvelope> {
    return this.http.get<LocationsResponse>(this.quotePath(folio, '/locations')).pipe(
      map((response) => this.normalizeLocationsEnvelope(response)),
      tap((response) => this.syncVersionOnly(response.version))
    );
  }

  updateLocations(folio: string, request: UpdateLocationsRequest): Observable<Cotizacion> {
    return this.http.put<Cotizacion>(this.quotePath(folio, '/locations'), request).pipe(
      tap((quote) => this.syncCurrentQuote(quote))
    );
  }

  patchLocationByIndex(
    folio: string,
    index: number,
    request: PatchLocationByIndexRequest
  ): Observable<Cotizacion> {
    return this.http
      .patch<Cotizacion>(this.quotePath(folio, `/locations/${index}`), request)
      .pipe(tap((quote) => this.syncCurrentQuote(quote)));
  }

  getLocationsLayout(folio: string): Observable<LocationsLayoutResponse> {
    return this.http.get<LocationsLayoutResponse>(this.quotePath(folio, '/locations/layout')).pipe(
      tap((response) => this.syncVersionOnly(response.version))
    );
  }

  updateLocationsLayout(
    folio: string,
    request: UpdateLocationsLayoutRequest
  ): Observable<Cotizacion> {
    return this.http.put<Cotizacion>(this.quotePath(folio, '/locations/layout'), request).pipe(
      tap((quote) => this.syncCurrentQuote(quote))
    );
  }

  getCoverageOptions(folio: string): Observable<CoverageOptionsResponse> {
    return this.http.get<CoverageOptionsResponse>(this.quotePath(folio, '/coverage-options')).pipe(
      tap((response) => this.syncVersionOnly(response.version))
    );
  }

  updateCoverageOptions(
    folio: string,
    request: UpdateCoverageOptionsRequest
  ): Observable<Cotizacion> {
    return this.http.put<Cotizacion>(this.quotePath(folio, '/coverage-options'), request).pipe(
      tap((quote) => this.syncCurrentQuote(quote))
    );
  }

  calculateQuote(folio: string, request: CalculateQuoteRequest): Observable<Cotizacion> {
    return this.http.post<Cotizacion>(this.quotePath(folio, '/calculate'), request).pipe(
      tap((quote) => this.syncCurrentQuote(quote))
    );
  }

  getQuoteState(folio: string): Observable<CurrentQuote> {
    return this.http.get<QuoteStateResponse>(this.quotePath(folio, '/state')).pipe(
      map((state) => this.toCurrentQuote(state)),
      tap((state) => this.currentQuoteSubject.next(state))
    );
  }

  getLocationsSummary(folio: string): Observable<LocationsSummaryResponse> {
    return this.http.get<LocationsSummaryResponse>(this.quotePath(folio, '/locations/summary'));
  }

  getSummary(folio: string): Observable<QuoteSummary> {
    return this.http.get<QuoteSummary>(this.quotePath(folio, '/summary')).pipe(
      tap((summary) => this.syncCurrentQuote(summary))
    );
  }

  clearCurrentQuote(): void {
    this.currentQuoteSubject.next(null);
  }

  refreshCurrentQuoteState(): Observable<CurrentQuote> {
    const currentQuote = this.currentQuoteSubject.value;

    if (!currentQuote?.folio) {
      return EMPTY;
    }

    return this.getQuoteState(currentQuote.folio);
  }

  requestQuoteRefresh(): void {
    this.refreshRequestedSubject.next();
  }

  toUpdateLocationsRequest(frontLocations: Location[], version: number): UpdateLocationsRequest {
    return {
      version,
      locations: frontLocations.map((location, idx) => ({
        index: location.indice ?? idx,
        locationName: location.locationName ?? location.nombreUbicacion ?? '',
        address: location.address ?? location.direccion,
        zipCode: location.zipCode ?? location.codigoPostal,
        state: location.state,
        municipality: location.municipality,
        neighborhood: location.neighborhood,
        city: location.city,
        constructionType: location.constructionType,
        level: location.level,
        constructionYear: location.constructionYear,
        giro: location.giro,
        fireKey: location.fireKey ?? location.claveIncendio,
        coverages: location.coverages ?? location.garantias,
        insuredAmount: location.insuredAmount ?? location.buildingValue,
        contentsValue: location.contentsValue
      }))
    };
  }

  toPatchLocationByIndexRequest(frontLocation: Partial<Location>, version: number): PatchLocationByIndexRequest {
    return {
      version,
      nombreUbicacion: frontLocation.nombreUbicacion ?? frontLocation.locationName,
      direccion: frontLocation.direccion ?? frontLocation.address,
      codigoPostal: frontLocation.codigoPostal ?? frontLocation.zipCode,
      giro: frontLocation.giro,
      claveIncendio: frontLocation.claveIncendio ?? frontLocation.fireKey,
      buildingValue: frontLocation.buildingValue,
      contentsValue: frontLocation.contentsValue,
      garantias: frontLocation.garantias ?? frontLocation.coverages,
      alertasBloqueantes: frontLocation.alertasBloqueantes,
      estadoValidacion: frontLocation.estadoValidacion as EstadoValidacionUbicacion | undefined
    };
  }

  private quotePath(folio: string, suffix: string): string {
    return `${this.apiBaseUrl}/quotes/${encodeURIComponent(folio)}${suffix}`;
  }

  private normalizeLocationsEnvelope(response: LocationsResponse): LocationsEnvelope {
    if (Array.isArray(response)) {
      return {
        locations: response,
        version: this.currentQuoteSubject.value?.version ?? 0
      };
    }

    return {
      locations: response.locations,
      version: response.version
    };
  }

  private syncCurrentQuote(response: Partial<Cotizacion>): void {
    const folio = typeof response.numeroFolio === 'string' ? response.numeroFolio : null;
    const version = typeof response.version === 'number' ? response.version : null;
    const status = this.parseEstado(response.estadoCotizacion);

    if (!folio || version === null || status === null) {
      return;
    }

    this.currentQuoteSubject.next({
      folio,
      version,
      status
    });
  }

  private syncVersionOnly(version: number): void {
    const current = this.currentQuoteSubject.value;

    if (!current) {
      return;
    }

    this.currentQuoteSubject.next({
      ...current,
      version
    });
  }

  private toCurrentQuote(state: QuoteStateResponse): CurrentQuote {
    return {
      folio: state.numeroFolio,
      version: state.version,
      status: state.estadoCotizacion
    };
  }

  private parseEstado(value: unknown): EstadoCotizacion | null {
    if (value === EstadoCotizacion.PENDING || value === EstadoCotizacion.PENDIENTE) {
      return EstadoCotizacion.PENDING;
    }

    if (value === EstadoCotizacion.CALCULATED || value === EstadoCotizacion.CALCULADO) {
      return EstadoCotizacion.CALCULATED;
    }

    if (value === EstadoCotizacion.ERROR) {
      return EstadoCotizacion.ERROR;
    }

    return null;
  }
}
