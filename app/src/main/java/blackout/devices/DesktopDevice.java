package unsw.blackout.devices;

import unsw.utils.Angle;

public class DesktopDevice extends Device {
    private final int maxRange = 200000;
    private final int linearVelocity = 20;

    public DesktopDevice(String deviceId, String deviceType, Angle devicePosition) {
        super(deviceId, deviceType, devicePosition);
    }

    public int getMaxRange() {
        return maxRange;
    }

    public int getLinearVelocity() {
        return linearVelocity;
    }
}
