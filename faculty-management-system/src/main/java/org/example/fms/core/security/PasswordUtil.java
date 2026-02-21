package org.example.fms.core.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for handling password hashing and verification securely using
 * BCrypt.
 */
public class PasswordUtil {

    private static final int BCRYPT_WORK_FACTOR = 12;

    /**
     * Hashes a plain text password using BCrypt.
     * 
     * @param plainTextPassword The password to hash.
     * @return The hashed password string.
     */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
    }

    /**
     * Verifies if a plain text password matches a hashed password.
     * 
     * @param plainTextPassword The plain text password to check.
     * @param hashedPassword    The existing hashed password to compare against.
     * @return true if the password matches, false otherwise.
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            throw new IllegalArgumentException("Invalid hash provided for comparison");
        }
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}
