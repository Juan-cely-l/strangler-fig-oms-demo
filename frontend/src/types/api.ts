export type MigrationStatus = {
  ordersEnabled: boolean;
  inventoryEnabled: boolean;
};

export type OrderRoute = 'LEGACY' | 'MODERN_ORDER';

export type InventoryRoute = 'LEGACY' | 'INVENTORY_SERVICE';

export type HealthStatus = {
  status: string;
};

export type OrderItemRequest = {
  sku: string;
  quantity: number;
  unitPrice: number;
};

export type CreateOrderRequest = {
  customerId: string;
  items: OrderItemRequest[];
};

export type OrderResponse = {
  id?: number;
  customerId?: string;
  status?: string;
  createdAt?: string;
  backend?: string;
  items?: OrderItemRequest[];
  correlationId?: string;
};

export type InventoryResponse = {
  sku?: string;
  productName?: string;
  availableQuantity?: number;
  reservedQuantity?: number;
  backend?: string;
};

export type ApiResult<T> = {
  data: T;
  correlationId?: string;
  latencyMs: number;
};

export type DemoPhaseKey = 'A' | 'B' | 'C' | 'MIXED';

export type DemoPhase = {
  key: DemoPhaseKey;
  title: string;
  description: string;
};

export type OrderHistoryEntry = {
  localId: string;
  orderId?: number;
  customerId?: string;
  skuSummary: string;
  phaseKey: DemoPhaseKey;
  phaseLabel: string;
  initialBackend: string;
  initialStatus: string;
  currentStatus: string;
  createdAt?: string;
  lastCheckedAt?: string;
  observedConvergenceLatencyMs?: number;
  initialResponse: ApiResult<OrderResponse>;
  latestResponse?: ApiResult<OrderResponse>;
  checking: boolean;
  message: string;
  error?: string;
};
