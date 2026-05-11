# PRD - StreamControl App (v2)

## 1. Descripción General

### 1.1 Propósito del Documento
Este documento establece los requisitos funcionales y no funcionales para el desarrollo de la aplicación móvil StreamControl, diseñada para conectarse vía Bluetooth Low Energy (BLE) con un sistema embebido basado en ESP32 para monitoreo y control de procesos.

### 1.2 Descripción de la Aplicación
Aplicación Android nativa desarrollada en Kotlin con Jetpack Compose que permite:
- Conectarse vía BLE con un dispositivo ESP32
- Configurar parámetros de controladores (PID, ITAE-IMC, RST)
- Iniciar/detener procesos de control
- Visualizar datos en tiempo real mediante gráficas
- Recolectar y guardar datos en archivos CSV
- Compartir archivos recolectados

### 1.3 Usuario Objetivo
Ingenieros y técnicos que trabajan con sistemas de control embebidos y necesitan monitorear, configurar y recolectar datos de procesos controlados por ESP32.

---

## 2. Arquitectura de la Aplicación

### 2.1 Estructura Visual

```
┌─────────────────────────────────────────────────────────┐
│                    Bottom Navigation                    │
│  [Inicio]    [Control]    [Archivos]    [Config]      │
│     ○          ●            ●            ●              │
└─────────────────────────────────────────────────────────┘
```

La navegación es mediante botones inferiores (Button Navigation). El orden de izquierda a derecha es:
1. **Inicio** - Visualización de datos y control
2. **Control** - Configuración de controladores
3. **Archivos** - Lista de archivos guardados
4. **Configuración** - Ajustes de conexión BLE

### 2.2 Arquitectura de Software

**Paradigmas aplicados:**
- **Screaming Architecture**: Los nombres de paquetes y objetos describen QUÉ hacen, no qué son
- **Scope Rule**: Cada feature/module tiene scopes definidos y no cruza límites
- **MVVM**: Model-View-ViewModel para separación de concerns
- **Principio Componente-Presentacional**: Componentes reutilizables de UI separados de su presentación

**Estructura de paquetes:**
```
com.streamcontrol/
├── app/
│   └── StreamControlApp.kt           # Application class
├── navigation/
│   └── Navigation.kt                 # Bottom nav + rutas
├── core/
│   ├── ble/
│   │   ├── BleManager.kt            # Gestión conexión BLE
│   │   └── BleConnectionConfig.kt   # Config UUIDs BLE
│   ├── storage/
│   │   ├── GlobalConfigStorage.kt   # Persistencia local
│   │   └── CsvFileManager.kt        # Manejo archivos CSV
│   └── scope/
│       └── AppScopes.kt             # Definición de scopes
├── features/
│   ├── home/
│   │   ├── HomeScreen.kt            # Composable presentacional
│   │   ├── HomeViewModel.kt         # ViewModel
│   │   └── HomeState.kt             # Estado
│   ├── control/
│   │   ├── ControlScreen.kt
│   │   ├── ControlViewModel.kt
│   │   └── ControlState.kt
│   ├── files/
│   │   ├── FilesScreen.kt
│   │   ├── FilesViewModel.kt
│   │   └── FilesState.kt
│   └── config/
│       ├── ConfigScreen.kt
│       ├── ConfigViewModel.kt
│       └── ConfigState.kt
├── domain/
│   └── model/
│       ├── ControlConfig.kt
│       ├── Controllers.kt
│       ├── PidSettings.kt
│       ├── ImcSettings.kt
│       ├── RstSettings.kt
│       ├── ConnectionConfig.kt
│       ├── ProcessSample.kt
│       └── LiveControlSession.kt
└── ui/
    ├── theme/
    │   └── Theme.kt
    └── components/
        ├── StreamControlButton.kt
        └── ...
```

### 2.3 Arquitectura de Datos (Screaming Architecture)

#### LocalStorage (Persistencia - SharedPreferences/JSON)

