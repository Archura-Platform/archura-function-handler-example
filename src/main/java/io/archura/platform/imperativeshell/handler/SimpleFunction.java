package io.archura.platform.imperativeshell.handler;

import io.archura.platform.api.cache.Cache;
import io.archura.platform.api.context.Context;
import io.archura.platform.api.http.HttpServerRequest;
import io.archura.platform.api.http.HttpServerResponse;
import io.archura.platform.api.http.HttpStatusCode;
import io.archura.platform.api.logger.Logger;
import io.archura.platform.api.mapper.Mapper;
import io.archura.platform.api.publish.Publisher;
import io.archura.platform.api.stream.LightStream;
import io.archura.platform.api.type.Configurable;
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
import java.util.function.Function;

public class SimpleFunction implements Function<HttpServerRequest, HttpServerResponse>, Configurable {

    private Map<String, Object> configuration;
    private final Random random = new Random();

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    @Override
    public HttpServerResponse apply(HttpServerRequest request) {
        final Context context = (Context) request.getAttributes().get(Context.class.getSimpleName());
        final Optional<Cache> optionalCache = context.getCache();
        final Optional<LightStream> optionalStream = context.getLightStream();
        final Optional<Publisher> optionalPublisher = context.getPublisher();
        final HttpClient httpClient = context.getHttpClient();
        final Logger logger = context.getLogger();
        final Mapper mapper = context.getMapper();
        logger.info(" configuration: " + configuration);

        try {
            new Thread(() -> logger.info("NEW THREAD STARTED")).start();
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }

        try {
            final ServerSocket ss = new ServerSocket(6666);
            final Socket s = ss.accept();
            final DataInputStream dis = new DataInputStream(s.getInputStream());
            final String str = dis.readUTF();
            logger.info("message= " + str);
            ss.close();
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }

        try {
            String hostname = "time.nist.gov";
            int port = 13;
            final Socket socket = new Socket(hostname, port);
            final InputStream input = socket.getInputStream();
            final InputStreamReader reader = new InputStreamReader(input);
            int character;
            final StringBuilder data = new StringBuilder();
            while ((character = reader.read()) != -1) {
                data.append((char) character);
            }
            logger.info("data = ", data);
        } catch (Exception exception) {
            logger.error(exception.getMessage());
        }


        try {
            final Class<?> c = Class.forName("io.archura.platform.securitymanager.ThreadSecurityManager");
            final Constructor<?> constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);
            final Object o = constructor.newInstance();
            logger.info(">>> io.archura.platform.securitymanager.ThreadSecurityManager: " + o);
        } catch (Exception exception) {
            logger.error(exception.getCause().getMessage());
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

                                final Map<String, Object> cacheData = Map.of(
                                        "k1", "value1",
                                        "k2", 2,
                                        "k3", true,
                                        "k4", new HashMap<>(),
                                        "k5", new Date());
                                final String cacheValue = mapper.writeValueAsString(cacheData);
                                final String cacheKey = "keyValues";
                                logger.info("cache.set, cacheKey: '%s' cacheValue: '%s'", cacheKey, cacheValue);
                                cache.set(cacheKey, cacheValue);
                                @SuppressWarnings("unchecked") final Map<String, Object> map = mapper.readValue(cache.get(cacheKey).getBytes(StandardCharsets.UTF_8), Map.class);
                                logger.info("cache.get, map: '%s'", map);
                            } catch (Exception e) {
                                logger.error("Got error while doing cache operations, error: %s", e.getMessage());
                            }
                        }
                );

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

        optionalPublisher.ifPresent(publisher -> {
            publisher.publish("message", "Hello! this is an message.");
        });

        final String url = Optional.ofNullable(configuration.get("JSON_URL"))
                .map(String::valueOf)
                .orElse("http://localhost:9090/sono.json");
        final String forwarded = Optional.ofNullable(request.getRequestHeaders().get("Forwarded"))
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
            final InputStream responseInputStream = httpResponse.body();
            final byte[] bytes = responseInputStream.readAllBytes();
            final Employee employee = mapper.readValue(bytes, Employee.class);
            logger.info(employee.toString());

            return HttpServerResponse.builder()
                    .status(HttpStatusCode.HTTP_OK)
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
