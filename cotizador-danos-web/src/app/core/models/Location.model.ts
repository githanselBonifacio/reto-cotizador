export type EstadoValidacionUbicacion =
  | 'INCOMPLETE'
  | 'VALID'
  | 'PENDIENTE'
  | 'VALIDADA'
  | 'RECHAZADA';

export interface Location {
  id?: string;
  numeroFolio?: string;
  indice?: number;
  version?: number;
  locationName?: string;
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
  insuredAmount?: number | null;
  buildingValue?: number | null;
  contentsValue?: number | null;
  coverages?: string[];
  garantias?: string[];
  alertasBloqueantes?: string[];
  estadoValidacion?: EstadoValidacionUbicacion;
  nombreUbicacion?: string;
  direccion?: string;
  codigoPostal?: string;
  claveIncendio?: string;
  nombre?: string;
  primaNeta?: number | null;
  primaComercial?: number | null;
  [key: string]: unknown;
}

export interface PutLocationsRequest {
  locations: Location[];
}

export interface PatchLocationsRequest {
  version: number;
  nombreUbicacion?: string;
  direccion?: string;
  codigoPostal?: string;
  giro?: string;
  claveIncendio?: string;
  buildingValue?: number | null;
  contentsValue?: number | null;
  garantias?: string[];
  alertasBloqueantes?: string[];
  estadoValidacion?: EstadoValidacionUbicacion;
}

export type LocationsResponse =
  | {
      locations: Location[];
      version: number;
    }
  | Location[];
