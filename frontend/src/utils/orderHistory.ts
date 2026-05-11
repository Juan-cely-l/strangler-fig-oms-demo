import type { ApiResult, DemoPhase, OrderHistoryEntry, OrderResponse } from '../types/api';

const maxRecentOrders = 10;
const terminalStatuses = new Set(['CONFIRMED', 'REJECTED', 'INVENTORY_REJECTED', 'CANCELLED']);

export function isTerminalOrderStatus(status?: string): boolean {
  return terminalStatuses.has((status ?? '').toUpperCase());
}

export function orderStatusNarrative(entry: Pick<OrderHistoryEntry, 'initialBackend' | 'currentStatus'>): string {
  const status = entry.currentStatus.toUpperCase();
  if (entry.initialBackend === 'LEGACY') {
    return 'Orden legacy procesada de forma sincrona en el monolito.';
  }
  if (status === 'PENDING') {
    return 'Waiting for inventory event...';
  }
  if (status === 'CONFIRMED') {
    return 'Inventory reserved.';
  }
  if (status === 'REJECTED' || status === 'INVENTORY_REJECTED') {
    return 'Inventory rejected / insufficient stock.';
  }
  return 'Estado actualizado desde el backend de ordenes.';
}

export function createOrderHistoryEntry(
  localId: string,
  result: ApiResult<OrderResponse>,
  phase: DemoPhase,
  observedAt: string
): OrderHistoryEntry {
  const status = result.data.status ?? 'UNKNOWN';
  const entry: OrderHistoryEntry = {
    localId,
    orderId: result.data.id,
    customerId: result.data.customerId,
    skuSummary: summarizeSku(result.data),
    phaseKey: phase.key,
    phaseLabel: phaseDisplayName(phase.key),
    initialBackend: result.data.backend ?? 'UNKNOWN',
    initialStatus: status,
    currentStatus: status,
    createdAt: result.data.createdAt ?? observedAt,
    lastCheckedAt: observedAt,
    initialResponse: result,
    checking: false,
    message: ''
  };

  return {
    ...entry,
    observedConvergenceLatencyMs: computeObservedConvergenceLatency(entry, observedAt),
    message: orderStatusNarrative(entry)
  };
}

export function addOrderHistoryEntry(history: OrderHistoryEntry[], entry: OrderHistoryEntry): OrderHistoryEntry[] {
  return [entry, ...history.filter((item) => item.localId !== entry.localId)].slice(0, maxRecentOrders);
}

export function markOrderHistoryChecking(
  history: OrderHistoryEntry[],
  localId: string,
  checking: boolean,
  error?: string
): OrderHistoryEntry[] {
  return history.map((entry) => entry.localId === localId
    ? { ...entry, checking, error, message: checking ? 'Waiting for inventory event...' : entry.message }
    : entry);
}

export function updateOrderHistoryEntry(
  history: OrderHistoryEntry[],
  localId: string,
  latest: ApiResult<OrderResponse>,
  observedAt: string
): OrderHistoryEntry[] {
  return history.map((entry) => {
    if (entry.localId !== localId) {
      return entry;
    }

    const currentStatus = latest.data.status ?? entry.currentStatus;
    const next: OrderHistoryEntry = {
      ...entry,
      currentStatus,
      latestResponse: latest,
      lastCheckedAt: observedAt,
      checking: false,
      error: undefined
    };

    return {
      ...next,
      observedConvergenceLatencyMs: computeObservedConvergenceLatency(next, observedAt),
      message: orderStatusNarrative(next)
    };
  });
}

export function failOrderHistoryRefresh(
  history: OrderHistoryEntry[],
  localId: string,
  error: string
): OrderHistoryEntry[] {
  return history.map((entry) => entry.localId === localId
    ? { ...entry, checking: false, error }
    : entry);
}

function summarizeSku(order: OrderResponse): string {
  if (!order.items || order.items.length === 0) {
    return 'No items';
  }

  return order.items
    .map((item) => `${item.sku} x${item.quantity}`)
    .join(', ');
}

function phaseDisplayName(key: DemoPhase['key']): string {
  return key === 'MIXED' ? 'Estado mixto' : `Fase ${key}`;
}

function computeObservedConvergenceLatency(entry: OrderHistoryEntry, observedAt: string): number | undefined {
  if (entry.initialBackend !== 'MODERN_ORDER' || !isTerminalOrderStatus(entry.currentStatus) || !entry.createdAt) {
    return undefined;
  }

  const startedAt = Date.parse(entry.createdAt);
  const endedAt = Date.parse(observedAt);
  if (!Number.isFinite(startedAt) || !Number.isFinite(endedAt) || endedAt < startedAt) {
    return undefined;
  }

  return endedAt - startedAt;
}
