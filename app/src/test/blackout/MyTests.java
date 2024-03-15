package blackout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import unsw.blackout.BlackoutController;
import unsw.blackout.FileTransferException;
import unsw.response.models.FileInfoResponse;
import unsw.utils.Angle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unsw.utils.MathsHelper.RADIUS_OF_JUPITER;
import static blackout.TestHelpers.assertListAreEqualIgnoringOrder;

import java.util.Arrays;
import java.util.Collections;

@TestInstance(value = Lifecycle.PER_CLASS)
public class MyTests {
        /*
         * Custom tests for satellites
         */
        @Test
        public void testConnectionsStandard() {
                /*
                 * Checking if the standard sat avoids connecting to the desktop devices vice versa
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "StandardSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(0));
                controller.createDevice("Device", "DesktopDevice", Angle.fromDegrees(10));

                assertEquals(controller.communicableEntitiesInRange("Satellite"), Collections.emptyList());
                assertEquals(controller.communicableEntitiesInRange("Device"), Collections.emptyList());
        }

        @Test
        public void testForRelayRestrictions() {
                /*
                 * To check if relay sat doesnt bypass standardSat -> Desktop
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "StandardSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(10));
                controller.createDevice("Device", "DesktopDevice", Angle.fromDegrees(0));
                controller.createSatellite("Relay", "RelaySatellite", 10000 + RADIUS_OF_JUPITER, Angle.fromDegrees(5));

                assertEquals(controller.communicableEntitiesInRange("Satellite"), Arrays.asList("Relay"));
                assertEquals(controller.communicableEntitiesInRange("Device"), Arrays.asList("Relay"));
                assertListAreEqualIgnoringOrder(Arrays.asList("Satellite", "Device"),
                                controller.communicableEntitiesInRange("Relay"));
        }

        @Test
        public void testStandardSatFileRestrictions() {
                /*
                 * Can store up to either 3 files or 80 bytes (whichever is smallest for the current situation).
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "StandardSatellite", 50000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(50));
                controller.createDevice("Device", "LaptopDevice", Angle.fromDegrees(0));

                String msg = "Hello MateHello MateHello MateHello MateHello MateHello MateHello Mate";
                controller.addFileToDevice("Device", "1", msg);
                controller.addFileToDevice("Device", "2", "Iwanttogetover80bytes");
                controller.addFileToDevice("Device", "3", "*");
                controller.addFileToDevice("Device", "4", "*");
                controller.addFileToDevice("Device", "5", "*");

                assertDoesNotThrow(() -> controller.sendFile("1", "Device", "Satellite"));
                controller.simulate(70);
                controller.simulate(270);

                assertThrows(FileTransferException.VirtualFileNoStorageSpaceException.class,
                                () -> controller.sendFile("2", "Device", "Satellite"));

                assertDoesNotThrow(() -> controller.sendFile("3", "Device", "Satellite"));
                // need a minute to send one byte file
                controller.simulate(1);
                assertDoesNotThrow(() -> controller.sendFile("4", "Device", "Satellite"));
                controller.simulate(1);
                assertThrows(FileTransferException.VirtualFileNoStorageSpaceException.class,
                                () -> controller.sendFile("5", "Device", "Satellite"));
        }

        @Test
        public void testStandardBandwidth() {
                /*
                 * Testing whether the satellite throws an error if two files are sent at the simulataneously
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "StandardSatellite", 50000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(50));
                controller.createDevice("Device", "LaptopDevice", Angle.fromDegrees(0));

                controller.addFileToDevice("Device", "1", "Hello1");
                controller.addFileToDevice("Device", "2", "Hello2");

                assertDoesNotThrow(() -> controller.sendFile("1", "Device", "Satellite"));

                assertThrows(FileTransferException.VirtualFileNoBandwidthException.class,
                                () -> controller.sendFile("2", "Device", "Satellite"));
        }

        @Test
        public void testBandwidthDeviceToTeleport() {
                /*
                 * Checking if the teleport satellite rations (15/3) = 5 bytes to receive each file
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "TeleportingSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(0));
                controller.createDevice("Device", "LaptopDevice", Angle.fromDegrees(10));

                controller.addFileToDevice("Device", "1", "Hello Mate");
                controller.addFileToDevice("Device", "2", "hello mate");
                controller.addFileToDevice("Device", "3", "HELLO MATE");

                assertDoesNotThrow(() -> controller.sendFile("1", "Device", "Satellite"));
                assertDoesNotThrow(() -> controller.sendFile("2", "Device", "Satellite"));
                assertDoesNotThrow(() -> controller.sendFile("3", "Device", "Satellite"));

                controller.simulate();

                assertEquals(controller.getInfo("Satellite").getFiles().get("1"),
                                new FileInfoResponse("1", "Hello", 10, false));

                assertEquals(controller.getInfo("Satellite").getFiles().get("2"),
                                new FileInfoResponse("2", "hello", 10, false));

                assertEquals(controller.getInfo("Satellite").getFiles().get("3"),
                                new FileInfoResponse("3", "HELLO", 10, false));

        }

        @Test
        public void testBandwidthTeleportToDevice() {
                /*
                 * Checking if the teleport satellite rations (10/3) = 3 bytes to send each file
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "TeleportingSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(0));
                controller.createDevice("Device", "LaptopDevice", Angle.fromDegrees(10));

                controller.createDevice("Receiving", "LaptopDevice", Angle.fromDegrees(20));

                controller.addFileToDevice("Device", "1", "Hello Mate");
                controller.addFileToDevice("Device", "2", "hello mate");
                controller.addFileToDevice("Device", "3", "HELLO MATE");

                assertDoesNotThrow(() -> controller.sendFile("1", "Device", "Satellite"));
                assertDoesNotThrow(() -> controller.sendFile("2", "Device", "Satellite"));
                assertDoesNotThrow(() -> controller.sendFile("3", "Device", "Satellite"));

                controller.simulate(5);

                assertEquals(controller.getInfo("Satellite").getFiles().get("1"),
                                new FileInfoResponse("1", "Hello Mate", 10, true));

                assertEquals(controller.getInfo("Satellite").getFiles().get("2"),
                                new FileInfoResponse("2", "hello mate", 10, true));

                assertEquals(controller.getInfo("Satellite").getFiles().get("3"),
                                new FileInfoResponse("3", "HELLO MATE", 10, true));

                assertDoesNotThrow(() -> controller.sendFile("1", "Satellite", "Receiving"));
                assertDoesNotThrow(() -> controller.sendFile("2", "Satellite", "Receiving"));
                assertDoesNotThrow(() -> controller.sendFile("3", "Satellite", "Receiving"));

                controller.simulate();

                assertEquals(controller.getInfo("Receiving").getFiles().get("1"),
                                new FileInfoResponse("1", "Hel", 10, false));
                assertEquals(controller.getInfo("Receiving").getFiles().get("2"),
                                new FileInfoResponse("2", "hel", 10, false));
                assertEquals(controller.getInfo("Receiving").getFiles().get("3"),
                                new FileInfoResponse("3", "HEL", 10, false));
        }

        @Test
        public void testStorageTPSatellite() {
                /*
                 * Check if the satellite throws error if more than 200 bytes is received
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "TeleportingSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(0));
                controller.createDevice("Device", "LaptopDevice", Angle.fromDegrees(10));

                String msg = "1234567890123456789012345678901234567890" + "1234567890123456789012345678901234567890"
                                + "1234567890123456789012345678901234567890"
                                + "1234567890123456789012345678901234567890"
                                + "1234567890123456789012345678901234567890";

                controller.addFileToDevice("Device", "1", msg);
                controller.addFileToDevice("Device", "2", "You have a lot of bytes mate");

                assertDoesNotThrow(() -> controller.sendFile("1", "Device", "Satellite"));

                controller.simulate(14);

                assertEquals(controller.getInfo("Satellite").getFiles().get("1"),
                                new FileInfoResponse("1", msg, msg.length(), true));

                assertThrows(FileTransferException.VirtualFileNoStorageSpaceException.class,
                                () -> controller.sendFile("2", "Device", "Satellite"));

        }

        @Test
        public void testDeviceToTp() {
                /*
                 * When it tps the file is removed from device with all t bytes gone
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "TeleportingSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(179));
                controller.createDevice("Device", "LaptopDevice", Angle.fromDegrees(180));

                String msg = "The two twenty two train tore through the tunnel";

                controller.addFileToDevice("Device", "1", msg);

                assertDoesNotThrow(() -> controller.sendFile("1", "Device", "Satellite"));

                controller.simulate(1);

                assertEquals(controller.getInfo("Satellite").getFiles().get("1"),
                                new FileInfoResponse("1", "The two twenty ", msg.length(), false));

                controller.simulate(1);

                // file is removed after teleporting
                assertEquals(controller.getInfo("Satellite").getFiles().get("1"), null);

                assertEquals(controller.getInfo("Device").getFiles().get("1"),
                                new FileInfoResponse("1", "he wo weny wo rain ore hrough he unnel", 38, true));
        }

        @Test
        public void testTptoDevice() {
                /*
                 * When it tp sat tps away the file should be instantly downloaded but the t letter
                 *  bytes are removed from the remaining bytes
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "TeleportingSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(0));

                controller.createDevice("Send", "LaptopDevice", Angle.fromDegrees(0));
                controller.createDevice("Receive", "LaptopDevice", Angle.fromDegrees(180));

                String msg = "The two twenty two train tore through the tunnel";

                controller.addFileToDevice("Send", "1", msg);

                assertDoesNotThrow(() -> controller.sendFile("1", "Send", "Satellite"));

                controller.simulate(250);

                assertEquals(controller.getInfo("Satellite").getFiles().get("1"),
                                new FileInfoResponse("1", msg, msg.length(), true));

                assertDoesNotThrow(() -> controller.sendFile("1", "Satellite", "Receive"));

                controller.simulate(1);

                assertEquals(controller.getInfo("Receive").getFiles().get("1"),
                                new FileInfoResponse("1", "The two tw", msg.length(), false));

                controller.simulate(1);

                assertTrue(controller.getInfo("Satellite").getPosition().toDegrees() % 360 == 0); // has teleported

                assertEquals(controller.getInfo("Receive").getFiles().get("1"),
                                new FileInfoResponse("1", "The two tweny wo rain ore hrough he unnel", 41, true));
        }

        @Test
        public void testSattoSatTp() {

                /*
                 * Same as the one before but satellite to satellite
                 */
                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "TeleportingSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(0));

                controller.createDevice("Send", "LaptopDevice", Angle.fromDegrees(0));

                String msg = "The two twenty two train tore through the tunnel";

                controller.addFileToDevice("Send", "1", msg);

                assertDoesNotThrow(() -> controller.sendFile("1", "Send", "Satellite"));

                controller.simulate(250);

                assertEquals(controller.getInfo("Satellite").getFiles().get("1"),
                                new FileInfoResponse("1", msg, msg.length(), true));

                controller.createSatellite("Receive", "StandardSatellite", 4000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(180));

                assertDoesNotThrow(() -> controller.sendFile("1", "Satellite", "Receive"));

                controller.simulate(1);

                assertEquals(controller.getInfo("Receive").getFiles().get("1"),
                                new FileInfoResponse("1", "T", msg.length(), false));

                controller.simulate(1);

                assertTrue(controller.getInfo("Satellite").getPosition().toDegrees() % 360 == 0); // has teleported

                assertEquals(controller.getInfo("Receive").getFiles().get("1"),
                                new FileInfoResponse("1", "The wo weny wo rain ore hrough he unnel", 39, true));

        }

