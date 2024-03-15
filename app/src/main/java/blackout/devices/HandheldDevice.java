package unsw.blackout.devices;

import unsw.utils.Angle;

public class HandheldDevice extends Device {
    private final int maxRange = 50000;
    private final int linearVelocity = 50;

    public HandheldDevice(String deviceId, String deviceType, Angle devicePosition) {
        super(deviceId, deviceType, devicePosition);
    }

    public int getMaxRange() {
        return maxRange;
    }

    public int getLinearVelocity() {
        return linearVelocity;
    }
}
