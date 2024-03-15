package unsw.blackout.satellites;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import unsw.blackout.File;
import unsw.blackout.FileTransferException;
import unsw.blackout.devices.Device;
import unsw.blackout.devices.DesktopDevice;
import unsw.response.models.EntityInfoResponse;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;
import unsw.utils.MathsHelper;

public abstract class Satellite {
    private String satelliteId;
    private String satelliteType;
    private double satelliteHeight;
    private Angle satellitePosition;
    private Map<String, File> filesMap;

    public Satellite(String satelliteId, String satelliteType, double satelliteHeight, Angle satellitePosition) {
        this.satelliteId = satelliteId;
        this.satelliteType = satelliteType;
        this.satelliteHeight = satelliteHeight;
        this.satellitePosition = satellitePosition;
        this.filesMap = new HashMap<>();
    }

    public void sendFileHelperToDevice(String fileName, File file, String fromId, String toId,
            Map<List<String>, List<File>> filesToTransfer, Map<String, Device> deviceMap,
            Map<String, Satellite> satelliteMap) throws FileTransferException {

        Device currDevice = deviceMap.get(toId);

        if (file == null || file.getByteSent() != file.getSize()) {
            throw new FileTransferException.VirtualFileNotFoundException(fileName);
        }

        if (currDevice.getFilesMap().values().stream().filter(sat -> sat.getFilename().equals(file.getFilename()))
                .count() > 0) {
            throw new FileTransferException.VirtualFileAlreadyExistsException(file.getFilename());
        }

        file.setByteSent(0);
        file.setFrom(fromId);
        file.setTo(toId);

        if (communicableEntitiesInRangeHelper(satelliteMap, deviceMap, fromId).contains(toId)) {
            currDevice.addFiletoDevice(file, "", file.getSize());
            List<File> newFilesArray = filesToTransfer.get(Arrays.asList(fromId, toId));
            if (newFilesArray == null) {
                newFilesArray = new ArrayList<>();
                filesToTransfer.put(Arrays.asList(fromId, toId), newFilesArray);
            }
            newFilesArray.add(file);
        }
    }

    public void sendFileHelperToSatellite(String fileName, File file, String fromId, String toId,
            Map<List<String>, List<File>> filesToTransfer, Map<String, Device> deviceMap,
            Map<String, Satellite> satelliteMap) throws FileTransferException {

        Satellite currSatellite = satelliteMap.get(toId);

        if (currSatellite instanceof RelaySatellite) {
            throw new FileTransferException.VirtualFileNoBandwidthException(fileName);
        }

        if (currSatellite.getFilesMap().values().stream().filter(sat -> sat.getFilename().equals(file.getFilename()))
                .count() > 0) {
            throw new FileTransferException.VirtualFileAlreadyExistsException(file.getFilename());
        }

        if (file == null || file.getByteSent() != file.getSize()) {
            throw new FileTransferException.VirtualFileNotFoundException(fileName);
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

        int maxRange = this.getMaxRange();
        ArrayList<String> nearbyEntities = new ArrayList<>();

        for (Map.Entry<String, Satellite> entry : satelliteMap.entrySet()) {
            Satellite nextSatellite = entry.getValue();

            double distanceBetweenSatellites = MathsHelper.getDistance(this.getSatelliteHeight(),
                    this.getSatellitePosition(), nextSatellite.getSatelliteHeight(),
                    nextSatellite.getSatellitePosition());

            boolean isVisible = MathsHelper.isVisible(this.getSatelliteHeight(), this.getSatellitePosition(),
                    nextSatellite.getSatelliteHeight(), nextSatellite.getSatellitePosition());

            if (distanceBetweenSatellites != 0.0 && distanceBetweenSatellites < maxRange
                    && !nearbyEntities.contains(nextSatellite.getSatelliteId()) && isVisible) {
                nearbyEntities.add(nextSatellite.getSatelliteId());
            }
        }

        for (Map.Entry<String, Device> entry : deviceMap.entrySet()) {
            Device nextDevice = entry.getValue();

            double distanceBetweenSatelliteAndDevice = MathsHelper.getDistance(this.getSatelliteHeight(),
                    this.getSatellitePosition(), nextDevice.getDevicePosition());

            boolean isVisible = MathsHelper.isVisible(this.getSatelliteHeight(), this.getSatellitePosition(),
                    nextDevice.getDevicePosition());

            if (distanceBetweenSatelliteAndDevice < maxRange && !nearbyEntities.contains(nextDevice.getDeviceId())
                    && isVisible && this.getAllowedConnections().contains(nextDevice.getClass().getSimpleName())) {
                nearbyEntities.add(nextDevice.getDeviceId());
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

        if (satelliteMap.get(id) instanceof StandardSatellite) {
            nearbyEntities.removeIf(element -> {
                Device device = deviceMap.get(element);
                return device instanceof DesktopDevice;
            });
        }

        nearbyEntities.remove(this.satelliteId);

        return nearbyEntities;

    }

    public EntityInfoResponse getInfoHelper(Map<String, FileInfoResponse> filesMap) {

        for (File file : (this.getFilesMap()).values()) {
            FileInfoResponse response = new FileInfoResponse(file.getFilename(), file.getContent(), file.getSize(),
                    file.getByteSent() == file.getSize());
            filesMap.put(file.getFilename(), response);
        }

        return new EntityInfoResponse(this.satelliteId, this.satellitePosition, this.satelliteHeight,
                this.satelliteType, filesMap);
    }

    public abstract int getSendingBytes();

    public abstract int getGettingBytes();

    public abstract ArrayList<String> getAllowedConnections();

    public abstract void setNewPosition();

    public abstract boolean storageOverflow(File file) throws FileTransferException;

    public abstract int getMaxRange();

    public String getSatelliteId() {
        return satelliteId;
    }

    public void setSatelliteId(String satelliteId) {
        this.satelliteId = satelliteId;
    }

    public String getSatelliteType() {
        return satelliteType;
    }

    public void setSatelliteType(String satelliteType) {
        this.satelliteType = satelliteType;
    }

    public double getSatelliteHeight() {
        return satelliteHeight;
    }

    public void setSatelliteHeight(double satelliteHeight) {
        this.satelliteHeight = satelliteHeight;
    }

    public Angle getSatellitePosition() {
        return satellitePosition;
    }

    public void setSatellitePosition(Angle satellitePosition) {
        if (satellitePosition.toDegrees() < 0) {
            this.satellitePosition = satellitePosition.add(Angle.fromDegrees(360));
        } else {
            this.satellitePosition = satellitePosition;
        }
    }

    public Map<String, File> getFilesMap() {
        return filesMap;
    }

    public void setFilesMap(Map<String, File> filesMap) {
        this.filesMap = filesMap;
    }

    public void addFiletoSatellite(File file, String content, int size) {
        File newfile = new File(file, content, size);
        filesMap.put(file.getFilename(), newfile);
    }

    public void removeFiles(File file) {
        filesMap.remove(file.getFilename());
    }
}
