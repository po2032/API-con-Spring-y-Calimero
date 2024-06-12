package proyecto_sencillo;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.calimero.GroupAddress;
import io.calimero.datapoint.Datapoint;
import io.calimero.knxnetip.Discoverer.Result;
import io.calimero.knxnetip.servicetype.SearchResponse;
import io.calimero.tools.DeviceInfo;
import io.calimero.tools.Discover;
import io.calimero.tools.ProcComm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(value = "/network")
public class NetworkKNX {

    private static final Logger logger = LoggerFactory.getLogger(NetworkKNX.class);
    // Lista de dispositivos en la clase NetworkKNX
    private List<Device> devices;

    // Constructor de la clase NetworkKNX
    public NetworkKNX() {
        this.devices = new ArrayList<>();
        initializeDevices();
    }

    // Método para inicializar los dispositivos virtuales creados
    private void initializeDevices() {
        // Creación de instancias de dispositivos tipo Light (on/off)
       // Light l1 = new Light("l1", 1, 1, 60, "Light 1", "192.168.1.60", new GroupAddress(1, 0, 6));
        Light l2 = new Light("l2", 1, 1, 127, "Light 2", "192.168.1.127", new GroupAddress(1, 0, 7));
       // Light l3 = new Light("l3", 1, 1, 62, "Light 3", "192.168.1.62", new GroupAddress(1, 0, 3));

        // Creación de instancias de dispositivos tipo LightWithDimmer (on/off con
        // dimmer)
        LightWithDimmer d1 = new LightWithDimmer("d1", "Light with Dimmer 1", 1, 1, 61, "192.168.1.61", 100);
        //LightWithDimmer d2 = new LightWithDimmer("d2", "Light with Dimmer 2", 1, 1, 51, "192.168.1.51", 50);

        // Inicia los dispositivos simulados (esto podría implicar que comienzan a
        // responder a comandos o enviar actualizaciones de estado)
    //    l1.start();
        l2.start();
     //   l3.start();
        d1.start();
        //d2.start();

        // Agrega los dispositivos creados a la lista de dispositivos de la red
      //  devices.add(l1);
        devices.add(l2);
     //   devices.add(l3);
        devices.add(d1);
        //devices.add(d2);
    }

// Anotación para manejar solicitudes GET en la ruta "/search"
@GetMapping("/search")
// Anotación para ejecutar el método de forma asíncrona
@Async
public CompletableFuture<List<String>> search() {
    // Objeto CompletableFuture para manejar la respuesta de la búsqueda de dispositivos
    CompletableFuture<List<String>> res = new CompletableFuture<>();
    // Lista que almacenará las respuestas en formato JSON
    List<String> jsonResponseList = new ArrayList<>();
    // Objeto Gson para convertir objetos Java a JSON
    Gson gson = new Gson();

    try {
        // Argumentos para inicializar el objeto Discover de Calimero
        String args2[] = { "search" };
        // Instancia de la clase Discover para realizar la búsqueda de dispositivos
        final Discover d = new Discover(args2) {
            // Método llamado al completar la búsqueda
            @Override
            protected void onCompletion(Exception arg0, boolean arg1) {
                // Verifica si hubo una excepción durante la búsqueda
                if (arg0 != null) {
                    // Registra el error si hubo una excepción
                    logger.error("Error during search completion: {}", arg0.getMessage());
                }
                // Completa el CompletableFuture con la lista de respuestas JSON
                res.complete(jsonResponseList);
            }

            // Método llamado al recibir información de un endpoint de un dispositivo
            @Override
            protected void onEndpointReceived(final Result<SearchResponse> result) {
                // Obtiene la respuesta de búsqueda
                final SearchResponse r = result.response();
                // Crea un objeto JSON para almacenar la información del dispositivo
                JsonObject endpointJson = new JsonObject();
                endpointJson.addProperty("deviceName", r.getDevice().getName());
                endpointJson.addProperty("controlEndpoint", r.getControlEndpoint().toString());
                endpointJson.addProperty("networkInterface", result.networkInterface().getName());
                endpointJson.addProperty("localEndpoint", result.localEndpoint().getAddress().toString());
                endpointJson.addProperty("deviceDescription", r.getDevice().toString().replaceAll("\".*\"", ""));
                endpointJson.addProperty("supportedServices", r.getServiceFamilies().toString());
                // Convierte el objeto JSON a una cadena
                String jsonResponse = gson.toJson(endpointJson);
                // Agrega la respuesta JSON a la lista de respuestas
                jsonResponseList.add(jsonResponse);
                // Registra la información del dispositivo encontrado
                logger.info("Device found: {}", jsonResponse);
            }
        };
        // Ejecuta la búsqueda
        d.run();
    } catch (final Throwable t) {
        // Registra cualquier excepción que ocurra durante la búsqueda
        logger.error("Exception during search: {}", t.getMessage(), t);
        // Completa el CompletableFuture excepcionalmente si ocurre un error
        res.completeExceptionally(t);
    }
    // Retorna el CompletableFuture con la lista de dispositivos encontrados
    return res;
}


