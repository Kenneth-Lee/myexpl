package app.myexpl;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MyCipher {

    private final static String alg = "AES";

    private static Cipher createCipher(String password, int mode) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(getRawKey(password), alg);
        Cipher cipher = Cipher.getInstance(alg);
        cipher.init(mode, skeySpec);
        return cipher;
    }

    public static void mask(byte[] rawkey, byte[] data, int len) throws Exception {
        for(int i=0; i<len; i++) {
            data[i] = (byte)(data[i]^rawkey[i%rawkey.length]);
        }    	
    }
    
    public static void mask(String password, byte[] data) throws Exception {
    	mask(getRawKey(password), data, data.length);
    }

    public static byte[] encrypt(String password, byte[] data) throws Exception {
        return createCipher(password, Cipher.ENCRYPT_MODE).doFinal(data);
    }
    
    public static byte[] decrypt(String password, byte[] data) throws Exception {
        return createCipher(password, Cipher.DECRYPT_MODE).doFinal(data);
    }

    public static byte[] getRawKey(String password) throws Exception {
    	byte[] seed = password.getBytes();
        KeyGenerator kgen = KeyGenerator.getInstance(alg);
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey skey = kgen.generateKey();
        return skey.getEncoded();
    }
}

/* vim: set et ts=4 sw=4: */
