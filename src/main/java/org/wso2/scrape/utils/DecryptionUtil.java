package org.wso2.scrape.utils;
import org.apache.axiom.om.util.Base64;
import javax.crypto.Cipher;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;

public class DecryptionUtil {

    /**
     * Decrypts the given ciphertext using the provided keystore details.
     *
     * @param encryptedText     The Base64-encoded encrypted text.
     * @param keystorePath      The path to the keystore file.
     * @param keystoreAlias     The alias of the key in the keystore.
     * @param keystorePassword  The password for the keystore.
     * @return The decrypted plaintext.
     * @throws Exception If decryption fails.
     */
    public static String decrypt(String encryptedText, String keystorePath, String keystoreAlias, String keystorePassword) throws Exception {
        byte[] ciphertext = Base64.decode(encryptedText);
        KeyStore keyStore = getKeyStore(keystorePath, keystorePassword);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keystoreAlias, keystorePassword.toCharArray());
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(ciphertext);
        return new String(decryptedBytes);
    }

    /**
     * Loads the keystore from the specified path.
     *
     * @param keystorePath      The path to the keystore file.
     * @param keystorePassword  The password for the keystore.
     * @return The loaded keystore.
     * @throws Exception If the keystore cannot be loaded.
     */
    private static KeyStore getKeyStore(String keystorePath, String keystorePassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream in = new FileInputStream(keystorePath)) {
            keyStore.load(in, keystorePassword.toCharArray());
        }
        return keyStore;
    }
}
