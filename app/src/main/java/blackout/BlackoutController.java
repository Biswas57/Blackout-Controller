package unsw.blackout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import unsw.blackout.devices.DesktopDevice;
import unsw.blackout.devices.Device;
import unsw.blackout.devices.HandheldDevice;
import unsw.blackout.devices.LaptopDevice;
import unsw.blackout.satellites.RelaySatellite;
import unsw.blackout.satellites.Satellite;
import unsw.blackout.satellites.StandardSatellite;
import unsw.blackout.satellites.TeleportingSatellite;
import unsw.response.models.EntityInfoResponse;
import unsw.response.models.FileInfoResponse;

import unsw.utils.Angle;

/**
 * The controller for the Blackout system.
 *
 * WARNING: Do not move this file or modify any of the existing method
 * signatures
 */
public class BlackoutController {
    private Map<String, Device> deviceMap = new HashMap<>();
    private Map<String, Satellite> satelliteMap = new HashMap<>();
    private Map<List<String>, List<File>> filesToTransfer = new HashMap<>();
    private List<Slope> slopesArray = new ArrayList<>();

    /**
    * Creates a device and adds it to the system.
    *
    * @param deviceId the unique identifier for the device
    * @param type     the type of device (e.g., "HandheldDevice", "LaptopDevice", "DesktopDevice")
    * @param position the initial position of the device, specified as an angle
    */
    public void createDevice(String deviceId, String type, Angle position) {
        if (type.equals("HandheldDevice")) {
            HandheldDevice newDevice = new HandheldDevice(deviceId, type, position);
            deviceMap.put(deviceId, newDevice);
        } else if (type.equals("LaptopDevice")) {
            LaptopDevice newDevice = new LaptopDevice(deviceId, type, position);
            deviceMap.put(deviceId, newDevice);
        } else if (type.equals("DesktopDevice")) {
            DesktopDevice newDevice = new DesktopDevice(deviceId, type, position);
            deviceMap.put(deviceId, newDevice);
        }
    }

    /**
    * Removes a device from the system.
    *
    * @param deviceId the unique identifier of the device to remove
    */
    public void removeDevice(String deviceId) {
        deviceMap.remove(deviceId);
        filesToTransfer.keySet().removeIf(key -> key.contains(deviceId));

        satelliteMap.values().forEach(sat -> sat.getFilesMap().values().removeIf(file -> {
            return file.getTo().equals(deviceId) || file.getFrom().equals(deviceId);
        }));
    }

    /**
    * Creates a satellite and adds it to the system.
    *
    * @param satelliteId the unique identifier for the satellite
    * @param type        the type of satellite (e.g., "StandardSatellite", "TeleportingSatellite", "RelaySatellite")
    * @param height      the height of the satellite above the reference surface
    * @param position    the initial position of the satellite, specified as an angle
    */
    public void createSatellite(String satelliteId, String type, double height, Angle position) {
        if (type.equals("StandardSatellite")) {
            StandardSatellite newSatellite = new StandardSatellite(satelliteId, type, height, position);
            satelliteMap.put(satelliteId, newSatellite);
        } else if (type.equals("TeleportingSatellite")) {
            TeleportingSatellite newSatellite = new TeleportingSatellite(satelliteId, type, height, position);
            satelliteMap.put(satelliteId, newSatellite);
        } else if (type.equals("RelaySatellite")) {
            RelaySatellite newSatellite = new RelaySatellite(satelliteId, type, height, position);
            satelliteMap.put(satelliteId, newSatellite);
        }
    }

    /**
    * Removes a satellite from the system.
    *
    * @param satelliteId the unique identifier of the satellite to remove
    */
    public void removeSatellite(String satelliteId) {
        satelliteMap.remove(satelliteId);
        filesToTransfer.keySet().removeIf(key -> key.contains(satelliteId));

        deviceMap.values().forEach(dev -> dev.getFilesMap().values().removeIf(file -> {
            return file.getTo().equals(satelliteId) || file.getFrom().equals(satelliteId);
        }));
    }

    /**
    * Lists the IDs of all devices currently in the system.
    *
    * @return a list of device IDs
    */
    public List<String> listDeviceIds() {
        return new ArrayList<String>(deviceMap.keySet());
    }

    /**
    * Lists the IDs of all satellites currently in the system.
    *
    * @return a list of satellite IDs
    */
    public List<String> listSatelliteIds() {
        return new ArrayList<String>(satelliteMap.keySet());
    }

    /**
    * Adds a file to a device.
    *
    * @param deviceId the unique identifier of the device to add the file to
    * @param filename the name of the file to add
    * @param content  the content of the file
    */
    public void addFileToDevice(String deviceId, String filename, String content) {
        deviceMap.get(deviceId).addToFilesArray(filename, content, deviceId);
    }

