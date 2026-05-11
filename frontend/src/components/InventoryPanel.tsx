import type { ApiResult, InventoryResponse, MigrationStatus } from '../types/api';
import { activeInventoryRoute } from '../utils/demoPhase';
import { ResponsePanel } from './ResponsePanel';
import { StatusBadge } from './StatusBadge';

const skuOptions = ['MOUSE-001', 'LAPTOP-001', 'KEYBOARD-001'];

type InventoryPanelProps = {
  sku: string;
  status: MigrationStatus;
  loading: boolean;
  result?: ApiResult<InventoryResponse>;
  error?: string;
  onSkuChange: (value: string) => void;
  onLookup: () => void;
};

export function InventoryPanel({
  sku,
  status,
  loading,
  result,
  error,
  onSkuChange,
  onLookup
}: InventoryPanelProps) {
  const route = activeInventoryRoute(status);
  const inventory = result?.data;

  return (
    <section className="panel action-panel" aria-labelledby="inventory-title">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">GET /inventory/{'{sku}'}</span>
          <h2 id="inventory-title">Inventory</h2>
        </div>
        <StatusBadge tone={route === 'LEGACY' ? 'legacy' : 'modern'}>{route}</StatusBadge>
      </div>

      {error ? (
        <div className="banner banner-error" role="alert">
          <strong>Inventory request failed</strong>
          <span>{error}</span>
        </div>
      ) : null}

      {result ? (
        <div className="banner banner-success" role="status">
          <strong>Inventory served by {inventory?.backend ?? route}</strong>
          <span>Latency: {result.latencyMs} ms.</span>
        </div>
      ) : null}

      <div className="form-stack">
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

        <button className="btn btn-primary" disabled={loading} onClick={onLookup}>
          {loading ? 'Querying...' : 'Query inventory'}
        </button>
      </div>

      {inventory ? (
        <dl className="inventory-result">
          <div>
            <dt>SKU</dt>
            <dd>{inventory.sku ?? '-'}</dd>
          </div>
          <div>
            <dt>Product</dt>
            <dd>{inventory.productName ?? '-'}</dd>
          </div>
          <div>
            <dt>Available</dt>
            <dd>{inventory.availableQuantity ?? '-'}</dd>
          </div>
          <div>
            <dt>Reserved</dt>
            <dd>{inventory.reservedQuantity ?? '-'}</dd>
          </div>
        </dl>
      ) : null}

      <ResponsePanel title="Inventory response JSON" result={result} />
    </section>
  );
}
