import { useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from '../services/authService';
const RAW_SOCKET_URL = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080/ws';
const SOCKET_URL = RAW_SOCKET_URL.startsWith('ws')
  ? RAW_SOCKET_URL.replace(/^ws/, 'http')
  : RAW_SOCKET_URL;

if (typeof window !== 'undefined' && typeof window.global === 'undefined') {
  window.global = window;
}

const useLobbyEvents = ({ roomCode, onMessage }) => {
  const clientRef = useRef(null);

  useEffect(() => {
    if (!roomCode) {
      return undefined;
    }

    const client = new Client({
      reconnectDelay: 5000,
      connectHeaders: {
        Authorization: `Bearer ${getToken()}`,
      },
      webSocketFactory: () => new SockJS(SOCKET_URL),
    });

    client.onConnect = () => {
      client.subscribe(`/topic/rooms/${roomCode}`, (message) => {
        if (typeof onMessage === 'function') {
          const payload = JSON.parse(message.body);
          onMessage(payload);
        }
      });
    };

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
    };
  }, [roomCode, onMessage]);
};

export default useLobbyEvents;