    /**
    * Retrieves information about an entity (device or satellite) by ID.
    *
    * @param id the unique identifier of the entity to retrieve information for
    * @return an EntityInfoResponse containing the entity's details or null if the entity is not found
    */
    public EntityInfoResponse getInfo(String id) {
        Map<String, FileInfoResponse> filesMap = new HashMap<>();

        if (deviceMap.containsKey(id)) {
            return deviceMap.get(id).getInfoHelper(filesMap);
        } else if (satelliteMap.containsKey(id)) {
            return satelliteMap.get(id).getInfoHelper(filesMap);
        } else {
            return null;
        }
    }

    /**
    * Simulates the movement of satellites, transfer of files and progression of satellite and device interaction,
    * updating the positions of moving entities for 1 tick.
    */
    public void simulate() {
        satelliteMap.values().forEach(Satellite::setNewPosition);
        deviceMap.values().stream().filter(device -> device.isMoving())
                .forEach(device -> device.setNewPosition(slopesArray));

        List<File> filesToRemove = new ArrayList<>();

        for (List<File> fileList : filesToTransfer.values()) {
            List<File> files = new ArrayList<>(fileList);
            files.stream().forEach(file -> file.transferFile(file, files, deviceMap, satelliteMap, filesToRemove));
            fileList.removeAll(filesToRemove);
        }
    }

    /**
     * Simulate for the specified number of minutes. You shouldn't need to modify
     * this function.
     * @param numberOfMinutes the number of minutes to simulate
     */
    public void simulate(int numberOfMinutes) {
        for (int i = 0; i < numberOfMinutes; i++) {
            simulate();
        }
    }

    /**
    * Finds all entities that are communicable (within range and visible) from a given entity ID.
    *
    * @param id the unique identifier of the entity to check for communicable entities
    * @return a list of IDs of entities that are in range and visible from the given entity
    */
    public List<String> communicableEntitiesInRange(String id) {

        if (deviceMap.containsKey(id)) {
            return deviceMap.get(id).communicableEntitiesInRangeHelper(satelliteMap, deviceMap, id);
        } else if (satelliteMap.containsKey(id)) {
            return satelliteMap.get(id).communicableEntitiesInRangeHelper(satelliteMap, deviceMap, id);
        } else {
            return null;
        }

    }

    /**
    * Initiates the sending of a file from one entity to another.
    *
    * @param fileName the name of the file to send
    * @param fromId   the unique identifier of the sending entity
    * @param toId     the unique identifier of the receiving entity
    * @throws FileTransferException if the file cannot be sent
    */
    public void sendFile(String fileName, String fromId, String toId) throws FileTransferException {

        if (deviceMap.containsKey(fromId) && satelliteMap.containsKey(toId)) {
            Device currDevice = deviceMap.get(fromId);
            File currFile = currDevice.getFilesMap().get(fileName);
            currDevice.sendFileHelper(fileName, currFile, fromId, toId, filesToTransfer, satelliteMap, deviceMap);

        } else if (satelliteMap.containsKey(fromId) && deviceMap.containsKey(toId)) {
            Satellite currSatellite = satelliteMap.get(fromId);
            File currFile = currSatellite.getFilesMap().get(fileName);
            currSatellite.sendFileHelperToDevice(fileName, currFile, fromId, toId, filesToTransfer, deviceMap,
                    satelliteMap);

        } else if (satelliteMap.containsKey(fromId) && satelliteMap.containsKey(toId)) {
            Satellite currSatellite = satelliteMap.get(fromId);
            File currFile = currSatellite.getFilesMap().get(fileName);
            currSatellite.sendFileHelperToSatellite(fileName, currFile, fromId, toId, filesToTransfer, deviceMap,
                    satelliteMap);
        } else {
            return;
        }
    }

    /**
    * Creates a moving device and adds it to the system.
    *
    * @param deviceId the unique identifier for the device
    * @param type     the type of device
    * @param position the initial position of the device, specified as an angle
    * @param isMoving flag indicating whether the device is moving
    */
    public void createDevice(String deviceId, String type, Angle position, boolean isMoving) {
        createDevice(deviceId, type, position);
        if (isMoving) {
            deviceMap.get(deviceId).setMoving(true);
        }
    }

    /**
    * Creates a slope object that defines a range of angles and a gradient,
    * then adds it to the list of slopes managed by the controller.
    * This can be used to simulate movement along a path with varying gradient.
    *
    * @param startAngle the starting angle of the slope in degrees
    * @param endAngle   the ending angle of the slope in degrees
    * @param gradient   the gradient of the slope, representing the change in height per degree of angle
    */
    public void createSlope(int startAngle, int endAngle, int gradient) {
        Slope slope = new Slope(startAngle, endAngle, gradient);
        slopesArray.add(slope);
    }
}
