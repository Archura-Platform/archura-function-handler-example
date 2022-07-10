package io.archura.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.archura.platform.function.Configurable;
import io.archura.platform.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class SimpleFunction implements HandlerFunction<ServerResponse>, Configurable {

    private Map<String, Object> configuration;

    @Override
    public void setConfiguration(Map<String, Object> config) {
        this.configuration = config;
    }

    @Override
    public ServerResponse handle(ServerRequest request) throws IOException, InterruptedException {
        System.out.println("SimpleFunction request = " + request + " configuration: " + configuration);

        request.attribute(Cache.class.getSimpleName())
                .map(Cache.class::cast)
                .ifPresent(cache -> {
                    try {
                        final Class<? extends Cache> aClass = cache.getClass();
                        final Field[] declaredFields = aClass.getDeclaredFields();
                        System.out.println("declaredFields.length = " + declaredFields.length);
                        for (Field field : declaredFields) {
                            field.setAccessible(true);
                            final Object value = field.get(cache);
                            System.out.println("field = " + field + " value: " + value);
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
                    System.out.println("map = " + map);
                });

        final HttpClient httpClient = HttpClient.newHttpClient();
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://mocki.io/v1/0b14838c-6b95-4072-b38f-d5085d69fd72"))
                .build();
        final HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        final ObjectMapper objectMapper = new ObjectMapper();
        final Employee employee = objectMapper.readValue(httpResponse.body(), Employee.class);

        return ServerResponse.ok()
                .header("SIMPLE_FUNCTION_HEADER", "GOT_EMPLOYEE")
                .body(employee);
    }

}
