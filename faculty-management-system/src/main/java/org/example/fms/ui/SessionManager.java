package org.example.fms.ui;

public class SessionManager {
    private static String token;
    private static String role;

    public static void setToken(String jwtToken) {
        token = jwtToken;
    }

    public static String getToken() {
        return token;
    }

    public static void setRole(String userRole) {
        role = userRole;
    }

    public static String getRole() {
        return role;
    }

    public static void clear() {
        token = null;
        role = null;
    }
}
