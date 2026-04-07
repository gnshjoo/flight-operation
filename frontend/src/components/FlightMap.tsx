import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import { FlightWithPosition } from '../types';
import { flightApi } from '../api/client';
import 'leaflet/dist/leaflet.css';

// Airplane icon
const airplaneIcon = new L.DivIcon({
  html: '<div style="font-size:20px;transform:rotate(var(--heading, 0deg))">✈️</div>',
  className: '',
  iconSize: [24, 24],
  iconAnchor: [12, 12],
});

function createIcon(heading: number) {
  return new L.DivIcon({
    html: `<div style="font-size:20px;transform:rotate(${heading}deg)">✈️</div>`,
    className: '',
    iconSize: [24, 24],
    iconAnchor: [12, 12],
  });
}

export default function FlightMap() {
  const [data, setData] = useState<FlightWithPosition[]>([]);

  useEffect(() => {
    const load = () => flightApi.getLiveTracking().then(setData).catch(console.warn);
    load();
    const iv = setInterval(load, 30000);
    return () => clearInterval(iv);
  }, []);

  return (
    <div style={{ padding: '16px 24px', background: 'white', borderTop: '1px solid #e2e8f0' }}>
      <h2 style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>Live Flight Map</h2>
      <div style={{ height: 300, borderRadius: 8, overflow: 'hidden', border: '1px solid #e2e8f0' }}>
        <MapContainer
          center={[36.5, 127.5]}
          zoom={4}
          style={{ height: '100%', width: '100%' }}
          scrollWheelZoom={true}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {data.map(d => d.position && (
            <Marker
              key={d.flight.id}
              position={[d.position.latitude, d.position.longitude]}
              icon={createIcon(d.position.heading)}
            >
              <Popup>
                <strong>{d.flight.flightNumber}</strong><br />
                {d.flight.departureAirport} → {d.flight.arrivalAirport}<br />
                Alt: {Math.round(d.position.altitude)}ft | {Math.round(d.position.velocity)}kts
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
      {data.length === 0 && (
        <div style={{ textAlign: 'center', color: '#94a3b8', fontSize: 13, marginTop: 8 }}>
          No aircraft currently in flight
        </div>
      )}
    </div>
  );
}
