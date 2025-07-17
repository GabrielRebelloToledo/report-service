package com.example.relatorio.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CriptoUtil {

    public static String decryptAES(String base64Encrypted, String secretKey) throws Exception {
        byte[] encryptedData = Base64.getDecoder().decode(base64Encrypted);

        // Extrai IV (16 bytes)
        byte[] iv = new byte[16];
        System.arraycopy(encryptedData, 0, iv, 0, 16);

        // Extrai dados criptografados (o resto)
        byte[] cipherText = new byte[encryptedData.length - 16];
        System.arraycopy(encryptedData, 16, cipherText, 0, cipherText.length);

        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decrypted = cipher.doFinal(cipherText);
        return new String(decrypted, "UTF-8");
    }
}
