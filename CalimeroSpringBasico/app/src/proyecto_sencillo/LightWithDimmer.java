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

public class LightWithDimmer extends KnxDeviceServiceLogic implements Runnable, Serializable {

    private String deviceName = "Light with Dimmer (KNX IP)";
    private String networkname;
    private boolean run;
    private boolean state;
    private IndividualAddress deviceAddress;
    private int dimmer = 0;

    private static final GroupAddress dpAddressLight = new GroupAddress(1, 0, 3);
    private static final GroupAddress dpAddressDimmer = new GroupAddress(1, 0, 4);

    public LightWithDimmer(String name, int area, int line, int device, String networkname, int dimmer) {
        this.deviceName = name;
        this.deviceAddress = new IndividualAddress(area, line, device);
        this.networkname = networkname;
        this.run = false;
        this.dimmer = dimmer;
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

    public static GroupAddress getDpaddressDimmer() {
        return dpAddressDimmer;
    }

    public static GroupAddress getDpaddressLight() {
        return dpAddressLight;
    }

    @Override
    public void run() {
        try {

            // Crear el Data Point (DPTXlator) del dispositivo
            StateDP pushButton = new StateDP(dpAddressLight, this.getDeviceName(), 0,
                    DPTXlatorBoolean.DPT_SWITCH.getID());
            StateDP dimmer = new StateDP(dpAddressDimmer, this.getDeviceName(), 0,
                    DPTXlator8BitEnum.DptDimmPushbuttonModel.getID());

            // Añadir el dispositivo
            this.getDatapointModel().add(pushButton);
            this.getDatapointModel().add(dimmer);

            var device = new BaseKnxDevice(this.getDeviceName(), this);
            var netif = NetworkInterface.getByInetAddress(InetAddress.getByName(this.networkname));

            try (

                    var link = KNXNetworkLinkIP.newRoutingLink(netif, KNXNetworkLinkIP.DefaultMulticast,
                            new KnxIPSettings(this.getDeviceAddress()))) {
                device.setDeviceLink(link);
                System.out.println(device + " is up and running, dimmer datapoint address is " + dpAddressDimmer);

                // just let the service logic sit idle and wait for messages
                while (this.run)
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            } catch (final KNXException e) {
                System.out.println("Running " + this.getDeviceName() + " failed: " + e.getMessage());
                e.printStackTrace();
            }
            // catch (final InterruptedException e) {}
            finally {
                System.out.println(this.getDeviceName() + " has left the building.");

            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    @Override
    public void updateDatapointValue(final Datapoint ofDp, final DPTXlator update) {
        // Este método se llama para un servicio de indicación de escritura de
        // comunicación de proceso KNX:
        // actualizar el valor de punto de datos según el dispositivo
        if (update instanceof DPTXlator8BitEnum){
			try {
                int changeValue = ((DPTXlator8BitEnum) update).getValueUnsigned();
                System.out.println("Valor recibido: " + changeValue); // Muestra el valor recibido            

				this.dimmer=this.dimmer-((DPTXlator8BitEnum) update).getValueUnsigned();
				if(this.dimmer<0){
					this.dimmer=0;
				}
			} catch (KNXFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(update instanceof DPTXlatorBoolean){
			state = ((DPTXlatorBoolean) update).getValueBoolean();
			
		}
		System.out.println(LocalTime.now() + " " + ofDp.getName() + " switched \"" + update.getValue() + "\"");
		
	}

    @Override
    public DPTXlator requestDatapointValue(final Datapoint ofDp) throws KNXException {
        // Este método se llama si recibimos una solicitud de lectura de comunicación de
        // proceso KNX:

        // var m = ofDp.getMainAddress();
        if (ofDp.getMainAddress().equals(this.dpAddressLight)) {
            final DPTXlatorBoolean t = new DPTXlatorBoolean(ofDp.getDPT());
            // set our current button state, the translator will translate it accordingly
            t.setValue(state);

            System.out.println(
                    LocalTime.now() + " Respond with \"" + t.getValue() + "\" to read-request for " + ofDp.getName());
            return t;

        } else {

            final DPTXlator8BitEnum t = new DPTXlator8BitEnum(ofDp.getDPT());
            // set our current button state, the translator will translate it accordingly
            t.setValue(String.valueOf(this.dimmer));

            System.out.println(
                    LocalTime.now() + " Respond with \"" + t.getValue() + "\" to read-request for " + ofDp.getName());
            return t;
        }
    }
}