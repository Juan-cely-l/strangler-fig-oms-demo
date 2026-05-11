import type { DemoPhase, DemoPhaseKey, InventoryRoute, MigrationStatus, OrderRoute } from '../types/api';

export const phases: DemoPhase[] = [
  {
    key: 'A',
    title: 'Fase A: Todo en legacy',
    description: 'Orders e inventory se atienden desde el monolito legacy.'
  },
  {
    key: 'B',
    title: 'Fase B: Orders migrado',
    description: 'Orders se atiende desde el servicio moderno y publica eventos.'
  },
  {
    key: 'C',
    title: 'Fase C: Orders + Inventory migrados',
    description: 'Orders e inventory ya conviven en servicios modernos.'
  }
];

export const mixedPhase: DemoPhase = {
  key: 'MIXED',
  title: 'Estado mixto: rollback parcial',
  description: 'Orders e inventory apuntan a backends distintos. Esto no es Fase A pura.'
};

export function currentPhase(status: MigrationStatus): DemoPhase {
  if (status.ordersEnabled && status.inventoryEnabled) {
    return phases[2];
  }
  if (status.ordersEnabled) {
    return phases[1];
  }
  if (status.inventoryEnabled) {
    return mixedPhase;
  }
  return phases[0];
}

export function phaseDisplayName(key: DemoPhaseKey): string {
  return key === 'MIXED' ? 'Estado mixto' : `Fase ${key}`;
}

export function activeOrderBackend(status: MigrationStatus): 'LEGACY' | 'MODERN' {
  return status.ordersEnabled ? 'MODERN' : 'LEGACY';
}

export function activeInventoryBackend(status: MigrationStatus): 'LEGACY' | 'MODERN' {
  return status.inventoryEnabled ? 'MODERN' : 'LEGACY';
}

export function activeOrderRoute(status: MigrationStatus): OrderRoute {
  return status.ordersEnabled ? 'MODERN_ORDER' : 'LEGACY';
}

export function activeInventoryRoute(status: MigrationStatus): InventoryRoute {
  return status.inventoryEnabled ? 'INVENTORY_SERVICE' : 'LEGACY';
}
