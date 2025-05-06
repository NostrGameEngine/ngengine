package org.ngengine.network;

import org.ngengine.nostr4j.utils.NostrUtils;

public class Crypto {
    public static String encrypt(String data, String passphrase) {
        org.ngengine.nostr4j.platform.Platform pl = NostrUtils.getPlatform();
        byte nonce[] = pl.randomBytes(12);
        byte chachaKey[] = pl.sha256(passphrase.getBytes());
        byte[] plaintext = data.getBytes();
        byte[] padded = new byte[plaintext.length + 16];
        System.arraycopy(plaintext, 0, padded, 0, plaintext.length);
        byte[] ciphertext = NostrUtils.getPlatform().chacha20(chachaKey, nonce, padded, true);
        return pl.base64encode(ciphertext)+"?"+pl.base64encode(nonce);
    }

    public static String decrypt(String data, String passphrase) {
        org.ngengine.nostr4j.platform.Platform pl = NostrUtils.getPlatform();
        String[] parts = data.split("\\?");
        byte[] ciphertext = pl.base64decode(parts[0]);
        byte[] nonce = pl.base64decode(parts[1]);
        byte chachaKey[] = pl.sha256(passphrase.getBytes());
        byte[] padded = NostrUtils.getPlatform().chacha20(chachaKey, nonce, ciphertext, false);
        return new String(padded).trim();
    }

    
}
