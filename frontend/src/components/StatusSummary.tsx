import type { MigrationStatus } from '../types/api';
import { activeInventoryRoute, activeOrderRoute } from '../utils/demoPhase';
import { StatusBadge } from './StatusBadge';

type StatusSummaryProps = {
  gatewayOnline: boolean;
  migrationStatus: MigrationStatus;
  loading: boolean;
};

export function StatusSummary({ gatewayOnline, migrationStatus, loading }: StatusSummaryProps) {
  const orderRoute = activeOrderRoute(migrationStatus);
  const inventoryRoute = activeInventoryRoute(migrationStatus);

  return (
    <section className="status-strip" aria-label="Runtime summary">
      <Metric
        label="Gateway status"
        value={loading ? 'CHECKING' : gatewayOnline ? 'ONLINE' : 'OFFLINE'}
        tone={loading ? 'neutral' : gatewayOnline ? 'online' : 'offline'}
      />
      <Metric
        label="Orders route"
        value={orderRoute}
        tone={orderRoute === 'LEGACY' ? 'legacy' : 'modern'}
      />
      <Metric
        label="Inventory route"
        value={inventoryRoute}
        tone={inventoryRoute === 'LEGACY' ? 'legacy' : 'modern'}
      />
    </section>
  );
}

function Metric({
  label,
  value,
  tone
}: {
  label: string;
  value: string;
  tone: 'online' | 'offline' | 'legacy' | 'modern' | 'neutral';
}) {
  return (
    <div className="metric">
      <span className="metric-label">{label}</span>
      <StatusBadge tone={tone}>{value}</StatusBadge>
    </div>
  );
}
