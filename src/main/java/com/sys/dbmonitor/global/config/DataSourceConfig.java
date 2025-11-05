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
     * 데이터소스 (Primary)
     * 기본 데이터 저장용 (Instance, Member 등 엔티티 저장)
     * JDBC URL에 따라 드라이버를 자동으로 선택
     */
    @Primary
    @Bean(name = "oracleDataSource")
    public DataSource oracleDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(oracleUrl);
        config.setUsername(oracleUsername);
        config.setPassword(oraclePassword);
        
        // JDBC URL에 따라 드라이버 클래스 자동 선택
        String driverClassName = determineDriverClassName(oracleUrl);
        config.setDriverClassName(driverClassName);
        
        String poolName = oracleUrl.contains("postgresql") ? "PostgreSQLHikariPool" : "OracleHikariPool";
        config.setPoolName(poolName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        return new HikariDataSource(config);
    }
    
    /**
     * JDBC URL에 따라 드라이버 클래스명 결정
     */
    private String determineDriverClassName(String jdbcUrl) {
        if (jdbcUrl == null) {
            throw new IllegalArgumentException("JDBC URL이 null입니다.");
        }
        
        if (jdbcUrl.startsWith("jdbc:oracle:")) {
            return "oracle.jdbc.OracleDriver";
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        } else if (jdbcUrl.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        } else {
            throw new IllegalArgumentException("지원하지 않는 JDBC URL입니다: " + jdbcUrl);
        }
    }
    
    /**
     * JDBC URL에 따라 Hibernate Dialect 결정
     */
    private String determineHibernateDialect(String jdbcUrl) {
        if (jdbcUrl == null) {
            throw new IllegalArgumentException("JDBC URL이 null입니다.");
        }
        
        if (jdbcUrl.startsWith("jdbc:oracle:")) {
            return "org.hibernate.dialect.OracleDialect";
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "org.hibernate.dialect.PostgreSQLDialect";
        } else if (jdbcUrl.startsWith("jdbc:mysql:")) {
            return "org.hibernate.dialect.MySQLDialect";
        } else {
            throw new IllegalArgumentException("지원하지 않는 JDBC URL입니다: " + jdbcUrl);
        }
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
        
        // JDBC URL에 따라 Hibernate Dialect 자동 선택
        String dialect = determineHibernateDialect(oracleUrl);
        properties.setProperty("hibernate.dialect", dialect);
        
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

