package com.sys.dbmonitor.domains.instance.service.command;

import com.sys.dbmonitor.domains.instance.domain.DBInfo;
import com.sys.dbmonitor.domains.instance.domain.Instance;
import com.sys.dbmonitor.domains.instance.repository.DBInfoRepository;
import com.sys.dbmonitor.domains.instance.repository.InstanceRepository;
import com.sys.dbmonitor.domains.instance.dto.request.DatabaseCreateRequest;
import com.sys.dbmonitor.domains.instance.dto.request.DatabaseInstanceCreateRequest;
import com.sys.dbmonitor.domains.instance.dto.request.DatabaseTestRequest;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceCreateRequest;
import com.sys.dbmonitor.domains.instance.dto.request.InstanceUpdateRequest;
import com.sys.dbmonitor.domains.instance.dto.response.InstanceListResponse;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstanceCommandService {

    private static final Logger log = LoggerFactory.getLogger(InstanceCommandService.class);

    private final InstanceRepository targetDatabaseRepository;
    private final DBInfoRepository dbInfoRepository;
    private final DynamicDataSourceFactory dynamicDataSourceFactory;
    private final MemberQueryService memberQueryService;

    @Value("${app.encryption.key}")
    private String encryptionKey;

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
     * DB 연결 테스트
     */
    public InstanceTestResponse testDatabaseConnection(DatabaseTestRequest request) {
        try {
            // JDBC URL 생성
            String jdbcUrl = request.generateJdbcUrl();
            
            // 테스트용 임시 ID 사용
            Long testInstanceId = -1L;
            
            dynamicDataSourceFactory.createDataSource(
                    testInstanceId,
                    "test-connection",
                    jdbcUrl,
                    request.account(),
                    request.password()
            );
            log.info("[Database] DB 연결 테스트 성공: ip={}, port={}, sid={}",
                    request.ip(), request.port(), request.sid());
            
            // 테스트 후 즉시 제거
            dynamicDataSourceFactory.removeDataSource(testInstanceId);

            // 성공 시에만 Response 반환
            return new InstanceTestResponse(
                    true,
                    "DB 연결에 성공했습니다.",
                    null
            );
        } catch (IllegalArgumentException e) {
            // 잘못된 타입이나 URL 형식
            log.error("[Database] DB 연결 테스트 실패: 잘못된 요청 -  ip={}, port={}, sid={}, error={}",
                     request.ip(), request.port(), request.sid(), e.getMessage());
            throw new BadRequestException(ExceptionMessage.DB_INVALID_URL, 
                    "데이터베이스 연결 URL 생성 실패: " + e.getMessage());
        } catch (Exception e) {
            // SQL 관련 에러 또는 기타 에러 처리
            String errorMsg = extractErrorMessage(e);
            log.error("[Database] DB 연결 테스트 실패:  ip={}, port={}, sid={}, error={}",
                    request.ip(), request.port(), request.sid(), errorMsg, e);
            
            // SQLException인지 확인 (cause를 통해)
            Throwable cause = e.getCause();
            if (cause instanceof java.sql.SQLException) {
                java.sql.SQLException sqlEx = (java.sql.SQLException) cause;
                // 인증 관련 에러인지 확인
                if (sqlEx.getErrorCode() == 1017 || sqlEx.getMessage().contains("invalid username/password") 
                        || sqlEx.getMessage().contains("ORA-01017")) {
                    throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS, 
                            "데이터베이스 인증 정보가 올바르지 않습니다: " + errorMsg);
                }
            }
            
            // 직접 SQLException인 경우
            if (e instanceof java.sql.SQLException) {
                java.sql.SQLException sqlEx = (java.sql.SQLException) e;
                if (sqlEx.getErrorCode() == 1017 || sqlEx.getMessage().contains("invalid username/password") 
                        || sqlEx.getMessage().contains("ORA-01017")) {
                    throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS, 
                            "데이터베이스 인증 정보가 올바르지 않습니다: " + errorMsg);
                }
            }
            
            throw new BadRequestException(ExceptionMessage.DB_CONNECTION_TEST_FAILED, 
                    "데이터베이스 연결 테스트 실패: " + errorMsg);
        }
    }


    /**
     * 데이터베이스 생성 (DBInfo와 Instance 함께 저장)
     */
    @Transactional
    public Instance createDatabase(DatabaseCreateRequest request, Long memberId) {
        // 이름 중복 확인
        if (dbInfoRepository.existsByName(request.name())) {
            throw new BadRequestException(ExceptionMessage.DB_NAME_ALREADY_EXISTS, 
                    "이미 존재하는 데이터베이스 이름입니다: " + request.name());
        }

        // Member 엔티티 조회
        Member member = memberQueryService.getMemberById(memberId);

        // 비밀번호 암호화
        String encryptedPassword;
        try {
            encryptedPassword = PasswordEncryptionUtil.encrypt(request.password(), encryptionKey);
        } catch (Exception e) {
            log.error("[Database] 비밀번호 암호화 실패: error={}", e.getMessage());
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, 
                    "비밀번호 암호화 중 오류가 발생했습니다.");
        }

        // DBInfo 엔티티 생성
        DBInfo dbInfo = DBInfo.builder()
                .member(member)
                .name(request.name())
                .ip(request.ip())
                .port(request.port())
                .userName(request.account())
                .password(encryptedPassword)
                .isActive(true)
                .finalAt(null)
                .build();

        // DBInfo 저장
        DBInfo savedDbInfo;
        try {
            savedDbInfo = dbInfoRepository.save(dbInfo);
            log.info("[Database] DBInfo 저장 완료: id={}, name={}", savedDbInfo.getId(), savedDbInfo.getName());
        } catch (Exception e) {
            log.error("[Database] DBInfo 저장 실패: name={}, error={}", request.name(), e.getMessage());
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, 
                    "데이터베이스 정보 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        // JDBC URL 생성 (IP, Port, SID를 조합하여 자동 생성)
        String jdbcUrl = savedDbInfo.generateJdbcUrlForInstance(request.sid());

        // Instance 엔티티 생성
        Instance instance = Instance.builder()
                .dbInfo(savedDbInfo)
                .sid(request.sid())
                .url(jdbcUrl)
                .build();

        // Instance 저장
        Instance savedInstance;
        try {
            savedInstance = targetDatabaseRepository.save(instance);
            log.info("[Database] Instance 저장 완료: id={}, sid={}, url={}", 
                    savedInstance.getId(), savedInstance.getSid(), savedInstance.getUrl());
        } catch (Exception e) {
            log.error("[Database] Instance 저장 실패: dbInfoId={}, sid={}, error={}", 
                    savedDbInfo.getId(), request.sid(), e.getMessage());
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, 
                    "데이터베이스 인스턴스 저장 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 활성화된 경우 동적 데이터소스 생성
        if (savedDbInfo.getIsActive()) {
            try {
                // 동적 데이터소스 생성 시에는 SID 형식 사용 (기존 generateJdbcUrl 메서드)
                String dataSourceUrl = savedDbInfo.generateJdbcUrl(request.sid());
                dynamicDataSourceFactory.createDataSource(
                        savedInstance.getId(),
                        savedDbInfo.getName(),
                        dataSourceUrl,
                        savedDbInfo.getUserName(),
                        request.password()  // 원본 비밀번호 사용
                );
                log.info("[Database] 동적 데이터소스 생성 완료: instanceId={}, dbInfoId={}, name={}", 
                        savedInstance.getId(), savedDbInfo.getId(), savedDbInfo.getName());
            } catch (Exception e) {
                log.error("[Database] 동적 데이터소스 생성 실패: instanceId={}, dbInfoId={}, name={}, error={}",
                        savedInstance.getId(), savedDbInfo.getId(), savedDbInfo.getName(), e.getMessage());
                // 데이터소스 생성 실패해도 엔티티는 저장 (나중에 재시도 가능)
                // 하지만 경고 로그는 남김
            }
        }

        // 성공 시에만 반환
        return savedInstance;
    }


    /**
     * DBInfo 수정
     */
    @Transactional
    public DBInfo updateTargetDatabase(Long dbInfoId, InstanceUpdateRequest request) {
        DBInfo dbInfo = dbInfoRepository.findById(dbInfoId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND, 
                        "데이터베이스 정보를 찾을 수 없습니다."));

        // 삭제된 DBInfo인지 확인
        if (dbInfo.getIsDeleted() != null && dbInfo.getIsDeleted()) {
            throw new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND, 
                    "삭제된 데이터베이스 정보입니다.");
        }

        // 이름 중복 확인 (자신 제외)
        if (request.name() != null && !request.name().equals(dbInfo.getName())) {
            if (dbInfoRepository.existsByNameAndIdNot(request.name(), dbInfoId)) {
                throw new BadRequestException(ExceptionMessage.DB_NAME_ALREADY_EXISTS, 
                        "이미 존재하는 데이터베이스 이름입니다.");
            }
        }

        // 비밀번호 암호화 처리 (변경된 경우에만 암호화하여 저장)
        String encryptedPassword = request.password() != null
                ? PasswordEncryptionUtil.encrypt(request.password(), encryptionKey)
                : null;

        // DBInfo 업데이트
        dbInfo.update(
                null,  // type
                null,  // version
                request.name(),
                null,  // ip
                null,  // port
                request.username(),
                encryptedPassword != null ? encryptedPassword : dbInfo.getPassword(),
                request.isActive()
        );

        DBInfo savedDbInfo = dbInfoRepository.save(dbInfo);
        log.info("[DBInfo] DBInfo 수정 완료: dbInfoId={}, name={}", 
                savedDbInfo.getId(), savedDbInfo.getName());

        // 연결된 모든 Instance의 데이터소스 제거 및 재생성
        List<Instance> instances = targetDatabaseRepository.findByDbInfoAndIsDeletedFalse(savedDbInfo);
        for (Instance instance : instances) {
            // 기존 데이터소스 제거
            dynamicDataSourceFactory.removeDataSource(instance.getId());

            // 활성화된 경우 새로운 데이터소스 생성
            if (savedDbInfo.getIsActive()) {
                try {
                    String password = request.password() != null 
                            ? request.password() 
                            : PasswordEncryptionUtil.decrypt(savedDbInfo.getPassword(), encryptionKey);
                    String jdbcUrl = savedDbInfo.generateJdbcUrl(instance.getSid());
                    dynamicDataSourceFactory.createDataSource(
                            instance.getId(),
                            savedDbInfo.getName(),
                            jdbcUrl,
                            savedDbInfo.getUserName(),
                            password
                    );
                    log.info("[DBInfo] 동적 데이터소스 재생성 완료: instanceId={}, dbInfoId={}, name={}", 
                            instance.getId(), savedDbInfo.getId(), savedDbInfo.getName());
                } catch (Exception e) {
                    log.error("[DBInfo] 동적 데이터소스 재생성 실패: instanceId={}, dbInfoId={}, name={}, error={}",
                            instance.getId(), savedDbInfo.getId(), savedDbInfo.getName(), e.getMessage());
                }
            }
        }

        return savedDbInfo;
    }

    /**
     * DBInfo 삭제 (논리적 삭제)
     */
    @Transactional
    public void deleteTargetDatabase(Long dbInfoId, String requestedName, String rawPassword) {
        DBInfo dbInfo = dbInfoRepository.findById(dbInfoId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND, 
                        "데이터베이스 정보를 찾을 수 없습니다."));

        // 이미 삭제된 경우
        if (dbInfo.getIsDeleted() != null && dbInfo.getIsDeleted()) {
            throw new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND, 
                    "이미 삭제된 데이터베이스 정보입니다.");
        }

        // DB 이름 검증
        if (StringUtils.hasText(requestedName) && !dbInfo.getName().equals(requestedName)) {
            throw new BadRequestException(ExceptionMessage.DB_INFO_NOT_FOUND,
                    "요청한 DB 이름이 일치하지 않습니다.");
        }

        // 비밀번호 검증
        validateDatabasePassword(dbInfo, rawPassword);

        // 연결된 모든 Instance 조회
        List<Instance> instances = targetDatabaseRepository.findByDbInfoAndIsDeletedFalse(dbInfo);

        // 연결된 모든 Instance의 동적 데이터소스 제거
        for (Instance instance : instances) {
            dynamicDataSourceFactory.removeDataSource(instance.getId());
        }

        // 연결된 모든 Instance 논리적 삭제
        for (Instance instance : instances) {
            instance.markAsDeleted();
            targetDatabaseRepository.save(instance);
        }

        // DBInfo 논리적 삭제
        dbInfo.markAsDeleted();
        dbInfoRepository.save(dbInfo);

        log.info("[DBInfo] DBInfo 논리적 삭제 완료: dbInfoId={}, name={}, 연결된 Instance 수={}", 
                dbInfoId, dbInfo.getName(), instances.size());
    }

    /**
     * DB 비밀번호 검증 (AES 암호화 또는 BCrypt 해시 지원)
     */
    private void validateDatabasePassword(DBInfo dbInfo, String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                    "비밀번호가 입력되지 않았습니다.");
        }

        String storedPassword = dbInfo.getPassword();
        if (!StringUtils.hasText(storedPassword)) {
            throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                    "저장된 비밀번호 정보가 존재하지 않습니다.");
        }

        // BCrypt 해시 처리
        if (PasswordEncryptionUtil.isBcryptHash(storedPassword)) {
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            if (!passwordEncoder.matches(rawPassword, storedPassword)) {
                throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                        "비밀번호가 일치하지 않습니다.");
            }
            return;
        }

        boolean matches = false;

        // AES 암호화된 값과 비교 (재암호화 방식)
        try {
            String encryptedInput = PasswordEncryptionUtil.encrypt(rawPassword, encryptionKey);
            matches = storedPassword.equals(encryptedInput);
        } catch (Exception e) {
            log.warn("[DBInfo] 비밀번호 재암호화 비교 실패: dbInfoId={}, name={}, error={}",
                    dbInfo.getId(), dbInfo.getName(), e.getMessage());
        }

        if (!matches) {
            try {
                String decryptedPassword = PasswordEncryptionUtil.decrypt(storedPassword, encryptionKey);
                matches = rawPassword.equals(decryptedPassword);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                        "비밀번호 형식을 확인할 수 없습니다. 비밀번호를 재설정 후 다시 시도해주세요.");
            } catch (RuntimeException e) {
                log.error("[DBInfo] 비밀번호 검증 실패: dbInfoId={}, name={}, error={}",
                        dbInfo.getId(), dbInfo.getName(), e.getMessage());
                throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                        "비밀번호 검증 중 오류가 발생했습니다.");
            }
        }

        if (!matches) {
            throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                    "비밀번호가 일치하지 않습니다.");
        }
    }

    private String resolvePasswordForConnection(DBInfo dbInfo) {
        String storedPassword = dbInfo.getPassword();
        if (!StringUtils.hasText(storedPassword)) {
            throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                    "저장된 비밀번호 정보가 존재하지 않습니다.");
        }

        if (PasswordEncryptionUtil.isBcryptHash(storedPassword)) {
            throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                    "비밀번호가 BCrypt 해시 형식입니다. 연결 API를 통해 비밀번호를 재입력해주세요.");
        }

        try {
            return PasswordEncryptionUtil.decrypt(storedPassword, encryptionKey);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                    "비밀번호 형식을 확인할 수 없습니다. 비밀번호를 재설정 후 다시 시도해주세요.");
        } catch (RuntimeException e) {
            log.error("[DBInfo] 비밀번호 복호화 실패: dbInfoId={}, name={}, error={}",
                    dbInfo.getId(), dbInfo.getName(), e.getMessage());
            throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                    "비밀번호 검증 중 오류가 발생했습니다.");
        }
    }

    /**
     * 타겟 DB 활성화 (암호화된 비밀번호 복호화하여 자동 연결)
     */
    @Transactional
    public Instance activateTargetDatabase(Long id) {
        Instance instance = targetDatabaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INSTANCE_NOT_FOUND, 
                        "데이터베이스 인스턴스를 찾을 수 없습니다."));

        DBInfo dbInfo = instance.getDbInfo();
        if (dbInfo == null) {
            throw new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND, 
                    "데이터베이스 정보를 찾을 수 없습니다.");
        }

        dbInfo.activate();
        DBInfo savedDbInfo = dbInfoRepository.save(dbInfo);

        // 암호화된 비밀번호 복호화하여 자동 연결
        try {
            String storedPassword = savedDbInfo.getPassword();

            // BCrypt 해시인 경우 복호화 불가능
            if (PasswordEncryptionUtil.isBcryptHash(storedPassword)) {
                log.warn("[TargetDatabase] 타겟 DB 비밀번호가 BCrypt 해시 형식입니다. " +
                        "자동 연결을 위해 비밀번호를 AES 암호화로 변경해야 합니다. " +
                        "instanceId={}, name={}. " +
                        "연결 API(/api/databases/{id}/connect)를 통해 비밀번호를 재입력하세요.",
                        id, savedDbInfo.getName());
                return instance;
            }

            String decryptedPassword = PasswordEncryptionUtil.decrypt(storedPassword, encryptionKey);
            String jdbcUrl = savedDbInfo.generateJdbcUrl(instance.getSid());
            dynamicDataSourceFactory.createDataSource(
                    id,
                    savedDbInfo.getName(),
                    jdbcUrl,
                    savedDbInfo.getUserName(),
                    decryptedPassword
            );
            log.info("[TargetDatabase] 타겟 DB 활성화 및 자동 연결 완료: instanceId={}, name={}",
                    id, savedDbInfo.getName());
        } catch (IllegalArgumentException e) {
            // BCrypt 해시 에러
            log.warn("[TargetDatabase] 타겟 DB 활성화 후 자동 연결 스킵: BCrypt 해시 형식. " +
                    "instanceId={}, name={}. 연결 API를 통해 비밀번호를 재입력하세요.",
                    id, savedDbInfo.getName());
        } catch (Exception e) {
            log.error("[TargetDatabase] 타겟 DB 활성화 후 자동 연결 실패: instanceId={}, name={}, error={}",
                    id, savedDbInfo.getName(), e.getMessage());
            // 연결 실패해도 활성화 상태는 유지 (나중에 재시도 가능)
        }

        return instance;
    }

    /**
     * 타겟 DB 연결 (비밀번호 재입력하여 데이터소스 생성)
     */
    @Transactional
    public Instance connectTargetDatabase(Long id, String password) {
        Instance instance = targetDatabaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INSTANCE_NOT_FOUND, 
                        "데이터베이스 인스턴스를 찾을 수 없습니다."));

        DBInfo dbInfo = instance.getDbInfo();
        if (dbInfo == null) {
            throw new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND, 
                    "데이터베이스 정보를 찾을 수 없습니다.");
        }

        String storedPassword = dbInfo.getPassword();
        boolean isBcryptHash = PasswordEncryptionUtil.isBcryptHash(storedPassword);

        // BCrypt 해시가 아닌 경우에만 비밀번호 검증
        if (!isBcryptHash) {
            try {
                String decryptedPassword = PasswordEncryptionUtil.decrypt(storedPassword, encryptionKey);
                if (!password.equals(decryptedPassword)) {
                    throw new BadRequestException(ExceptionMessage.DB_INVALID_CREDENTIALS,
                            "비밀번호가 일치하지 않습니다.");
                }
            } catch (IllegalArgumentException e) {
                // 복호화 불가능한 경우 (BCrypt가 아닌 다른 형식)
                log.warn("[TargetDatabase] 비밀번호 검증 스킵: 복호화 불가능한 형식. instanceId={}, name={}",
                        id, dbInfo.getName());
            }
        } else {
            log.info("[TargetDatabase] BCrypt 해시 형식 감지. 비밀번호를 AES 암호화로 마이그레이션합니다. instanceId={}, name={}",
                    id, dbInfo.getName());
        }

        // 기존 데이터소스 제거
        dynamicDataSourceFactory.removeDataSource(id);

        // 데이터소스 생성 (입력한 비밀번호 사용)
        try {
            String jdbcUrl = dbInfo.generateJdbcUrl(instance.getSid());
            dynamicDataSourceFactory.createDataSource(
                    id,
                    dbInfo.getName(),
                    jdbcUrl,
                    dbInfo.getUserName(),
                    password  // 입력한 원본 비밀번호 사용
            );
            log.info("[TargetDatabase] 타겟 DB 연결 및 데이터소스 생성 완료: instanceId={}, name={}",
                    id, dbInfo.getName());
        } catch (Exception e) {
            log.error("[TargetDatabase] 타겟 DB 연결 실패: instanceId={}, name={}, error={}",
                    id, dbInfo.getName(), e.getMessage());
            throw new BadRequestException(ExceptionMessage.DB_CONNECTION_FAILED,
                    "DB 연결에 실패했습니다: " + e.getMessage());
        }

        // 비밀번호를 AES 암호화로 저장 (BCrypt 해시 마이그레이션)
        String encryptedPassword = PasswordEncryptionUtil.encrypt(password, encryptionKey);
        dbInfo.update(
                null,  // type
                null,  // version
                null,  // name
                null,  // ip
                null,  // port
                null,  // userName
                encryptedPassword,  // 비밀번호를 AES 암호화로 저장
                true   // 활성화
        );

        DBInfo savedDbInfo = dbInfoRepository.save(dbInfo);
        log.info("[TargetDatabase] 타겟 DB 비밀번호를 AES 암호화로 저장 완료: instanceId={}, name={}",
                id, savedDbInfo.getName());

        return instance;
    }

    /**
     * 타겟 DB 비활성화
     */
    @Transactional
    public Instance deactivateTargetDatabase(Long id) {
        Instance instance = targetDatabaseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INSTANCE_NOT_FOUND, 
                        "데이터베이스 인스턴스를 찾을 수 없습니다."));

        DBInfo dbInfo = instance.getDbInfo();
        if (dbInfo == null) {
            throw new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND, 
                    "데이터베이스 정보를 찾을 수 없습니다.");
        }

        dbInfo.deactivate();
        DBInfo savedDbInfo = dbInfoRepository.save(dbInfo);

        // 동적 데이터소스 제거
        dynamicDataSourceFactory.removeDataSource(id);
        log.info("[TargetDatabase] 타겟 DB 비활성화 및 데이터소스 제거 완료: instanceId={}, name={}", 
                id, savedDbInfo.getName());

        return instance;
    }

    /**
     * 기존 DB에 새로운 인스턴스(SID) 추가
     */
    @Transactional
    public InstanceListResponse createInstanceForDatabase(Long dbInfoId, DatabaseInstanceCreateRequest request) {
        DBInfo dbInfo = dbInfoRepository.findByIdAndIsDeletedFalse(dbInfoId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND,
                        "데이터베이스 정보를 찾을 수 없습니다."));

        if (!StringUtils.hasText(request.sid())) {
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, "SID를 입력해주세요.");
        }

        String trimmedSid = request.sid().trim();

        if (targetDatabaseRepository.existsByDbInfoIdAndSid(dbInfoId, trimmedSid)) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE,
                    "이미 등록된 SID입니다: " + trimmedSid);
        }

        String jdbcUrl = dbInfo.generateJdbcUrlForInstance(trimmedSid);

        Instance instance = Instance.builder()
                .dbInfo(dbInfo)
                .sid(trimmedSid)
                .url(jdbcUrl)
                .build();

        Instance saved = targetDatabaseRepository.save(instance);
        log.info("[Instance] DBInfo에 인스턴스 추가 완료: dbInfoId={}, sid={}", dbInfoId, trimmedSid);

        return InstanceListResponse.from(saved);
    }

    public InstanceTestResponse testInstanceForDatabase(Long dbInfoId, DatabaseInstanceCreateRequest request) {
        DBInfo dbInfo = dbInfoRepository.findByIdAndIsDeletedFalse(dbInfoId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND,
                        "데이터베이스 정보를 찾을 수 없습니다."));

        if (!StringUtils.hasText(request.sid())) {
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, "SID를 입력해주세요.");
        }

        String password = resolvePasswordForConnection(dbInfo);
        DatabaseTestRequest testRequest = new DatabaseTestRequest(
                dbInfo.getIp(),
                dbInfo.getPort(),
                dbInfo.getUserName(),
                password,
                request.sid().trim()
        );

        return testDatabaseConnection(testRequest);
    }

    @Transactional
    public InstanceListResponse updateInstanceForDatabase(Long dbInfoId, Long instanceId, DatabaseInstanceCreateRequest request) {
        DBInfo dbInfo = dbInfoRepository.findByIdAndIsDeletedFalse(dbInfoId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INFO_NOT_FOUND,
                        "데이터베이스 정보를 찾을 수 없습니다."));

        Instance instance = targetDatabaseRepository.findByIdAndDbInfoIdAndIsDeletedFalse(instanceId, dbInfoId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INSTANCE_NOT_FOUND,
                        "데이터베이스 인스턴스를 찾을 수 없습니다."));

        if (!StringUtils.hasText(request.sid())) {
            throw new BadRequestException(ExceptionMessage.INVALID_REQUEST, "SID를 입력해주세요.");
        }

        String trimmedSid = request.sid().trim();

        if (!trimmedSid.equals(instance.getSid()) &&
                targetDatabaseRepository.existsByDbInfoIdAndSid(dbInfoId, trimmedSid)) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_VALUE,
                    "이미 등록된 SID입니다: " + trimmedSid);
        }

        String jdbcUrl = dbInfo.generateJdbcUrlForInstance(trimmedSid);
        instance.update(trimmedSid, jdbcUrl);
        Instance saved = targetDatabaseRepository.save(instance);
        log.info("[Instance] DBInfo 인스턴스 수정 완료: dbInfoId={}, instanceId={}, sid={}",
                dbInfoId, instanceId, trimmedSid);

        return InstanceListResponse.from(saved);
    }

    @Transactional
    public void deleteInstanceForDatabase(Long dbInfoId, Long instanceId) {
        Instance instance = targetDatabaseRepository.findByIdAndDbInfoIdAndIsDeletedFalse(instanceId, dbInfoId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DB_INSTANCE_NOT_FOUND,
                        "데이터베이스 인스턴스를 찾을 수 없습니다."));

        dynamicDataSourceFactory.removeDataSource(instance.getId());
        instance.markAsDeleted();
        targetDatabaseRepository.save(instance);

        log.info("[Instance] DBInfo 인스턴스 삭제 완료: dbInfoId={}, instanceId={}", dbInfoId, instanceId);
    }
}


