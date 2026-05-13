# Protocolo de Comunicación BLE - StreamControl

## 1. Descripción General

Este documento define el protocolo de comunicación entre la aplicación móvil StreamControl (Android) y el dispositivo embebido basado en ESP32. La comunicación se realiza mediante Bluetooth Low Energy (BLE).

---

## 2. Estructura BLE

| Elemento | UUID | Descripción |
|----------|------|-------------|
| **Servicio** | `0000FFF0-0000-1000-8000-00805F9B34FB` | Servicio principal BLE |
| **TX (App→ESP)** | `0000FFF1-0000-1000-8000-00805F9B34FB` | Característica para escribir desde la app |
| **RX (ESP→App)** | `0000FFF2-0000-1000-8000-00805F9B34FB` | Notificación desde ESP hacia app |

**Nota:** Los UUIDs son configurables desde la aplicación.

---

## 3. Formato de Mensajes

Todos los mensajes se intercambian en formato JSON con codificación UTF-8.

### 3.1 Convenciones

- Todos los valores numéricos usan `double` o `int`
- Los timestamps de samples (`t`) están en milisegundos desde el inicio del control

---

## 4. Mensajes de App → ESP32

### 4.1 Sincronizar Configuración (`config_sync`)

Envía la configuración de los controladores (PID, IMC, RST) al ESP32.

```json
{
  "type": "config_sync",
  "pid": {
    "kp": 1.5,
    "ki": 0.5,
    "kd": 0.2,
    "setpoint": 50.0
  },
  "imc": {
    "K": 1.0,
    "tau": 2.0,
    "theta": 0.5,
    "lambda": 1.0
  },
  "rst": {
    "R": [1.0, -0.5, 0.1],
    "S": [1.0, 0.3, -0.1],
    "T": [1.0, -0.5, 0.1],
    "A": [1.0, -0.8, 0.2],
    "B": [1.0, 0.5],
    "P": [1.0, -0.5, 0.1],
    "integrator": true
  }
}
```

#### Campos del Payload

| Campo | Tipo | Descripción | Rango |
|-------|------|-------------|-------|
| `pid.kp` | double | Ganancia proporcional | 0 - 100 |
| `pid.ki` | double | Tiempo integral | 0 - 100 |
| `pid.kd` | double | Tiempo derivativo | 0 - 100 |
| `pid.setpoint` | double | Valor objetivo | 0 - 120 |
| `imc.K` | double | Ganancia del proceso | > 0 |
| `imc.tau` | double | Constante de tiempo | > 0 |
| `imc.theta` | double | Tiempo muerto | ≥ 0 |
| `imc.lambda` | double | Parámetro de diseño IMC | > 0 |
| `rst.R` | array[double] | Coeficientes polinomio R | - |
| `rst.S` | array[double] | Coeficientes polinomio S | - |
| `rst.T` | array[double] | Coeficientes polinomio T | - |
| `rst.A` | array[double] | Coeficientes polinomio A (polos planta) | - |
| `rst.B` | array[double] | Coeficientes polinomio B (ceros planta) | - |
| `rst.P` | array[double] | Coeficientes polinomio P (polos deseados) | - |
| `rst.integrator` | boolean | Incluye integrador (1-z⁻¹) en R | true/false |

**Respuesta del ESP32:** (opcional, para debugging)

