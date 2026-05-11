import type { MigrationStatus } from '../types/api';
import { StatusBadge } from './StatusBadge';

type ArchitectureDiagramProps = {
  status: MigrationStatus;
};

export function ArchitectureDiagram({ status }: ArchitectureDiagramProps) {
  return (
    <section className="panel architecture-panel" aria-labelledby="architecture-title">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Routing story</span>
          <h2 id="architecture-title">Client → Gateway → Backend</h2>
        </div>
        <p>La ruta publica no cambia; solo cambia el destino interno.</p>
      </div>

      <div className="architecture-map">
        <DiagramNode title="Client" note="Browser / curl" active />
        <span className="diagram-arrow" aria-hidden="true">→</span>
        <DiagramNode title="Gateway" note="localhost:8080" active />
        <span className="diagram-arrow" aria-hidden="true">→</span>

        <div className="backend-lanes">
          <RouteLane
            label="Orders"
            targets={[
              {
                name: 'Legacy OMS',
                note: 'Synchronous order + stock',
                active: !status.ordersEnabled,
                badge: 'LEGACY'
              },
              {
                name: 'Modern Order Service',
                note: 'PENDING order + SQS event',
                active: status.ordersEnabled,
                badge: 'MODERN_ORDER'
              }
            ]}
          />
          <RouteLane
            label="Inventory"
            targets={[
              {
                name: 'Legacy OMS',
                note: 'Remaining monolith capability',
                active: !status.inventoryEnabled,
                badge: 'LEGACY'
              },
              {
                name: 'Inventory Service',
                note: 'Eventual stock reservation',
                active: status.inventoryEnabled,
                badge: 'INVENTORY_SERVICE'
              }
            ]}
          />
        </div>
      </div>
    </section>
  );
}

type Target = {
  name: string;
  note: string;
  active: boolean;
  badge: string;
};

function RouteLane({ label, targets }: { label: string; targets: Target[] }) {
  const activeTarget = targets.find((target) => target.active);

  return (
    <div className="route-lane">
      <div className="route-lane-head">
        <strong>{label}</strong>
        {activeTarget ? (
          <StatusBadge tone={activeTarget.badge === 'LEGACY' ? 'legacy' : 'modern'}>
            {activeTarget.badge}
          </StatusBadge>
        ) : null}
      </div>
      <div className="route-targets">
        {targets.map((target) => (
          <DiagramNode key={`${label}-${target.name}`} title={target.name} note={target.note} active={target.active} />
        ))}
      </div>
    </div>
  );
}

function DiagramNode({ title, note, active }: { title: string; note: string; active: boolean }) {
  return (
    <div className={`diagram-node ${active ? 'diagram-node-active' : ''}`}>
      <strong>{title}</strong>
      <span>{note}</span>
    </div>
  );
}