    @GetMapping("/infodevice/{individualAddress}")
    @Async
    public CompletableFuture<String> info(@PathVariable String individualAddress) {
        CompletableFuture<String> res = new CompletableFuture<>();
        Gson gson = new Gson();
        // Mapa para almacenar la información del dispositivo
        Map<String, String> deviceInfo = new LinkedHashMap<>();
        try {
            logger.info("Iniciando recuperación de información para la dirección: {}", individualAddress);
            // Argumentos para inicializar el objeto DeviceInfo de Calimero
            String args2[] = { "224.0.23.12", individualAddress };

            DeviceInfo d = new DeviceInfo(args2) {
                @Override
                protected void onCompletion(Exception thrown, boolean canceled) {
                    if (thrown != null) {
                        logger.error("Error durante la finalización de la información del dispositivo: {}", thrown.getMessage());
                    }
                    res.complete(gson.toJson(deviceInfo));
                }

                @Override
                protected void onDeviceInformation(Parameter parameter, String value, byte[] raw) {
                    deviceInfo.put(parameter.toString(), value);
                    logger.info("Parámetro del dispositivo recibido: {} = {}", parameter, value);
                }
            };

            d.run();

        } catch (final Throwable t) {
            logger.error("Excepción durante la recuperación de información del dispositivo: {}", t.getMessage(), t);
            res.completeExceptionally(t);
        }
        logger.info("Finalizada la recuperación de información para la dirección: {}", individualAddress);
        return res;
    }

    @GetMapping("/state")
    @Async
    public CompletableFuture<String> getState(@RequestParam String individualAddress) {
        CompletableFuture<String> res = new CompletableFuture<>();
        Device device = getDeviceByIndividualAddress(individualAddress);
        if (device == null) {
            String errorMessage = "Device not found for address: " + individualAddress;
            logger.error(errorMessage);
            res.completeExceptionally(new NullPointerException(errorMessage));
            return res;
        }
        String groupAddress = device.getDpaddress().toString();
        try {
            String args2[] = { "read", groupAddress, "switch", "224.0.23.12", "--json" };
            final ProcComm d = new ProcComm(args2) {
                @Override
                protected void onReadResponse(Datapoint dp, String value) {
                    final StringBuilder buf = new StringBuilder();
                    buf.append(value);
                    logger.info("Received read response: {}", value);
                    res.complete(buf.toString());
                }

                @Override
                protected void onCompletion(Exception thrown, boolean canceled) {
                    if (thrown != null) {
                        logger.error("Error during read completion: {}", thrown.getMessage());
                    } else {
                        logger.info("Read completion status: canceled={}", canceled);
                    }
                }
            };
            d.run();
        } catch (final Throwable t) {
            logger.error("Exception during state retrieval: {}", t.getMessage(), t);
            res.completeExceptionally(t);
        }
        //res.complete("Device not found");
        return res;
    }

    @PostMapping("/on")
    @Async
    public CompletableFuture<String> setOn(@RequestParam String individualAddress) {
        CompletableFuture<String> res = new CompletableFuture<>();
        // Obtiene el dispositivo por su dirección individual
        Device device = getDeviceByIndividualAddress(individualAddress);
        // Si el dispositivo existe, llama al método para encenderlo
        if (device != null) {
            turnOnDevice(device, res, individualAddress);
        } else {
            res.complete("Device not found");
        }
        return res;
    }

    private void turnOnDevice(Device device, CompletableFuture<String> res, String individualAddress) {
        try {
            // Obtiene la dirección de grupo del dispositivo
            String groupAddress = device.getDpaddress().toString();
            // Argumentos para el comando ProcComm de Calimero
            String[] args2 = { "write", groupAddress, "switch", "on", "224.0.23.12" };
            // Instancia ProcComm para enviar el comando de encendido
            final ProcComm d = new ProcComm(args2) {
                @Override
                protected void onCompletion(Exception thrown, boolean canceled) {
                    super.onCompletion(thrown, canceled);
                    res.complete("Device: " + individualAddress + " turned on");
                    logger.info("Device {} turned on", individualAddress);
                }
            };
            d.run();
        } catch (final Throwable t) {
            logger.error("Exception during turn on: {}", t.getMessage(), t);
            res.completeExceptionally(t);
        }
    }

    @PostMapping("/off")
    @Async
    public CompletableFuture<String> setOff(@RequestParam String individualAddress) {
        CompletableFuture<String> res = new CompletableFuture<>();
        Device device = getDeviceByIndividualAddress(individualAddress);

        if (device != null) {
            turnOffDevice(device, res, individualAddress);
        } else {
            res.complete("Device not found");
        }
        return res;
    }

