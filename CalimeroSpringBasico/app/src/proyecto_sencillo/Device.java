package proyecto_sencillo;

import io.calimero.GroupAddress;
import io.calimero.IndividualAddress;

public interface Device {
    String getId();
    String getDeviceName();
    void start();
    void stop();
    IndividualAddress getDeviceAddress();
    GroupAddress getDpaddress();
}
