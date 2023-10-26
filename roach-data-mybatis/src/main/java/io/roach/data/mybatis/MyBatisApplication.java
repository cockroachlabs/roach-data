package io.roach.data.mybatis;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.MyBatisJdbcConfiguration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@EnableAutoConfiguration
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableJdbcRepositories
@EnableSpringDataWebSupport
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE - 1)
@Configuration
@ComponentScan
@Import(MyBatisJdbcConfiguration.class)
public class MyBatisApplication implements CommandLineRunner {
    protected static final Logger logger = LoggerFactory.getLogger(MyBatisApplication.class);

    @Autowired
    private DataSource dataSource;

    public static void main(String[] args) {
        new SpringApplicationBuilder(MyBatisApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(args);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Lets move some $$ around!");

        final Link transferLink = Link.of("http://localhost:9090/transfer{?fromId,toId,amount}");

        final int concurrency = args.length > 0 ? Integer.parseInt(args[0]) : 1;

        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(concurrency);

        Deque<Future<Integer>> futures = new ArrayDeque<>();

        for (int i = 0; i < concurrency; i++) {
            Future<Integer> future = executorService.submit(() -> {
                RestTemplate template = new RestTemplate();
                int errors = 0;
                for (int j = 0; j < 100; j++) {
                    int fromId = j % 4 + 1;
                    int toId = fromId % 4 + 1;

                    BigDecimal amount = new BigDecimal("10.00");

                    Map<String, Object> form = new HashMap<>();
                    form.put("fromId", fromId);
                    form.put("toId", toId);
                    form.put("amount", amount);

                    String uri = transferLink.expand(form).getHref();

                    logger.debug("({}) Transfer {} from {} to {}", uri, amount, fromId, toId);

                    try {
                        template.postForEntity(uri, null, String.class);
                    } catch (HttpStatusCodeException e) {
                        logger.warn(e.toString());
                        errors++;
                    }
                }
                return errors;
            });
            futures.add(future);
        }

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

