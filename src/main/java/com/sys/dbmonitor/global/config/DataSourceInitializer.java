package com.sys.dbmonitor.global.config;

import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.global.common.util.PasswordEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 타겟 Oracle DB 동적 리소스 관리
 * 애플리케이션 시작 시 활성화된 타겟 DB의 동적 데이터소스를 초기화합니다.
 * 암호화된 비밀번호를 복호화하여 자동으로 연결합니다.
 */
@Component
public class DataSourceInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataSourceInitializer.class);

    private final InstanceRepository targetDatabaseRepository;
    private final DynamicDataSourceFactory dynamicDataSourceFactory;

    @Value("${app.encryption.key}")
    private String encryptionKey;

    public DataSourceInitializer(
            InstanceRepository targetDatabaseRepository,
            DynamicDataSourceFactory dynamicDataSourceFactory) {
        this.targetDatabaseRepository = targetDatabaseRepository;
        this.dynamicDataSourceFactory = dynamicDataSourceFactory;
    }

    /**
     * 애플리케이션 시작 완료 후 활성화된 타겟 DB 데이터소스 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initializeDataSources() {
        log.info("[DataSourceInitializer] 타겟 DB 데이터소스 초기화 시작");

        // Oracle에서 활성화된 타겟 DB 목록 조회
        List<Instance> activeTargetDatabases = targetDatabaseRepository.findByIsActiveTrue();

        if (activeTargetDatabases.isEmpty()) {
            log.info("[DataSourceInitializer] 활성화된 타겟 DB가 없습니다.");
            return;
        }

        log.info("[DataSourceInitializer] 활성화된 타겟 DB 개수: {}", activeTargetDatabases.size());

        // 각 타겟 DB에 대해 데이터소스 생성
        for (Instance instance : activeTargetDatabases) {
            try {
                String storedPassword = instance.getPassword();
                
                // BCrypt 해시인 경우 복호화 불가능
                if (PasswordEncryptionUtil.isBcryptHash(storedPassword)) {
                    log.warn("[DataSourceInitializer] 타겟 DB 비밀번호가 BCrypt 해시 형식입니다. " +
                            "AES 암호화 형식으로 변경하기 위해 비밀번호를 재입력해야 합니다. " +
                            "id={}, name={}, url={}. " +
                            "연결 API(/api/databases/{id}/connect)를 통해 비밀번호를 재입력하세요.",
                            instance.getId(), instance.getName(), instance.getUrl());
                    continue;  // 이 인스턴스는 스킵하고 다음으로
                }

                // 암호화된 비밀번호 복호화 (AES 암호화 형식만)
                String decryptedPassword = PasswordEncryptionUtil.decrypt(storedPassword, encryptionKey);

                // 동적 데이터소스 생성
                dynamicDataSourceFactory.createDataSource(
                        instance.getId(),
                        instance.getName(),
                        instance.getUrl(),
                        instance.getUsername(),
                        decryptedPassword
                );

                log.info("[DataSourceInitializer] 타겟 DB 자동 연결 완료: id={}, name={}, url={}",
                        instance.getId(), instance.getName(), instance.getUrl());
            } catch (IllegalArgumentException e) {
                // BCrypt 해시 에러는 이미 처리됨
                log.warn("[DataSourceInitializer] 타겟 DB 자동 연결 스킵: id={}, name={}, url={}, reason={}",
                        instance.getId(), instance.getName(), instance.getUrl(), e.getMessage());
            } catch (Exception e) {
                log.error("[DataSourceInitializer] 타겟 DB 자동 연결 실패: id={}, name={}, url={}, error={}",
                        instance.getId(), instance.getName(), instance.getUrl(), e.getMessage(), e);
                // 연결 실패해도 애플리케이션 시작은 계속 진행
            }
        }

        log.info("[DataSourceInitializer] 타겟 DB 데이터소스 초기화 완료");
    }
}

