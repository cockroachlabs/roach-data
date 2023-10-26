package io.roach.data.reactive;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import io.r2dbc.spi.ConnectionFactory;

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement
@SpringBootApplication
@EnableR2dbcRepositories
public class ReactiveApplication implements CommandLineRunner {
    protected static final Logger logger = LoggerFactory.getLogger(ReactiveApplication.class);

    public static void main(String[] args) {
        new SpringApplicationBuilder(ReactiveApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(args);
    }

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        initializer.setDatabasePopulator(new ResourceDatabasePopulator(
                new ClassPathResource("db/create.sql")));
        return initializer;
    }

    @Override
    public void run(String... args) {
        logger.info("Lets move some $$ around!");

        int concurrency = 10;

        LinkedList<String> stack = new LinkedList<>(Arrays.asList(args));
        while (!stack.isEmpty()) {
            String arg = stack.pop();
            if (arg.equals("--concurrency")) {
                concurrency = Integer.parseInt(stack.pop());
            }
        }

        final Link transferLink = Link.of("http://localhost:9090/transfer{?fromId,toId,amount}");
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(concurrency);
        final Deque<CompletableFuture<Integer>> futures = new ArrayDeque<>();

        IntStream.rangeClosed(1, concurrency).forEach(value -> {
            CompletableFuture<Integer> f = CompletableFuture.supplyAsync(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();

                RestTemplate template = new RestTemplate();

                int errors = 0;

                for (int j = 0; j < 100; j++) {
                    int fromId = 1 + random.nextInt(10000);
                    int toId = fromId % 10000 + 1;

                    BigDecimal amount = new BigDecimal("10.00");

                    Map<String, Object> form = new HashMap<>();
                    form.put("fromId", fromId);
                    form.put("toId", toId);
                    form.put("amount", amount);

                    try {
                        String uri = transferLink.expand(form).getHref();
                        logger.debug("({}) Transfer {} from {} to {}", uri, amount, fromId, toId);
                        template.postForEntity(uri, null, String.class);
                    } catch (HttpStatusCodeException e) {
                        logger.warn(e.getResponseBodyAsString());
                        errors++;
                    }
                }

                return errors;
            });

            futures.add(f);
        });

        int totalErrors = 0;

        while (!futures.isEmpty()) {
            try {
                int errors = futures.pop().get();
                totalErrors += errors;
                logger.info("Worker finished with {} errors - {} remaining", errors, futures.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                logger.warn("Worker failed", e.getCause());
            }
        }

        logger.info("All client workers finished with {} errors and server keeps running. Have a nice day!",
                totalErrors);

        executorService.shutdownNow();
    }
}

