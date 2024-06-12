package proyecto_sencillo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalTime;

import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXException;
import io.calimero.KNXFormatException;
import io.calimero.datapoint.Datapoint;
import io.calimero.datapoint.StateDP;
import io.calimero.device.BaseKnxDevice;
import io.calimero.device.KnxDeviceServiceLogic;
import io.calimero.dptxlator.DPTXlator;
import io.calimero.dptxlator.DPTXlator8BitEnum;
import io.calimero.dptxlator.DPTXlatorBoolean;
import io.calimero.link.KNXNetworkLinkIP;
import io.calimero.link.medium.KnxIPSettings;
import java.io.Serializable;

public class LightWithDimmer extends KnxDeviceServiceLogic implements Device, Runnable, Serializable {

    private String id;
    private String deviceName;
    private String networkname;
    private boolean run;
    private boolean state;
    private IndividualAddress deviceAddress;
    private GroupAddress dpAddressPushButton;
    private GroupAddress dpAddressDimmer;
    private NetworkInterface networkInterface;
    private InetAddress localEndpoint;
    private int dimmer;

    public LightWithDimmer(String id, String name, int area, int line, int device, String networkname, int dimmer) {
        this.id = id;
        this.deviceName = name;
        this.deviceAddress = new IndividualAddress(area, line, device);
        this.networkname = networkname;
        this.dpAddressPushButton = new GroupAddress(1, 0, 5); // Ejemplo Group Address para Push Button
        this.dpAddressDimmer = new GroupAddress(1, 0, 4); // Ejemplo Group Address para Dimmer
        this.run = false;
        this.dimmer = dimmer;
        try {
            this.networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(this.networkname));
            this.localEndpoint = InetAddress.getByName(this.networkname);
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }
    }

    public String getNetworkName() {
        return networkname;
    }

    public String getNetworkInterfaceName() {
        return networkInterface.getName();
    }

    public String getLocalEndpoint() {
        return localEndpoint.toString();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void start() {
        this.run = true;
        Thread t1 = new Thread(this);
        t1.start();
    }

    public void stop() {
        this.run = false;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public IndividualAddress getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(IndividualAddress deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    @Override
    public GroupAddress getDpaddress() {
        return dpAddressPushButton;
    }

    public int getDimmer() {
        return this.dimmer;
    }

    public void setDimmer(int dimmer) {
        this.dimmer = dimmer;
    }

    public GroupAddress getDpaddressDimmer() {
        return this.dpAddressDimmer;
    }

    public void setDpAddressDimmer(GroupAddress dpAddress) {
        this.dpAddressDimmer = dpAddress;
    }

    @Override
    public void run() {
        try {
            StateDP pushButton = new StateDP(getDpaddress(), this.getDeviceName(), 0,
                    DPTXlatorBoolean.DPT_SWITCH.getID());
            StateDP dimmerButton = new StateDP(dpAddressDimmer, this.getDeviceName(), 0,
                    DPTXlator8BitEnum.DptDimmPushbuttonModel.getID());

            this.getDatapointModel().add(pushButton);
            this.getDatapointModel().add(dimmerButton);

            var device = new BaseKnxDevice(this.getDeviceName(), this);
            var netif = NetworkInterface.getByInetAddress(InetAddress.getByName(this.networkname));

            try (var link = KNXNetworkLinkIP.newRoutingLink(netif, KNXNetworkLinkIP.DefaultMulticast,
                    new KnxIPSettings(this.getDeviceAddress()))) {
                device.setDeviceLink(link);
                System.out.println(device + " is up and running, dimmer datapoint address is " + dpAddressDimmer);

                while (this.run) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (final KNXException e) {
                System.out.println("Running " + this.getDeviceName() + " failed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                System.out.println(this.getDeviceName() + " has left the building.");
            }
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    @Override
    public void updateDatapointValue(final Datapoint ofDp, final DPTXlator update) {
        if (update instanceof DPTXlator8BitEnum) {
            try {
                this.dimmer = this.dimmer - ((DPTXlator8BitEnum) update).getValueUnsigned();
                if (this.dimmer < 0) {
                    this.dimmer = 0;
                }
            } catch (KNXFormatException e) {
                e.printStackTrace();
            }
        } else if (update instanceof DPTXlatorBoolean) {
            state = ((DPTXlatorBoolean) update).getValueBoolean();
        }
        System.out.println(LocalTime.now() + " " + ofDp.getName() + " switched \"" + update.getValue() + "\"");
    }

    @Override
    public DPTXlator requestDatapointValue(final Datapoint ofDp) throws KNXException {
        if (ofDp.getMainAddress().equals(this.dpAddressPushButton)) {
            final DPTXlatorBoolean t = new DPTXlatorBoolean(ofDp.getDPT());
            t.setValue(state);

            System.out.println(LocalTime.now() + " Respond with \"" + t.getValue() + "\" to read-request for "
                    + ofDp.getName());
            return t;
        } else {
            final DPTXlator8BitEnum t = new DPTXlator8BitEnum(ofDp.getDPT());
            t.setValue(this.dimmer);
            System.out.println(LocalTime.now() + " Respond with \"" + t.getValue() + "\" to read-request for "
                    + ofDp.getName());
            return t;
        }
    }
}
