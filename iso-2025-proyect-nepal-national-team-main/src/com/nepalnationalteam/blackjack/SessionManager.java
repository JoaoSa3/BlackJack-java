package com.nepalnationalteam.blackjack;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"email\":\"").append(js(n(s.email))).append("\",");
            sb.append("\"displayName\":\"").append(js(n(s.displayName))).append("\",");
            sb.append("\"idToken\":\"").append(js(n(s.idToken))).append("\",");
            sb.append("\"refreshToken\":\"").append(js(n(s.refreshToken))).append("\",");
            sb.append("\"localId\":\"").append(js(n(s.localId))).append("\",");
            sb.append("\"selectedId\":\"").append(js(n(s.selectedId))).append("\",");
            sb.append("\"idType\":\"").append(js(n(s.idType))).append("\",");
            sb.append("\"idValue\":\"").append(js(n(s.idValue))).append("\",");
            sb.append("\"rememberMe\":").append(s.rememberMe ? "true" : "false");
            sb.append("}");
            w.write(sb.toString());
            return true;
        } catch (Exception e) {
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
            sess.email = getString(s, "email");
            sess.displayName = getString(s, "displayName");
            sess.idToken = getString(s, "idToken");
            sess.refreshToken = getString(s, "refreshToken");
            sess.localId = getString(s, "localId");
            sess.selectedId = getString(s, "selectedId");
            sess.idType = getString(s, "idType");
            sess.idValue = getString(s, "idValue");
            sess.rememberMe = s.contains("\"rememberMe\":true");
            return sess;
        } catch (Exception e) {
            return null;
        }
    }

    public static void clear() {
        try { new File(SESSION_FILE).delete(); } catch (Exception ignored) {}
    }

    private static String getString(String json, String key) {
        String p = "\"" + key + "\":\"";
        int i = json.indexOf(p);
        if (i < 0) return null;
        int a = i + p.length();
        int b = json.indexOf("\"", a);
        if (b < 0) return null;
        return json.substring(a, b).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String n(String s) { return s == null ? "" : s; }

    private static String js(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
