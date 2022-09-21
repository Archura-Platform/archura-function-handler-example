package io.archura.platform.imperativeshell.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.archura.platform.api.cache.Cache;
import io.archura.platform.api.context.Context;
import io.archura.platform.api.logger.Logger;
import io.archura.platform.api.stream.LightStream;
import io.archura.platform.api.type.Configurable;
import io.archura.platform.imperativeshell.handler.model.Employee;
import io.archura.platform.imperativeshell.handler.model.Movie;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Component
public class SimpleFunction implements HandlerFunction<ServerResponse>, Configurable {

    private Map<String, Object> configuration;
    private final Random random = new Random();

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    @Override
    public ServerResponse handle(ServerRequest request) throws IOException, InterruptedException {
        final Context context = (Context) request.attributes().get(Context.class.getSimpleName());
        final Optional<Cache> optionalCache = context.getCache();
        final Optional<LightStream> optionalStream = context.getLightStream();
        final HttpClient httpClient = context.getHttpClient();
        final Logger logger = context.getLogger();
        logger.info("request = " + request + " configuration: " + configuration);

        try {
            new Thread(() -> {
                logger.info("Thread will sleep");
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                logger.info("Thread done");
            }, "NEW_THREAD_1").start();
        } catch (Exception ex) {
            logger.error("Thread Exception: " + ex.getMessage());
        }

        String hostname = "time.nist.gov";
        int port = 13;
        try (Socket socket = new Socket(hostname, port)) {
            InputStream input = socket.getInputStream();
            InputStreamReader reader = new InputStreamReader(input);
            int character;
            StringBuilder data = new StringBuilder();
            while ((character = reader.read()) != -1) {
                data.append((char) character);
            }
            logger.info("Socket Data: %s", data);
        } catch (Exception ex) {
            logger.error("Socket Exception: " + ex.getMessage());
        }

        int serverPort = 9876;
        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            logger.info("ServerSocket is listening on serverPort " + serverPort);
            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("ServerSocket New client connected");
                OutputStream output = socket.getOutputStream();
                PrintWriter writer = new PrintWriter(output, true);
                writer.println(new Date());
            }
        } catch (Exception ex) {
            logger.info("ServerSocket Exception: " + ex.getMessage());
        }

        try {
            String parseLine; /* variable definition */
            /* create objects */
            URL URL = new URL("http://www.example.com/");
            BufferedReader br = new BufferedReader(new InputStreamReader(URL.openStream()));

            while ((parseLine = br.readLine()) != null) {
                /* read each line */
                System.out.println(parseLine);
            }
            br.close();
        } catch (Exception ex) {
            logger.info("URL Exception: " + ex.getMessage());
        }

        optionalCache
                .ifPresent(
                        cache -> {
                            try {
                                final Class<? extends Cache> aClass = cache.getClass();
                                final Field[] declaredFields = aClass.getDeclaredFields();
                                logger.info("declaredFields.length = " + declaredFields.length);
                                for (Field field : declaredFields) {
                                    field.setAccessible(true);
                                    final Object value = field.get(cache);
                                    logger.info("field = " + field + " value: " + value);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            final Map<String, Object> cacheData = Map.of(
                                    "k1", "value1",
                                    "k2", 2,
                                    "k3", true,
                                    "k4", new HashMap<>(),
                                    "k5", new Date());
                            final String keyValues = "keyValues";
                            cache.put(keyValues, cacheData);
                            final Map<String, Object> map = cache.get(keyValues);
                            logger.info("map = " + map);
                        }
                );

        optionalStream.ifPresent(stream -> {
            final ObjectMapper objectMapper = context.getObjectMapper();
            final Movie movie = new Movie("Movie Title", random.nextInt(1900, 2000));
            String movieString = "";
            try {
                movieString = objectMapper.writeValueAsString(movie);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            final byte[] value = movieString.getBytes(StandardCharsets.UTF_8);

            final String topic = Optional.ofNullable(configuration.get("topicName"))
                    .map(String::valueOf)
                    .orElse("movies");

            final String key = stream.send(topic, value).orElse("UNKNOWN_KEY");
            logger.info("Sent value '%s' for key '%s' to topic '%s'", key, movieString, topic);
        });

        final String url = Optional.ofNullable(configuration.get("JSON_URL"))
                .map(String::valueOf)
                .orElse("http://localhost:9090/sono.json");
        final String forwarded = Optional.ofNullable(request.headers().firstHeader("Forwarded"))
                .orElse("");
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("Forwarded", forwarded)
                .timeout(Duration.ofMillis(100))
                .build();
        final HttpResponse<InputStream> httpResponse = httpClient
                .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        final ObjectMapper objectMapper = new ObjectMapper();
        final Employee employee = objectMapper.readValue(httpResponse.body(), Employee.class);
        logger.info(employee.toString());

        return ServerResponse.ok()
                .header("SIMPLE_FUNCTION_HEADER", "GOT_EMPLOYEE")
                .body(employee);
    }
}
