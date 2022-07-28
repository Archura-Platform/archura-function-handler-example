package io.archura.platform.imperativeshell.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.archura.platform.api.cache.Cache;
import io.archura.platform.api.context.Context;
import io.archura.platform.api.logger.Logger;
import io.archura.platform.api.type.Configurable;
import io.archura.platform.imperativeshell.handler.model.Employee;
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
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class SimpleFunction implements HandlerFunction<ServerResponse>, Configurable {

    private Map<String, Object> configuration;

    @Override
    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    @Override
    public ServerResponse handle(ServerRequest request) throws IOException, InterruptedException {
        final Context context = (Context) request.attributes().get(Context.class.getSimpleName());
        final Optional<Cache> optionalCache = context.getCache();
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
