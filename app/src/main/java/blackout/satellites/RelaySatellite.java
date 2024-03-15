package unsw.blackout.satellites;

import java.util.ArrayList;
import java.util.Arrays;

import unsw.blackout.File;
import unsw.blackout.FileTransferException;
import unsw.utils.Angle;

public class RelaySatellite extends Satellite {
    private final int linearVelocity = 1500;
    private final int maxRange = 300000;
    private int orientation;
    private boolean inRange;
    private final int thresholdAngle = 345;
    private final int startAngle = 140;
    private final int endAngle = 190;
    private final ArrayList<String> allowedConnections;

    public RelaySatellite(String satelliteId, String satelliteType, double satelliteHeight, Angle satellitePosition) {
        super(satelliteId, satelliteType, satelliteHeight, satellitePosition);
        this.allowedConnections = new ArrayList<String>(Arrays.asList("HandheldDevice", "LaptopDevice", "DesktopDevice",
                "StandardSatellite", "TeleportingSatellite", "RelaySatellite"));

        double satPos = satellitePosition.toDegrees();
        inRange = false;

        if (satPos > endAngle && satPos < thresholdAngle) {
            orientation = 1;
        } else if (satPos < startAngle && satPos > 0 || (satPos > thresholdAngle && satPos < 360)) {
            orientation = -1;
        } else if (satPos > startAngle && satPos < endAngle) {
            inRange = true;
            orientation = 1;
        }
    }

    public int getSendingBytes() {
        return 0;
    }

    public int getGettingBytes() {
        return 0;
    }

    public boolean storageOverflow(File file) throws FileTransferException {
        return false;
    }

    public ArrayList<String> getAllowedConnections() {
        return allowedConnections;
    }

    public int getLinearVelocity() {
        return linearVelocity;
    }

    public int getMaxRange() {
        return maxRange;
    }

    public double getAngularVelocity() {
        return linearVelocity / getSatelliteHeight();
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public void setNewPosition() {
        Angle angularVAngle = Angle.fromRadians(getAngularVelocity());
        Angle currAngle = getSatellitePosition();

        if (!getInRange()) {
            if (orientation == -1) {
                setSatellitePosition(currAngle.add(angularVAngle));
            } else if (orientation == 1) {
                setSatellitePosition(currAngle.subtract(angularVAngle));
            }
            if (currAngle.toDegrees() >= startAngle && currAngle.toDegrees() <= endAngle) {
                setInRange(true);
            }
            return;
        }

        if (Math.abs((int) currAngle.toDegrees()) < startAngle) {
            setSatellitePosition(currAngle.add(angularVAngle));
            setOrientation(-1);
            return;
        } else if (Math.abs((int) currAngle.toDegrees()) > endAngle) {
            setSatellitePosition(currAngle.subtract(angularVAngle));
            setOrientation(1);
            return;
        }

        if (orientation == -1) {
            setSatellitePosition(currAngle.add(angularVAngle));
        } else if (orientation == 1) {
            setSatellitePosition(currAngle.subtract(angularVAngle));
        }

    }

    public boolean getInRange() {
        return inRange;
    }

    public void setInRange(boolean inRange) {
        this.inRange = inRange;
    }

}
