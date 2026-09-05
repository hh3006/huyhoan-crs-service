// path: crs-frontend/src/types/auth.ts
// purpose: bo sung userId khop voi LoginResponseDTO moi cua auth-service
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  userId: number;
  token: string;
  username: string;
  role: 'ADMIN' | 'STUDENT';
}
