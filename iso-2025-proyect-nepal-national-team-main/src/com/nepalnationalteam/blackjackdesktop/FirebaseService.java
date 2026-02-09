
package com.nepalnationalteam.blackjackdesktop;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class FirebaseService {
    public static String API_KEY = "AIzaSyDNaHevzVntqeuhzZPn9a6iy-l8UdSDmqc";
    public static String DATABASE_URL = "https://nepalnationalteamblackjack-default-rtdb.europe-west1.firebasedatabase.app/";

    private static String esc(String s) { return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static String httpWithBody(String method, String urlStr, String body) throws IOException {
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
    private static String post(String urlStr, String body) throws IOException { return httpWithBody("POST", urlStr, body); }
    private static String put(String urlStr, String body) throws IOException { return httpWithBody("PUT", urlStr, body); }

    public static class AuthResult {
        public String idToken;
        public String refreshToken;
        public String localId;
        public String displayName;
        public String email;
    }

    public AuthResult signIn(String email, String password) throws IOException {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;
        String payload = "{\"email\":\"" + esc(email) + "\",\"password\":\"" + esc(password) + "\",\"returnSecureToken\":true}";
        String resp = post(endpoint, payload);
        AuthResult r = new AuthResult();
        r.email = extract(resp, "email");
        r.idToken = extract(resp, "idToken");
        r.refreshToken = extract(resp, "refreshToken");
        r.localId = extract(resp, "localId");
        r.displayName = extract(resp, "displayName");
        return r;
    }

    public AuthResult signUp(String email, String password, String displayName) throws IOException {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;
        String payload = "{\"email\":\"" + esc(email) + "\",\"password\":\"" + esc(password) + "\",\"returnSecureToken\":true}";
        String resp = post(endpoint, payload);
        AuthResult r = new AuthResult();
        r.email = extract(resp, "email");
        r.idToken = extract(resp, "idToken");
        r.refreshToken = extract(resp, "refreshToken");
        r.localId = extract(resp, "localId");
        r.displayName = displayName;

        if (DATABASE_URL.startsWith("http")) {
            String userPath = DATABASE_URL + "users/" + r.localId + ".json?auth=" + r.idToken;
            String body = "{\"email\":\""+esc(email)+"\",\"displayName\":\""+esc(displayName)+"\",\"createdAt\":"+System.currentTimeMillis()+"}";
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
        return extract(resp, "id_token");
    }

    private static String extract(String json, String key) {
        String k = "\"" + key + "\":";
        int i = json.indexOf(k);
        if (i < 0) return null;
        int start = json.indexOf("\"", i + k.length());
        if (start < 0) return null;
        int end = json.indexOf("\"", start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
