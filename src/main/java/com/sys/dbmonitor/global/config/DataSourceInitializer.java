/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.global.config;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.DBInfoRepository;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.global.common.util.PasswordEncryptionUtil;
import lombok.RequiredArgsConstructor;
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
 */
@Component
@RequiredArgsConstructor
public class DataSourceInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataSourceInitializer.class);

    private final DBInfoRepository dbInfoRepository;
    private final InstanceRepository instanceRepository;
    private final DynamicDataSourceFactory dynamicDataSourceFactory;

    @Value("${app.encryption.key}")
    private String encryptionKey;


    /**
     * 애플리케이션 시작 완료 후 활성화된 타겟 DB 데이터소스 초기화
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initializeDataSources() {
        log.info("[DataSourceInitializer] 타겟 DB 데이터소스 초기화 시작");

        // 활성화된 DBInfo 목록 조회
        List<DBInfo> activeDbInfos = dbInfoRepository.findByIsActiveTrueAndIsDeletedFalse();

        if (activeDbInfos.isEmpty()) {
            log.info("[DataSourceInitializer] 활성화된 타겟 DB가 없습니다.");
            return;
        }

        log.info("[DataSourceInitializer] 활성화된 DBInfo 개수: {}", activeDbInfos.size());

        // 각 활성화된 DBInfo에 대해 연결된 Instance들을 찾아 데이터소스 생성
        for (DBInfo dbInfo : activeDbInfos) {
            // 해당 DBInfo에 연결된 모든 Instance 조회
            List<Instance> instances = instanceRepository.findByDbInfoAndIsDeletedFalse(dbInfo);

            if (instances.isEmpty()) {
                log.warn("[DataSourceInitializer] DBInfo(id={}, name={})에 연결된 Instance가 없습니다.", 
                        dbInfo.getId(), dbInfo.getName());
                continue;
            }

            // 각 Instance에 대해 데이터소스 생성
            for (Instance instance : instances) {
                try {
                    // Instance에서 URL 가져오기
                    String instanceUrl = instance.getUrl();
                    if (instanceUrl == null || instanceUrl.isEmpty()) {
                        log.warn("[DataSourceInitializer] Instance(id={})에 URL이 없습니다. 스킵합니다.", 
                                instance.getId());
                        continue;
                    }

                    // DBInfo에서 비밀번호 가져오기
                    String storedPassword = dbInfo.getPassword();
                    
                    // BCrypt 해시인 경우 복호화 불가능
                    if (PasswordEncryptionUtil.isBcryptHash(storedPassword)) {
                        log.warn("[DataSourceInitializer] 타겟 DB 비밀번호가 BCrypt 해시 형식입니다. " +
                                "AES 암호화 형식으로 변경하기 위해 비밀번호를 재입력해야 합니다. " +
                                "instanceId={}, dbInfoId={}, name={}, url={}. " +
                                "연결 API(/api/databases/{id}/connect)를 통해 비밀번호를 재입력하세요.",
                                instance.getId(), dbInfo.getId(), dbInfo.getName(), instanceUrl);
                        continue;  // 이 인스턴스는 스킵하고 다음으로
                    }

                    // 암호화된 비밀번호 복호화 (AES 암호화 형식만)
                    String decryptedPassword = PasswordEncryptionUtil.decrypt(storedPassword, encryptionKey);

                    // 동적 데이터소스 생성
                    // URL은 Instance에서 가져오고, 나머지는 DBInfo에서 가져옴
                    dynamicDataSourceFactory.createDataSource(
                            instance.getId(),
                            dbInfo.getName(),
                            instanceUrl,  // Instance의 URL 사용
                            dbInfo.getUserName(),
                            decryptedPassword
                    );

                    log.info("[DataSourceInitializer] 타겟 DB 자동 연결 완료: instanceId={}, dbInfoId={}, name={}, url={}",
                            instance.getId(), dbInfo.getId(), dbInfo.getName(), instanceUrl);
                } catch (IllegalArgumentException e) {
                    // BCrypt 해시 에러는 이미 처리됨
                    log.warn("[DataSourceInitializer] 타겟 DB 자동 연결 스킵: instanceId={}, dbInfoId={}, name={}, url={}, reason={}",
                            instance.getId(), dbInfo.getId(), dbInfo.getName(), instance.getUrl(), e.getMessage());
                } catch (Exception e) {
                    log.error("[DataSourceInitializer] 타겟 DB 자동 연결 실패: instanceId={}, dbInfoId={}, name={}, url={}, error={}",
                            instance.getId(), dbInfo.getId(), dbInfo.getName(), instance.getUrl(), e.getMessage(), e);
                    // 연결 실패해도 애플리케이션 시작은 계속 진행
                }
            }
        }

        log.info("[DataSourceInitializer] 타겟 DB 데이터소스 초기화 완료");
    }
}

