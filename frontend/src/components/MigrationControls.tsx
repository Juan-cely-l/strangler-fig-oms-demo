import type { MigrationStatus } from '../types/api';

type MigrationControlsProps = {
  status: MigrationStatus;
  loading: boolean;
  onEnableOrders: () => void;
  onDisableOrders: () => void;
  onEnableInventory: () => void;
  onDisableInventory: () => void;
};

export function MigrationControls({
  status,
  loading,
  onEnableOrders,
  onDisableOrders,
  onEnableInventory,
  onDisableInventory
}: MigrationControlsProps) {
  return (
    <section className="panel controls-panel" aria-labelledby="migration-controls-title">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Gateway flags</span>
          <h2 id="migration-controls-title">Migration controls</h2>
        </div>
        <p>Activa o revierte rutas sin cambiar la URL publica del cliente.</p>
      </div>

      <div className="control-rows">
        <ControlRow
          label="Orders"
          enabled={status.ordersEnabled}
          loading={loading}
          onEnable={onEnableOrders}
          onDisable={onDisableOrders}
        />
        <ControlRow
          label="Inventory"
          enabled={status.inventoryEnabled}
          loading={loading}
          onEnable={onEnableInventory}
          onDisable={onDisableInventory}
        />
      </div>
    </section>
  );
}

function ControlRow({
  label,
  enabled,
  loading,
  onEnable,
  onDisable
}: {
  label: string;
  enabled: boolean;
  loading: boolean;
  onEnable: () => void;
  onDisable: () => void;
}) {
  return (
    <div className="control-row">
      <div>
        <strong>{label}</strong>
        <span>{enabled ? 'Modern route enabled' : 'Legacy route active'}</span>
      </div>
      <div className="button-cluster">
        <button className="btn btn-primary" disabled={loading || enabled} onClick={onEnable}>
          {loading ? 'Working...' : 'Enable'}
        </button>
        <button className="btn btn-secondary" disabled={loading || !enabled} onClick={onDisable}>
          Disable
        </button>
      </div>
    </div>
  );
}
