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

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class SimpleFunction implements Function<HttpServerRequest, HttpServerResponse>, Configurable {

    private static final Map<String, Object> staticMapOne = new HashMap<>();

    static {
        final Map<String, Object> staticMapTwo = new HashMap<>();
        staticMapTwo.put("b", "2");
    }

    private Map<String, Object> configuration;
    private final Random random = new Random();

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    @Override
    public HttpServerResponse apply(HttpServerRequest request) {
        staticMapOne.put(String.valueOf(random.nextDouble()), random.nextDouble());
        final Context context = (Context) request.getAttributes().get(Context.class.getSimpleName());
        final Optional<Cache> optionalCache = context.getCache();
        final Optional<LightStream> optionalStream = context.getLightStream();
        final Optional<Publisher> optionalPublisher = context.getPublisher();
        final HttpClient httpClient = context.getHttpClient();
        final Logger logger = context.getLogger();
        final Mapper mapper = context.getMapper();
        logger.debug("configuration: " + configuration);

        try {
            final HttpClient myClient = HttpClient.newHttpClient();
            final HttpResponse<String> httpResponse = myClient.send(HttpRequest.newBuilder().uri(URI.create("https://www.google.com/")).GET().build(), HttpResponse.BodyHandlers.ofString());
            final String body = httpResponse.body();
            logger.debug("Google response: %s", body);
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            final File myObj = new File("/tmp/filename.txt");
            if (myObj.createNewFile()) {
                logger.debug("File created: %s", myObj.getName());
            } else {
                logger.debug("File already exists.");
            }
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            final File myObj = new File("filename.txt");
            final Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                final String data = myReader.nextLine();
                logger.debug("data: %s", data);
            }
            myReader.close();
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            final File myObj = new File("filename.txt");
            if (myObj.delete()) {
                logger.debug("Deleted the file: " + myObj.getName());
            } else {
                logger.debug("Failed to delete the file.");
            }
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            new Thread(() -> logger.debug("NEW THREAD STARTED")).start();
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            System.exit(1);
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            FunctionSecurityManager securityManager = new FunctionSecurityManager();
            System.setSecurityManager(securityManager);
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            final Process process = Runtime.getRuntime().exec("ls");
            final OutputStream outputStream = process.getOutputStream();
            byte[] bytes = new byte[1024];
            outputStream.write(bytes);
            logger.debug("Output: %s", new String(bytes));
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            final Process process = Runtime.getRuntime().exec(new String[]{"ls"});
            final OutputStream outputStream = process.getOutputStream();
            byte[] bytes = new byte[1024];
            outputStream.write(bytes);
            logger.debug("Output: %s", new String(bytes));
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            final Process process = new ProcessBuilder().command("ls").start();
            final OutputStream outputStream = process.getOutputStream();
            byte[] bytes = new byte[1024];
            outputStream.write(bytes);
            logger.debug("Output: %s", new String(bytes));
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            final ServerSocket ss = new ServerSocket(6666);
            final Socket s = ss.accept();
            final DataInputStream dis = new DataInputStream(s.getInputStream());
            final String str = dis.readUTF();
            logger.debug("message= " + str);
            ss.close();
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
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
            logger.debug("data = ", data);
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            final Class<?> c = Class.forName("io.archura.platform.securitymanager.ArchuraSecurityManager");
            final Constructor<?> constructor = c.getDeclaredConstructor();
            final Object o = constructor.newInstance();
            logger.debug("Created object ArchuraSecurityManager: " + o);
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        try {
            final Class<?> c = Class.forName("io.archura.platform.internal.configuration.GlobalConfiguration");
            Field[] declaredFields = c.getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                field.set(null, new Object());
            }
            final Constructor<?> constructor = c.getDeclaredConstructor();
            constructor.setAccessible(true);
            final Object o = constructor.newInstance();
            logger.debug("Created object GlobalConfiguration: " + o);
        } catch (Exception exception) {
            logger.error(getErrorMessage(exception));
        }

        optionalCache
                .ifPresent(
                        cache -> {
                            try {
                                try {
                                    final Class<? extends Cache> aClass = cache.getClass();
                                    final Field[] declaredFields = aClass.getDeclaredFields();
                                    logger.debug("declaredFields.length = " + declaredFields.length);
                                    for (Field field : declaredFields) {
                                        field.setAccessible(true);
                                        final Object value = field.get(cache);
                                        logger.debug("field = " + field + " value: " + value);
                                    }
                                } catch (Exception exception) {
                                    logger.error(getErrorMessage(exception));
                                }

                                final Map<String, Object> cacheData = Map.of(
                                        "k1", "value1",
                                        "k2", 2,
                                        "k3", true,
                                        "k4", new HashMap<>(),
                                        "k5", new Date());
                                final String cacheValue = mapper.writeValueAsString(cacheData);
                                final String cacheKey = "keyValues";
                                logger.debug("cache.set, cacheKey: '%s' cacheValue: '%s'", cacheKey, cacheValue);
                                cache.set(cacheKey, cacheValue);
                                @SuppressWarnings("unchecked") final Map<String, Object> map = mapper.readValue(cache.get(cacheKey).getBytes(StandardCharsets.UTF_8), Map.class);
                                logger.debug("cache.get, map: '%s'", map);
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

            logger.debug("stream.send, topic: '%s', movieMap: '%s', ", topic, movieMap);
            final String key = stream.send(topic, movieMap);
            logger.debug("Sent value '%s' for key '%s' to topic '%s'", movieMap, key, topic);
        });

        optionalPublisher.ifPresent(publisher -> {
            publisher.publish("message", "Hello! this is an message.");
        });

        try {
            final String url = Optional.ofNullable(configuration.get("JSON_URL"))
                    .map(String::valueOf)
                    .orElse("http://localhost:9090/sono.json");
            logger.debug("Will get JSON from URL: %s", url);
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

            final HttpResponse<InputStream> httpResponse = httpClient
                    .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            final InputStream responseInputStream = httpResponse.body();
            final byte[] bytes = responseInputStream.readAllBytes();
            final Employee employee = mapper.readValue(bytes, Employee.class);
            logger.debug(employee.toString());

            return HttpServerResponse.builder()
                    .status(HttpStatusCode.HTTP_OK)
                    .header("SIMPLE_FUNCTION_HEADER", "GOT_EMPLOYEE")
                    .bytes(bytes)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            final String errorMessage = getErrorMessage(e);
            logger.error("Got error while running function: %s", errorMessage);
            return HttpServerResponse.builder()
                    .status(HttpStatusCode.HTTP_INTERNAL_ERROR)
                    .bytes(errorMessage.getBytes(StandardCharsets.UTF_8))
                    .build();
        }
    }

    private String getErrorMessage(final Exception exception) {
        if (isNull(exception)) {
            return "null";
        }
        if (nonNull(exception.getMessage())) {
            return exception.getMessage();
        }
        Throwable cause = exception.getCause();
        while (nonNull(cause)) {
            if (nonNull(cause.getMessage())) {
                return cause.getMessage();
            }
            cause = cause.getCause();
        }
        return "no-error-message-found";
    }

}
