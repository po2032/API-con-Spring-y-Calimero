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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(value = "/network")
public class NetworkKNX {

	private List<Light> devices;

	public NetworkKNX() {
		this.devices = new ArrayList<>();
		initializeDevices();
	}

	private void initializeDevices() {
		Light l1 = new Light("l1", 1, 1, 131, "Light",
				"192.168.1.131",
				new GroupAddress(1, 0, 3));
		Light l2 = new Light("l2", 1, 1, 129, "Light 2",
				"192.168.1.129",
				new GroupAddress(1, 0, 3));
		Light l3 = new Light("l3", 1, 1, 51, "Light 3",
				"192.168.1.51",
				new GroupAddress(1, 0, 4));

		l1.start();
		l2.start();
		l3.start();

		devices.add(l1);
		devices.add(l2);
		devices.add(l3);
	}

	/**
	 * ---------------------------- /SEARCH METHOD (ALL DEVICES)
	 * ---------------------------- /INFEDEVICE/{INDIVIDUAL ADDRESS} METHOD (ALL
	 * DEVICES)
	 * La información de ambos se obtiene en Formato JSON
	 * 
	 * ---------------------------- /STATE/{INDIVIDUAL ADDRESS} METHOD (ALL
	 * DEVICES)
	 */
	@GetMapping("/search")
	@Async
	public CompletableFuture<List<String>> search() {
		CompletableFuture<List<String>> res = new CompletableFuture<>();
		List<String> jsonResponseList = new ArrayList<>();
		Gson gson = new Gson();

		try {
			String args2[] = { "search" };
			final Discover d = new Discover(args2) {
				@Override
				protected void onCompletion(Exception arg0, boolean arg1) {
					if (arg0 != null) {
						System.err.println("Error during search completion: " + arg0.getMessage());
					}
					res.complete(jsonResponseList);
				}

				@Override
				protected void onEndpointReceived(final Result<SearchResponse> result) {
					final SearchResponse r = result.response();
					JsonObject endpointJson = new JsonObject();
					endpointJson.addProperty("deviceName", r.getDevice().getName());
					endpointJson.addProperty("controlEndpoint", r.getControlEndpoint().toString());
					endpointJson.addProperty("networkInterface", result.networkInterface().getName());
					endpointJson.addProperty("localEndpoint", result.localEndpoint().getAddress().toString());
					endpointJson.addProperty("deviceDescription", r.getDevice().toString().replaceAll("\".*\"", ""));
					endpointJson.addProperty("supportedServices", r.getServiceFamilies().toString());
					String jsonResponse = gson.toJson(endpointJson);
					jsonResponseList.add(jsonResponse);
					System.out.println("Device found: " + jsonResponse);
				}
			};
			d.run();
		} catch (final Throwable t) {
			t.printStackTrace();
			res.completeExceptionally(t);
		}
		return res;
	}

	/**
	 * Informacion del dispositivo en base a su Dirección individual
	 * 
	 * @return
	 */
	@GetMapping("/infodevice/{individualAddress}")
	@Async
	public CompletableFuture<String> info(@PathVariable String individualAddress) {
		CompletableFuture<String> res = new CompletableFuture<>();
		Gson gson = new Gson();

		try {
			Map<String, String> deviceInfo = new LinkedHashMap<>();
			String args2[] = { "224.0.23.12", individualAddress };

			DeviceInfo d = new DeviceInfo(args2) {
				@Override
				protected void onCompletion(Exception thrown, boolean canceled) {
					if (thrown != null) {
						System.err.println("Error during device info completion: " + thrown.getMessage());
					}
					res.complete(gson.toJson(deviceInfo));
				}

				@Override
				protected void onDeviceInformation(Parameter parameter, String value, byte[] raw) {
					deviceInfo.put(parameter.toString(), value);
					System.out.println("Device parameter received: " + parameter + " = " + value);
				}
			};

			d.run();

		} catch (final Throwable t) {
			t.printStackTrace();
			res.completeExceptionally(t);
		}
		return res;
	}

