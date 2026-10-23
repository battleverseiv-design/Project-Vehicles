# Project Vehicles (CarDecor)

Realistic physics-driven vehicle mod for Minecraft 1.20.1 (Forge & Fabric).

## Architecture & Features
- **Physics Core**: Powered by ODE4J (Open Dynamics Engine for Java) for realistic rigid-body vehicle dynamics, inertia tensors, collision hulls, and raycast suspension.
- **Powertrain & Drivetrain**:
  - Realistic engine torque curves and flywheel momentum.
  - Manual transmission with reverse and neutral gears.
  - Realistic clutch physics with thermal dissipation, slip friction degradation ($\mu(T)$), and overheating smoke particles.
  - Deterministic gear tooth fatigue model under excessive torque overload.
  - Natural engine stall through flywheel compression damping.
- **Controls**:
  - `W` — Throttle (Forward in gears 1–6, Backward in Reverse `R`)
  - `S` — Footbrake
  - `Space` — Handbrake
  - `R` — Upshift
  - `F` — Downshift
  - `Left Alt` — Clutch pedal (mandatory for clean manual shifts)
  - `Alt + 1` .. `Alt + 6` — Direct gear selection
  - `Alt + 0` — Neutral gear
  - `Alt + ~` / `Alt + 9` — Reverse gear
- **Vehicle Packs**: Data-driven JSON vehicle and engine definitions located in `run/cardecor_packs/`.

## Key Files for AI Analysis
- [`JoltStylePowertrain.java`](src/main/java/com/evgen/cardecor/physics/dynamics/JoltStylePowertrain.java) — Engine torque, manual transmission, clutch thermodynamics, and tooth fatigue.
- [`OdeVehicleBody.java`](src/main/java/com/evgen/cardecor/physics/ode/OdeVehicleBody.java) — ODE4J rigid-body simulation, suspension raycasts, and chassis dynamics.
- [`DrivableVehicleEntity.java`](src/main/java/com/evgen/cardecor/entity/DrivableVehicleEntity.java) — Vehicle entity lifecycle, player inputs, and state synchronization.
- [`ClientEvents.java`](src/main/java/com/evgen/cardecor/client/ClientEvents.java) — Forge client input handling, gear latches, speedometer HUD, and camera effects.
- [`FabricClientEvents.java`](fabric/src/main/java/com/evgen/cardecor/fabric/client/FabricClientEvents.java) — Fabric client input handling and effects.

## Branches
- `main` — Main branch with complete ODE physics and realistic transmission.
- `active-dev` — Active development branch.
- `backup-stable` — Stable backup snapshot.
