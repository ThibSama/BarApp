package com.lebarapp.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Default {@link PublicCodeGenerator}: draws a fixed-length code from a
 * {@link SecureRandom}, so codes are not derived from timestamps or from the
 * order UUID in a predictable sequential way.
 *
 * <p>The alphabet excludes visually ambiguous characters (0/O, 1/I/L) for
 * readability. The length (6) fits the {@code VARCHAR(8)} column and the
 * required 6–8 range.</p>
 */
@Component
public class RandomPublicCodeGenerator implements PublicCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int CODE_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
