package com.nepalnationalteam.blackjack;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class FirebaseService {
    public static String API_KEY = "AIzaSyDNaHevzVntqeuhzZPn9a6iy-l8UdSDmqc";
    public static String DATABASE_URL = "https://nepalnationalteamblackjack-default-rtdb.europe-west1.firebasedatabase.app/";

    private static String jsonString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String httpJson(String method, String urlStr, String body) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        byte[] out = body.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(out.length);
        conn.connect();
        try (OutputStream os = conn.getOutputStream()) { os.write(out); }
        int code = conn.getResponseCode();
        InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
        String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (code >= 400) throw new IOException(resp);
        return resp;
    }

    private static String post(String url, String body) throws IOException { return httpJson("POST", url, body); }
    private static String put(String url, String body) throws IOException { return httpJson("PUT", url, body); }

    public static class AuthResult {
        public String idToken;
        public String refreshToken;
        public String localId;
        public String displayName;
        public String email;
    }

    public AuthResult signIn(String email, String password) throws IOException {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;
        String payload = "{\"email\":\"" + jsonString(email) + "\",\"password\":\"" + jsonString(password) + "\",\"returnSecureToken\":true}";
        String resp = post(endpoint, payload);
        AuthResult r = new AuthResult();
        r.email = extractString(resp, "email");
        r.idToken = extractString(resp, "idToken");
        r.refreshToken = extractString(resp, "refreshToken");
        r.localId = extractString(resp, "localId");
        r.displayName = extractString(resp, "displayName");
        return r;
    }

    public AuthResult signUp(String email, String password, String displayName) throws IOException {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;
        String payload = "{\"email\":\"" + jsonString(email) + "\",\"password\":\"" + jsonString(password) + "\",\"returnSecureToken\":true}";
        String resp = post(endpoint, payload);
        AuthResult r = new AuthResult();
        r.email = extractString(resp, "email");
        r.idToken = extractString(resp, "idToken");
        r.refreshToken = extractString(resp, "refreshToken");
        r.localId = extractString(resp, "localId");
        r.displayName = displayName;

        if (DATABASE_URL != null && DATABASE_URL.startsWith("http")) {
            String userPath = DATABASE_URL + "users/" + r.localId + ".json?auth=" + r.idToken;
            String body = "{\"email\":\"" + jsonString(email) + "\",\"displayName\":\"" + jsonString(displayName) + "\",\"createdAt\":" + System.currentTimeMillis() + "}";
            put(userPath, body);
        }
        return r;
    }

    public String refreshIdToken(String refreshToken) throws IOException {
        String endpoint = "https://securetoken.googleapis.com/v1/token?key=" + API_KEY;
        String payload = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        byte[] out = payload.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(out.length);
        conn.connect();
        try (OutputStream os = conn.getOutputStream()) { os.write(out); }
        int code = conn.getResponseCode();
        InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
        String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (code >= 400) throw new IOException(resp);
        return extractString(resp, "id_token");
    }

    // tiny string extractor: looks for "key":"value"
    private static String extractString(String json, String key) {
        if (json == null || key == null) return null;
        String p = "\"" + key + "\":\"";
        int i = json.indexOf(p);
        if (i < 0) return null;
        int a = i + p.length();
        int b = json.indexOf("\"", a);
        if (b < 0) return null;
        String v = json.substring(a, b);
        return v.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
