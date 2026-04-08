import React, { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell
} from 'recharts';
import { flightApi } from '../api/client';

const STATUS_COLORS: Record<string, string> = {
  '출발': '#0072CE',
  '도착': '#059669',
  '지연': '#d97706',
  '결항': '#dc2626',
  '수속중': '#8b5cf6',
  '탑승중': '#6366f1',
  '탑승최종': '#4f46e5',
  '탑승장 입장': '#818cf8',
  'other': '#94a3b8',
};

const AIRPORT_COLORS = ['#003876', '#0072CE', '#059669', '#d97706', '#dc2626', '#8b5cf6', '#6366f1', '#0891b2'];

export default function StatsCharts() {
  const [airportData, setAirportData] = useState<any[]>([]);
  const [statusData, setStatusData] = useState<any[]>([]);
  const [source, setSource] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const kac = await flightApi.getKac();
        if (kac.source === 'kac' && kac.flights?.length > 0) {
          const flights = kac.flights;

          // Airport distribution from real KAC data
          const airportCounts: Record<string, number> = {};
          flights.forEach((f: any) => {
            const airport = f.airport || f.AIRPORT;
            if (airport) airportCounts[airport] = (airportCounts[airport] || 0) + 1;
          });
          setAirportData(
            Object.entries(airportCounts)
              .map(([airport, count]) => ({ airport, flights: count }))
              .sort((a, b) => b.flights - a.flights)
          );

          // Status distribution from real KAC data
          const statusCounts: Record<string, number> = {};
          flights.forEach((f: any) => {
            const rmk = (f.rmkKor || f.RMK_KOR || '').trim();
            const status = rmk || 'other';
            statusCounts[status] = (statusCounts[status] || 0) + 1;
          });
          setStatusData(
            Object.entries(statusCounts)
              .map(([name, value]) => ({ name, value }))
              .sort((a, b) => b.value - a.value)
          );

          setSource('kac');
          return;
        }
      } catch {}
      setSource('unavailable');
    };

    load();
    const iv = setInterval(load, 60000);
    return () => clearInterval(iv);
  }, []);

  return (
    <div style={{ padding: '16px 24px', background: 'white', borderTop: '1px solid #e2e8f0' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>Statistics</h2>
        {source === 'kac' && (
          <span style={{ fontSize: 11, padding: '2px 8px', borderRadius: 10, background: '#d1fae5', color: '#065f46' }}>
            Real-time KAC data
          </span>
        )}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 16 }}>
        {/* Flights by Airport */}
        <div style={{ background: '#f8fafc', borderRadius: 8, padding: 16 }}>
          <h4 style={{ fontSize: 12, color: '#64748b', marginBottom: 8, margin: 0 }}>KE Flights by Airport</h4>
          <ResponsiveContainer width="100%" height={180}>
            <BarChart data={airportData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="airport" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 10 }} />
              <Tooltip />
              <Bar dataKey="flights" radius={[4, 4, 0, 0]}>
                {airportData.map((_, i) => (
                  <Cell key={i} fill={AIRPORT_COLORS[i % AIRPORT_COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Status Distribution */}
        <div style={{ background: '#f8fafc', borderRadius: 8, padding: 16 }}>
          <h4 style={{ fontSize: 12, color: '#64748b', marginBottom: 8, margin: 0 }}>Flight Status Distribution</h4>
          <ResponsiveContainer width="100%" height={180}>
            <PieChart>
              <Pie
                data={statusData}
                cx="50%"
                cy="50%"
                innerRadius={35}
                outerRadius={65}
                dataKey="value"
                label={({ name, value }) => `${name}: ${value}`}
                labelLine={false}
              >
                {statusData.map((entry, i) => (
                  <Cell key={i} fill={STATUS_COLORS[entry.name] || STATUS_COLORS['other']} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