```json
{
  "type": "config_sync_ack",
  "status": "ok",
  "error": null
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `status` | string | `"ok"` o `"error"` |
| `error` | string | Descripción del error o `null` |

> **Nota:** Este ack es opcional. Si el ESP32 no lo envía, la app continúa de todas formas.

---

### 4.2 Iniciar Control (`start_control`)

Inicia una nueva sesión de control.

```json
{
  "type": "start_control",
  "perturbation": false,
  "duration_ms": 60000,
  "sample_interval_ms": 1000,
  "pid": true,
  "imc": false,
  "rst": false
}
```

#### Campos del Mensaje

| Campo | Tipo | Descripción | Rango |
|-------|------|-------------|-------|
| `perturbation` | boolean | Si se activa perturbación durante control | true/false |
| `duration_ms` | int | Duración máxima del control | 0 - 120000 |
| `sample_interval_ms` | int | Intervalo de muestreo | 1000 o 2000 |
| `pid` | boolean | Habilitar controlador PID | true/false |
| `imc` | boolean | Habilitar controlador IMC | true/false |
| `rst` | boolean | Habilitar controlador RST | true/false |

**Respuesta del ESP32:** (opcional)

```json
{
  "type": "start_control_ack",
  "status": "started"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `status` | string | `"started"`, `"error"` |

> **Nota:** Este ack es opcional. La app comienza el control aunque no reciba respuesta.

---

### 4.3 Parar Control (`stop_control`)

Detiene la sesión de control activa.

```json
{
  "type": "stop_control"
}
```

**Respuesta del ESP32:** (opcional)

```json
{
  "type": "stop_control_ack",
  "status": "stopped",
  "samples_sent": 60
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `status` | string | `"stopped"` o `"error"` |
| `samples_sent` | int | Cantidad total de samples enviados |

> **Nota:** Este ack es opcional. La app cierra el control aunque no reciba respuesta.

---

## 5. Mensajes de ESP32 → App

### 5.1 Datos de Control (`control_data`)

Envía un batch de samples collected during control. Se envía cada `sample_interval_ms`.

```json
{
  "type": "control_data",
  "sequence": 1,
  "samples": [
    {
      "t": 0,
      "temp": 25.2,
      "angle": 45.0,
      "pwm": 80
    },
    {
      "t": 50,
      "temp": 25.4,
      "angle": 45.3,
      "pwm": 82
    },
    {
      "t": 100,
      "temp": 25.7,
      "angle": 46.0,
      "pwm": 85
    }
  ]
}
```

#### Campos del Mensaje

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `sequence` | int | Número de secuencia del paquete (incrementa) |
| `samples` | array | Array de samples colectados |

#### Campos de Cada Sample

| Campo | Tipo | Descripción | Rango |
|-------|------|-------------|-------|
| `t` | int | Tiempo transcurrido en ms (desde 0) | 0 - duration_ms |
| `temp` | double | Temperatura medida | 0 - 180 |
| `angle` | double | Ángulo de disparo | 0 - 180 |
| `pwm` | int | PWM de ventiladores | 0 - 255 |

**Nota:** Cada mensaje puede contener 10-20 samples dependiendo del `sample_interval_ms`.

---

### 5.2 Estado del Control (`control_status`)

Envía el estado actual del control (opcional, para debugging).

```json
{
  "type": "control_status",
  "status": "running",
  "current_values": {
    "temp": 25.5,
    "angle": 45.2,
    "pwm": 82,
    "setpoint": 50.0,
    "error": -0.5
  },
  "stats": {
    "elapsed_ms": 30000,
    "samples_sent": 30,
    "perturbation_active": false
  }
}
```

#### Campos del Mensaje

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `status` | string | `running`, `paused`, `stopped`, `error` |

#### Campos de current_values

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `temp` | double | Temperatura actual |
| `angle` | double | Ángulo de disparo actual |
| `pwm` | int | PWM actual |
| `setpoint` | double | Setpoint configurado |
| `error` | double | Error actual (setpoint - pv) |

#### Campos de stats

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `elapsed_ms` | int | Tiempo transcurrido en ms |
| `samples_sent` | int | Cantidad de samples enviados |
| `perturbation_active` | boolean | Si perturbación está activa |

---

### 5.3 Error (`error`)

Reporta un error durante la ejecución.

```json
{
  "type": "error",
  "error_code": "SENSOR_TIMEOUT",
  "message": "Sensor de temperatura sin respuesta"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `error_code` | string | Código de error |
| `message` | string | Descripción legible |

---

## 6. Códigos de Error

| Código | Descripción |
|--------|-------------|
| `SENSOR_TIMEOUT` | Sensor no responde |
| `BLE_DISCONNECT` | Conexión BLE perdida |
| `INVALID_CONFIG` | Configuración inválida |
| `MEMORY_FULL` | Memoria del ESP32 llena |
| `OVERFLOW` | Overflow en cálculos |
| `UNKNOWN` | Error desconocido |

---

## 7. Diagrama de Flujo de Comunicación

```
┌──────────┐                        ┌──────────┐
│   APP    │                        │   ESP32  │
└────┬─────┘                        └────┬─────┘
     │                                     │
     │  1. config_sync ──────────────────► │
     │         (sin ack obligatorio)       │
     │                                     │
     │  2. start_control ────────────────► │
     │         (sin ack obligatorio)       │
     │                                     │
     │         [ Control Loop ]            │
     │                                     │
     │  ◄──────── control_data ──────────│ │
     │  ◄──────── control_data ──────────│ │
     │  ◄──────── control_data ──────────│ │
     │                                     │
     │  3. stop_control ─────────────────► │
     │         (sin ack obligatorio)      │
     │                                     │
     └──────────┘                        └──────────┘
```

---

## 8. Resumen de Mensajes

| Tipo | Dirección | Descripción | Notas |
|------|-----------|-------------|-------|
| `config_sync` | App → ESP | Enviar configuración de controladores | Sin ack obligatorio |
| `start_control` | App → ESP | Iniciar sesión de control | Sin ack obligatorio |
| `stop_control` | App → ESP | Detener control | Sin ack obligatorio |
| `control_data` | ESP → App | Batch de samples de control | - |
| `control_status` | ESP → App | Estado actual del control | Opcional, debugging |
| `error` | ESP → App | Reporte de error | Opcional |

---

## 9. Notas de Implementación

### 9.1 Secuencia Típica

1. App se conecta al ESP32 por BLE
2. App envía `config_sync` con parámetros de controladores
3. Usuario presiona "Iniciar" en la app
4. App envía `start_control`
5. ESP32 inicia el loop de control y envía `control_data` cada `sample_interval_ms`
6. Cuando el usuario presiona "Parar" o se alcanza `duration_ms`, app envía `stop_control`
7. App guarda los datos en CSV

### 9.2 Validación

- El ESP32 debe validar todos los campos numéricos
- Si un valor está fuera de rango, ignorar o responder con `error` tipo `INVALID_CONFIG`
- **No se requiere ack de la app** - si la conexión se pierde, el ESP32 reinicia su estado

### 9.3 Reordenamiento

- Los samples pueden llegar fuera de orden debido a BLE
- Usar el campo `t` (tiempo) para ordenar correctamente
- El campo `sequence` ayuda a detectar paquetes perdidos

---

## 10. Historial de Versiones

| Versión | Fecha | Descripción |
|---------|-------|-------------|
| 1.0 | 2026-05-10 | Versión inicial del protocolo |
| 1.1 | 2026-05-13 | Removido wrapper `payload`, simplificados mensajes, eliminados ACKs obligatorios, agregados flags de selección de controlador (pid/imc/rst) |

---

*Documento generado para el proyecto StreamControl*