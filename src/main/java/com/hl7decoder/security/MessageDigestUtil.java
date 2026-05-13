package com.hl7decoder.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class MessageDigestUtil {
    private MessageDigestUtil() {
    }

    static boolean equals(String left, String right) {
        return MessageDigest.isEqual(
                left == null ? new byte[0] : left.getBytes(StandardCharsets.UTF_8),
                right == null ? new byte[0] : right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
