import assert from 'node:assert/strict';
import test from 'node:test';
import { isJsonContentType } from '../src/utils/httpContent.ts';

test('detects normal and vendor JSON content types', () => {
  assert.equal(isJsonContentType('application/json'), true);
  assert.equal(isJsonContentType('application/vnd.spring-boot.actuator.v3+json'), true);
  assert.equal(isJsonContentType('text/html'), false);
});
