package org.ngengine.network;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestCrypto {
    @Test
    public void testCrypto(){
        String passphrase = "test";
        String data = "Hello World!";
        String encrypted = Crypto.encrypt(data, passphrase);
        System.out.println("Encrypted: " + encrypted);
        String decrypted = Crypto.decrypt(encrypted, passphrase);
        System.out.println("Decrypted: " + decrypted);
       assertEquals(data, decrypted);
    }
}