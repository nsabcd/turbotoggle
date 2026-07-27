package com.turbotoggle.service.evaluator;

import java.nio.charset.StandardCharsets;

public class HashingUtil {
    private HashingUtil(){}

    public static int getBucket(String flagKey, String contextKey){
        String combinedKey = flagKey + ":" + contextKey;
        int hash = murmurhash3_x86_32(combinedKey.getBytes(StandardCharsets.UTF_8), 0, combinedKey.length(), 0x971e37b9);
        return Math.abs(hash%100);
    }
    private static int murmurhash3_x86_32(byte[] data, int offset, int len, int seed) {
        int c1 = 0xcc9e2d51;
        int c2 = 0x1b873593;
        int h1 = seed;
        int roundedEnd = offset + (len & 0xfffffffc);

        for (int i = offset; i < roundedEnd; i += 4) {
            int k1 = (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8) | ((data[i + 2] & 0xff) << 16) | (data[i + 3] << 24);
            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;

            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
        }

        int k1 = 0;
        int tail = len & 3;
        if (tail == 3) k1 ^= (data[roundedEnd + 2] & 0xff) << 16;
        if (tail >= 2) k1 ^= (data[roundedEnd + 1] & 0xff) << 8;
        if (tail >= 1) {
            k1 ^= (data[roundedEnd] & 0xff);
            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;
            h1 ^= k1;
        }

        h1 ^= len;
        h1 ^= (h1 >>> 16);
        h1 *= 0x85ebca6b;
        h1 ^= (h1 >>> 13);
        h1 *= 0xc2b2ae35;
        h1 ^= (h1 >>> 16);

        return h1;
    }
}
