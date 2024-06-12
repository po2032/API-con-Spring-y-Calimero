package proyecto_sencillo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalTime;

import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;
import io.calimero.KNXException;
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

public class Light extends KnxDeviceServiceLogic implements Device, Runnable, Serializable {

	private String id;
    private String deviceName;
    private String networkname;
    private boolean run;
    private boolean state;
    private IndividualAddress deviceAddress;
    private GroupAddress dpAddressPushButton;
    private NetworkInterface networkInterface;
    private InetAddress localEndpoint;
	
    public Light(String id, int area, int line, int device, String name, String networkname, GroupAddress dpAddressPushButton) {
        this.id = id;
        this.deviceName = name;
        this.deviceAddress = new IndividualAddress(area, line, device);
        this.networkname = networkname;
        this.dpAddressPushButton = dpAddressPushButton;
        this.run = false;
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

	@Override
	public void run() {
		try {

			// Crear el Data Point (DPTXlator) del dispositivo
			StateDP pushButton = new StateDP(dpAddressPushButton, this.getDeviceName(), 0,
					DPTXlatorBoolean.DPT_SWITCH.getID());

			// Añadir el dispositivo
			this.getDatapointModel().add(pushButton);

			var device = new BaseKnxDevice(this.getDeviceName(), this);
			var netif = NetworkInterface.getByInetAddress(InetAddress.getByName(this.networkname));

			try (

					var link = KNXNetworkLinkIP.newRoutingLink(netif, KNXNetworkLinkIP.DefaultMulticast,
							new KnxIPSettings(this.getDeviceAddress()))) {
				device.setDeviceLink(link);
				System.out.println(
						device + " iniciado push-button datapoint address: " + dpAddressPushButton + 
						", IP: " + this.networkname);

				// just let the service logic sit idle and wait for messages
				while (this.run)
					try {
						Thread.sleep(1000);
						//System.out.println("Hilo ejecutando: " + this.deviceName.toString());
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
		// Este método se llama wrt para un servicio de indicación de escritura de
		// comunicación de proceso KNX:
		// actualizar el valor de punto de datos según el dispositivo
		if (update instanceof DPTXlator8BitEnum) {
			// Valores de otros dispositivos (ejemplo dimmer)
		} else if (update instanceof DPTXlatorBoolean) {
			state = ((DPTXlatorBoolean) update).getValueBoolean();
		}
		System.out.println(LocalTime.now() + " " + ofDp.getName() + " switched \"" + update.getValue() + "\"");
	}

	@Override
	public DPTXlator requestDatapointValue(final Datapoint ofDp) throws KNXException {
		// Este método se llama si recibimos una solicitud de lectura de comunicación de
		// proceso KNX:
		// respondemos con nuestro valor del punto de datos actual que representa el
		// estado del dispositivo

		// var m = ofDp.getMainAddress();

		if (ofDp.getMainAddress().equals(this.dpAddressPushButton)) {
			final DPTXlatorBoolean t = new DPTXlatorBoolean(ofDp.getDPT());
			// establece el estado actual del PushButton
			t.setValue(state);

			System.out.println(
					LocalTime.now() + " Respond with \"" + t.getValue() + "\" to read-request for " + ofDp.getName());
			return t;

		} else {
			// valores para otros dispositivos (Ejemplo Dimmer)
			final DPTXlator8BitEnum t = new DPTXlator8BitEnum(ofDp.getDPT());
			// establece el estado actual del Dimmer
			t.setValue("44");

			System.out.println(
					LocalTime.now() + " Respond with \"" + t.getValue() + "\" to read-request for " + ofDp.getName());
			return t;
		}
	}
}
