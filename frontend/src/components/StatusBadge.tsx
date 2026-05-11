type StatusBadgeProps = {
  children: string;
  tone?: 'online' | 'offline' | 'legacy' | 'modern' | 'neutral';
};

export function StatusBadge({ children, tone = 'neutral' }: StatusBadgeProps) {
  return <span className={`status-badge status-badge-${tone}`}>{children}</span>;
}