	/**
	 * Obtener el estado del dispositivo
	 * 
	 * @return
	 */
	@GetMapping("/state")
    @Async
	public CompletableFuture<String>  getSate(){
		CompletableFuture<String> res = new CompletableFuture<>();
		try {
			
            String args2[]={"read", "1/0/3", "switch", "224.0.23.12", "--json"};
            final String sep = "\n";
			final  ProcComm d =  new ProcComm(args2){
               
				//lectura
				@Override
                protected void onReadResponse(Datapoint dp, String value) {
                    // TODO Auto-generated method stub
                    final StringBuilder buf = new StringBuilder();
					//final SearchResponse r = result.response();
                    buf.append(value);
					System.out.println("VALUE: "+value.toString());
                    super.onReadResponse(dp, value);
					System.out.println("RESPUESTA: "+res.toString());
                    res.complete(buf.toString());
                }			
			};
			//final ShutdownHandler sh = new ShutdownHandler().register();
			d.run();
            
			//sh.unregister();
		}
		catch (final Throwable t) {
			//out.log(ERROR, "parsing options", t);
			System.out.println("MENSAJE DE ERROR ESTADO: "+t.getMessage());
		}
		res.complete("Device not found");
		System.out.println("respuesta ESTADO: "+res.toString());
		return res;
    }

	/*@GetMapping("/state")
	@Async
	public CompletableFuture<String> getState(@RequestParam String individualAddress) {
		CompletableFuture<String> res = new CompletableFuture<>();
		Light device = getDeviceByIndividualAddress(individualAddress);

		if (device != null) {
			try {
				String groupAddress = device.getDpaddress().toString();
				String[] args2 = { "read", groupAddress, "switch", "224.0.23.12", "--timeout", "10" }; // Añadido
																										// timeout de 10
																										// segundos

				final ProcComm d = new ProcComm(args2) {
					@Override
					protected void onReadResponse(Datapoint dp, String value) {
						final StringBuilder buf = new StringBuilder();
						buf.append(value);
						System.out.println("Estado: " + value);
						super.onReadResponse(dp, value);
						res.complete(buf.toString());
					}

					@Override
					protected void onCompletion(Exception thrown, boolean canceled) {
						if (thrown != null) {
							System.err.println("Error during read completion: " + thrown.getMessage());
							res.completeExceptionally(thrown);
						} else {
							res.complete("Completed without exception.");
						}
					}
				};
				d.run();
			} catch (final Throwable t) {
				t.printStackTrace();
				res.completeExceptionally(t);
			}
		} else {
			res.complete("Device not found");
		}
		return res;
	}*/

	/**
	 * --------------------------------- PUSH BUTTON (LIGHT) METHODS
	 * ON METHOD - Enciende luz
	 * OFF METHOD - Apaga luz
	 */

	/*@PostMapping("/on")
	@Async
	public CompletableFuture<String> setOn() {
		CompletableFuture<String> res = new CompletableFuture<>();
		try {
			String args2[] = { "write", "1/0/3", "switch", "on", "224.0.23.12" };

			final ProcComm d = new ProcComm(args2) {

				@Override
				protected void onCompletion(Exception thrown, boolean canceled) {
					super.onCompletion(thrown, canceled);
					res.complete("En on");
				}
			};
			d.run();
		} catch (final Throwable t) {
			// out.log(ERROR, "parsing options", t);
		}
		return res;
	}*/

	/*
	 * METHOD ON que sirve para especificar el grupo de direcciones al que se quiere
	 * acceder
	 * 
	 */
	@PostMapping("/on")
	@Async
	public CompletableFuture<String> setOn(@RequestParam String individualAddress) {
		CompletableFuture<String> res = new CompletableFuture<>();
		Light device = getDeviceByIndividualAddress(individualAddress);

		if (device != null) {
			try {
				String groupAddress = device.getDpaddress().toString();
				String[] args2 = { "write", groupAddress, "switch", "on", "224.0.23.12" };

				final ProcComm d = new ProcComm(args2) {
					@Override
					protected void onCompletion(Exception thrown, boolean canceled) {
						super.onCompletion(thrown, canceled);
						res.complete("Device: " + individualAddress + " turned on");
					}
				};
				d.run();
			} catch (final Throwable t) {
				t.printStackTrace();
				res.completeExceptionally(t);
			}
		} else {
			res.complete("Device not found");
		}
		return res;
	}

	private Light getDeviceByIndividualAddress(String individualAddress) {
		for (Light device : devices) {
			if (device.getDeviceAddress().toString().equals(individualAddress)) {
				return device;
			}
		}
		return null;
	}

	/*@PostMapping("/off")
	@Async
	public CompletableFuture<String> setOff() {
		CompletableFuture<String> res = new CompletableFuture<>();
		try {
			String args2[] = { "write", "1/0/3", "switch", "off", "224.0.23.12" };

			final ProcComm d = new ProcComm(args2) {

				@Override
				protected void onCompletion(Exception thrown, boolean canceled) {
					// TODO Auto-generated method stub

					super.onCompletion(thrown, canceled);
					res.complete("En off");
				}

			};
			// final ShutdownHandler sh = new ShutdownHandler().register();
			d.run();

			// sh.unregister();
		} catch (final Throwable t) {
			// out.log(ERROR, "parsing options", t);
		}
		return res;
	}*/

