package org.thoth.crypto.symmetric;

import java.security.SecureRandom;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 *
 * @author Michael Remijan mjremijan@yahoo.com @mjremijan
 */
public class Aes256 {

    // Use 128 if you don't have the Java Cryptography Extension (JCE) Unlimited Strength installed
    public static int KEY_SIZE = 256;
    public static int IV_SIZE = 12; // 12bytes * 8 = 96bits
    public static int TAG_BIT_SIZE = 128;
    public static String ALGORITHM_NAME = "AES";
    public static String MODE_OF_OPERATION = "GCM";
    public static String PADDING_SCHEME = "PKCS5Padding";

    protected SecretKey secretKey;
    protected SecureRandom secureRandom;

    public Aes256(SecretKey secretKey) {
        this.secretKey = secretKey;
        this.secureRandom = new SecureRandom();
    }


    public byte[] encrypt(String message, Optional<String> aad) {
        try {
            // Transformation specifies algortihm, mode of operation and padding
            Cipher c = Cipher.getInstance(
                String.format("%s/%s/%s",ALGORITHM_NAME,MODE_OF_OPERATION,PADDING_SCHEME)
            );

            // Generate IV
            byte iv[] = new byte[IV_SIZE];
            secureRandom.nextBytes(iv); // SecureRandom initialized using self-seeding

            // Initialize GCM Parameters
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_SIZE, iv);

            // Init for encryption
            c.init(Cipher.ENCRYPT_MODE, secretKey, spec, secureRandom);

            // Add AAD tag data if present
            aad.ifPresent(t -> {
                try {
                    c.updateAAD(t.getBytes("UTF-8"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Add message to encrypt
            c.update(message.getBytes("UTF-8"));

            // Encrypt
            byte[] encryptedBytes
                = c.doFinal();


            // Concatinate IV and encrypted bytes.  The IV is needed later
            // in order to to decrypt.  The IV value does not need to be
            // kept secret, so it's OK to encode it in the return value
            //
            // Create a new byte[] the combined length of IV and encryptedBytes
            byte[] ivPlusEncryptedBytes = new byte[iv.length + encryptedBytes.length];
            // Copy IV bytes into the new array
            System.arraycopy(iv, 0, ivPlusEncryptedBytes, 0, iv.length);
            // Copy encryptedBytes into the new array
            System.arraycopy(encryptedBytes, 0, ivPlusEncryptedBytes, iv.length, encryptedBytes.length);

            // Return
            return ivPlusEncryptedBytes;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public String decrypt(byte[] ivPlusEncryptedBytes, Optional<String> aad) {

        try {
            // Get IV
            byte iv[] = new byte[IV_SIZE];
            System.arraycopy(ivPlusEncryptedBytes, 0, iv, 0, IV_SIZE);

            // Initialize GCM Parameters
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_SIZE, iv);

            // Transformation specifies algortihm, mode of operation and padding
            Cipher c = Cipher.getInstance(
                String.format("%s/%s/%s",ALGORITHM_NAME,MODE_OF_OPERATION,PADDING_SCHEME)
            );

            // Get encrypted bytes
            byte [] encryptedBytes = new byte[ivPlusEncryptedBytes.length - IV_SIZE];
            System.arraycopy(ivPlusEncryptedBytes, IV_SIZE, encryptedBytes, 0, encryptedBytes.length);

            // Init for decryption
            c.init(Cipher.DECRYPT_MODE, secretKey, spec, secureRandom);

            // Add AAD tag data if present
            aad.ifPresent(t -> {
                try {
                    c.updateAAD(t.getBytes("UTF-8"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Add message to decrypt
            c.update(encryptedBytes);

            // Decrypt
            byte[] decryptedBytes
                = c.doFinal();

            // Return
            return new String(decryptedBytes, "UTF-8");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
