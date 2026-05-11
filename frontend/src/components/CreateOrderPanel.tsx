import type { FormEvent } from 'react';
import type { ApiResult, MigrationStatus, OrderResponse } from '../types/api';
import { activeOrderRoute } from '../utils/demoPhase';
import { ResponsePanel } from './ResponsePanel';
import { StatusBadge } from './StatusBadge';

const skuOptions = ['MOUSE-001', 'LAPTOP-001', 'KEYBOARD-001'];

type CreateOrderPanelProps = {
  customerId: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  status: MigrationStatus;
  loading: boolean;
  polling: boolean;
  result?: ApiResult<OrderResponse>;
  finalResult?: ApiResult<OrderResponse>;
  error?: string;
  convergenceMessage?: string;
  onCustomerIdChange: (value: string) => void;
  onSkuChange: (value: string) => void;
  onQuantityChange: (value: number) => void;
  onUnitPriceChange: (value: number) => void;
  onCreateOrder: () => void;
};

function toNumber(value: string, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function CreateOrderPanel({
  customerId,
  sku,
  quantity,
  unitPrice,
  status,
  loading,
  polling,
  result,
  finalResult,
  error,
  convergenceMessage,
  onCustomerIdChange,
  onSkuChange,
  onQuantityChange,
  onUnitPriceChange,
  onCreateOrder
}: CreateOrderPanelProps) {
  const route = activeOrderRoute(status);

  function submitOrder(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onCreateOrder();
  }

  return (
    <section className="panel action-panel" aria-labelledby="create-order-title">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">POST /orders</span>
          <h2 id="create-order-title">Create order</h2>
        </div>
        <StatusBadge tone={route === 'LEGACY' ? 'legacy' : 'modern'}>{route}</StatusBadge>
      </div>

      {error ? (
        <div className="banner banner-error" role="alert">
          <strong>Order request failed</strong>
          <span>{error}</span>
        </div>
      ) : null}

      {result ? (
        <div className="banner banner-success" role="status">
          <strong>Order {result.data.id ?? ''} {result.data.status ?? 'created'}</strong>
          <span>Backend: {result.data.backend ?? route}. Latency: {result.latencyMs} ms.</span>
        </div>
      ) : null}

      {convergenceMessage ? (
        <div className={finalResult && finalResult.data.status !== 'PENDING' ? 'banner banner-success' : 'banner banner-neutral'} role="status">
          <strong>{polling ? 'Checking convergence' : 'Order convergence'}</strong>
          <span>{convergenceMessage}</span>
        </div>
      ) : null}

      <form className="form-stack" onSubmit={submitOrder}>
        <label>
          <span>Customer ID</span>
          <input value={customerId} onChange={(event) => onCustomerIdChange(event.target.value)} />
        </label>

        <label>
          <span>SKU</span>
          <select value={sku} onChange={(event) => onSkuChange(event.target.value)}>
            {skuOptions.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>

        <div className="form-grid-two">
          <label>
            <span>Quantity</span>
            <input
              min="1"
              type="number"
              value={quantity}
              onChange={(event) => onQuantityChange(toNumber(event.target.value, quantity))}
            />
          </label>
          <label>
            <span>Unit price</span>
            <input
              min="0"
              step="0.01"
              type="number"
              value={unitPrice}
              onChange={(event) => onUnitPriceChange(toNumber(event.target.value, unitPrice))}
            />
          </label>
        </div>

        <button className="btn btn-primary" disabled={loading || !customerId || !sku || quantity < 1 || unitPrice < 0}>
          {loading ? 'Creating...' : 'Create order'}
        </button>
      </form>

      <ResponsePanel title="Order response JSON" result={result} />
      <ResponsePanel title="Latest order state JSON" result={finalResult} />
    </section>
  );
}
