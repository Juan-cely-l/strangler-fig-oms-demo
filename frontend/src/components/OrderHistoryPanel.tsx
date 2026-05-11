import type { MigrationStatus, OrderHistoryEntry } from '../types/api';
import { activeOrderRoute } from '../utils/demoPhase';
import { isTerminalOrderStatus } from '../utils/orderHistory';
import { ResponsePanel } from './ResponsePanel';
import { StatusBadge } from './StatusBadge';

type OrderHistoryPanelProps = {
  entries: OrderHistoryEntry[];
  status: MigrationStatus;
  onRefresh: (entry: OrderHistoryEntry) => void;
};

export function OrderHistoryPanel({ entries, status, onRefresh }: OrderHistoryPanelProps) {
  return (
    <section className="panel history-panel" aria-labelledby="order-history-title">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Order trace</span>
          <h2 id="order-history-title">Recent order history</h2>
        </div>
        <p>Changing phases changes gateway routing, not historical data.</p>
      </div>

      <p className="history-note">
        Cambiar de fase modifica el enrutamiento del gateway, no mueve datos historicos. Las ordenes modernas
        permanecen en modern-order-service y convergen mediante eventos de inventario.
      </p>

      {entries.length === 0 ? (
        <div className="history-empty">
          Crea una orden en Fase A, B o C para ver aqui su backend inicial, estado actual y convergencia.
        </div>
      ) : (
        <div className="history-list">
          {entries.map((entry) => (
            <OrderHistoryRow
              key={entry.localId}
              entry={entry}
              currentOrderRoute={activeOrderRoute(status)}
              onRefresh={() => onRefresh(entry)}
            />
          ))}
        </div>
      )}
    </section>
  );
}

function OrderHistoryRow({
  entry,
  currentOrderRoute,
  onRefresh
}: {
  entry: OrderHistoryEntry;
  currentOrderRoute: string;
  onRefresh: () => void;
}) {
  const canRefresh = entry.orderId !== undefined && entry.initialBackend === currentOrderRoute;
  const terminal = isTerminalOrderStatus(entry.currentStatus);
  const response = entry.latestResponse ?? entry.initialResponse;

  return (
    <article className="history-row">
      <div className="history-row-top">
        <div>
          <strong>Order {entry.orderId ? `#${entry.orderId}` : 'without id'}</strong>
          <span>{entry.customerId ?? 'Unknown customer'} · {entry.skuSummary}</span>
        </div>
        <div className="history-badges">
          <StatusBadge tone={entry.phaseKey === 'MIXED' ? 'neutral' : 'online'}>{entry.phaseLabel}</StatusBadge>
          <StatusBadge tone={entry.initialBackend === 'LEGACY' ? 'legacy' : 'modern'}>{entry.initialBackend}</StatusBadge>
          <StatusBadge tone={statusTone(entry.currentStatus)}>{entry.currentStatus}</StatusBadge>
        </div>
      </div>

      <dl className="history-meta">
        <div>
          <dt>Initial status</dt>
          <dd>{entry.initialStatus}</dd>
        </div>
        <div>
          <dt>Current status</dt>
          <dd>{entry.currentStatus}</dd>
        </div>
        <div>
          <dt>Created at</dt>
          <dd>{formatDate(entry.createdAt)}</dd>
        </div>
        <div>
          <dt>Convergence</dt>
          <dd>{formatLatency(entry.observedConvergenceLatencyMs, terminal, entry.initialBackend)}</dd>
        </div>
      </dl>

      <div className="history-row-bottom">
        <p className={entry.currentStatus === 'PENDING' ? 'history-narrative pending' : 'history-narrative'}>
          {entry.message}
        </p>
        <button className="btn btn-secondary" disabled={!canRefresh || entry.checking} onClick={onRefresh}>
          {entry.checking ? 'Refreshing...' : 'Refresh status'}
        </button>
      </div>

      {!canRefresh && entry.initialBackend === 'MODERN_ORDER' ? (
        <p className="history-hint">
          Para refrescar esta orden moderna por el gateway, Orders route debe estar en MODERN_ORDER.
        </p>
      ) : null}

      {entry.error ? (
        <div className="banner banner-error" role="alert">
          <strong>Refresh failed</strong>
          <span>{entry.error}</span>
        </div>
      ) : null}

      <ResponsePanel title="Order trace JSON" result={response} />
    </article>
  );
}

function statusTone(status: string): 'online' | 'offline' | 'legacy' | 'modern' | 'neutral' {
  switch (status) {
    case 'CONFIRMED':
      return 'online';
    case 'PENDING':
      return 'neutral';
    case 'REJECTED':
    case 'INVENTORY_REJECTED':
    case 'CANCELLED':
      return 'offline';
    default:
      return 'modern';
  }
}

function formatDate(value?: string): string {
  if (!value) {
    return 'N/A';
  }

  return new Intl.DateTimeFormat('es-CO', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(value));
}

function formatLatency(value: number | undefined, terminal: boolean, backend: string): string {
  if (backend !== 'MODERN_ORDER') {
    return 'Sync legacy';
  }
  if (!terminal) {
    return 'Pending';
  }
  if (value === undefined) {
    return 'Observed';
  }
  return `${value} ms`;
}
