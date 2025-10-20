package com.sys.dbmonitor.global.config;

import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement            // @Transactional 어노테이션을 사용 가능하게 함
@MapperScan(                            // 	지정된 패키지에서 @Mapper 인터페이스를 자동으로 찾아 등록
        basePackages = "com.sys.dbmonitor.domains.*.dao",
        sqlSessionFactoryRef = "sqlSessionFactory"
)
public class MyBatisConfig {

    /**
     * SqlSessionFactory 설정
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        // Mapper XML 위치 설정
        sessionFactory.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:/mappers/*.xml")
        );

        // Type Aliases 패키지 설정
        sessionFactory.setTypeAliasesPackage("com.sys.dbmonitor.domains.*.domain");

        // MyBatis Configuration 설정
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();

        // ✅ 캐시 설정
        configuration.setCacheEnabled(false);

        // ✅ 지연 로딩 설정
        configuration.setLazyLoadingEnabled(false);
        configuration.setAggressiveLazyLoading(false);

        // ✅ 다중 ResultSet 허용
        configuration.setMultipleResultSetsEnabled(true);

        // ✅ 컬럼명 사용 설정
        configuration.setUseColumnLabel(true);

        // ✅ 자동 생성 키 사용
        configuration.setUseGeneratedKeys(true);

        // ✅ 자동 매핑 설정
        configuration.setAutoMappingBehavior(AutoMappingBehavior.PARTIAL);
        configuration.setAutoMappingUnknownColumnBehavior(org.apache.ibatis.session.AutoMappingUnknownColumnBehavior.WARNING);

        // ✅ 실행자 타입 (SIMPLE, REUSE, BATCH)
        configuration.setDefaultExecutorType(ExecutorType.SIMPLE);

        // ✅ SQL 실행 타임아웃 (초)
        configuration.setDefaultStatementTimeout(30);

        // ✅ Fetch Size
        configuration.setDefaultFetchSize(100);

        // ✅ 결과 셋 타입
        configuration.setDefaultResultSetType(org.apache.ibatis.mapping.ResultSetType.DEFAULT);

        // ✅ 로컬 캐시 범위 (SESSION, STATEMENT)
        configuration.setLocalCacheScope(LocalCacheScope.SESSION);

        // ✅ NULL 값 처리 (Oracle 필수!)
        configuration.setJdbcTypeForNull(JdbcType.NULL);

        // ✅ Snake case -> Camel case 자동 변환
        configuration.setMapUnderscoreToCamelCase(true);

        // ✅ 로그 구현체 (SLF4J, LOG4J2, STDOUT_LOGGING 등)
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);

        // ✅ 안전한 ResultHandler
        configuration.setSafeResultHandlerEnabled(true);

        // ✅ 안전한 RowBounds
        configuration.setSafeRowBoundsEnabled(false);

        // ✅ 매개변수에 실제 값 표시 (개발 환경에서만 true)
        configuration.setLogPrefix("[MyBatis] ");

        sessionFactory.setConfiguration(configuration);

        return sessionFactory.getObject();
    }

    /**
     * SqlSessionTemplate 설정 (Thread-safe)
     */
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 트랜잭션 매니저 설정
     */
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}