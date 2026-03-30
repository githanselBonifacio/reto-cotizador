import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

interface PagedResponse<T> {
  total: number;
  items: T[];
}

export interface AgentCatalogItem {
  _id: string;
  name: string;
  agent_code: string;
  status: string;
}

export interface BusinessLineCatalogItem {
  _id: string;
  code: string;
  name: string;
  status: string;
}

export interface GuaranteeCatalogItem {
  _id: string;
  code: string;
  name: string;
  coverage_type: string;
  coverage_amount: number;
}

export interface ZipValidationResponse {
  is_valid: boolean;
  zip_code: string;
  city: string | null;
  state: string | null;
  risk_zone: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class CoreCatalogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.coreApiUrl;

  getAgents(limit = 100): Observable<AgentCatalogItem[]> {
    const params = new HttpParams().set('skip', 0).set('limit', limit);

    return this.http
      .get<PagedResponse<AgentCatalogItem>>(`${this.baseUrl}/agents`, { params })
      .pipe(map((response) => response.items ?? []));
  }

  getBusinessLines(limit = 100): Observable<BusinessLineCatalogItem[]> {
    const params = new HttpParams().set('skip', 0).set('limit', limit);

    return this.http
      .get<PagedResponse<BusinessLineCatalogItem>>(`${this.baseUrl}/business-lines`, { params })
      .pipe(map((response) => response.items ?? []));
  }

  getGuarantees(limit = 100): Observable<GuaranteeCatalogItem[]> {
    const params = new HttpParams().set('skip', 0).set('limit', limit);

    return this.http
      .get<PagedResponse<GuaranteeCatalogItem>>(`${this.baseUrl}/catalogs/guarantees`, { params })
      .pipe(map((response) => response.items ?? []));
  }

  validateZipCode(zipCode: string): Observable<ZipValidationResponse> {
    return this.http.post<ZipValidationResponse>(`${this.baseUrl}/zip-codes/validate`, {
      zip_code: zipCode
    });
  }
}
