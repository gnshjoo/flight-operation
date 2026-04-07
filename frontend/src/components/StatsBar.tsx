import React, { useEffect, useState } from 'react';
import { flightApi } from '../api/client';

interface Stats {
  total: number;
  inFlight: number;
  onGround: number;
  source: string;
  // from mock stats
  onTimeRate?: string;
  delayed?: number;
  cancelled?: number;
}

export default function StatsBar() {
  const [stats, setStats] = useState<Stats | null>(null);

  useEffect(() => {
    const load = async () => {
      // Try KAC stats first (has departure/arrival/delay counts)
      try {
        const kac = await flightApi.getKacStats();
        if (kac.source === 'kac' && kac.koreanAir > 0) {
          setStats({
            total: kac.koreanAir,
            inFlight: kac.departed,
            onGround: kac.arrived,
            source: 'kac',
            delayed: kac.delayed,
            cancelled: kac.cancelled,
          });
          return;
        }
      } catch {}

      // Try OpenSky stats
      try {
        const opensky = await flightApi.getOpenSkyStats();
        if (opensky.source === 'opensky' && opensky.total > 0) {
          setStats(opensky);
          return;
        }
      } catch {}

      // Fallback to mock stats
      try {
        const mock = await flightApi.getDailyStats();
        setStats({
          total: mock.totalFlights,
          inFlight: 0,
          onGround: 0,
          source: 'mock',
          onTimeRate: mock.onTimeRate,
          delayed: mock.delayed,
          cancelled: mock.cancelled,
        });
      } catch {}
    };

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

  const isRealData = stats.source === 'opensky' || stats.source === 'kac';

  const cards = stats.source === 'kac' ? [
    { label: 'KE Flights', value: stats.total, color: '#1a1a2e', sub: '한국공항공사 실시간' },
    { label: 'Departed', value: stats.inFlight, color: '#0072CE', sub: '출발' },
    { label: 'Arrived', value: stats.onGround, color: '#059669', sub: '도착' },
    { label: 'Delayed', value: stats.delayed ?? 0, color: '#d97706', sub: '지연' },
  ] : isRealData ? [
    { label: 'Total Aircraft', value: stats.total, color: '#1a1a2e', sub: 'tracked by OpenSky' },
    { label: 'In Flight', value: stats.inFlight, color: '#0072CE', sub: 'currently airborne' },
    { label: 'On Ground', value: stats.onGround, color: '#059669', sub: 'at airports' },
    { label: 'Data Source', value: 'LIVE', color: '#003876', sub: 'OpenSky Network' },
  ] : [
    { label: 'Total Flights', value: stats.total, color: '#1a1a2e', sub: 'demo data' },
    { label: 'On Time', value: `${stats.onTimeRate}%`, color: '#059669', sub: '' },
    { label: 'Delayed', value: stats.delayed ?? 0, color: '#d97706', sub: '' },
    { label: 'Cancelled', value: stats.cancelled ?? 0, color: '#dc2626', sub: '' },
  ];

  return (
    <div style={{ display: 'flex', gap: 16, padding: '16px 24px', background: 'white', borderBottom: '1px solid #e2e8f0' }}>
      {cards.map(c => (
        <div key={c.label} style={{ flex: 1, padding: '12px 16px', borderRadius: 8, background: '#f8fafc' }}>
          <div style={{ fontSize: 11, color: '#64748b', textTransform: 'uppercase', letterSpacing: 0.5 }}>{c.label}</div>
          <div style={{ fontSize: 28, fontWeight: 700, color: c.color, marginTop: 4 }}>{c.value}</div>
          {c.sub && <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>{c.sub}</div>}
        </div>
      ))}
    </div>
  );
}
