
package com.nepalnationalteam.blackjackdesktop;

import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;

/**
 * Stores user session and "remember me" preference in user_session.json
 */
public class SessionManager {
    private static final String SESSION_FILE = "user_session.json";
    private static final Gson gson = new Gson();

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
            gson.toJson(s, w);
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
            try (Reader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
                return gson.fromJson(reader, Session.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void clear() {
        try { new File(SESSION_FILE).delete(); } catch (Exception ignored) {}
    }
}
