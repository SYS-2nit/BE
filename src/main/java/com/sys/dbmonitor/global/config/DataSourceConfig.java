package com.sys.dbmonitor.global.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Oracle 기본 값 설정하는 곳
 * JPA 설정
 */
/**
 * 다중 데이터소스 설정
 * - Oracle (Primary): 기본 데이터 저장용 (Instance, Member 등 엔티티 저장)
 * - Oracle (동적): 타겟 DB 데이터 수집용 (DynamicDataSourceFactory에서 관리)
 */
@Configuration
@Profile("!test")  // 테스트 프로파일에서는 제외
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = {
                "com.sys.dbmonitor.domains.instance.repository",
                "com.sys.dbmonitor.domains.member.repository"
        },
        entityManagerFactoryRef = "oracleEntityManagerFactory",
        transactionManagerRef = "oracleTransactionManager"
)
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String oracleUrl;

    @Value("${spring.datasource.username}")
    private String oracleUsername;

    @Value("${spring.datasource.password}")
    private String oraclePassword;

    /**
     * Oracle 데이터소스 (Primary)
     * 기본 데이터 저장용 (Instance, Member 등 엔티티 저장)
     */
    @Primary
    @Bean(name = "oracleDataSource")
    public DataSource oracleDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(oracleUrl);
        config.setUsername(oracleUsername);
        config.setPassword(oraclePassword);
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setPoolName("OracleHikariPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        return new HikariDataSource(config);
    }

    /**
     * Oracle용 EntityManagerFactory
     */
    @Primary
    @Bean(name = "oracleEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean oracleEntityManagerFactory(
            @Qualifier("oracleDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.sys.dbmonitor.domains");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.OracleDialect");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "true");
        properties.setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");

        em.setJpaProperties(properties);
        return em;
    }

    /**
     * Oracle용 TransactionManager
     */
    @Primary
    @Bean(name = "oracleTransactionManager")
    public PlatformTransactionManager oracleTransactionManager(
            @Qualifier("oracleEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}

