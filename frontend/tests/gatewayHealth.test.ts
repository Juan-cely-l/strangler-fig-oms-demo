import assert from 'node:assert/strict';
import test from 'node:test';
import { isGatewayOnline } from '../src/utils/gatewayHealth.ts';

test('gateway health UP is online even when migration flags are disabled', () => {
  assert.equal(isGatewayOnline({ status: 'UP' }), true);
});

test('missing or non-UP health is offline', () => {
  assert.equal(isGatewayOnline({ status: 'DOWN' }), false);
  assert.equal(isGatewayOnline(undefined), false);
});
