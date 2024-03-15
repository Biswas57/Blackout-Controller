package unsw.blackout.devices;

import unsw.utils.Angle;

public class LaptopDevice extends Device {
    private final int maxRange = 100000;
    private final int linearVelocity = 30;

    public LaptopDevice(String deviceId, String deviceType, Angle devicePosition) {
        super(deviceId, deviceType, devicePosition);
    }

    public int getMaxRange() {
        return maxRange;
    }

    public int getLinearVelocity() {
        return linearVelocity;
    }
}
