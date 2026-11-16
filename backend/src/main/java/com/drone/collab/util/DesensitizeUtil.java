package com.drone.collab.util;

public final class DesensitizeUtil {

    private DesensitizeUtil() {
    }

    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        int n = phone.length();
        return phone.substring(0, 3) + "****" + phone.substring(n - 4);
    }

    public static String idCard(String id) {
        if (id == null || id.length() < 8) {
            return id;
        }
        return id.substring(0, 4) + "**********" + id.substring(id.length() - 4);
    }
}
