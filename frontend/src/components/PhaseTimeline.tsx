import type { MigrationStatus } from '../types/api';
import { currentPhase, phaseDisplayName, phases } from '../utils/demoPhase';
import { StatusBadge } from './StatusBadge';

type PhaseTimelineProps = {
  status: MigrationStatus;
};

export function PhaseTimeline({ status }: PhaseTimelineProps) {
  const phase = currentPhase(status);
  const isMixed = phase.key === 'MIXED';

  return (
    <section className="panel phase-panel" aria-labelledby="current-phase-title">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Current phase</span>
          <h2 id="current-phase-title">{phaseDisplayName(phase.key)}</h2>
        </div>
        {isMixed ? (
          <StatusBadge tone="neutral">ROLLBACK PARCIAL</StatusBadge>
        ) : (
          <p>{phase.title.replace(`Fase ${phase.key}: `, '')}</p>
        )}
      </div>

      <p className="phase-summary">{phase.description}</p>
      {isMixed ? (
        <p className="phase-note">
          Fase A pura requiere orders en LEGACY e inventory en LEGACY. Desactivar solo un flag deja un estado mixto.
        </p>
      ) : null}

      <ol className="phase-rail" aria-label="Demo phases">
        {phases.map((item) => (
          <li className={item.key === phase.key ? 'active' : ''} key={item.key}>
            <span>{item.key}</span>
            <strong>{item.title.replace(`Fase ${item.key}: `, '')}</strong>
          </li>
        ))}
      </ol>
    </section>
  );
}
