package sample;

import com.intel.bluetooth.RemoteDeviceHelper;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by Pim on 27-5-2014.
 * <p/>
 * This is the SPP (Serial Port Profile) client to connect with bluetooth devices.
 * The Arduino should have an serial bluetooth shield attached.
 */

public class SPPClient implements DiscoveryListener {

    private static SPPClient instance;
    private CheckBox resCB1;
    private CheckBox resCB2;
    private CheckBox resCB3;
    private CheckBox resCB4;

    public static SPPClient getInstance(CheckBox[] checkBoxes) {
        if (instance == null) {
            instance = new SPPClient(checkBoxes);
        }
        return instance;
    }


    /**
     * The inquiry access code for General/Unlimited Inquiry Access Code (DISCOVERY_MODE).
     * This is used to specify the type of inquiry to complete or respond to. The value of DISCOVERY_MODE is 0x9E8B33 (10390323).
     * This value is defined in the Bluetooth Assigned Numbers document.
     */
    private static final int DISCOVERY_MODE = DiscoveryAgent.GIAC;

    /**
     * Object used for waiting.
     */
    private static final Object lock = new Object();

    @Getter
    private Set<RemoteDevice> devices = new HashSet<>();

    @Getter
    @Setter
//    private MenuPane menuPane;

    /**
     * The discovery agent to find devices and services on that devices.
     */
    private DiscoveryAgent agent;

    /**
     * The printWriter is used to send data to the bluetooth device using the Serial Port Profile.
     */
    private volatile PrintWriter printWriter;

    /**
     * The bufferedReader is used to read data from the device.
     */
    @Getter
    private BufferedReader bufferedReader;

    /**
     * Create the SPPClient and retrieve the {@link DiscoveryAgent}.
     */
    private SPPClient(CheckBox[] checkBoxes) {
        try {
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            agent = localDevice.getDiscoveryAgent();
            resCB1 = checkBoxes[0];
            resCB2 = checkBoxes[1];
            resCB3 = checkBoxes[2];
            resCB4 = checkBoxes[3];
        } catch (BluetoothStateException e) {
            System.out.println("Error while retrieving discovery agent " + e);
        }
    }

    /**
     * Scan for devices.
     */
    public void scanForDevices() {
        System.out.println("Starting device inquiry.");
        try {
            agent.startInquiry(DISCOVERY_MODE, this);

            try {
                synchronized (lock) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                System.out.println(e.getMessage() + e);
            }

            if (devices.isEmpty()) {
                System.out.println("No devices found.");
                return;
            }

            System.out.println("Found " + devices.size() + " devices.");

            List<String> deviceList = getDeviceList();
            for (String deviceName: deviceList){
                if(deviceName.equals("AltanArduino"))
                    connect(deviceName);
            }
            while (printWriter == null){
                System.out.println("Not ready");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Ready");


        } catch (BluetoothStateException e) {
            System.out.println("Error while scanning for devices." + e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass deviceClass) {
        System.out.println("Device discovered.");
        devices.add(remoteDevice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void servicesDiscovered(int i, ServiceRecord[] serviceRecords) {
        System.out.println("Service discovered.");

        if (serviceRecords.length > 0) {
            String connectionURL = serviceRecords[0].getConnectionURL(0, false);
            try {
                StreamConnection connection = (StreamConnection) Connector.open(connectionURL);
                DataOutputStream outputStream = connection.openDataOutputStream();
                printWriter = new PrintWriter(new OutputStreamWriter(outputStream));

                System.out.println("Set printwriter" + printWriter);

                DataInputStream dataInputStream = connection.openDataInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));
            } catch (IOException e) {
                System.out.println("Error opening service connection" + e);
            }
        }

        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serviceSearchCompleted(int i, int i2) {
        System.out.println("Service search completed.");

        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inquiryCompleted(int i) {
        System.out.println("Inquire completed.");

        synchronized (lock) {
            lock.notify();
        }
    }

    /**
     * Connect with a device.
     *
     * @param deviceName the name of the device to connect.
     */
    public void connect(final String deviceName) {
        System.out.println(String.format("Connecting to %s", deviceName));
        for (RemoteDevice device : devices) {
            try {
//                int rssi = RemoteDeviceHelper.readRSSI(device);
//                System.out.println("RSSI: " + rssi);
                if (device.getFriendlyName(false).equals(deviceName)) {
                    if (!device.isAuthenticated()) {
                        authenticate(device);
                    }
                }

                searchForServices(deviceName, device);

            } catch (IOException e) {
                System.out.println("Failed to read device name" + e);
            }
        }

    }

    private void searchForServices(String deviceName, RemoteDevice device) throws BluetoothStateException {
        System.out.println(String.format("Searching for services on %s", deviceName));
        UUID[] uuidSet = new UUID[1];
        uuidSet[0] = new UUID("1101", true);
        agent.searchServices(null, uuidSet, device, this);
    }

    private void authenticate(final RemoteDevice device) throws IOException {
        String pin = "1234";
        boolean authenticated = RemoteDeviceHelper.authenticate(device, pin);
        if (authenticated) {
            System.out.println(String.format("Paired with %s using pin \"%s\".", device.getFriendlyName(false), pin));
        } else {
            System.out.println(String.format("Failed to pair with %s.", device.getFriendlyName(false)));
        }
    }

    /**
     * Read the names of the devices and add them to the combobox.
     */
    private List<String> getDeviceList() {
        List<String> items = new ArrayList<>();
        for (RemoteDevice device : devices) {
            try {
                items.add(device.getFriendlyName(false));
            } catch (IOException e) {
                System.out.println("Error while retrieving device name" + e);
            }
        }
        return items;
    }

    /**
     * Send a command using the printWriter. The command will be prefixed with a '?'. The '?' char is used to indicate the first
     * byte of a cmd for the arduino.
     *
     * @param input the command to sent.
     */
    public void write(final String input) {
        String cmd = input + "?";
        cmd = new String(cmd.getBytes(), StandardCharsets.US_ASCII);
        if (printWriter != null) {
            System.out.println("Sending command to bluetooth device: " + input);
            printWriter.write(cmd);
            printWriter.flush();
        } else {
            System.out.println("Command could not be send, print writer is null...");
        }
    }

    public void read(){
        try {
            System.out.println("Command from bluetooth device: " + bufferedReader.readLine());
            String[] states = bufferedReader.readLine().substring(1).replace("\n", "").split("#");
            Platform.runLater(()->this.resCB1.setSelected(states[0].equals("1")));;
            Platform.runLater(()->this.resCB2.setSelected(states[1].equals("1")));;
            Platform.runLater(()->this.resCB3.setSelected(states[2].equals("1")));;
            Platform.runLater(()->this.resCB4.setSelected(states[3].equals("1")));;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}