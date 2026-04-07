import React, { useEffect, useState } from 'react';
import { Flight, FlightStatus } from '../types';
import { flightApi } from '../api/client';

const STATUS_STYLES: Record<FlightStatus, { bg: string; color: string }> = {
  SCHEDULED: { bg: '#f1f5f9', color: '#475569' },
  BOARDING: { bg: '#e0e7ff', color: '#3730a3' },
  DEPARTED: { bg: '#cffafe', color: '#155e75' },
  IN_FLIGHT: { bg: '#dbeafe', color: '#1e40af' },
  ARRIVED: { bg: '#d1fae5', color: '#065f46' },
  DELAYED: { bg: '#fef3c7', color: '#92400e' },
  CANCELLED: { bg: '#fee2e2', color: '#991b1b' },
};

function formatTime(iso: string | null): string {
  if (!iso) return '-';
  return new Date(iso).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
}

interface Props {
  onSelectFlight?: (flight: Flight) => void;
}

export default function FlightBoard({ onSelectFlight }: Props) {
  const [flights, setFlights] = useState<Flight[]>([]);
  const [filter, setFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = () => {
      flightApi.getAll()
        .then(data => { setFlights(data); setLoading(false); setError(null); })
        .catch(e => { setError('Failed to load flights'); setLoading(false); });
    };
    load();
    const iv = setInterval(load, 15000);
    return () => clearInterval(iv);
  }, []);

  const filtered = flights.filter(f => {
    if (filter && !f.flightNumber.toLowerCase().includes(filter.toLowerCase())) return false;
    if (statusFilter && f.status !== statusFilter) return false;
    return true;
  });

  if (loading) {
    return (
      <div style={{ padding: '16px 24px', flex: 2 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>Flight Board</h2>
        {[1,2,3,4,5].map(i => (
          <div key={i} style={{ height: 40, background: '#f1f5f9', borderRadius: 6, marginBottom: 8, animation: 'pulse 1.5s infinite' }} />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '16px 24px', flex: 2 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>Flight Board</h2>
        <div style={{ padding: 24, textAlign: 'center', color: '#dc2626' }}>
          {error}
          <button onClick={() => window.location.reload()} style={{ display: 'block', margin: '8px auto', padding: '6px 16px', border: '1px solid #d1d5db', borderRadius: 6, cursor: 'pointer', background: 'white' }}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ flex: 2, overflow: 'auto', padding: '16px 24px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>Flight Board</h2>
        <span style={{ fontSize: 12, color: '#64748b' }}>
          {filtered.length} flights
        </span>
      </div>

      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <input
          placeholder="Search flight..."
          value={filter}
          onChange={e => setFilter(e.target.value)}
          style={{ padding: '6px 10px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 12, width: 160 }}
        />
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          style={{ padding: '6px 10px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 12 }}
        >
          <option value="">All Status</option>
          {Object.keys(STATUS_STYLES).map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {filtered.length === 0 ? (
        <div style={{ padding: 40, textAlign: 'center', color: '#94a3b8' }}>
          No flights found. Try adjusting your filters.
        </div>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
          <thead>
            <tr>
              {['Flight', 'Route', 'Scheduled', 'Actual', 'Status', 'Gate'].map(h => (
                <th key={h} style={{ textAlign: 'left', padding: '8px 12px', borderBottom: '2px solid #e2e8f0', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.5, color: '#64748b', fontWeight: 600 }}>
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filtered.map(f => {
              const style = STATUS_STYLES[f.status];
              return (
                <tr key={f.id} onClick={() => onSelectFlight?.(f)} style={{ cursor: 'pointer' }}
                    onMouseEnter={e => (e.currentTarget.style.background = '#f8fafc')}
                    onMouseLeave={e => (e.currentTarget.style.background = '')}>
                  <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9', fontWeight: 600 }}>{f.flightNumber}</td>
                  <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9' }}>{f.departureAirport} → {f.arrivalAirport}</td>
                  <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9' }}>{formatTime(f.scheduledDeparture)}</td>
                  <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9' }}>{formatTime(f.actualDeparture)}</td>
                  <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9' }}>
                    <span style={{ padding: '3px 10px', borderRadius: 12, fontSize: 11, fontWeight: 600, background: style.bg, color: style.color }}>
                      {f.status}
                    </span>
                  </td>
                  <td style={{ padding: '10px 12px', borderBottom: '1px solid #f1f5f9' }}>{f.gate || '-'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
