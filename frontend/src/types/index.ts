export type FlightStatus =
  | 'SCHEDULED' | 'BOARDING' | 'DEPARTED'
  | 'IN_FLIGHT' | 'ARRIVED' | 'DELAYED' | 'CANCELLED';

export interface Flight {
  id: number;
  flightNumber: string;
  airline: string;
  departureAirport: string;
  arrivalAirport: string;
  scheduledDeparture: string;
  scheduledArrival: string;
  actualDeparture: string | null;
  actualArrival: string | null;
  status: FlightStatus;
  gate: string | null;
  aircraft: string | null;
  departureTimezone: string;
  arrivalTimezone: string;
}

export interface FlightPosition {
  latitude: number;
  longitude: number;
  altitude: number;
  velocity: number;
  heading: number;
}

export interface FlightWithPosition {
  flight: Flight;
  position: FlightPosition | null;
}

export type AlertType = 'DELAY' | 'CANCELLATION' | 'GATE_CHANGE' | 'STATUS_CHANGE';
export type Severity = 'INFO' | 'WARNING' | 'CRITICAL';

export interface Alert {
  id: number;
  flightId: number;
  flightNumber: string;
  type: AlertType;
  message: string;
  severity: Severity;
  createdAt: string;
  acknowledged: boolean;
}

export interface DailyStats {
  totalFlights: number;
  onTime: number;
  onTimeRate: string;
  delayed: number;
  cancelled: number;
}
