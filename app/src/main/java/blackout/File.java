package unsw.blackout;

import java.util.Map;
import java.util.List;

import unsw.blackout.devices.Device;
import unsw.blackout.satellites.Satellite;
import unsw.blackout.satellites.TeleportingSatellite;

public class File {
    private String filename;
    private String content;
    private int size;
    private int byteSent;
    private String from;
    private String to;

    public File(String filename, String content, int size, String from) {
        this.filename = filename;
        this.content = content;
        this.size = size;
        this.byteSent = size;
        this.from = from;
        this.to = null;
    }

    public File(File file, String content, int size) {
        this.filename = file.filename;
        this.content = content;
        this.size = size;
        this.byteSent = file.byteSent;
        this.from = file.from;
        this.to = file.to;

    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public String toString() {
        return "File [filename=" + filename + ", content=" + content + ", size=" + size + ", byteSent=" + byteSent
                + ", from=" + from + ", to=" + to + "]";
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getByteSent() {
        return byteSent;
    }

    public void setByteSent(int byteSent) {
        this.byteSent = byteSent;
    }

    public List<String> communicableEntitiesInRange(String id, Map<String, Device> deviceMap,
            Map<String, Satellite> satelliteMap) {

        if (deviceMap.containsKey(id)) {
            return deviceMap.get(id).communicableEntitiesInRangeHelper(satelliteMap, deviceMap, id);
        } else if (satelliteMap.containsKey(id)) {
            return satelliteMap.get(id).communicableEntitiesInRangeHelper(satelliteMap, deviceMap, id);
        } else {
            return null;
        }

    }

    public void transferFile(File file, List<File> files, Map<String, Device> deviceMap,
            Map<String, Satellite> satelliteMap, List<File> filesToRemove) {
        String to = file.getTo();
        String from = file.getFrom();

        if (communicableEntitiesInRange(from, deviceMap, satelliteMap).contains(to) && satelliteMap.containsKey(to)
                && deviceMap.containsKey(from)) {

            Satellite currSatellite = satelliteMap.get(to);
            int maxReceiving = currSatellite.getGettingBytes();
            int numberOfFiles = files.size();

            int rationedBytes = maxReceiving / numberOfFiles;
            rationedBytes = rationedBytes == 0 ? 1 : rationedBytes;

            if (byteSent + rationedBytes > size) {
                file.setByteSent(size);
            } else {
                file.setByteSent(byteSent + rationedBytes);
            }

            String newContent = file.getContent().substring(0, byteSent);
            currSatellite.addFiletoSatellite(file, newContent, size);

        } else if (communicableEntitiesInRange(from, deviceMap, satelliteMap).contains(to)
                && deviceMap.containsKey(to)) {

            Satellite currSatellite = satelliteMap.get(from);
            int maxSending = currSatellite.getSendingBytes();
            int numberOfFiles = files.size();

            int rationedBytes = maxSending / numberOfFiles;
            rationedBytes = rationedBytes == 0 ? 1 : rationedBytes;

            if (byteSent + rationedBytes > size) {
                file.setByteSent(size);
            } else {
                file.setByteSent(byteSent + rationedBytes);
            }

            Device currDevice = deviceMap.get(to);
            String newContent = file.getContent().substring(0, byteSent);
            currDevice.addFiletoDevice(file, newContent, size);

        } else if (communicableEntitiesInRange(from, deviceMap, satelliteMap).contains(to)
                && satelliteMap.containsKey(from) && satelliteMap.containsKey(to)) {

            Satellite currSatellite = satelliteMap.get(to);
            int numberOfFiles = files.size();

            int maxSending = satelliteMap.get(from).getSendingBytes();
            int maxReceiving = satelliteMap.get(to).getGettingBytes();

            int maxBandwidth = Math.min(maxSending, maxReceiving);

            int rationedBytes = maxBandwidth / numberOfFiles;
            rationedBytes = rationedBytes == 0 ? 1 : rationedBytes;

            if (byteSent + rationedBytes > size) {
                file.setByteSent(size);
            } else {
                file.setByteSent(byteSent + rationedBytes);
            }

            String newContent = file.getContent().substring(0, byteSent);
            currSatellite.addFiletoSatellite(file, newContent, size);

        }

        if (byteSent >= size) {

            filesToRemove.add(file);
            return;
        }

        if (!communicableEntitiesInRange(from, deviceMap, satelliteMap).contains(to)) {
            Satellite currSatellite = satelliteMap.get(to);
            Satellite sendingSatellite = satelliteMap.get(from);
            if (currSatellite == null) {
                currSatellite = satelliteMap.get(from);
            }
            if (currSatellite instanceof TeleportingSatellite && deviceMap.containsKey(from)
                    && satelliteMap.containsKey(to)) {
                if (((TeleportingSatellite) currSatellite).isHasTeleported()) {
                    File fileToBeModified = deviceMap.get(from).getFilesMap().get(file.getFilename());
                    String contentWithNoTs = fileToBeModified.getContent().replaceAll("[Tt]", "");
                    fileToBeModified.setContent(contentWithNoTs);
                    fileToBeModified.setByteSent(contentWithNoTs.length());
                    fileToBeModified.setSize(contentWithNoTs.length());
                }

                currSatellite.removeFiles(file);
                filesToRemove.add(file);
                return;

            } else if ((currSatellite instanceof TeleportingSatellite
                    || sendingSatellite instanceof TeleportingSatellite)
                    && ((satelliteMap.containsKey(from) && deviceMap.containsKey(to))
                            || (satelliteMap.containsKey(from) && satelliteMap.containsKey(to)))) {

                TeleportingSatellite teleportingSatellite = null;

                if (currSatellite instanceof TeleportingSatellite) {
                    teleportingSatellite = (TeleportingSatellite) currSatellite;
                } else if (sendingSatellite instanceof TeleportingSatellite) {
                    teleportingSatellite = (TeleportingSatellite) sendingSatellite;
                }

                if (((TeleportingSatellite) teleportingSatellite).isHasTeleported()) {
                    File fileToBeModified = satelliteMap.get(from).getFilesMap().get(file.getFilename());

                    String contentWithNoTs = fileToBeModified.getContent().substring(byteSent, size).replaceAll("[Tt]",
                            "");

                    File sent = null;
                    if (satelliteMap.get(to) != null) {
                        sent = satelliteMap.get(to).getFilesMap().get(filename);
                        sent.setContent(content.substring(0, byteSent) + contentWithNoTs);
                    } else if (deviceMap.get(to) != null) {
                        sent = deviceMap.get(to).getFilesMap().get(filename);
                        sent.setContent(content.substring(0, byteSent) + contentWithNoTs);
                    }

                    int size = content.substring(0, byteSent).length() + contentWithNoTs.length();

                    sent.setSize(size);
                    sent.setByteSent(size);

                }

                teleportingSatellite.removeFiles(file);
                filesToRemove.add(file);
                return;

            } else if (satelliteMap.containsKey(to) && deviceMap.containsKey(from)) {

                deviceMap.get(from).removeFiles(file);
                satelliteMap.get(to).removeFiles(file);
                filesToRemove.add(file);
            } else if (satelliteMap.containsKey(from) && deviceMap.containsKey(to)) {

                deviceMap.get(to).removeFiles(file);
                satelliteMap.get(from).removeFiles(file);
                filesToRemove.add(file);
            }
        }
    }
}
