import type { HealthStatus } from '../types/api';

export function isGatewayOnline(health?: HealthStatus): boolean {
  return health?.status === 'UP';
}
