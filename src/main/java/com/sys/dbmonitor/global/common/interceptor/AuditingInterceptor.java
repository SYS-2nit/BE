package com.sys.dbmonitor.global.common.interceptor;


import com.sys.dbmonitor.global.common.entity.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.util.Properties;


@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AuditingInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];

        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        if (parameter instanceof BaseEntity) {
            BaseEntity entity = (BaseEntity) parameter;

            switch (sqlCommandType) {
                case INSERT:
                    entity.onCreate();
                    log.debug("Auditing: INSERT - Entity created at {}", entity.getCreatedAt());
                    break;
                case UPDATE:
                    entity.onUpdate();
                    log.debug("Auditing: UPDATE - Entity updated at {}", entity.getUpdatedAt());
                    break;
                case DELETE:
                    entity.markAsDeleted();
                    log.debug("Auditing: DELETE - Entity marked as deleted");
                    break;
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 필요시 설정
    }
}