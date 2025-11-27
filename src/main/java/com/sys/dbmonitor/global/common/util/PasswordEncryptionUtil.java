/*
******************************************************************
작성자: 배지원
******************************************************************
*/
package com.sys.dbmonitor.global.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 비밀번호 암호화/복호화 유틸리티
 * AES-128 암호화 사용
 */
public class PasswordEncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";

    /**
     * 비밀번호 암호화
     *
     * @param password 원본 비밀번호
     * @param key      암호화 키 (16바이트)
     * @return 암호화된 비밀번호 (Base64 인코딩)
     */
    public static String encrypt(String password, String key) {
        try {
            if (password == null || password.isEmpty()) {
                return password;
            }

            SecretKeySpec secretKey = new SecretKeySpec(
                    getKeyBytes(key),
                    ALGORITHM
            );

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("비밀번호 암호화 실패", e);
        }
    }

    /**
     * BCrypt 해시 형식인지 확인
     * BCrypt 해시는 $2a$, $2b$, $2x$, $2y$ 등으로 시작
     */
    public static boolean isBcryptHash(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.startsWith("$2a$") || 
               password.startsWith("$2b$") || 
               password.startsWith("$2x$") || 
               password.startsWith("$2y$");
    }

    /**
     * 비밀번호 복호화
     * BCrypt 해시는 복호화 불가능하므로 예외 발생
     *
     * @param encryptedPassword 암호화된 비밀번호 (Base64 인코딩)
     * @param key               암호화 키 (16바이트)
     * @return 복호화된 비밀번호
     * @throws IllegalArgumentException BCrypt 해시인 경우
     */
    public static String decrypt(String encryptedPassword, String key) {
        try {
            if (encryptedPassword == null || encryptedPassword.isEmpty()) {
                return encryptedPassword;
            }

            // BCrypt 해시인 경우 복호화 불가능
            if (isBcryptHash(encryptedPassword)) {
                throw new IllegalArgumentException(
                    "BCrypt 해시는 복호화할 수 없습니다. 비밀번호를 재입력하여 AES 암호화로 저장하세요."
                );
            }

            SecretKeySpec secretKey = new SecretKeySpec(
                    getKeyBytes(key),
                    ALGORITHM
            );

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decoded = Base64.getDecoder().decode(encryptedPassword);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // BCrypt 해시 에러는 그대로 전달
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("비밀번호 복호화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 키를 16바이트로 변환 (AES-128)
     * 키가 16바이트보다 길면 자르고, 짧으면 패딩 추가
     */
    private static byte[] getKeyBytes(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[16];

        if (keyBytes.length >= 16) {
            System.arraycopy(keyBytes, 0, result, 0, 16);
        } else {
            System.arraycopy(keyBytes, 0, result, 0, keyBytes.length);
            // 나머지는 0으로 채움
            for (int i = keyBytes.length; i < 16; i++) {
                result[i] = 0;
            }
        }

        return result;
    }
}

