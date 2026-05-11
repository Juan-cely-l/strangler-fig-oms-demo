import assert from 'node:assert/strict';
import test from 'node:test';
import type { ApiResult, OrderResponse } from '../src/types/api.ts';
import { phases } from '../src/utils/demoPhase.ts';
import {
  addOrderHistoryEntry,
  createOrderHistoryEntry,
  isTerminalOrderStatus,
  orderStatusNarrative,
  updateOrderHistoryEntry
} from '../src/utils/orderHistory.ts';

test('adds a newly created modern order to recent history as pending', () => {
  const entry = createOrderHistoryEntry('order-1', apiResult({
    id: 1,
    customerId: 'CUST-001',
    status: 'PENDING',
    backend: 'MODERN_ORDER',
    createdAt: '2026-05-10T10:00:00.000Z',
    items: [{ sku: 'MOUSE-001', quantity: 1, unitPrice: 25 }]
  }), phases[1], '2026-05-10T10:00:01.000Z');

  const history = addOrderHistoryEntry([], entry);

  assert.equal(history.length, 1);
  assert.equal(history[0].phaseKey, 'B');
  assert.equal(history[0].currentStatus, 'PENDING');
  assert.equal(orderStatusNarrative(history[0]), 'Waiting for inventory event...');
});

test('updates pending history entry when order converges to confirmed', () => {
  const entry = createOrderHistoryEntry('order-1', apiResult({
    id: 1,
    customerId: 'CUST-001',
    status: 'PENDING',
    backend: 'MODERN_ORDER',
    createdAt: '2026-05-10T10:00:00.000Z',
    items: [{ sku: 'MOUSE-001', quantity: 1, unitPrice: 25 }]
  }), phases[2], '2026-05-10T10:00:00.500Z');

  const history = updateOrderHistoryEntry([entry], 'order-1', apiResult({
    id: 1,
    customerId: 'CUST-001',
    status: 'CONFIRMED',
    backend: 'MODERN_ORDER',
    createdAt: '2026-05-10T10:00:00.000Z',
    items: [{ sku: 'MOUSE-001', quantity: 1, unitPrice: 25 }]
  }), '2026-05-10T10:00:03.000Z');

  assert.equal(history[0].currentStatus, 'CONFIRMED');
  assert.equal(history[0].observedConvergenceLatencyMs, 3000);
  assert.equal(orderStatusNarrative(history[0]), 'Inventory reserved.');
});

test('identifies terminal and pending order statuses', () => {
  assert.equal(isTerminalOrderStatus('CONFIRMED'), true);
  assert.equal(isTerminalOrderStatus('REJECTED'), true);
  assert.equal(isTerminalOrderStatus('PENDING'), false);
});

function apiResult(data: OrderResponse): ApiResult<OrderResponse> {
  return {
    data,
    latencyMs: 12,
    correlationId: 'corr-test'
  };
}
