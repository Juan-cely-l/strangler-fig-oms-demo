import { useCallback, useEffect, useState } from 'react';
import { gatewayApi } from './api/gatewayApi';
import { AppHeader } from './components/AppHeader';
import { ArchitectureDiagram } from './components/ArchitectureDiagram';
import { CreateOrderPanel } from './components/CreateOrderPanel';
import { InventoryPanel } from './components/InventoryPanel';
import { MigrationControls } from './components/MigrationControls';
import { OrderHistoryPanel } from './components/OrderHistoryPanel';
import { PhaseTimeline } from './components/PhaseTimeline';
import { StatusSummary } from './components/StatusSummary';
import type { ApiResult, InventoryResponse, MigrationStatus, OrderHistoryEntry, OrderResponse } from './types/api';
import { currentPhase } from './utils/demoPhase';
import { isGatewayOnline } from './utils/gatewayHealth';
import {
  addOrderHistoryEntry,
  createOrderHistoryEntry,
  failOrderHistoryRefresh,
  isTerminalOrderStatus,
  markOrderHistoryChecking,
  updateOrderHistoryEntry
} from './utils/orderHistory';

const defaultStatus: MigrationStatus = {
  ordersEnabled: false,
  inventoryEnabled: false
};

export default function App() {
  const [gatewayOnline, setGatewayOnline] = useState(false);
  const [status, setStatus] = useState<MigrationStatus>(defaultStatus);
  const [statusLoading, setStatusLoading] = useState(false);
  const [globalError, setGlobalError] = useState<string>();

  const [customerId, setCustomerId] = useState('CUST-UI-001');
  const [orderSku, setOrderSku] = useState('MOUSE-001');
  const [quantity, setQuantity] = useState(1);
  const [unitPrice, setUnitPrice] = useState(25.0);
  const [orderLoading, setOrderLoading] = useState(false);
  const [orderPolling, setOrderPolling] = useState(false);
  const [orderResult, setOrderResult] = useState<ApiResult<OrderResponse>>();
  const [orderFinalResult, setOrderFinalResult] = useState<ApiResult<OrderResponse>>();
  const [orderError, setOrderError] = useState<string>();
  const [orderConvergenceMessage, setOrderConvergenceMessage] = useState<string>();
  const [orderHistory, setOrderHistory] = useState<OrderHistoryEntry[]>([]);

  const [inventorySku, setInventorySku] = useState('MOUSE-001');
  const [inventoryLoading, setInventoryLoading] = useState(false);
  const [inventoryResult, setInventoryResult] = useState<ApiResult<InventoryResponse>>();
  const [inventoryError, setInventoryError] = useState<string>();

  const loadStatus = useCallback(async () => {
    setStatusLoading(true);
    setGlobalError(undefined);
    try {
      const health = await gatewayApi.health();
      setGatewayOnline(isGatewayOnline(health.data));
    } catch (error) {
      setGatewayOnline(false);
      setGlobalError(error instanceof Error ? error.message : 'No se pudo contactar el gateway');
      setStatusLoading(false);
      return;
    }

    try {
      const migration = await gatewayApi.getMigrationStatus();
      setStatus(migration.data);
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : 'No se pudo leer el estado de migracion');
    } finally {
      setStatusLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadStatus();
  }, [loadStatus]);

  async function runMigrationAction(action: () => Promise<ApiResult<MigrationStatus>>) {
    setStatusLoading(true);
    setGlobalError(undefined);
    try {
      const response = await action();
      setStatus(response.data);
      setGatewayOnline(true);
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : 'No se pudo actualizar la migracion');
    } finally {
      setStatusLoading(false);
    }
  }

  async function createOrder() {
    setOrderLoading(true);
    setOrderError(undefined);
    setOrderResult(undefined);
    setOrderFinalResult(undefined);
    setOrderConvergenceMessage(undefined);
    const creationPhase = currentPhase(status);
    try {
      const response = await gatewayApi.createOrder({
        customerId,
        items: [{ sku: orderSku, quantity, unitPrice }]
      });
      const localId = `${response.data.backend ?? 'UNKNOWN'}-${response.data.id ?? Date.now()}-${Date.now()}`;
      const entry = createOrderHistoryEntry(localId, response, creationPhase, new Date().toISOString());
      setOrderResult(response);
      setOrderHistory((current) => addOrderHistoryEntry(current, entry));
      if (response.data.backend === 'MODERN_ORDER' && response.data.id && response.data.status === 'PENDING') {
        if (status.inventoryEnabled) {
          setOrderConvergenceMessage('Fase C: PENDING es temporal; esperando resultado de inventario...');
        } else {
          setOrderConvergenceMessage('Fase B: POST /orders ya va a modern-order-service. La consulta publica de inventario sigue en legacy, pero la orden puede converger por eventos.');
        }
        void pollOrderConvergence(localId, response.data.id);
      }
    } catch (error) {
      setOrderError(error instanceof Error ? error.message : 'No se pudo crear la orden');
    } finally {
      setOrderLoading(false);
    }
  }

  async function pollOrderConvergence(localId: string, orderId: number) {
    setOrderPolling(true);
    setOrderHistory((current) => markOrderHistoryChecking(current, localId, true));
    setOrderConvergenceMessage('Esperando resultado asincrono de inventario...');
    try {
      for (let attempt = 0; attempt < 10; attempt += 1) {
        await delay(1200);
        const latest = await gatewayApi.getOrder(orderId);
        const observedAt = new Date().toISOString();
        setOrderFinalResult(latest);
        setOrderHistory((current) => updateOrderHistoryEntry(current, localId, latest, observedAt));
        if (isTerminalOrderStatus(latest.data.status)) {
          setOrderConvergenceMessage(`Orden convergida a ${latest.data.status}.`);
          return;
        }
        setOrderHistory((current) => markOrderHistoryChecking(current, localId, true));
      }
      setOrderConvergenceMessage('La orden sigue PENDING. Revisa logs de SQS/inventory o consulta nuevamente por ID.');
      setOrderHistory((current) => markOrderHistoryChecking(current, localId, false));
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo consultar la convergencia de la orden';
      setOrderConvergenceMessage(message);
      setOrderHistory((current) => failOrderHistoryRefresh(current, localId, message));
    } finally {
      setOrderPolling(false);
    }
  }

  async function refreshHistoryEntry(entry: OrderHistoryEntry) {
    if (!entry.orderId) {
      return;
    }
    setOrderHistory((current) => markOrderHistoryChecking(current, entry.localId, true));
    try {
      const latest = await gatewayApi.getOrder(entry.orderId);
      setOrderHistory((current) => updateOrderHistoryEntry(current, entry.localId, latest, new Date().toISOString()));
    } catch (error) {
      setOrderHistory((current) => failOrderHistoryRefresh(
        current,
        entry.localId,
        error instanceof Error ? error.message : 'No se pudo refrescar la orden'
      ));
    }
  }

  async function queryInventory() {
    setInventoryLoading(true);
    setInventoryError(undefined);
    setInventoryResult(undefined);
    try {
      const response = await gatewayApi.getInventory(inventorySku);
      setInventoryResult(response);
    } catch (error) {
      setInventoryError(error instanceof Error ? error.message : 'No se pudo consultar inventario');
    } finally {
      setInventoryLoading(false);
    }
  }

  return (
    <main className="app-shell">
      <AppHeader gatewayOnline={gatewayOnline} />

      {globalError ? (
        <div className="banner banner-error" role="alert">
          <strong>{gatewayOnline ? 'Gateway reachable' : 'Gateway unavailable'}</strong>
          <span>{globalError}</span>
        </div>
      ) : null}

      <StatusSummary gatewayOnline={gatewayOnline} migrationStatus={status} loading={statusLoading} />

      <section className="executive-grid" aria-label="Demo state and controls">
        <PhaseTimeline status={status} />
        <MigrationControls
          status={status}
          loading={statusLoading}
          onEnableOrders={() => runMigrationAction(gatewayApi.enableOrdersMigration)}
          onDisableOrders={() => runMigrationAction(gatewayApi.disableOrdersMigration)}
          onEnableInventory={() => runMigrationAction(gatewayApi.enableInventoryMigration)}
          onDisableInventory={() => runMigrationAction(gatewayApi.disableInventoryMigration)}
        />
      </section>

      <section className="workspace-grid" aria-label="Demo actions">
        <CreateOrderPanel
          status={status}
          customerId={customerId}
          sku={orderSku}
          quantity={quantity}
          unitPrice={unitPrice}
          loading={orderLoading}
          polling={orderPolling}
          result={orderResult}
          finalResult={orderFinalResult}
          error={orderError}
          convergenceMessage={orderConvergenceMessage}
          onCustomerIdChange={setCustomerId}
          onSkuChange={setOrderSku}
          onQuantityChange={setQuantity}
          onUnitPriceChange={setUnitPrice}
          onCreateOrder={createOrder}
        />
        <InventoryPanel
          sku={inventorySku}
          status={status}
          loading={inventoryLoading}
          result={inventoryResult}
          error={inventoryError}
          onSkuChange={setInventorySku}
          onLookup={queryInventory}
        />
      </section>

      <OrderHistoryPanel entries={orderHistory} status={status} onRefresh={refreshHistoryEntry} />

      <ArchitectureDiagram status={status} />
    </main>
  );
}

function delay(ms: number) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
}
