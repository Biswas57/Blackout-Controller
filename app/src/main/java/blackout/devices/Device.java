package unsw.blackout.devices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import unsw.blackout.File;
import unsw.blackout.Slope;
import unsw.blackout.FileTransferException;
import unsw.blackout.satellites.RelaySatellite;
import unsw.blackout.satellites.Satellite;
import unsw.blackout.satellites.StandardSatellite;
import unsw.response.models.EntityInfoResponse;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;
import unsw.utils.MathsHelper;

import static unsw.utils.MathsHelper.RADIUS_OF_JUPITER;

public abstract class Device {
    private String deviceId;
    private String deviceType;
    private Angle devicePosition;
    private Map<String, File> filesMap;
    private double deviceHeight = RADIUS_OF_JUPITER;
    private boolean isMoving = false;

    public Device(String deviceId, String deviceType, Angle devicePosition) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.devicePosition = devicePosition;
        this.filesMap = new HashMap<>();
        this.isMoving = false;
    }

    public abstract int getMaxRange();

    public abstract int getLinearVelocity();

    public void setNewPosition(List<Slope> slopesArray) {
        if (slopesArray.stream().anyMatch(slope -> devicePosition.toDegrees() < slope.getEndAngle()
                && devicePosition.toDegrees() > slope.getStartAngle())) {

            List<Slope> filteredSlopes = slopesArray.stream()
                    .filter(slope -> devicePosition.toDegrees() < slope.getEndAngle()
                            && devicePosition.toDegrees() > slope.getStartAngle())
                    .collect(Collectors.toList());

            Slope slope = filteredSlopes.get(0);

            int angle = 0;
            if (slope.getGradient() > 0) {
                angle = slope.getStartAngle();
            } else if (slope.getGradient() < 0) {
                angle = slope.getEndAngle();
            }
            double height = Math.abs(Math.abs(devicePosition.toDegrees() - angle) * slope.getGradient());

            this.setDeviceHeight(RADIUS_OF_JUPITER + height);

        }

        Angle angularVAngle = Angle.fromRadians(getLinearVelocity() / getDeviceHeight());
        Angle currAngle = getDevicePosition();
        setDevicePosition(currAngle.subtract(angularVAngle));

        if (this.getDevicePosition().toDegrees() < 0) {
            this.devicePosition = devicePosition.add(Angle.fromDegrees(360));
        }
    }

    public void sendFileHelper(String filename, File file, String fromId, String toId,
            Map<List<String>, List<File>> filesToTransfer, Map<String, Satellite> satelliteMap,
            Map<String, Device> deviceMap) throws FileTransferException {

        Satellite currSatellite = satelliteMap.get(toId);

        if (file == null || file.getByteSent() != file.getSize()) {
            throw new FileTransferException.VirtualFileNotFoundException(filename);
        }

        if (currSatellite.getFilesMap().values().stream().filter(sat -> sat.getFilename().equals(file.getFilename()))
                .count() > 0) {
            throw new FileTransferException.VirtualFileAlreadyExistsException(file.getFilename());
        }

        if (currSatellite instanceof RelaySatellite) {
            throw new FileTransferException.VirtualFileNoBandwidthException(filename);
        }

        if (!currSatellite.storageOverflow(file)) {
            List<File> newFilesArray = filesToTransfer.get(Arrays.asList(fromId, toId));
            currSatellite.removeFiles(file);
            newFilesArray.remove(file);
        }

        file.setByteSent(0);
        file.setFrom(fromId);
        file.setTo(toId);

        if (file != null && communicableEntitiesInRangeHelper(satelliteMap, deviceMap, fromId).contains(toId)) {
            currSatellite.addFiletoSatellite(file, "", file.getSize());
            List<File> newFilesArray = filesToTransfer.get(Arrays.asList(fromId, toId));
            if (newFilesArray == null) {
                newFilesArray = new ArrayList<>();
                filesToTransfer.put(Arrays.asList(fromId, toId), newFilesArray);
            }
            newFilesArray.add(file);

        }

    }

    public List<String> communicableEntitiesInRangeHelper(Map<String, Satellite> satelliteMap,
            Map<String, Device> deviceMap, String id) {

        List<String> nearbyEntities = new ArrayList<>();

        int maxRange = this.getMaxRange();

        for (Map.Entry<String, Satellite> entry : satelliteMap.entrySet()) {
            Satellite nextSatellite = entry.getValue();

            double distanceBetweenSatellites = MathsHelper.getDistance(nextSatellite.getSatelliteHeight(),
                    nextSatellite.getSatellitePosition(), this.getDevicePosition());

            boolean isVisible = MathsHelper.isVisible(nextSatellite.getSatelliteHeight(),
                    nextSatellite.getSatellitePosition(), this.getDevicePosition());

            if (distanceBetweenSatellites < maxRange && !nearbyEntities.contains(nextSatellite.getSatelliteId())
                    && isVisible && nextSatellite.getAllowedConnections().contains(this.getClass().getSimpleName())) {
                nearbyEntities.add(nextSatellite.getSatelliteId());
            }
        }

        List<String> relayList = nearbyEntities.stream()
                .filter(element -> satelliteMap.get(element) instanceof RelaySatellite).flatMap(element -> {
                    RelaySatellite relaySatellite = (RelaySatellite) satelliteMap.get(element);
                    List<String> closeEntities = relaySatellite.communicableEntitiesInRangeHelper(satelliteMap,
                            deviceMap, id);
                    return closeEntities.stream();
                }).collect(Collectors.toList());

        nearbyEntities.addAll(relayList);

        if (deviceMap.get(id) instanceof DesktopDevice) {
            nearbyEntities.removeIf(element -> {
                Satellite satellite = satelliteMap.get(element);
                return satellite instanceof StandardSatellite;
            });
        }

        nearbyEntities.remove(this.deviceId);

        return nearbyEntities;
    }

    public EntityInfoResponse getInfoHelper(Map<String, FileInfoResponse> filesMap) {

        for (File file : (this.getFilesMap()).values()) {
            FileInfoResponse response = new FileInfoResponse(file.getFilename(), file.getContent(), file.getSize(),
                    file.getByteSent() == file.getSize());
            filesMap.put(file.getFilename(), response);
        }

        return new EntityInfoResponse(this.deviceId, this.devicePosition, this.deviceHeight, this.deviceType, filesMap);
    }

    public void removeFiles(File file) {
        filesMap.remove(file.getFilename());
    }

    public void addFiletoDevice(File file, String content, int size) {
        File newfile = new File(file, content, size);
        filesMap.put(file.getFilename(), newfile);
    }

    public void addToFilesArray(String filename, String content, String deviceId) {
        File newFile = new File(filename, content, content.length(), deviceId);
        filesMap.put(filename, newFile);
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Angle getDevicePosition() {
        return devicePosition;
    }

    public void setDevicePosition(Angle devicePosition) {
        this.devicePosition = devicePosition;
    }

    public Map<String, File> getFilesMap() {
        return filesMap;
    }

    public void setFilesMap(Map<String, File> filesMap) {
        this.filesMap = filesMap;
    }

    public double getDeviceHeight() {
        return deviceHeight;
    }

    public void setDeviceHeight(double height) {
        this.deviceHeight = height;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean isMoving) {
        this.isMoving = isMoving;
    }
}
