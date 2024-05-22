package proyecto_sencillo;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.calimero.datapoint.Datapoint;
import io.calimero.knxnetip.Discoverer.Result;
import io.calimero.knxnetip.servicetype.SearchResponse;
import io.calimero.tools.DeviceInfo;
import io.calimero.tools.Discover;
import io.calimero.tools.ProcComm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(value = "/network")
public class NetworkKNX {

	/**
	 * ---------------------------- /SEARCH METHOD (ALL DEVICES)
	 * ---------------------------- /INFEDEVICE/{INDIVIDUAL ADDRESS} METHOD (ALL
	 * DEVICES)
	 * La información de ambos se obtiene en Formato JSON
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

					// Convertir JsonObject a cadena JSON
					String jsonResponse = gson.toJson(endpointJson);
					jsonResponseList.add(jsonResponse);
				}
			};
			d.run();
		} catch (final Throwable t) {
			t.printStackTrace();
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
			final DeviceInfo d = new DeviceInfo(args2) {
				@Override
				protected void onCompletion(Exception thrown, boolean canceled) {
					res.complete(gson.toJson(deviceInfo));
				}

				@Override
				protected void onDeviceInformation(Parameter parameter, String value, byte[] raw) {
					deviceInfo.put(parameter.toString(), value);
				}
			};
			d.run();
		} catch (final Throwable t) {
			System.out.println("Error: " + t.toString());
		}
		return res;
	}

	/**
	 * --------------------------------- PUSH BUTTON (LIGHT) METHODS
	 * ON METHOD - Enciende luz
	 * OFF METHOD - Apaga luz
	 * STATE - METHOD - Obtiene información del estado de la luz
	 */

	@GetMapping("/on")
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
	}

	@GetMapping("/off")
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
	}

	/**
	 * Obtener el estado del dispositivo
	 * 
	 * @return
	 */
	@GetMapping("/state")
	@Async
	public CompletableFuture<String> getSate() {
		CompletableFuture<String> res = new CompletableFuture<>();
		try {

			String args2[] = { "read", "1/0/3", "switch", "224.0.23.12", "--json" };
			final String sep = "\n";
			final ProcComm d = new ProcComm(args2) {

				// lectura
				@Override
				protected void onReadResponse(Datapoint dp, String value) {
					// TODO Auto-generated method stub
					final StringBuilder buf = new StringBuilder();
					// final SearchResponse r = result.response();
					buf.append(value);

					super.onReadResponse(dp, value);
					res.complete(buf.toString());
				}
			};
			// final ShutdownHandler sh = new ShutdownHandler().register();
			d.run();

			// sh.unregister();
		} catch (final Throwable t) {
			// out.log(ERROR, "parsing options", t);
		}
		// res.complete("fdsfdsfdsfdsfds");
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
					res.complete("Dimmer Decrease 2 points");
					String valor = String.valueOf(res.complete("Dimmer Decrease 2 points"));
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
	}

	@GetMapping("/dimmerstate")
	@Async
	public CompletableFuture<String> getDimmerSate() {
		CompletableFuture<String> res = new CompletableFuture<>();
		try {

			String args2[] = { "read", "1/0/4", "dimmer", "224.0.23.12", "--json" };
			// final String sep = "\n";
			final ProcComm d = new ProcComm(args2) {

				// lectura
				@Override
				protected void onReadResponse(Datapoint dp, String value) {
					// TODO Auto-generated method stub
					final StringBuilder buf = new StringBuilder();
					// final SearchResponse r = result.response();
					buf.append(value);

					super.onReadResponse(dp, value);
					res.complete(buf.toString());
				}

			};
			// final ShutdownHandler sh = new ShutdownHandler().register();
			d.run();

			// sh.unregister();
		} catch (final Throwable t) {
			// out.log(ERROR, "parsing options", t);
		}
		// res.complete("fdsfdsfdsfdsfds");
		return res;
	}

}