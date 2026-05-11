import { StatusBadge } from './StatusBadge';

type AppHeaderProps = {
  gatewayOnline: boolean;
};

export function AppHeader({ gatewayOnline }: AppHeaderProps) {
  return (
    <header className="hero-shell">
      <div className="hero-copy">
        <span className="eyebrow">Retail OMS modernization</span>
        <h1>Strangler Fig OMS Demo</h1>
        <p>
          Modernizacion incremental de un OMS retail: el cliente conserva la misma URL, mientras el
          gateway cambia internamente el backend activo.
        </p>
      </div>

      <div className="hero-status" aria-label="Gateway health">
        <span>Gateway</span>
        <StatusBadge tone={gatewayOnline ? 'online' : 'offline'}>
          {gatewayOnline ? 'ONLINE' : 'OFFLINE'}
        </StatusBadge>
      </div>
    </header>
  );
}
