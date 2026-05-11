import type { ApiResult } from '../types/api';

type ResponsePanelProps<T> = {
  title: string;
  result?: ApiResult<T>;
};

export function ResponsePanel<T>({ title, result }: ResponsePanelProps<T>) {
  if (!result) {
    return null;
  }

  return (
    <details className="response-details">
      <summary>{title}</summary>
      <div className="response-meta">
        <span>Latency: {result.latencyMs} ms</span>
        {result.correlationId ? <span>Correlation ID: {result.correlationId}</span> : null}
      </div>
      <pre className="response-json">{JSON.stringify(result.data, null, 2)}</pre>
    </details>
  );
}
