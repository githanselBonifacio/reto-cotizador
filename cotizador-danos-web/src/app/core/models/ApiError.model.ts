export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}

export type ApiErrorResponse = ApiError;
