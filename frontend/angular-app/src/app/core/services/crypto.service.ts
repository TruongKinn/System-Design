import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface VaultStatus {
  vaultEnabled: boolean;
  vaultConnected: boolean;
  vaultUri: string;
  secretPath: string;
  keyName: string;
  algorithm: string;
  keySizeBits: number;
  statusMessage: string;
}

export interface EncryptResult {
  cipherText: string;
  ivHex: string;
  authTagHex: string;
  cipherPayloadHex: string;
  algorithm: string;
  keySizeBits: number;
  vaultSource: boolean;
  keyStatus: string;
}

export interface DecryptResult {
  success: boolean;
  plainText: string;
  tampered: boolean;
  ivHex: string;
  authTagHex: string;
  algorithm: string;
  message: string;
  vaultSource: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class CryptoService {
  private baseUrl = 'http://localhost:8080/api/security/crypto';

  constructor(private http: HttpClient) {}

  getCryptoStatus(): Observable<VaultStatus> {
    return this.http.get<VaultStatus>(`${this.baseUrl}/status`);
  }

  encryptData(plainText: string): Observable<EncryptResult> {
    return this.http.post<EncryptResult>(`${this.baseUrl}/encrypt`, { plainText });
  }

  decryptData(cipherText: string): Observable<DecryptResult> {
    return this.http.post<DecryptResult>(`${this.baseUrl}/decrypt`, { cipherText });
  }
}
