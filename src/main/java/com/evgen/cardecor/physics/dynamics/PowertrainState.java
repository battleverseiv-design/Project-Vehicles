package com.evgen.cardecor.physics.dynamics;

/**
 * High-level dynamic state and solver interface for vehicle powertrain,
 * integrating JoltStylePowertrain with extended vehicle telemetry.
 */
public class PowertrainState extends JoltStylePowertrain {

    public float currentSpeedKmh = 0.0f;

    public PowertrainState() {
        super(PowertrainSpec.createFamilySedan());
    }

    public PowertrainState(PowertrainSpec spec) {
        super(spec);
    }

    /**
     * Extended powertrain update method matching specification interface.
     */
    public float updatePowertrain(
            float dt,
            float throttleInput,
            float forwardSpeedMps,
            float avgWheelAngularVel,
            boolean isGrounded,
            float maxWheelSlip
    ) {
        float torque = stepEngineAndTransmission(dt, throttleInput, forwardSpeedMps, avgWheelAngularVel, isGrounded, maxWheelSlip);
        this.currentSpeedKmh = Math.abs(forwardSpeedMps) * 3.6f;
        return torque;
    }
}