```kotlin
object GlobalConfigStorage {

    data class ControlConfig(
        val controllers: Controllers,
        val connection: ConnectionConfig
    )
    
    data class Controllers(
        val pid: PidSettings,
        val imc: ImcSettings,
        val rst: RstSettings
    )
    
    // PID: Proportional-Integral-Derivative
    data class PidSettings(
        val proportionalGain: Double,    // Kp (0-100, 4 decimales)
        val integralTime: Double,         // Ki (0-100, 4 decimales)
        val derivativeTime: Double,       // Kd (0-100, 4 decimales)
        val setpoint: Double             // Setpoint (0-120)
    )
    
    // IMC: Internal Model Control con ITAE
    data class ImcSettings(
        val processGain: Double,     // K
        val timeConstant: Double,    // τ (tau)
        val deadTime: Double,        // θ (theta)
        val lambda: Double         // λ
    )
    
    // RST: Retroalimentación, Control, Tracking
    data class RstSettings(
        val feedbackCoeffs: List<Double>,       // R(z^{-1})
        val controlActionCoeffs: List<Double>,   // S(z^{-1})
        val feedforwardCoeffs: List<Double>,    // T(z^{-1})
        val plantPoles: List<Double>,            // A(z^{-1})
        val plantZeros: List<Double>,            // B(z^{-1})
        val desiredPoles: List<Double>,         // P(z^{-1})
        val hasIntegrator: Boolean              // (1 - z^{-1})
    )
    
    data class ConnectionConfig(
        val deviceName: String,
        val pairingPin: String,
        val bleServiceUuid: String,       // UUID del servicio BLE
        val bleTxCharacteristicUuid: String,  // Para enviar a ESP32
        val bleRxCharacteristicUuid: String,  // Para recibir de ESP32
        val sampleIntervalMs: Int,        // 1000 o 2000
        val maxControlDurationMs: Int     // 0 a 120000
    )
}
```

#### Memoria (Durante Control Activo)

```kotlin
data class LiveControlSession(
    val dataBuffer: ArrayList<ProcessSample>,
    val isRunning: Boolean,
    val elapsedTimeMs: Long
)

data class ProcessSample(
    val elapsedTimeMs: Double,      // ms desde inicio (0 a maxControlDurationMs)
    val temperature: Double,          // Temperatura (0-180°C)
    val firingAngle: Double,         // Ángulo de disparo (0-180°)
    val fanPwm: Int                 // PWM ventiladores (0-255)
)
```

**Capacidad máxima en memoria:**
- Máximo 2400 samples (120s × 20 samples/s)
- Tamaño aproximado: 134 KB
- Completamente manejable en RAM

#### Archivo CSV (Exportación)

```
tiempo_ms,temperatura,angulo_disparo,pwm_ventiladores
0,25.2,45.0,80
1000,25.4,45.3,80
2000,25.7,46.0,82
```

**Ubicación:** `Documents/StreamControl/[nombre_usuario].csv`

---

## 3. Requisitos Funcionales

### 3.1 Módulo de Conexión Bluetooth (Configuración)

| ID | Requisito | Prioridad |
|----|-----------|-----------|
| RF-01 | Escaneo de dispositivos BLE disponibles | Alta |
| RF-02 | Conexión con dispositivo ESP32 por nombre y PIN | Alta |
| RF-03 | Indicador de estado de conexión (conectado/desconectado) | Alta |
| RF-04 | Manejo de desconexión accidental con alert + stop de control | Alta |
| RF-05 | Guardar configuración de conexión en localStorage | Alta |
| RF-06 | Modificar UUIDs de servicio y características BLE desde config | Alta |

### 3.2 Módulo de Controladores (Control)

| ID | Requisito | Prioridad |
|----|-----------|-----------|
| RF-07 | Ingresar parámetros PID (Kp, Ki, Kd, Setpoint) | Alta |
| RF-08 | Ingresar parámetros IMC-ITAE (K, τ, θ, λ) | Alta |
| RF-09 | Ingresar polinomios RST (coeficientes: R, S, T, A, B, P) | Alta |
| RF-10 | Botón "Guardar y Sincronizar" que guarda config y envía a ESP32 | Alta |
| RF-11 | Toast de error si no hay conexión al sincronizar | Alta |
| RF-12 | Guardar configuración en localStorage al presionar guardar | Alta |

