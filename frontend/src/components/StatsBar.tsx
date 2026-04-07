import React, { useEffect, useState } from 'react';
import { flightApi } from '../api/client';
import { DailyStats } from '../types';

export default function StatsBar() {
  const [stats, setStats] = useState<DailyStats | null>(null);

  useEffect(() => {
    const load = () => flightApi.getDailyStats().then(setStats).catch(console.warn);
    load();
    const iv = setInterval(load, 30000);
    return () => clearInterval(iv);
  }, []);

  if (!stats) {
    return (
      <div style={{ display: 'flex', gap: 16, padding: '16px 24px', background: 'white', borderBottom: '1px solid #e2e8f0' }}>
        {[1,2,3,4].map(i => (
          <div key={i} style={{ flex: 1, padding: '12px 16px', borderRadius: 8, background: '#f1f5f9', height: 72 }} />
        ))}
      </div>
    );
  }

  const cards = [
    { label: 'Total Flights', value: stats.totalFlights, color: '#1a1a2e' },
    { label: 'On Time', value: `${stats.onTimeRate}%`, color: '#059669' },
    { label: 'Delayed', value: stats.delayed, color: '#d97706' },
    { label: 'Cancelled', value: stats.cancelled, color: '#dc2626' },
  ];

  return (
    <div style={{ display: 'flex', gap: 16, padding: '16px 24px', background: 'white', borderBottom: '1px solid #e2e8f0' }}>
      {cards.map(c => (
        <div key={c.label} style={{ flex: 1, padding: '12px 16px', borderRadius: 8, background: '#f8fafc' }}>
          <div style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5 }}>{c.label}</div>
          <div style={{ fontSize: 28, fontWeight: 700, color: c.color, marginTop: 4 }}>{c.value}</div>
        </div>
      ))}
    </div>
  );
}
