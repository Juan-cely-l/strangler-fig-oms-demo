import assert from 'node:assert/strict';
import test from 'node:test';
import { currentPhase } from '../src/utils/demoPhase.ts';

test('calculates pure demo phases from gateway flags', () => {
  assert.equal(currentPhase({ ordersEnabled: false, inventoryEnabled: false }).key, 'A');
  assert.equal(currentPhase({ ordersEnabled: true, inventoryEnabled: false }).key, 'B');
  assert.equal(currentPhase({ ordersEnabled: true, inventoryEnabled: true }).key, 'C');
});

test('detects mixed rollback state when only inventory remains modern', () => {
  const phase = currentPhase({ ordersEnabled: false, inventoryEnabled: true });

  assert.equal(phase.key, 'MIXED');
  assert.match(phase.description, /no es Fase A/i);
});