    private void turnOffDevice(Device device, CompletableFuture<String> res, String individualAddress) {
        try {
            String groupAddress = device.getDpaddress().toString();
            String[] args2 = { "write", groupAddress, "switch", "off", "224.0.23.12" };

            final ProcComm d = new ProcComm(args2) {
                @Override
                protected void onCompletion(Exception thrown, boolean canceled) {
                    super.onCompletion(thrown, canceled);
                    res.complete("Device: " + individualAddress + " turned off");
                    logger.info("Device {} turned off", individualAddress);
                }
            };
            d.run();
        } catch (final Throwable t) {
            logger.error("Exception during turn off: {}", t.getMessage(), t);
            res.completeExceptionally(t);
        }
    }

    @PostMapping("/dimmerdecrease")
    @Async
    public CompletableFuture<String> setDimmerMinus(@RequestParam String individualAddress) {
        CompletableFuture<String> res = new CompletableFuture<>();
        Device device = getDeviceByIndividualAddress(individualAddress);

        if (device instanceof LightWithDimmer) {
            adjustDimmer((LightWithDimmer) device, res, "decrease 2");
        } else {
            res.complete("Dimmer device not found");
        }
        return res;
    }

    @PostMapping("/dimmerincrease")
    @Async
    public CompletableFuture<String> setDimmerPlus(@RequestParam String individualAddress) {
        CompletableFuture<String> res = new CompletableFuture<>();
        Device device = getDeviceByIndividualAddress(individualAddress);

        if (device instanceof LightWithDimmer) {
            adjustDimmer((LightWithDimmer) device, res, "increase 2");
        } else {
            res.complete("Dimmer device not found");
        }
        return res;
    }

    private void adjustDimmer(LightWithDimmer dimmerDevice, CompletableFuture<String> res, String action) {
        try {
            String groupAddress = dimmerDevice.getDpaddressDimmer().toString();
            String[] args2 = { "write", groupAddress, "dimmer", action, "224.0.23.12" };

            final ProcComm d = new ProcComm(args2) {
                @Override
                protected void onCompletion(Exception thrown, boolean canceled) {
                    super.onCompletion(thrown, canceled);
                    res.complete("Dimmer " + action);
                    logger.info("Dimmer action {} completed", action);
                }
            };
            d.run();
        } catch (final Throwable t) {
            logger.error("Exception during dimmer adjustment: {}", t.getMessage(), t);
            res.completeExceptionally(t);
        }
    }

    @GetMapping("/dimmerstate")
    @Async
    public CompletableFuture<String> getDimmerState(@RequestParam String individualAddress) {
        CompletableFuture<String> res = new CompletableFuture<>();
        Device device = getDeviceByIndividualAddress(individualAddress);

        if (device instanceof LightWithDimmer) {
            try {
                String args2[] = { "read", ((LightWithDimmer) device).getDpaddress().toString(), "dimmer",
                        "224.0.23.12", "--json" };

                final ProcComm d = new ProcComm(args2) {
                    @Override
                    protected void onReadResponse(Datapoint dp, String value) {
                        final StringBuilder buf = new StringBuilder();
                        buf.append(value);
                        logger.info("Received dimmer state: {}", value);
                        res.complete(buf.toString());
                    }

                    @Override
                    protected void onCompletion(Exception thrown, boolean canceled) {
                        if (thrown != null) {
                            logger.error("Error during dimmer state retrieval: {}", thrown.getMessage());
                        } else {
                            logger.info("Dimmer state retrieval status: canceled={}", canceled);
                        }
                    }
                };
                d.run();
            } catch (final Throwable t) {
                logger.error("Exception during dimmer state retrieval: {}", t.getMessage(), t);
                res.completeExceptionally(t);
            }
        } else {
            res.complete("Dimmer device not found");
        }
        return res;
    }

    /*
     * DIMMER STATE ANTERIOR
     */

    /*
     * @GetMapping("/dimmer")
     * 
     * @Async
     * public CompletableFuture<String> getDimmerSate() {
     * CompletableFuture<String> res = new CompletableFuture<>();
     * try {
     * // no funciona con ningún grupo!!
     * String args2[] = { "read", "1/0/3", "dimmer", "224.0.23.12", "--json" };
     * final String sep = "\n";
     * final ProcComm d = new ProcComm(args2) {
     * 
     * // lectura
     * 
     * @Override
     * protected void onReadResponse(Datapoint dp, String value) {
     * // TODO Auto-generated method stub
     * final StringBuilder buf = new StringBuilder();
     * // final SearchResponse r = result.response();
     * buf.append(value);
     * 
     * super.onReadResponse(dp, value);
     * res.complete(buf.toString());
     * }
     * 
     * };
     * // final ShutdownHandler sh = new ShutdownHandler().register();
     * d.run();
     * 
     * // sh.unregister();
     * } catch (final Throwable t) {
     * // out.log(ERROR, "parsing options", t);
     * }
     * res.complete("fdsfdsfdsfdsfds");
     * return res;
     * }
     */

    private Device getDeviceByIndividualAddress(String individualAddress) {
        for (Device device : devices) {
            if (device.getDeviceAddress().toString().equals(individualAddress)) {
                return device;
            }
        }
        return null;
    }
}