        @Test
        public void checkIfRemovingWorksWithFiles() {
                /*
                 * Checking if a device gets removed while sending a file gets removed
                 * alongside it
                 */

                BlackoutController controller = new BlackoutController();

                controller.createSatellite("Satellite", "StandardSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(0));
                controller.createDevice("Device", "DesktopDevice", Angle.fromDegrees(10));

                String msg = "The two twenty two train tore through the tunnel";

                controller.addFileToDevice("Device", "1", msg);
                assertDoesNotThrow(() -> controller.sendFile("1", "Device", "Satellite"));

                controller.removeDevice("Device");

                assertEquals(controller.getInfo("Satellite").getFiles(), Collections.emptyMap());
        }

        @Test
        public void testingSendingReceivingBandwidth() {
                /*
                 * Checking for the condition of the min(sending speed, receiving speed)
                 * A file transfer between standard and teleporting satellite should be transfering one byte
                 * although tp can receive up to 15 bytes a tick
                 */

                BlackoutController controller = new BlackoutController();
                controller.createSatellite("Satellite", "StandardSatellite", 10000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(0));
                controller.createSatellite("TpSatellite", "TeleportingSatellite", 12000 + RADIUS_OF_JUPITER,
                                Angle.fromDegrees(1));
                controller.createDevice("Device", "HandheldDevice", Angle.fromDegrees(10));

                String msg = "Hey";

                controller.addFileToDevice("Device", "1", msg);

                assertDoesNotThrow(() -> controller.sendFile("1", "Device", "Satellite"));
                controller.simulate(3);

                assertEquals(controller.getInfo("Satellite").getFiles().get("1"),
                                new FileInfoResponse("1", "Hey", msg.length(), true));

                assertDoesNotThrow(() -> controller.sendFile("1", "Satellite", "TpSatellite"));

                controller.simulate();

                assertEquals(controller.getInfo("TpSatellite").getFiles().get("1"),
                                new FileInfoResponse("1", "H", msg.length(), false));
        }
}
