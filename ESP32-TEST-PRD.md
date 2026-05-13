# PRD: ESP32 BLE Communication Test

## 1. Objective

Desarrollar un sketch ESP32 que permita verificar la comunicación BLE con la aplicación StreamControl para Android. El objetivo es confirmar que los mensajes se reciben y envían correctamente, sin implementar lógica de control real.

## 2. Scope

### 2.1 Mensajes Recibidos (App → ESP32)

| Mensaje | Acción |
|---------|--------|
| `config_sync` | Parsear JSON, mostrar en terminal, guardar valores (sin usar) |
| `start_control` | Mostrar en terminal, marcar como "running" |
| `stop_control` | Mostrar en terminal, marcar como "stopped" |

### 2.2 Mensajes Enviados (ESP32 → App)

| Mensaje | Acción |
|---------|--------|
| `control_data` | Enviar cada `sample_interval_ms` con datos fake |

### 2.3 Out of Scope

- Cualquier lógica de control (PID, IMC, RST)
- Control de sensores reales
- Control de actuadores reales
- Cálculos de temperatura o ángulo

## 3. Datos Fake

Para testing, los samples se generan con valores inventados:

```json
{
  "type": "control_data",
  "sequence": 1,
  "samples": [
    { "t": 0, "temp": 25.2, "angle": 45.0, "pwm": 80 }
  ]
}
```

| Campo | Fuente |
|-------|--------|
| `t` | Contador incremental (0, 50, 100, 150...) |
| `temp` | Constante + random suave (25.0 - 26.0) |
| `angle` | Constante + random (45.0 - 47.0) |
| `pwm` | Constante + random (80 - 90) |

## 4. Estados del Sistema

```
IDLE ──────► RUNNING ──────► IDLE
  │              │              │
  │         start_control      │
  │              │              │
  │         stop_control        │
  │              │              │
  └──────────────┴──────────────┘
```

| Estado | Descripción |
|--------|-------------|
| `IDLE` | Esperando start_control |
| `RUNNING` | Enviando control_data cada sample_interval_ms |

## 5. Hardware

- ESP32 (cualquier modelo con BLE)
- Conexión BLE con smartphone

## 6. BLE Configuration

| Elemento | UUID |
|----------|------|
| Service | `0000FFF0-0000-1000-8000-00805F9B34FB` |
| TX (ESP recibe) | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| RX (ESP envía) | `0000FFF2-0000-1000-8000-00805F9B34FB` |

## 7. Flujo de Prueba

1. Flash ESP32 con sketch
2. Abrir terminal serial (115200 baud)
3. Conectar app StreamControl al ESP32
4. Presionar "Sincronizar" → ver `config_sync` en terminal
5. Presionar "Iniciar" → ver `start_control` + comenzar a ver `control_data` en terminal
6. Presionar "Parar" → ver `stop_control` en terminal

## 8. Criterios de Éxito

- [ ] `config_sync` se recibe y parsea correctamente
- [ ] `start_control` se recibe y muestra en terminal
- [ ] `stop_control` se recibe y muestra en terminal
- [ ] `control_data` se envía periódicamente mientras está en RUNNING
- [ ] La app muestra los samples recibidos

## 9. Notas

- No se requiere precisión en los datos fake
- El sketch es solo para verificación de comunicación
- FreeRTOS no es obligatorio para esta prueba (puede ser single-task)