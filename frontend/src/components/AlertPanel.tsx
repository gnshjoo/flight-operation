import React from 'react';
import { Alert, Severity } from '../types';

const SEVERITY_COLOR: Record<Severity, string> = {
  CRITICAL: '#dc2626',
  WARNING: '#d97706',
  INFO: '#2563eb',
};

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  return `${hrs}h ago`;
}

interface Props {
  alerts: Alert[];
  onAcknowledge: (id: number) => void;
}

export default function AlertPanel({ alerts, onAcknowledge }: Props) {
  const activeCount = alerts.filter(a => !a.acknowledged).length;

  return (
    <div style={{ width: 340, minWidth: 340, borderLeft: '1px solid #e2e8f0', background: 'white', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: 16, borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h3 style={{ fontSize: 14, fontWeight: 600, margin: 0 }}>Alerts</h3>
        {activeCount > 0 && (
          <span style={{ background: '#dc2626', color: 'white', padding: '2px 8px', borderRadius: 10, fontSize: 11 }}>
            {activeCount} new
          </span>
        )}
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        {alerts.length === 0 ? (
          <div style={{ padding: 40, textAlign: 'center', color: '#94a3b8' }}>
            <div style={{ fontSize: 32, marginBottom: 8 }}>&#10003;</div>
            All clear. No alerts.
          </div>
        ) : (
          alerts.slice(0, 50).map(alert => (
            <div
              key={alert.id}
              style={{
                padding: '12px 16px',
                borderBottom: '1px solid #f1f5f9',
                opacity: alert.acknowledged ? 0.6 : 1,
              }}
            >
              <div style={{
                fontSize: 11, fontWeight: 600, textTransform: 'uppercase',
                letterSpacing: 0.5, color: SEVERITY_COLOR[alert.severity],
              }}>
                {alert.type.replace('_', ' ')}
              </div>
              <div style={{ fontSize: 13, marginTop: 4, color: '#374151' }}>
                {alert.message}
              </div>
              <div style={{ fontSize: 11, color: '#9ca3af', marginTop: 4 }}>
                {timeAgo(alert.createdAt)}
              </div>
              {!alert.acknowledged ? (
                <button
                  onClick={() => onAcknowledge(alert.id)}
                  style={{
                    fontSize: 11, color: '#0072CE', cursor: 'pointer',
                    border: 'none', background: 'none', padding: '4px 0', marginTop: 4,
                  }}
                >
                  Acknowledge
                </button>
              ) : (
                <div style={{ fontSize: 11, color: '#059669', marginTop: 4 }}>&#10003; Acknowledged</div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
