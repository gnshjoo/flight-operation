import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { flightApi } from '../api/client';
import 'leaflet/dist/leaflet.css';

interface AircraftPosition {
  callsign: string;
  latitude: number;
  longitude: number;
  altitude: number;
  velocity: number;
  heading: number;
  onGround: boolean;
}

interface OpenSkyResponse {
  source: string;
  count: number;
  aircraft: AircraftPosition[];
}

function createIcon(heading: number, onGround: boolean) {
  const emoji = onGround ? '🛬' : '✈️';
  const opacity = onGround ? '0.7' : '1';
  return new L.DivIcon({
    html: `<div style="font-size:18px;transform:rotate(${onGround ? 0 : heading}deg);opacity:${opacity}">${emoji}</div>`,
    className: '',
    iconSize: [22, 22],
    iconAnchor: [11, 11],
  });
}

function formatCallsign(callsign: string): string {
  // KAL001 → KE001
  return callsign.replace(/^KAL/, 'KE');
}

export default function FlightMap() {
  const [aircraft, setAircraft] = useState<AircraftPosition[]>([]);
  const [source, setSource] = useState<string>('loading');

  useEffect(() => {
    const load = async () => {
      try {
        // Try OpenSky first
        const opensky: OpenSkyResponse = await flightApi.getOpenSky();
        if (opensky.source === 'opensky' && opensky.aircraft?.length > 0) {
          setAircraft(opensky.aircraft);
          setSource('opensky');
          return;
        }
      } catch {}

      // Fallback to mock data
      try {
        const mock = await flightApi.getLiveTracking();
        setAircraft(mock
          .filter((d: any) => d.position)
          .map((d: any) => ({
            callsign: d.flight.flightNumber,
            latitude: d.position.latitude,
            longitude: d.position.longitude,
            altitude: d.position.altitude,
            velocity: d.position.velocity,
            heading: d.position.heading,
            onGround: false,
          }))
        );
        setSource('mock');
      } catch {
        setSource('error');
      }
    };

    load();
    const iv = setInterval(load, 60000); // 1min on frontend, backend polls OpenSky every 5min
    return () => clearInterval(iv);
  }, []);

  const sourceLabel = source === 'opensky'
    ? `OpenSky Network (real data) — ${aircraft.length} aircraft`
    : source === 'mock'
    ? 'Demo data (OpenSky not configured)'
    : 'Loading...';

  return (
    <div style={{ padding: '16px 24px', background: 'white', borderTop: '1px solid #e2e8f0' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
        <h2 style={{ fontSize: 15, fontWeight: 600, margin: 0 }}>Live Flight Map</h2>
        <span style={{
          fontSize: 11,
          padding: '2px 8px',
          borderRadius: 10,
          background: source === 'opensky' ? '#d1fae5' : '#f1f5f9',
          color: source === 'opensky' ? '#065f46' : '#64748b',
        }}>
          {sourceLabel}
        </span>
      </div>
      <div style={{ height: 350, borderRadius: 8, overflow: 'hidden', border: '1px solid #e2e8f0' }}>
        <MapContainer
          center={[36.5, 127.5]}
          zoom={3}
          style={{ height: '100%', width: '100%' }}
          scrollWheelZoom={true}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {aircraft.map((a, i) => (
            <Marker
              key={`${a.callsign}-${i}`}
              position={[a.latitude, a.longitude]}
              icon={createIcon(a.heading, a.onGround)}
            >
              <Popup>
                <strong>{formatCallsign(a.callsign)}</strong><br />
                Alt: {Math.round(a.altitude * 3.281)}ft<br />
                Speed: {Math.round(a.velocity * 1.944)}kts<br />
                Heading: {Math.round(a.heading)}°
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
      {aircraft.length === 0 && source !== 'loading' && (
        <div style={{ textAlign: 'center', color: '#94a3b8', fontSize: 13, marginTop: 8 }}>
          No Korean Air aircraft currently in flight
        </div>
      )}
    </div>
  );
}
