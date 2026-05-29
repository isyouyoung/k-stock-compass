package kopo.kstockcompass.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptUtil {

    // AES-128 CBC 암호화에 사용할 키 (16바이트 = 128비트)
    private static final String AES_KEY = "kstockcompass12!"; // 16자 ✅
    private static final String AES_IV  = "kstockcompass12!"; // 16자 ✅

    /**
     * AES-128 CBC 암호화
     * 이메일, 전화번호 등 식별정보 암호화에 사용
     */
    public static String encAES128CBC(String str) throws Exception {
        SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes("UTF-8"), "AES");
        IvParameterSpec iv = new IvParameterSpec(AES_IV.getBytes("UTF-8"));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        byte[] encrypted = cipher.doFinal(str.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * AES-128 CBC 복호화
     * 암호화된 이메일, 전화번호 복호화에 사용
     */
    public static String decAES128CBC(String str) throws Exception {
        SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes("UTF-8"), "AES");
        IvParameterSpec iv = new IvParameterSpec(AES_IV.getBytes("UTF-8"));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        byte[] decoded = Base64.getDecoder().decode(str);
        return new String(cipher.doFinal(decoded), "UTF-8");
    }
}