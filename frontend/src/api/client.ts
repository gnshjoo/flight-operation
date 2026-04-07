const FLIGHT_API = process.env.REACT_APP_FLIGHT_API || 'http://localhost:8081';
const ALERT_API = process.env.REACT_APP_ALERT_API || 'http://localhost:8082';

async function fetchJson<T>(url: string): Promise<T> {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`API error: ${res.status}`);
  return res.json();
}

export const flightApi = {
  getAll: () => fetchJson<any[]>(`${FLIGHT_API}/api/flights`),
  getById: (id: number) => fetchJson<any>(`${FLIGHT_API}/api/flights/${id}`),
  getLiveTracking: () => fetchJson<any[]>(`${FLIGHT_API}/api/flights/live-tracking`),
  getOpenSky: () => fetchJson<any>(`${FLIGHT_API}/api/flights/opensky`),
  getOpenSkyStats: () => fetchJson<any>(`${FLIGHT_API}/api/flights/opensky/stats`),
  getKac: () => fetchJson<any>(`${FLIGHT_API}/api/flights/kac`),
  getKacStats: () => fetchJson<any>(`${FLIGHT_API}/api/flights/kac/stats`),
  getCombined: () => fetchJson<any>(`${FLIGHT_API}/api/flights/combined`),
  getDailyStats: () => fetchJson<any>(`${FLIGHT_API}/api/stats/daily`),
  getDelayRate: () => fetchJson<any>(`${FLIGHT_API}/api/stats/delay-rate`),
  getDepartures: (code: string) => fetchJson<any[]>(`${FLIGHT_API}/api/airports/${code}/departures`),
  getArrivals: (code: string) => fetchJson<any[]>(`${FLIGHT_API}/api/airports/${code}/arrivals`),
  updateStatus: async (id: number, body: any, token: string) => {
    const res = await fetch(`${FLIGHT_API}/api/flights/${id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error(`API error: ${res.status}`);
    return res.json();
  },
  login: async (username: string, password: string) => {
    const res = await fetch(`${FLIGHT_API}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    });
    if (!res.ok) throw new Error('Login failed');
    return res.json();
  },
};

export const alertApi = {
  getAll: () => fetchJson<any[]>(`${ALERT_API}/api/alerts`),
  getActive: () => fetchJson<any[]>(`${ALERT_API}/api/alerts/active`),
  acknowledge: async (id: number) => {
    const res = await fetch(`${ALERT_API}/api/alerts/${id}/acknowledge`, { method: 'PUT' });
    if (!res.ok) throw new Error(`API error: ${res.status}`);
    return res.json();
  },
};

export const ALERT_WS_URL = `${ALERT_API.replace('http', 'ws')}/ws/alerts`;
export { FLIGHT_API, ALERT_API };