	/*
	 * METHOD OFF que sirve para especificar el grupo de direcciones al que se quiere
	 * acceder
	 * 
	 */
	@PostMapping("/off")
	@Async
	public CompletableFuture<String> setOff(@RequestParam String individualAddress) {
		CompletableFuture<String> res = new CompletableFuture<>();
		Light device = getDeviceByIndividualAddress(individualAddress);

		if (device != null) {
			try {
				String groupAddress = device.getDpaddress().toString();
				String[] args2 = { "write", groupAddress, "switch", "off", "224.0.23.12" };

				final ProcComm d = new ProcComm(args2) {
					@Override
					protected void onCompletion(Exception thrown, boolean canceled) {
						super.onCompletion(thrown, canceled);
						res.complete("Device: " + individualAddress + " turned off");
					}
				};
				d.run();
			} catch (final Throwable t) {
				t.printStackTrace();
				res.completeExceptionally(t);
			}
		} else {
			res.complete("Device not found");
		}
		return res;
	}

	/**
	 * ------------------------------ DIMMER METHODS
	 * 
	 * DECREASE METHOD - Disminuye la intensidad
	 * INCREASE METHOD - Aumenta la intensidad
	 * DIMMERSTATE METHOD - Obtiene información del estado del DIMMER
	 */

	@GetMapping("/dimmerdecrease")
	@Async
	public CompletableFuture<String> setDimmerMinus() {
		CompletableFuture<String> res = new CompletableFuture<>();
		try {
			// String v=String.valueOf((int)(Math.random()*100));
			String args2[] = { "write", "1/0/4", "dimmer", "decrease 2", "224.0.23.12" };

			final ProcComm d = new ProcComm(args2) {

				@Override
				protected void onCompletion(Exception thrown, boolean canceled) {
					// TODO Auto-generated method stub
					super.onCompletion(thrown, canceled);
					res.complete("Dimmer Decrease");
					String valor = res.toString();
					System.out.println("Respuesta: " + valor);
				}
			};
			// final ShutdownHandler sh = new ShutdownHandler().register();
			d.run();

			// sh.unregister();
		} catch (final Throwable t) {
			// out.log(ERROR, "parsing options", t);
		}

		return res;
	}

	@GetMapping("/dimmerincrease")
	@Async
	public CompletableFuture<String> setDimmerPlus() {
		CompletableFuture<String> res = new CompletableFuture<>();
		try {
			// String v=String.valueOf((int)(Math.random()*100));
			String args2[] = { "write", "1/0/4", "dimmer", "increase 2", "224.0.23.12" };

			final ProcComm d = new ProcComm(args2) {

				@Override
				protected void onCompletion(Exception thrown, boolean canceled) {
					// TODO Auto-generated method stub
					super.onCompletion(thrown, canceled);
					res.complete("Dimmer Increase");
					String valor = res.toString();
					System.out.println("Respuesta: " + valor);
				}
			};
			// final ShutdownHandler sh = new ShutdownHandler().register();
			d.run();

			// sh.unregister();
		} catch (final Throwable t) {
			// out.log(ERROR, "parsing options", t);
		}
		return res;
	}

	@GetMapping("/dimmerstate")
	@Async
	public CompletableFuture<String> getDimmerSate() {
		CompletableFuture<String> res = new CompletableFuture<>();
		try {
			String args2[] = { "read", "1/0/5", "dimmer", "224.0.23.12", "--json" };
			// final String sep = "\n";
			final ProcComm d = new ProcComm(args2) {

				// lectura
				@Override
				protected void onReadResponse(Datapoint dp, String value) {
					System.out.println("Received read response: Datapoint=" + dp.getName() + ", Value=" + value);
					// TODO Auto-generated method stub
					final StringBuilder buf = new StringBuilder();
					// final SearchResponse r = result.response();
					buf.append(value);

					super.onReadResponse(dp, value);
					res.complete(buf.toString());
				}

				@Override
				protected void onCompletion(Exception thrown, boolean canceled) {
					if (thrown != null) {
						System.out.println("Error during ProcComm execution: " + thrown.getMessage());
					} else if (canceled) {
						System.out.println("ProcComm execution was canceled.");
					} else {
						System.out.println("ProcComm execution completed successfully.");
					}
				}
			};
			// final ShutdownHandler sh = new ShutdownHandler().register();
			System.out.println("Starting ProcComm with args: " + Arrays.toString(args2));

			d.run();

			// sh.unregister();
		} catch (final Throwable t) {
			// out.log(ERROR, "parsing options", t);
		}
		// res.complete("fdsfdsfdsfdsfds");
		return res;
	}

}