**Validaciones:**
- Kp, Ki, Kd: 0 a 100, hasta 4 decimales
- Setpoint: 0 a 120
- K, τ, θ, λ: > 0 (sin límite definido aún)

### 3.3 Módulo de Inicio (Monitoreo y Control)

| ID | Requisito | Prioridad |
|----|-----------|-----------|
| RF-13 | Gráfica en tiempo real de variables (seleccionable) | Alta |
| RF-14 | Botón "Iniciar Control" | Alta |
| RF-15 | Botón "Parar Control" | Alta |
| RF-16 | Checkbox "Perturbación" | Alta |
| RF-17 | Indicador de estado de control (iniciado/parado) | Alta |
| RF-18 | Recibir array de datos JSON de ESP32 cada 1-2 segundos | Alta |
| RF-19 | Mostrar datos entrantes en gráfica en tiempo real | Alta |
| RF-20 | Temporizador de control (hasta tiempo configurado, máx 120s) | Alta |
| RF-21 | Selector de variable a visualizar en gráfica | Alta |
| RF-22 | Alerta cuando la app va a background durante control activo | Alta |

**Rangos de visualización en gráfica:**
- Temperatura: 0-180
- Ángulo de disparo: 0-180
- PWM ventiladores: 0-255

**Buffer circular:** Muestra todos los datos recolectados (hasta 2400 puntos)

### 3.4 Módulo de Archivos (Almacenamiento)

| ID | Requisito | Prioridad |
|----|-----------|-----------|
| RF-23 | Botón "Guardar" que solicita nombre de archivo | Alta |
| RF-24 | Guardar datos en CSV en Documents/StreamControl/ | Alta |
| RF-25 | Toast de error si no hay datos para guardar | Alta |
| RF-26 | Guardar automáticamente al finalizar control | Alta |
| RF-27 | Lista de archivos guardados | Alta |
| RF-28 | Compartir archivo por Intent (WhatsApp, email, Drive) | Media |
| RF-29 | Eliminar archivo | Media |

---

## 4. Requisitos No Funcionales

| ID | Requisito | Prioridad |
|----|-----------|-----------|
| RNF-01 | Tiempo de respuesta de UI < 100ms | Alta |
| RNF-02 | Actualización de gráfica a 60 FPS | Media |
| RNF-03 | Soporte para Android 10+ (API 29+) | Alta |
| RNF-04 | Permisos BLE: BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION | Alta |
| RNF-05 | Gráficas con Charts.kt soportando hasta 100k puntos | Alta |
| RNF-06 | Manejo de reconexión manual (sin auto-retry) | Alta |

---

## 5. Protocolo de Comunicación BLE

### 5.1 UUIDs BLE (Configurables)

**Valores por defecto:**
- Servicio BLE: `0000FFF0-0000-1000-8000-00805F9B34FB`
- Característica TX (app → ESP32): `0000FFF1-0000-1000-8000-00805F9B34FB`
- Característica RX (ESP32 → app): `0000FFF2-0000-1000-8000-00805F9B34FB`

*Estos valores se modifican desde el menú de configuración y se guardan en localStorage.*

### 5.2 Formato de Mensajes

Todos los mensajes se intercambiarán en formato JSON.

#### app → ESP32: Sincronizar Configuración

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
    "R": [1.0, -0.5],
    "S": [1.0, 0.3],
    "T": [1.0, -0.5],
    "A": [1.0, -0.8],
    "B": [1.0],
    "P": [1.0, -0.5, 0.1],
    "integrator": true
  }
}
```

#### app → ESP32: Iniciar Control

```json
{
  "type": "start_control",
  "perturbation": false
}
```

#### app → ESP32: Parar Control

```json
{
  "type": "stop_control"
}
```

#### ESP32 → app: Datos de Control

```json
{
  "type": "control_data",
  "samples": [
    {"t": 0, "temp": 25.2, "angle": 45.0, "pwm": 80},
    {"t": 50, "temp": 25.4, "angle": 45.3, "pwm": 80},
    {"t": 100, "temp": 25.7, "angle": 46.0, "pwm": 82}
  ]
}
```

---

## 6. Flujos de Usuario

### 6.1 Flujo de Configuración + Sincronización

```
1. Usuario navega a [Configuración]
2. Ingresa nombre dispositivo y PIN
3. (Opcional) Modifica UUIDs de servicio/características BLE
4. Escanea y conecta a ESP32
5. Navega a [Control]
6. Ingresa parámetros PID, IMC, RST
7. Presiona "Guardar y Sincronizar"
   ├─ [Conexión activa] → Guarda en localStorage + Envía a ESP32
   └─ [Sin conexión] → Toast: "Error: Sin conexión a ESP32"
