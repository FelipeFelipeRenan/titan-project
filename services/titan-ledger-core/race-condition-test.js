import http from 'k6/http';
import { check, sleep } from 'k6';

// CONFIGURAÇÃO DO TESTE
export const options = {
  // Vamos subir gradualmente para 50 usuários simultâneos
  stages: [
    { duration: '5s', target: 50 },  // Ramp-up (Aquece os motores)
    { duration: '10s', target: 50 }, // Mantém a pressão (O Fogo)
    { duration: '5s', target: 0 },   // Ramp-down (Esfria)
  ],
};

const BASE_URL = 'http://localhost:8081/api/v1/accounts';

// --- COLOQUE SEUS IDs AQUI ---
const ID_SOURCE = 'cfdb1c76-348e-4c33-bf77-da0db889e13e';
const ID_TARGET = '0ef5e4fa-1381-46fd-ab9a-d2386ecf168e';
// -----------------------------

export default function () {
  const payload = JSON.stringify({
    fromAccountId: ID_SOURCE,
    toAccountId: ID_TARGET,
    amount: 1.00, // Transferir 1 real por vez
    description: "Load Test Transaction"
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Dispara a requisição
  const res = http.post(`${BASE_URL}/transfer`, payload, params);

  // Verifica se deu 200 OK
  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  // Pequena pausa aleatória entre 10ms e 100ms para simular comportamento humano/rede
  sleep(Math.random() * 0.1); 
}