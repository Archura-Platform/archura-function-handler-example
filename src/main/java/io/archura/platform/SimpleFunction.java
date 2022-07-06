package io.archura.platform;

import io.archura.platform.model.Employee;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class SimpleFunction implements HandlerFunction<ServerResponse> {

    @Override
    public Mono<ServerResponse> handle(ServerRequest request) {
        request.attribute(Cache.class.getSimpleName())
                .map(Cache.class::cast)
                .ifPresent(cache -> {

                    try {
                        final Class<? extends Cache> aClass = cache.getClass();
                        final Field[] fields = aClass.getFields();
                        final Field[] declaredFields = aClass.getDeclaredFields();
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
                    final Mono<Boolean> put = cache.put(keyValues, cacheData);
                    final Mono<Map<String, Object>> get = cache.get(keyValues)
                            .doOnNext(map -> System.out.println("map = " + map));
                    put.and(get).subscribe();
                });
        final Mono<Employee> response = WebClient.create()
                .method(HttpMethod.GET)
                .uri("https://mocki.io/v1/0b14838c-6b95-4072-b38f-d5085d69fd72")
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Employee.class))
                .timeout(Duration.ofSeconds(5))
                .retry(2);
        return ServerResponse.ok().body(response, Employee.class);
    }

}
