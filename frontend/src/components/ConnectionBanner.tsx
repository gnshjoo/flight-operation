import React from 'react';

interface Props {
  connected: boolean;
}

export default function ConnectionBanner({ connected }: Props) {
  if (connected) return null;

  return (
    <div style={{
      background: '#fef3c7',
      color: '#92400e',
      padding: '8px 24px',
      fontSize: 13,
      display: 'flex',
      alignItems: 'center',
      gap: 8,
      borderBottom: '1px solid #fcd34d',
    }}>
      <span style={{ animation: 'pulse 1.5s infinite' }}>&#9888;</span>
      Real-time connection lost. Reconnecting...
    </div>
  );
}
