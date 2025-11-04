package com.sys.dbmonitor.domains.instance.service.command;

import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceCreateRequest;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceUpdateRequest;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceTestResponse;
import com.sys.dbmonitor.domains.member.domain.Member;
import com.sys.dbmonitor.domains.member.service.query.MemberQueryService;
import com.sys.dbmonitor.global.common.util.PasswordEncryptionUtil;
import com.sys.dbmonitor.global.config.DynamicDataSourceFactory;
import com.sys.dbmonitor.global.exception.BadRequestException;
import com.sys.dbmonitor.global.exception.ExceptionMessage;
import com.sys.dbmonitor.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstanceCommandService {

    private static final Logger log = LoggerFactory.getLogger(InstanceCommandService.class);

    private final InstanceRepository targetDatabaseRepository;
    private final DynamicDataSourceFactory dynamicDataSourceFactory;
    private final MemberQueryService memberQueryService;

    @Value("${app.encryption.key}")
    private String encryptionKey;

    /**
     * 예외에서 상세한 에러 메시지 추출
     */
    private String extractErrorMessage(Exception e) {
        // RuntimeException이고 원인이 SQLException인 경우
        if (e instanceof RuntimeException && e.getCause() != null) {
            Throwable cause = e.getCause();
            if (cause instanceof java.sql.SQLException) {
                java.sql.SQLException sqlEx = (java.sql.SQLException) cause;
                return formatSQLException(sqlEx);
            }
            if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                return cause.getMessage();
            }
        }
        
        // 직접 SQLException인 경우
        if (e instanceof java.sql.SQLException) {
            return formatSQLException((java.sql.SQLException) e);
        }
        
        // getMessage()가 있으면 사용
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            return e.getMessage();
        }
        
        // 원인 예외 확인
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isEmpty()) {
            return cause.getMessage();
        }
        
        // 기본값: 예외 클래스 이름
        return e.getClass().getSimpleName() + " 발생";
    }
    
    /**
     * SQLException을 포맷팅하여 상세 정보 반환
     */
    private String formatSQLException(java.sql.SQLException sqlEx) {
        StringBuilder msg = new StringBuilder();
        
        if (sqlEx.getErrorCode() != 0) {
            msg.append("Error Code: ").append(sqlEx.getErrorCode());
        }
        if (sqlEx.getSQLState() != null && !sqlEx.getSQLState().isEmpty()) {
            if (msg.length() > 0) msg.append(", ");
            msg.append("SQL State: ").append(sqlEx.getSQLState());
        }
        if (sqlEx.getMessage() != null && !sqlEx.getMessage().isEmpty()) {
            if (msg.length() > 0) msg.append(" - ");
            msg.append(sqlEx.getMessage());
        }
        
        // 모든 정보가 없으면 기본 메시지
        if (msg.length() == 0) {
            return "SQLException 발생";
        }
        
        return msg.toString();
    }

    /**
     * DB 연결 테스트 (저장하지 않음)
     * 
     * @param url DB 연결 URL
     * @param username 사용자명
     * @param password 비밀번호
     * @return 테스트 결과 (성공 여부, 메시지, 에러 메시지)
     */
    public InstanceTestResponse testDatabaseConnection(String url, String username, String password) {
        // 테스트용 임시 ID 생성 (음수 사용하여 실제 ID와 구분)
        Long testId = -1L;
        
        try {
            dynamicDataSourceFactory.createDataSource(
                    testId,  // 임시 테스트용 ID 사용
                    "test-connection",
                    url,
                    username,
                    password
            );
            log.info("[TargetDatabase] DB 연결 테스트 성공: url={}, username={}", url, username);
            // 테스트 후 즉시 제거
            dynamicDataSourceFactory.removeDataSource(testId);

            return new InstanceTestResponse(
                    true,
                    "DB 연결에 성공했습니다.",
                    null
            );
        } catch (Exception e) {
            // 상세한 에러 메시지 추출
            String errorMsg = extractErrorMessage(e);
            log.error("[TargetDatabase] DB 연결 테스트 실패: url={}, username={}, error={}",
                    url, username, errorMsg, e);  // 전체 예외 스택도 로깅

            return new InstanceTestResponse(
                    false,
                    "DB 연결에 실패했습니다.",
                    errorMsg
            );
        }
    }

    /**
     * 타겟 DB 등록 (비밀번호는 암호화하여 저장)
     * 테스트는 프론트에서 이미 완료된 상태로 가정
     */
    @Transactional
    public Instance createTargetDatabase(InstanceCreateRequest request, Long memberId) {
        // 이름 중복 확인
        if (targetDatabaseRepository.existsByName(request.name())) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 타겟 DB 이름입니다.");
        }

        // Member 엔티티 생성
        Member member = memberQueryService.getMemberById(memberId);

        // 엔티티 생성 (비밀번호 암호화하여 저장)
        Instance instance = request.toEntity(encryptionKey, member);

        // Oracle에 저장
        Instance saved = targetDatabaseRepository.save(instance);
        log.info("[TargetDatabase] 타겟 DB 등록 완료: id={}, name={}", saved.getId(), saved.getName());

        // 활성화된 경우 동적 데이터소스 생성 (복호화된 비밀번호 사용)
        if (saved.getIsActive()) {
            try {
                String decryptedPassword = PasswordEncryptionUtil.decrypt(saved.getPassword(), encryptionKey);
                dynamicDataSourceFactory.createDataSource(
                        saved.getId(),
                        saved.getName(),
                        saved.getUrl(),
                        saved.getUsername(),
                        decryptedPassword
                );
                log.info("[TargetDatabase] 동적 데이터소스 생성 완료: id={}, name={}", saved.getId(), saved.getName());
            } catch (Exception e) {
                log.error("[TargetDatabase] 동적 데이터소스 생성 실패: id={}, name={}, error={}",
                        saved.getId(), saved.getName(), e.getMessage());
                // 데이터소스 생성 실패해도 엔티티는 저장 (나중에 재시도 가능)
            }
        }

        return saved;
    }

    /**
     * 타겟 DB 수정
     */
    @Transactional
    public Instance updateTargetDatabase(Long instanceId, InstanceUpdateRequest request) {
        Instance targetDatabase = targetDatabaseRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));

        // 이름 중복 확인 (자신 제외)
        if (request.name() != null && !request.name().equals(targetDatabase.getName())) {
            if (targetDatabaseRepository.existsByNameAndIdNot(request.name(), instanceId)) {
                throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE, "이미 존재하는 타겟 DB 이름입니다.");
            }
        }

        // 비밀번호 암호화 처리 (변경된 경우에만 암호화하여 저장)
        String encryptedPassword = request.password() != null
                ? PasswordEncryptionUtil.encrypt(request.password(), encryptionKey)  // AES 암호화로 저장
                : null;

        // 엔티티 업데이트
        targetDatabase.update(
                request.name(),
                request.url(),
                request.username(),
                encryptedPassword != null ? encryptedPassword : targetDatabase.getPassword(),  // 변경된 경우만 새 비밀번호, 아니면 기존 비밀번호 유지
                request.isActive()
        );


        Instance saved = targetDatabaseRepository.save(targetDatabase);
        log.info("[TargetDatabase] 타겟 DB 수정 완료: id={}, name={}", saved.getId(), saved.getName());

        // 기존 데이터소스 제거
        dynamicDataSourceFactory.removeDataSource(instanceId);

        // 활성화된 경우 새로운 데이터소스 생성 (복호화된 비밀번호 사용)
        if (saved.getIsActive()) {
            try {
                String decryptedPassword = PasswordEncryptionUtil.decrypt(saved.getPassword(), encryptionKey);
                dynamicDataSourceFactory.createDataSource(
                        saved.getId(),
                        saved.getName(),
                        saved.getUrl(),
                        saved.getUsername(),
                        decryptedPassword  // 복호화된 비밀번호 사용
                );
                log.info("[TargetDatabase] 동적 데이터소스 재생성 완료: id={}, name={}", saved.getId(), saved.getName());
            } catch (Exception e) {
                log.error("[TargetDatabase] 동적 데이터소스 재생성 실패: id={}, name={}, error={}",
                        saved.getId(), saved.getName(), e.getMessage());
            }
        }

        return saved;
    }

    /**
     * 타겟 DB 삭제
     */
    @Transactional
    public void deleteTargetDatabase(Long id) {
        Instance targetDatabase = targetDatabaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));

        // 동적 데이터소스 제거
        dynamicDataSourceFactory.removeDataSource(id);

        // 엔티티 삭제
        targetDatabaseRepository.delete(targetDatabase);
        log.info("[TargetDatabase] 타겟 DB 삭제 완료: id={}, name={}", id, targetDatabase.getName());
    }

    /**
     * 타겟 DB 활성화 (암호화된 비밀번호 복호화하여 자동 연결)
     */
    @Transactional
    public Instance activateTargetDatabase(Long id) {
        Instance targetDatabase = targetDatabaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));

        targetDatabase.activate();
        Instance saved = targetDatabaseRepository.save(targetDatabase);

        // 암호화된 비밀번호 복호화하여 자동 연결
        try {
            String storedPassword = saved.getPassword();

            // BCrypt 해시인 경우 복호화 불가능
            if (PasswordEncryptionUtil.isBcryptHash(storedPassword)) {
                log.warn("[TargetDatabase] 타겟 DB 비밀번호가 BCrypt 해시 형식입니다. " +
                        "자동 연결을 위해 비밀번호를 AES 암호화로 변경해야 합니다. " +
                        "id={}, name={}. " +
                        "연결 API(/api/databases/{id}/connect)를 통해 비밀번호를 재입력하세요.",
                        saved.getId(), saved.getName());
                return saved;
            }

            String decryptedPassword = PasswordEncryptionUtil.decrypt(storedPassword, encryptionKey);
            dynamicDataSourceFactory.createDataSource(
                    saved.getId(),
                    saved.getName(),
                    saved.getUrl(),
                    saved.getUsername(),
                    decryptedPassword
            );
            log.info("[TargetDatabase] 타겟 DB 활성화 및 자동 연결 완료: id={}, name={}",
                    saved.getId(), saved.getName());
        } catch (IllegalArgumentException e) {
            // BCrypt 해시 에러
            log.warn("[TargetDatabase] 타겟 DB 활성화 후 자동 연결 스킵: BCrypt 해시 형식. " +
                    "id={}, name={}. 연결 API를 통해 비밀번호를 재입력하세요.",
                    saved.getId(), saved.getName());
        } catch (Exception e) {
            log.error("[TargetDatabase] 타겟 DB 활성화 후 자동 연결 실패: id={}, name={}, error={}",
                    saved.getId(), saved.getName(), e.getMessage());
            // 연결 실패해도 활성화 상태는 유지 (나중에 재시도 가능)
        }

        return saved;
    }

    /**
     * 타겟 DB 연결 (비밀번호 재입력하여 데이터소스 생성)
     * BCrypt 해시인 경우 비밀번호를 AES 암호화로 마이그레이션
     */
    @Transactional
    public Instance connectTargetDatabase(Long id, String password) {
        Instance targetDatabase = targetDatabaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));

        String storedPassword = targetDatabase.getPassword();
        boolean isBcryptHash = PasswordEncryptionUtil.isBcryptHash(storedPassword);

        // BCrypt 해시가 아닌 경우에만 비밀번호 검증
        if (!isBcryptHash) {
            try {
                String decryptedPassword = PasswordEncryptionUtil.decrypt(storedPassword, encryptionKey);
                if (!password.equals(decryptedPassword)) {
                    throw new BadRequestException(ExceptionMessage.INVALID_REQUEST,
                            "비밀번호가 일치하지 않습니다.");
                }
            } catch (IllegalArgumentException e) {
                // 복호화 불가능한 경우 (BCrypt가 아닌 다른 형식)
                log.warn("[TargetDatabase] 비밀번호 검증 스킵: 복호화 불가능한 형식. id={}, name={}",
                        targetDatabase.getId(), targetDatabase.getName());
            }
        } else {
            log.info("[TargetDatabase] BCrypt 해시 형식 감지. 비밀번호를 AES 암호화로 마이그레이션합니다. id={}, name={}",
                    targetDatabase.getId(), targetDatabase.getName());
        }

        // 기존 데이터소스 제거
        dynamicDataSourceFactory.removeDataSource(id);

        // 데이터소스 생성 (입력한 비밀번호 사용)
        try {
            dynamicDataSourceFactory.createDataSource(
                    targetDatabase.getId(),
                    targetDatabase.getName(),
                    targetDatabase.getUrl(),
                    targetDatabase.getUsername(),
                    password  // 입력한 원본 비밀번호 사용
            );
            log.info("[TargetDatabase] 타겟 DB 연결 및 데이터소스 생성 완료: id={}, name={}",
                    targetDatabase.getId(), targetDatabase.getName());
        } catch (Exception e) {
            log.error("[TargetDatabase] 타겟 DB 연결 실패: id={}, name={}, error={}",
                    targetDatabase.getId(), targetDatabase.getName(), e.getMessage());
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST,
                    "DB 연결에 실패했습니다: " + e.getMessage());
        }

        // 비밀번호를 AES 암호화로 저장 (BCrypt 해시 마이그레이션)
        String encryptedPassword = PasswordEncryptionUtil.encrypt(password, encryptionKey);
        targetDatabase.update(
                null,  // name
                null,  // url
                null,  // username
                encryptedPassword,  // 비밀번호를 AES 암호화로 저장
                true   // 활성화
        );

        Instance saved = targetDatabaseRepository.save(targetDatabase);
        log.info("[TargetDatabase] 타겟 DB 비밀번호를 AES 암호화로 저장 완료: id={}, name={}",
                saved.getId(), saved.getName());

        return saved;
    }

    /**
     * 타겟 DB 비활성화
     */
    @Transactional
    public Instance deactivateTargetDatabase(Long id) {
        Instance targetDatabase = targetDatabaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND, "타겟 DB를 찾을 수 없습니다."));

        targetDatabase.deactivate();
        Instance saved = targetDatabaseRepository.save(targetDatabase);

        // 동적 데이터소스 제거
        dynamicDataSourceFactory.removeDataSource(id);
        log.info("[TargetDatabase] 타겟 DB 비활성화 및 데이터소스 제거 완료: id={}, name={}", saved.getId(), saved.getName());

        return saved;
    }
}

