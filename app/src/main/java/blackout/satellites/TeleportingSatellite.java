package unsw.blackout.satellites;

import java.util.ArrayList;
import java.util.Arrays;

import unsw.blackout.FileTransferException;
import unsw.blackout.File;
import unsw.utils.Angle;

public class TeleportingSatellite extends Satellite {
    private final int linearVelocity = 1000;
    private final int maxRange = 200000;
    private int orientation;
    private final int sendingBytes = 10;
    private final int gettingBytes = 15;
    private final ArrayList<String> allowedConnections;
    private int minutes;
    private boolean hasTeleported;
    private final int maxBytesStorage = 200;

    public TeleportingSatellite(String satelliteId, String satelliteType, double satelliteHeight,
            Angle satellitePosition) {
        super(satelliteId, satelliteType, satelliteHeight, satellitePosition);
        this.allowedConnections = new ArrayList<String>(Arrays.asList("HandheldDevice", "LaptopDevice", "DesktopDevice",
                "StandardSatellite", "TeleportingSatellite", "RelaySatellite"));
        this.orientation = -1;
        this.minutes = 0;
        this.hasTeleported = false;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public boolean storageOverflow(File currfile) throws FileTransferException {

        long count = getFilesMap().values().stream().filter(file -> file.getByteSent() != file.getSize()).count() + 1;

        if (count > gettingBytes) {
            throw new FileTransferException.VirtualFileNoBandwidthException(currfile.getFilename());
        }

        int totalSize = getFilesMap().values().stream().mapToInt(file -> file.getSize()).sum() + currfile.getSize();

        if (totalSize > maxBytesStorage) {
            throw new FileTransferException.VirtualFileNoStorageSpaceException("Max Storage Reached");
        }

        return true;
    }

    public int getSendingBytes() {
        return sendingBytes;
    }

    public int getGettingBytes() {
        return gettingBytes;
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
        this.orientation = (orientation == 1) ? -1 : 1;
    }

    public void setNewPosition() {
        Angle angularVAngle = Angle.fromRadians(getAngularVelocity());
        Angle currAngle = getSatellitePosition();

        if (orientation == -1) {
            setSatellitePosition(currAngle.add(angularVAngle));
            setHasTeleported(false);
        } else if (orientation == 1) {
            setSatellitePosition(currAngle.subtract(angularVAngle));
            setHasTeleported(false);
        }

        if (Math.abs((int) getSatellitePosition().toDegrees()) == 180) {
            setSatellitePosition(Angle.fromDegrees(360));
            setOrientation(orientation);
            setHasTeleported(true);
            return;
        }

        if (getSatellitePosition().toDegrees() > 360) {
            setSatellitePosition(getSatellitePosition().subtract(Angle.fromDegrees(360)));
        }
    }

    public boolean isHasTeleported() {
        return hasTeleported;
    }

    public void setHasTeleported(boolean hasTeleported) {
        this.hasTeleported = hasTeleported;
    }

}
