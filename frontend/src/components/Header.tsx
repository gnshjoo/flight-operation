import React from 'react';

interface Props {
  wsConnected: boolean;
}

export default function Header({ wsConnected }: Props) {
  return (
    <header style={{ background: '#003876', color: 'white', padding: '12px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <div>
        <h1 style={{ fontSize: 18, fontWeight: 600, margin: 0, letterSpacing: 0.5 }}>
          KOREAN AIR Flight Operations
        </h1>
        <div style={{ fontSize: 12, opacity: 0.7 }}>Operations Control Dashboard</div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, fontSize: 13 }}>
        <span style={{
          width: 8, height: 8, borderRadius: '50%',
          background: wsConnected ? '#34d399' : '#f87171',
          display: 'inline-block'
        }} />
        <span>{wsConnected ? 'Live' : 'Disconnected'}</span>
        <span style={{ background: '#0072CE', padding: '2px 8px', borderRadius: 10, fontSize: 11 }}>DEMO</span>
      </div>
    </header>
  );
}
