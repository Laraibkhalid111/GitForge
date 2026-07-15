package com.gitforge.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Generates simulated Git-style SHA-1 commit hashes.
 */
public final class CommitHashGenerator {

    private CommitHashGenerator() {
    }

    public static String generate(String message, String author, Instant committedAt,
                                  long repositoryId, long branchId) {
        String payload = String.join("|",
                nullToEmpty(message),
                nullToEmpty(author),
                committedAt == null ? "" : InstantFormats.format(committedAt),
                Long.toString(repositoryId),
                Long.toString(branchId),
                Long.toString(System.nanoTime())
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 not available", ex);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
