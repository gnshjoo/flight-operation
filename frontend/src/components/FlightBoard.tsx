import React, { useEffect, useState } from 'react';
import { flightApi } from '../api/client';

interface DisplayFlight {
  id: string;
  callsign: string;
  route: string;
  altitude: string;
  speed: string;
  status: string;
  statusStyle: { bg: string; color: string };
}

const IN_FLIGHT_STYLE = { bg: '#dbeafe', color: '#1e40af' };
const ON_GROUND_STYLE = { bg: '#d1fae5', color: '#065f46' };
const MOCK_STYLES: Record<string, { bg: string; color: string }> = {
  SCHEDULED: { bg: '#f1f5f9', color: '#475569' },
  BOARDING: { bg: '#e0e7ff', color: '#3730a3' },
  DEPARTED: { bg: '#cffafe', color: '#155e75' },
  IN_FLIGHT: { bg: '#dbeafe', color: '#1e40af' },
  ARRIVED: { bg: '#d1fae5', color: '#065f46' },
  DELAYED: { bg: '#fef3c7', color: '#92400e' },
  CANCELLED: { bg: '#fee2e2', color: '#991b1b' },
};

function formatCallsign(cs: string): string {
  return cs.replace(/^KAL/, 'KE');
}

export default function FlightBoard() {
  const [flights, setFlights] = useState<DisplayFlight[]>([]);
  const [filter, setFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [source, setSource] = useState('');

  useEffect(() => {
    const load = async () => {
      // Priority 1: Combined (KAC + OpenSky)
      try {
        const combined = await flightApi.getCombined();
        if (combined.totalFlights > 0) {
          const mapped: DisplayFlight[] = combined.flights.map((f: any) => {
            const status = f.status || '-';
            const style = status.includes('DEPART') ? MOCK_STYLES.DEPARTED
              : status.includes('ARRIV') ? MOCK_STYLES.ARRIVED
              : status.includes('DELAY') ? MOCK_STYLES.DELAYED
              : status.includes('CANCEL') ? MOCK_STYLES.CANCELLED
              : status.includes('BOARD') ? MOCK_STYLES.BOARDING
              : IN_FLIGHT_STYLE;
            return {
              id: `${f.flightNumber}-${f.scheduledTime}-${f.direction}`,
              callsign: f.flightNumber,
              route: `${f.departureAirport} → ${f.arrivalAirport}`,
              altitude: f.altitude ? `${Math.round(f.altitude * 3.281).toLocaleString()}ft` : f.scheduledTime || '-',
              speed: f.velocity ? `${Math.round(f.velocity * 1.944)}kts` : f.estimatedTime || '-',
              status: f.statusKor || status,
              statusStyle: style,
            };
          });
          setFlights(mapped);
          setSource(combined.kacSource === 'kac' ? 'kac+opensky' : 'opensky');
          setLoading(false);
          return;
        }
      } catch {}

      // Priority 2: OpenSky only
      try {
        const opensky = await flightApi.getOpenSky();
        if (opensky.source === 'opensky' && opensky.aircraft?.length > 0) {
          const mapped: DisplayFlight[] = opensky.aircraft.map((a: any) => ({
            id: a.callsign,
            callsign: formatCallsign(a.callsign),
            route: a.departureAirport && a.arrivalAirport ? `${a.departureAirport} → ${a.arrivalAirport}` : '-',
            altitude: a.onGround ? '-' : `${Math.round(a.altitude * 3.281).toLocaleString()}ft`,
            speed: a.onGround ? '-' : `${Math.round(a.velocity * 1.944)}kts`,
            status: a.onGround ? 'ON GROUND' : 'IN FLIGHT',
            statusStyle: a.onGround ? ON_GROUND_STYLE : IN_FLIGHT_STYLE,
          }));
          setFlights(mapped);
          setSource('opensky');
          setLoading(false);
          return;
        }
      } catch {}

      // Priority 3: Mock data
      try {
        const mock = await flightApi.getAll();
        setFlights(mock.map((f: any) => ({
          id: String(f.id),
          callsign: f.flightNumber,
          route: `${f.departureAirport} → ${f.arrivalAirport}`,
          altitude: '-',
          speed: '-',
          status: f.status,
          statusStyle: MOCK_STYLES[f.status] || MOCK_STYLES.SCHEDULED,
        })));
        setSource('mock');
      } catch {}
      setLoading(false);
    };

    load();
    const iv = setInterval(load, 60000);
    return () => clearInterval(iv);
  }, []);

  const filtered = flights.filter(f => {
    if (filter && !f.callsign.toLowerCase().includes(filter.toLowerCase()) && !f.route.toLowerCase().includes(filter.toLowerCase())) return false;
    if (statusFilter && f.status !== statusFilter) return false;
    return true;
  });

  const statusOptions = Array.from(new Set(flights.map(f => f.status)));

  if (loading) {
    return (
      <div style={{ padding: '16px 24px', flex: 1 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>Flight Board</h2>
        {[1,2,3,4,5].map(i => (
          <div key={i} style={{ height: 40, background: '#f1f5f9', borderRadius: 6, marginBottom: 8 }} />
        ))}
      </div>
    );
  }

  return (
    <div style={{ flex: 1, padding: '16px 24px', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>Flight Board</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{
            fontSize: 11, padding: '2px 8px', borderRadius: 10,
            background: source === 'opensky' ? '#d1fae5' : '#f1f5f9',
            color: source === 'opensky' ? '#065f46' : '#64748b',
          }}>
            {source === 'opensky' ? `OpenSky — ${filtered.length} aircraft` : `${filtered.length} flights`}
          </span>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <input
          placeholder="Search flight or route..."
          value={filter}
          onChange={e => setFilter(e.target.value)}
          style={{ padding: '6px 10px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 12, width: 180 }}
        />
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          style={{ padding: '6px 10px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 12 }}
        >
          <option value="">All Status</option>
          {statusOptions.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      <div style={{ flex: 1, overflowY: 'auto' }}>
      {filtered.length === 0 ? (
        <div style={{ padding: 40, textAlign: 'center', color: '#94a3b8' }}>
          No flights found.
        </div>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr>
              {['Flight', 'Route', source === 'opensky' ? 'Altitude' : 'Scheduled', source === 'opensky' ? 'Speed' : 'Actual', 'Status'].map(h => (
                <th key={h} style={{ textAlign: 'left', padding: '8px 12px', borderBottom: '2px solid #e2e8f0', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.5, color: '#64748b', fontWeight: 600 }}>
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.map(f => (
              <tr key={f.id}
                  onMouseEnter={e => (e.currentTarget.style.background = '#f8fafc')}
                  onMouseLeave={e => (e.currentTarget.style.background = '')}>
                <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9', fontWeight: 600 }}>{f.callsign}</td>
                <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9' }}>{f.route}</td>
                <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9' }}>{f.altitude}</td>
                <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9' }}>{f.speed}</td>
                <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9' }}>
                  <span style={{ padding: '3px 10px', borderRadius: 12, fontSize: 11, fontWeight: 600, background: f.statusStyle.bg, color: f.statusStyle.color }}>
                    {f.status}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      </div>
    </div>
  );
}