```

### 6.2 Flujo de Control

```
1. Usuario navega a [Inicio]
2. (Opcional) Activa checkbox "Perturbación"
3. Presiona "Iniciar Control"
4. ESP32 comienza a enviar datos
5. App recibe y muestra en gráfica en tiempo real
6. Usuario selecciona variable a visualizar
7. (Dos opciones):
   a) Presiona "Parar Control" → Finaliza control
   b) Llega al tiempo máximo → Finaliza automáticamente
8. Presiona "Guardar" → Solicita nombre → Guarda CSV
   ├─ [Hay datos] → Guarda archivo
   └─ [No hay datos] → Toast: "No hay datos para guardar"
9. (Opcional) Comparte archivo
```

### 6.3 Flujo de Desconexión Accidental

```
1. Durante control activo, se pierde conexión BLE
2. App muestra Alert: "Conexión perdida"
3. Control se detiene automáticamente
4. Usuario debe navegar a [Configuración]
5. Reconectar manualmente
```

---

## 7. Estados de la Aplicación

| Estado | Descripción | Transición |
|--------|-------------|-----------|
| DESCONECTADO | No hay conexión BLE | Conectar exitosamente → CONECTADO |
| CONECTADO | BLE conectado, control parado | Iniciar control → CONTROL_ACTIVO |
| CONTROL_ACTIVO | Control en ejecución | Parar/Max time → CONECTADO |
| ERROR_CONEXION | Se perdió conexión | Alert + Stop → DESCONECTADO |

---

## 8. Diseño de UI (Bocetos)

### 8.1 Pantalla Inicio

```
┌────────────────────────────────────────┐
│  ● CONECTADO              00:45 [⏹]   │
├────────────────────────────────────────┤
│                                        │
│           📈 GRÁFICA GRANDE             │
│           (tiempo real)                 │
│                                        │
├────────────────────────────────────────┤
│ Variable: [Temperatura ▼]               │
├────────────────────────────────────────┤
│ [☑ Perturbación]                        │
│                                        │
│        [▶ INICIAR]   [GUARDAR]          │
└────────────────────────────────────────┘
```

**Elementos:**
- Indicador de conexión (arriba izquierda)
- Temporizador (arriba derecha, formato MM:SS)
- Gráfica ocupa ~60% de la pantalla
- Selector de variable (dropdown)
- Checkbox perturbación
- Botones: Iniciar (verde), Parar (rojo cuando activo), Guardar

### 8.2 Pantalla Control

```
┌────────────────────────────────────────┐
│           CONTROLADORES               │
├────────────────────────────────────────┤
│ PID                    [▼]             │
│ ├─ Kp: [________] (0-100, 4 dec)      │
│ ├─ Ki: [________] (0-100, 4 dec)     │
│ ├─ Kd: [________] (0-100, 4 dec)     │
│ └─ Setpoint: [________] (0-120)       │
├────────────────────────────────────────┤
│ IMC-ITAE               [▼]             │
│ ├─ K:  [________]                      │
│ ├─ τ:  [________]                     │
│ ├─ θ:  [________]                     │
│ └─ λ:  [________]                     │
├────────────────────────────────────────┤
│ RST                   [▼]             │
│ ├─ R:  [________]                     │
│ ├─ S:  [________]                     │
│ ├─ T:  [________]                     │
│ ├─ A:  [________]                     │
│ ├─ B:  [________]                     │
│ ├─ P:  [________]                     │
│ └─ Integrador: [☑]                    │
├────────────────────────────────────────┤
│        💾 GUARDAR Y SINCRONIZAR        │
└────────────────────────────────────────┘
```

**Elementos:**
- Secciones colapsables (Accordion)
- Campos numéricos con teclado decimal
- Checkbox para integrator
- Botón principal al final

### 8.3 Pantalla Archivos

```
┌────────────────────────────────────────┐
│              ARCHIVOS                 │
├────────────────────────────────────────┤
│ ┌──────────────────────────────────┐  │
│ │ control_prueba_1.csv      [📤][🗑]│  │
│ │ 2024-01-15 10:30               │  │
│ └──────────────────────────────────┘  │
│ ┌──────────────────────────────────┐  │
│ │ experimento_dia.csv       [📤][🗑]│  │
│ │ 2024-01-14 14:22               │  │
│ └──────────────────────────────────┘  │
│                                        │
│         (Lista scrolleable)             │
└────────────────────────────────────────┘
```

**Elementos:**
- Card por archivo con nombre y fecha
- Botón compartir (📤) y eliminar (🗑)
- Confirmación antes de eliminar

### 8.4 Pantalla Configuración

```
┌────────────────────────────────────────┐
│            CONEXIÓN BLE                │
├────────────────────────────────────────┤
│ Dispositivo: [ESP32_StreamControl]    │
│                                        │
│ PIN: [________]                        │
│                                        │
│ ── Configuración avanzada ──           │
│ Servicio UUID: [________________]     │
│ TX Char UUID: [________________]      │
│ RX Char UUID: [________________]      │
│                                        │
│ Intervalo (ms): [1000 ▼]             │
│                                        │
│ DURACIÓN MÁXIMA (ms): [________]       │
│            (max 120000)               │
│                                        │
│         🔗 CONECTAR                    │
│                                        │
│ Estado: ● Desconectado                │
└────────────────────────────────────────┘
```

**Elementos:**
- Campos configurables (UUIDs editables)
- Dropdown para intervalo
- Botón conectar
- Indicador de estado

---

## 9. Excepciones y Manejo de Errores

| Escenario | Manejo |
|-----------|--------|
| Sin permisos BLE | Solicitar permisos, mostrar explicación |
| Dispositivo no encontrado | Toast: "Dispositivo no encontrado" |
| Error de conexión | Toast: "Error de conexión: [detalle]" |
| Conexión perdida durante control | Alert + Stop automático + Notificación |
| App va a background durante control | Notificación al usuario |
| Error al guardar archivo | Toast: "Error al guardar: [detalle]" |
| Sin conexión al sincronizar | Toast: "Error: Sin conexión a ESP32" |
| Sin datos al guardar | Toast: "No hay datos para guardar" |
| Nombre de archivo vacío | Validación + Toast: "Ingrese un nombre" |
| Campo numérico inválido | Validación inline + mensaje de error |

---

## 10. Librerías y Dependencias

| Librería | Versión | Uso |
|----------|---------|-----|
| Jetpack Compose BOM | 2024.02.00+ | UI framework |
| Compose Navigation | 2.7.7+ | Navegación |
| Charts.kt | 0.5.0+ | Graficación |
| Kotlinx Serialization JSON | 1.6.3+ | Parseo JSON |
| AndroidX Lifecycle ViewModel Compose | 2.7.0+ | ViewModels |
| AndroidX DataStore | 1.0.0+ | Persistencia |

---

## 11. Pendientes / Pendientes de Definir

| ID | Tema | Notas |
|----|------|-------|
| PD-01 | Rangos para K, τ, θ, λ | Pendiente definir límites |
| PD-02 | Diseño visual detallado | Colores, tipografía, iconos |

---

## 12. Aprobaciones

| Rol | Nombre | Firma | Fecha |
|-----|--------|-------|-------|
| Product Owner | | | |
| Lead Developer | | | |
| QA Lead | | | |

---

*Documento generado en colaboración con el equipo de desarrollo.*