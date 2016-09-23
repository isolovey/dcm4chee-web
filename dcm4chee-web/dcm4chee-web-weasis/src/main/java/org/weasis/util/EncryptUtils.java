package org.weasis.util;

import java.io.IOException;

import org.weasis.dicom.data.xml.Base64;

public class EncryptUtils {
    private static final char START = '~';
    private static final int KEY_MIN_SIZE = 10; // inclusive
    private static final int KEY_MAX_SIZE = 30; // inclusive

    public static String encrypt(String message, String key) {
        if (message == null || key == null) {
            throw new IllegalArgumentException("message or key arguments cannot be null!");
        }
        String result = xorMessage(message.trim(), key);
        try {
            return Base64.encodeBytes(result.getBytes(), Base64.URL_SAFE);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String xorMessage(String message, String key) {
        if (message == null || key == null) {
            throw new IllegalArgumentException("message or key arguments cannot be null!");
        }
        char[] mChars = message.toCharArray();
        char[] kChars = key.toCharArray();
        if (mChars.length < 1) {
            throw new IllegalArgumentException("Cannot encode empty message!");
        }
        if (kChars.length < KEY_MIN_SIZE || kChars.length > KEY_MAX_SIZE) {
            throw new IllegalArgumentException("key size must be >= " + KEY_MIN_SIZE + " and <= " + KEY_MAX_SIZE
                + " characters");
        }

        int kl = kChars.length;
        int ml = mChars.length;
        // If key is longer than message, take the key length for result
        char[] newmsg = new char[kl > ml ? kl : ml];

        for (int i = 0; i < newmsg.length; i++) {
            newmsg[i] = (char) (mChars[i % ml] ^ kChars[i % kl]);
        }
        if (ml < kl) {
            newmsg[ml] = (char) (START ^ kChars[ml]);
        }
        return new String(newmsg);
    }

    public static String decrypt(String message, String key) {
        if (message == null || key == null) {
            throw new IllegalArgumentException("message or key arguments cannot be null!");
        }
        String result = null;
        try {
            result = new String(Base64.decode(message.trim(), Base64.URL_SAFE));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return unXorMessage(result, key);
    }

    private static String unXorMessage(String message, String key) {
        if (message == null || key == null) {
            throw new IllegalArgumentException("message or key arguments cannot be null!");
        }
        char[] mChars = message.toCharArray();
        char[] kChars = key.toCharArray();

        int kl = kChars.length;
        int ml = mChars.length;
        // If key is longer than message, take the key length for result
        char[] newmsg = new char[kl > ml ? kl : ml];

        int cutMessage = -1;
        for (int i = 0; i < newmsg.length; i++) {
            newmsg[i] = (char) (mChars[i % ml] ^ kChars[i % kl]);
            if (newmsg[i] == START) {
                cutMessage = i;
                break;
            }
        }
        if (cutMessage > 0) {
            newmsg = copyOfRange(newmsg, 0, cutMessage);
        }
        return new String(newmsg);
    }

    // This method has been introduced in Arrays class of JAVA 6. Copy it to be compliant with Java 1.5
    public static char[] copyOfRange(char[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        char[] copy = new char[newLength];
        System.arraycopy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }
}
