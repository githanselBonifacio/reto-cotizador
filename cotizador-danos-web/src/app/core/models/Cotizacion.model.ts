import { Location } from './Location.model';

export enum EstadoCotizacion {
  PENDING = 'PENDING',
  PENDIENTE = 'PENDIENTE',
  CALCULATED = 'CALCULATED',
  CALCULADO = 'CALCULADO',
  ERROR = 'ERROR'
}

export interface DatosAsegurado {
  nombre: string;
  rfc: string;
}

export interface DatosConduccion {
  codigoAgente: string;
}

export interface PrimaPorUbicacion {
  indice: number;
  nombreUbicacion: string;
  incendio: number;
  cat: number;
  fhm: number;
  primaNeta: number;
  alertasBloqueantes: string[];
}

export interface Cotizacion {
  numeroFolio: string;
  estadoCotizacion: EstadoCotizacion;
  datosAsegurado?: Partial<DatosAsegurado>;
  datosConduccion?: Partial<DatosConduccion>;
  version: number;
  nombre?: string;
  rfc?: string;
  codigoAgente?: string;
  configuracionLayout?: Record<string, unknown>;
  opcionesCobertura?: string[];
  primaNeta: number | null;
  primaComercial: number | null;
  primasPorUbicacion?: PrimaPorUbicacion[];
  fechaUltimaActualizacion?: string;
  locations?: Location[];
  [key: string]: unknown;
}

export interface CurrentQuote {
  folio: string;
  version: number;
  status: EstadoCotizacion;
}

export interface QuoteStateResponse {
  numeroFolio: string;
  estadoCotizacion: EstadoCotizacion;
  version: number;
  fechaUltimaActualizacion?: string;
}

export interface QuoteSummary extends QuoteStateResponse {
  primaNeta: number | null;
  primaComercial: number | null;
  totalPrimaNeta?: number | null;
  totalPrimaComercial?: number | null;
  [key: string]: unknown;
}
