package unsw.blackout.satellites;

import java.util.ArrayList;
import java.util.Arrays;

import unsw.utils.Angle;
import unsw.blackout.File;

import unsw.blackout.FileTransferException;

public class StandardSatellite extends Satellite {
    private final int linearVelocity = 2500;
    private final int maxRange = 150000;
    private final int sendingBytes = 1;
    private final int gettingBytes = 1;
    private final int maxBytesStorage = 80;

    private final ArrayList<String> allowedConnections;

    public StandardSatellite(String satelliteId, String satelliteType, double satelliteHeight,
            Angle satellitePosition) {
        super(satelliteId, satelliteType, satelliteHeight, satellitePosition);
        this.allowedConnections = new ArrayList<String>(Arrays.asList("HandheldDevice", "LaptopDevice",
                "StandardSatellite", "TeleportingSatellite", "RelaySatellite"));
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

    public boolean storageOverflow(File currFile) throws FileTransferException {

        // + 1 for curr file being transfered
        long count = getFilesMap().values().stream().filter(file -> file.getByteSent() != file.getSize()).count() + 1;

        if (count > gettingBytes) {
            throw new FileTransferException.VirtualFileNoBandwidthException(currFile.getFilename());
        }

        if (getFilesMap().values().size() + 1 > 3) {
            throw new FileTransferException.VirtualFileNoStorageSpaceException("Max Files Reached");
        }

        int totalSize = getFilesMap().values().stream().mapToInt(file -> file.getSize()).sum() + currFile.getSize();

        if (totalSize > maxBytesStorage) {
            throw new FileTransferException.VirtualFileNoStorageSpaceException("Max Storage Reached");
        }

        return true;
    }

    public void setNewPosition() {
        Angle angularVAngle = Angle.fromRadians(getAngularVelocity());
        Angle currAngle = getSatellitePosition();
        setSatellitePosition(currAngle.subtract(angularVAngle));
    }
}
