import React from 'react';
import Header from './components/Header';
import ConnectionBanner from './components/ConnectionBanner';
import StatsBar from './components/StatsBar';
import FlightBoard from './components/FlightBoard';
import AlertPanel from './components/AlertPanel';
import StatsCharts from './components/StatsCharts';
import FlightMap from './components/FlightMap';
import { useAlerts } from './hooks/useAlerts';

function App() {
  const { alerts, connected, acknowledge } = useAlerts();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', minWidth: 1280 }}>
      <Header wsConnected={connected} />
      <ConnectionBanner connected={connected} />
      <StatsBar />
      <div style={{ display: 'flex', height: 520 }}>
        <FlightBoard />
        <AlertPanel alerts={alerts} onAcknowledge={acknowledge} />
      </div>
      <FlightMap />
      <StatsCharts />
    </div>
  );
}

export default App;
