
package com.nepalnationalteam.blackjackdesktop;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Stores user session and "remember me" preference in user_session.json
 */
public class SessionManager {
    private static final String SESSION_FILE = "user_session.json";

    public static class Session {
        public String email;
        public String displayName;
        public String idToken;
        public String refreshToken;
        public String localId;
        public String selectedId;
        public String idType;
        public String idValue;
        public boolean rememberMe;
    }

    public static boolean save(Session s) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(SESSION_FILE), StandardCharsets.UTF_8)) {
            String json = "{"
                + "\"email\":\"" + esc(nullToEmpty(s.email)) + "\","
                + "\"displayName\":\"" + esc(nullToEmpty(s.displayName)) + "\","
                + "\"idToken\":\"" + esc(nullToEmpty(s.idToken)) + "\","
                + "\"refreshToken\":\"" + esc(nullToEmpty(s.refreshToken)) + "\","
                + "\"localId\":\"" + esc(nullToEmpty(s.localId)) + "\","
                + "\"selectedId\":\"" + esc(nullToEmpty(s.selectedId)) + "\","
                + "\"rememberMe\":" + (s.rememberMe ? "true" : "false")
                + "}";
            w.write(json);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Session load() {
        try {
            File f = new File(SESSION_FILE);
            if (!f.exists()) return null;
            String s = new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
            if (s.isEmpty()) return null;
            Session sess = new Session();
            sess.email = extract(s, "email");
            sess.displayName = extract(s, "displayName");
            sess.idToken = extract(s, "idToken");
            sess.refreshToken = extract(s, "refreshToken");
            sess.localId = extract(s, "localId");
            sess.selectedId = extract(s, "selectedId");
            sess.idType = extract(s, "idType");
            sess.idValue = extract(s, "idValue");
            sess.rememberMe = s.contains("\"rememberMe\":true");
            return sess;
        } catch (Exception e) {
            return null;
        }
    }

    public static void clear() {
        try { new File(SESSION_FILE).delete(); } catch (Exception ignored) {}
    }

    private static String extract(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int i = json.indexOf(pattern);
        if (i < 0) return null;
        int start = i + pattern.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
