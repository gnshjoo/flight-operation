import { useState, useEffect, useCallback, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Alert } from '../types';
import { alertApi } from '../api/client';

const ALERT_API = process.env.REACT_APP_ALERT_API || 'http://localhost:8082';

export function useAlerts() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  const fetchAlerts = useCallback(async () => {
    try {
      const data = await alertApi.getAll();
      setAlerts(data);
    } catch (e) {
      console.warn('Failed to fetch alerts:', e);
    }
  }, []);

  useEffect(() => {
    fetchAlerts();

    const client = new Client({
      webSocketFactory: () => new SockJS(`${ALERT_API}/ws/alerts`),
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/alerts', (message) => {
          const alert: Alert = JSON.parse(message.body);
          setAlerts(prev => [alert, ...prev]);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
      reconnectDelay: 5000,
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [fetchAlerts]);

  const acknowledge = useCallback(async (id: number) => {
    try {
      await alertApi.acknowledge(id);
      setAlerts(prev => prev.map(a => a.id === id ? { ...a, acknowledged: true } : a));
    } catch (e) {
      console.error('Failed to acknowledge:', e);
    }
  }, []);

  return { alerts, connected, acknowledge, refetch: fetchAlerts };
}
