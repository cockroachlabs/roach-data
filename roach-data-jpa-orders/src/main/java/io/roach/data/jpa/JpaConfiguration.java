package io.roach.data.jpa;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableJpaRepositories(basePackages = {"io.roach"})
public class JpaConfiguration {
    private final Logger logger = LoggerFactory.getLogger("io.roach.SQL_TRACE");

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return loggingProxy(targetDataSource());
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource targetDataSource() {
        HikariDataSource ds = dataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.addDataSourceProperty("reWriteBatchedInserts", true);
        ds.addDataSourceProperty("ApplicationName", "jdbc-test");
        return ds;
    }

    private DataSource loggingProxy(DataSource dataSource) {
        final Formatter formatterBasic = FormatStyle.BASIC.getFormatter();
        final Formatter formatterHighlight = FormatStyle.HIGHLIGHT.getFormatter();
        // use pretty formatted query with multiline and ansi highlight enabled
        DefaultQueryLogEntryCreator creator = new DefaultQueryLogEntryCreator() {
            @Override
            protected String formatQuery(String query) {
                return formatterHighlight.format(formatterBasic.format(query));
            }
        };
        creator.setMultiline(true);

        SLF4JQueryLoggingListener listener = new SLF4JQueryLoggingListener();
        listener.setLogger(logger);
        listener.setLogLevel(SLF4JLogLevel.TRACE);
        listener.setWriteConnectionId(true);
        listener.setWriteIsolation(true);
        listener.setQueryLogEntryCreator(creator);

        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("SQL-Trace")
                .asJson()
                .listener(listener)
                .build();
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
