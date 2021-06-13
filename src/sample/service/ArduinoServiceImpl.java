package sample.service;

import lombok.SneakyThrows;

import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.OutputStream;

public class ArduinoServiceImpl implements ArduinoService {

    public static final String STOP_COMMAND = "S";
    public static final String JDY_32_SPP = "JDY-32-SPP";
    private OutputStream stream;
    private RemoteDevice remoteDevice;
    private boolean scanFinished = false;
    private boolean enabledFeatureStopping = true;

    @Override
    @SneakyThrows
    public void connect(String deviceUrl) {
        StreamConnection connection = (StreamConnection) Connector.open(deviceUrl);
        stream = connection.openOutputStream();
        sendData(STOP_COMMAND);
    }

    @Override
    @SneakyThrows
    public void sendData(String data) {
        System.out.println("Send to arduino  :: " + data);
        stream.write(data.getBytes());
        stream.flush();
        if (enabledFeatureStopping) {
            Thread.sleep(1000);
            if (!data.equals(STOP_COMMAND)) {
                stream.write(STOP_COMMAND.getBytes()); //Stopping the robot
                stream.flush();
            }
        }
    }

    @Override
    @SneakyThrows
    public String scanDevice() {
        //scan for all devices:
        String deviceUrl;
        LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, new DiscoveryListener() {
            @SneakyThrows
            public void deviceDiscovered(RemoteDevice device, DeviceClass cod) {
                String name = device.getFriendlyName(false);
                System.out.format("%s (%s)\n", name, device.getBluetoothAddress());
                if (name.contains(JDY_32_SPP)) {
                    remoteDevice = device;
                    System.out.println("got it!");
                }
            }

            public void inquiryCompleted(int discType) {
                scanFinished = true;
            }

            public void serviceSearchCompleted(int transID, int respCode) {
            }

            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
            }
        });
        while (!scanFinished) {
            Thread.sleep(500);
        }

        //search for services:
        UUID uuid = new UUID(0x1101); //scan for btspp://... services (as HC-05 offers it)
        UUID[] searchUuidSet = new UUID[]{uuid};
        int[] attrIDs = new int[]{
                0x0100 // service name
        };
        scanFinished = false;
        LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrIDs, searchUuidSet,
                remoteDevice, new DiscoveryListener() {
                    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                    }

                    public void inquiryCompleted(int discType) {
                    }

                    public void serviceSearchCompleted(int transID, int respCode) {
                        scanFinished = true;
                    }

                    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
                        for (int i = 0; i < servRecord.length; i++) {
                            if (servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false) != null) {
                                break; //take the first one
                            }
                        }
                    }
                });

        while (!scanFinished) {
            Thread.sleep(500);
        }

        deviceUrl = "btspp://" + remoteDevice.getBluetoothAddress() + ":1;authenticate=false;encrypt=false;master=false";
        System.out.println(remoteDevice.getBluetoothAddress());
        System.out.println(deviceUrl);
        return deviceUrl;
    }
}
