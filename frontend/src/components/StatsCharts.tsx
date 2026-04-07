import React, { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, LineChart, Line, Legend
} from 'recharts';
import { Flight } from '../types';
import { flightApi } from '../api/client';

const COLORS = ['#003876', '#0072CE', '#059669', '#d97706', '#dc2626'];

export default function StatsCharts() {
  const [flights, setFlights] = useState<Flight[]>([]);

  useEffect(() => {
    flightApi.getAll().then(setFlights).catch(console.warn);
  }, []);

  // Status distribution
  const statusCounts: Record<string, number> = {};
  flights.forEach(f => { statusCounts[f.status] = (statusCounts[f.status] || 0) + 1; });
  const pieData = Object.entries(statusCounts).map(([name, value]) => ({ name, value }));

  // Airport distribution
  const airportCounts: Record<string, number> = {};
  flights.forEach(f => {
    airportCounts[f.departureAirport] = (airportCounts[f.departureAirport] || 0) + 1;
  });
  const barData = Object.entries(airportCounts)
    .map(([airport, count]) => ({ airport, flights: count }))
    .sort((a, b) => b.flights - a.flights)
    .slice(0, 8);

  // Mock delay trend (last 7 "days")
  const delayTrend = [
    { day: 'Mon', rate: 8.2 }, { day: 'Tue', rate: 12.5 },
    { day: 'Wed', rate: 6.1 }, { day: 'Thu', rate: 15.3 },
    { day: 'Fri', rate: 10.8 }, { day: 'Sat', rate: 7.4 },
    { day: 'Today', rate: flights.length > 0 ? (statusCounts['DELAYED'] || 0) / flights.length * 100 : 0 },
  ];

  return (
    <div style={{ padding: '16px 24px', background: 'white', borderTop: '1px solid #e2e8f0' }}>
      <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>Statistics</h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        {/* Departures by Airport */}
        <div style={{ background: '#f8fafc', borderRadius: 8, padding: 16 }}>
          <h4 style={{ fontSize: 12, color: '#64748b', marginBottom: 8, margin: 0 }}>Departures by Airport</h4>
          <ResponsiveContainer width="100%" height={150}>
            <BarChart data={barData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="airport" tick={{ fontSize: 10 }} />
              <YAxis tick={{ fontSize: 10 }} />
              <Tooltip />
              <Bar dataKey="flights" fill="#003876" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Status Distribution */}
        <div style={{ background: '#f8fafc', borderRadius: 8, padding: 16 }}>
          <h4 style={{ fontSize: 12, color: '#64748b', marginBottom: 8, margin: 0 }}>Status Distribution</h4>
          <ResponsiveContainer width="100%" height={150}>
            <PieChart>
              <Pie data={pieData} cx="50%" cy="50%" innerRadius={30} outerRadius={55} dataKey="value" label={({ name, value }) => `${name}: ${value}`} labelLine={false}>
                {pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Delay Rate Trend */}
        <div style={{ background: '#f8fafc', borderRadius: 8, padding: 16 }}>
          <h4 style={{ fontSize: 12, color: '#64748b', marginBottom: 8, margin: 0 }}>Delay Rate Trend (%)</h4>
          <ResponsiveContainer width="100%" height={150}>
            <LineChart data={delayTrend}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="day" tick={{ fontSize: 10 }} />
              <YAxis tick={{ fontSize: 10 }} />
              <Tooltip />
              <Line type="monotone" dataKey="rate" stroke="#d97706" strokeWidth={2} dot={{ r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
