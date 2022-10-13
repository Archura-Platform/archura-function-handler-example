package io.archura.platform.imperativeshell.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.archura.platform.api.cache.Cache;
import io.archura.platform.api.context.Context;
import io.archura.platform.api.http.HttpServerRequest;
import io.archura.platform.api.http.HttpServerResponse;
import io.archura.platform.api.http.HttpStatusCode;
import io.archura.platform.api.logger.Logger;
import io.archura.platform.api.stream.LightStream;
import io.archura.platform.api.type.Configurable;
import io.archura.platform.api.type.functionalcore.HandlerFunction;
import io.archura.platform.imperativeshell.handler.model.Employee;
import io.archura.platform.imperativeshell.handler.model.Movie;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class SimpleFunction implements HandlerFunction<HttpServerResponse>, Configurable {

    private Map<String, Object> configuration;
    private final Random random = new Random();

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    @Override
    public HttpServerResponse handle(HttpServerRequest request) {
        final Context context = (Context) request.getAttributes().get(Context.class.getSimpleName());
        final Optional<Cache> optionalCache = context.getCache();
        final Optional<LightStream> optionalStream = context.getLightStream();
        final HttpClient httpClient = context.getHttpClient();
        final Logger logger = context.getLogger();
        logger.info("request = " + request + " configuration: " + configuration);

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

                                final ObjectMapper objectMapper = context.getObjectMapper();
                                final Map<String, Object> cacheData = Map.of(
                                        "k1", "value1",
                                        "k2", 2,
                                        "k3", true,
                                        "k4", new HashMap<>(),
                                        "k5", new Date());
                                final String cacheValue = objectMapper.writeValueAsString(cacheData);
                                final String cacheKey = "keyValues";
                                logger.info("cache.set, cacheKey: '%s' cacheValue: '%s'", cacheKey, cacheValue);
                                cache.set(cacheKey, cacheValue);
                                @SuppressWarnings("unchecked") final Map<String, Object> map = objectMapper.readValue(cache.get(cacheKey), Map.class);
                                logger.info("cache.get, map: '%s'", map);
                            } catch (Exception e) {
                                logger.error("Got error while doing cache operations, error: %s", e.getMessage());
                            }
                        }
                );

        try {
            new Thread(() -> System.out.println("NEW THREAD STARTED")).start();
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }

        try {
            ServerSocket ss = new ServerSocket(6666);
            Socket s = ss.accept();
            DataInputStream dis = new DataInputStream(s.getInputStream());
            String str = dis.readUTF();
            System.out.println("message= " + str);
            ss.close();
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }

        try {
            String hostname = "time.nist.gov";
            int port = 13;
            Socket socket = new Socket(hostname, port);
            InputStream input = socket.getInputStream();
            InputStreamReader reader = new InputStreamReader(input);
            int character;
            StringBuilder data = new StringBuilder();
            while ((character = reader.read()) != -1) {
                data.append((char) character);
            }
            System.out.println(data);
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }


        try {
            Class<?> c = Class.forName("io.archura.platform.securitymanager.ThreadSecurityManager");
            Constructor<?> constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object o = constructor.newInstance();
            System.out.println(">>> io.archura.platform.securitymanager.ThreadSecurityManager: " + o);
        } catch (Exception exception) {
            logger.error(exception.getCause().getMessage());
        }


        optionalStream.ifPresent(stream -> {
            final Movie movie = new Movie("Movie Title", random.nextInt(1900, 2000));
            final Map<String, String> movieMap = Map.of(
                    "title", movie.getTitle(),
                    "year", String.valueOf(movie.getYear())
            );
            final String topic = Optional.ofNullable(configuration.get("topicName"))
                    .map(String::valueOf)
                    .orElse("movies");

            logger.info("stream.send, topic: '%s', movieMap: '%s', ", topic, movieMap);
            final String key = stream.send(topic, movieMap);
            logger.info("Sent value '%s' for key '%s' to topic '%s'", movieMap, key, topic);
        });

        final String url = Optional.ofNullable(configuration.get("JSON_URL"))
                .map(String::valueOf)
                .orElse("http://localhost:9090/sono.json");
        String forwarded = Optional.ofNullable(request.getRequestHeaders().get("Forwarded"))
                .orElse(Collections.emptyList())
                .stream().findFirst()
                .orElse("");
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("Forwarded", forwarded)
                .timeout(Duration.ofMillis(100))
                .build();
        try {
            final HttpResponse<InputStream> httpResponse = httpClient
                    .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            final ObjectMapper objectMapper = new ObjectMapper();
            final InputStream responseInputStream = httpResponse.body();
            final byte[] bytes = responseInputStream.readAllBytes();
            final Employee employee = objectMapper.readValue(bytes, Employee.class);
            logger.info(employee.toString());

            return HttpServerResponse.builder()
                    .header("SIMPLE_FUNCTION_HEADER", "GOT_EMPLOYEE")
                    .bytes(bytes)
                    .build();
        } catch (Exception e) {
            return HttpServerResponse.builder()
                    .status(HttpStatusCode.HTTP_INTERNAL_ERROR)
                    .bytes(String.valueOf(e.getMessage()).getBytes(StandardCharsets.UTF_8))
                    .build();
        }
    }

}
