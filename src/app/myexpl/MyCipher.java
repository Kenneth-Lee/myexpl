package app.myexpl;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MyCipher {

    private final static String alg = "AES";

    private Cipher createCipher(String password, int mode) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(getRawKey(password.getBytes()), alg);
        Cipher cipher = Cipher.getInstance(alg);
        cipher.init(mode, skeySpec);
        return cipher;
    }

    public byte[] encrypt(String password, byte[] data) throws Exception {
        return createCipher(password, Cipher.ENCRYPT_MODE).doFinal(data);
    }
    
    public byte[] decrypt(String password, byte[] data) throws Exception {
        return createCipher(password, Cipher.DECRYPT_MODE).doFinal(data);
    }

    private static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance(alg);
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey skey = kgen.generateKey();
        return skey.getEncoded();
    }
}

/* vim: set et ts=4 sw=4: */