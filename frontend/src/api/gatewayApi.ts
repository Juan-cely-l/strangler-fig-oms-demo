import type {
  ApiResult,
  CreateOrderRequest,
  HealthStatus,
  InventoryResponse,
  MigrationStatus,
  OrderResponse
} from '../types/api';
import { isJsonContentType } from '../utils/httpContent';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

async function request<T>(path: string, init?: RequestInit): Promise<ApiResult<T>> {
  const startedAt = performance.now();
  let response: Response;
  const headers = new Headers(init?.headers);
  if (init?.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers
    });
  } catch {
    throw new Error(`No se pudo contactar el gateway en ${API_BASE_URL}. Verifica que este activo y que CORS permita el frontend.`);
  }

  const latencyMs = Math.round(performance.now() - startedAt);
  const contentType = response.headers.get('content-type') ?? '';
  const correlationId = response.headers.get('x-correlation-id') ?? undefined;
  const isJson = isJsonContentType(contentType);

  if (!response.ok) {
    const message = isJson
      ? JSON.stringify(await response.json())
      : await response.text();
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  const data = isJson
    ? ((await response.json()) as T)
    : ({} as T);

  return { data, correlationId, latencyMs };
}

export const gatewayApi = {
  health: () => request<HealthStatus>('/actuator/health'),
  getMigrationStatus: () => request<MigrationStatus>('/migration/status'),
  enableOrdersMigration: () => request<MigrationStatus>('/migration/orders/enable', { method: 'POST' }),
  disableOrdersMigration: () => request<MigrationStatus>('/migration/orders/disable', { method: 'POST' }),
  enableInventoryMigration: () => request<MigrationStatus>('/migration/inventory/enable', { method: 'POST' }),
  disableInventoryMigration: () => request<MigrationStatus>('/migration/inventory/disable', { method: 'POST' }),
  createOrder: (payload: CreateOrderRequest) =>
    request<OrderResponse>('/orders', {
      method: 'POST',
      body: JSON.stringify(payload)
    }),
  getOrder: (id: number) => request<OrderResponse>(`/orders/${id}`),
  getInventory: (sku: string) => request<InventoryResponse>(`/inventory/${encodeURIComponent(sku)}`)
};
