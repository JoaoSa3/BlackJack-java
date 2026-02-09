
package com.nepalnationalteam.blackjackdesktop;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class FirebaseService {
    public static String API_KEY = "AIzaSyCY6vV6CwyQRl9s6tOCuwoeJ7fWx-gdpJI";
    public static String DATABASE_URL = "https://nepal-national-team-default-rtdb.europe-west1.firebasedatabase.app/";


    static {
        // Load API_KEY and DATABASE_URL from serviceAccountKey.json if present
        try (InputStream is = FirebaseService.class.getResourceAsStream("/serviceAccountKey.json")) {
            if (is != null) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String k = extract(json, "apiKey");
                String d = extract(json, "databaseUrl");
                if (k != null && !k.isEmpty()) {
                    API_KEY = k;
                }
                if (d != null && !d.isEmpty()) {
                    DATABASE_URL = d.endsWith("/") ? d : d + "/";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


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
        if (is == null) {
            throw new IOException("No response body (HTTP " + code + ")");
        }
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
        if (is == null) {
            throw new IOException("No response body (HTTP " + code + ")");
        }
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


    // ============================================================
    //            Métodos extra: saldo por utilizador
    // ============================================================

    /**
     * Lê o saldo do utilizador a partir do Realtime Database.
     * Se não existir, inicializa com 1000 e devolve 1000.
     */
    public int fetchOrInitBalance(SessionManager.Session sess) {
        int defaultBalance = 1000;
        if (sess == null || sess.idToken == null || sess.idToken.isEmpty()
                || DATABASE_URL == null || DATABASE_URL.contains("YOUR-RTDATABASE-URL")) {
            return defaultBalance;
        }
        try {
            String path = "users/" + safe(sess.localId) + "/balance.json?auth="
                    + URLEncoder.encode(sess.idToken, StandardCharsets.UTF_8);
            URL url = new URL(DATABASE_URL + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) {
                throw new IOException("No response body (HTTP " + code + ")");
            }
            String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            if (code >= 400 || resp == null || resp.trim().equals("null") || resp.trim().isEmpty()) {
                // não existe saldo -> inicializa
                updateBalance(sess, defaultBalance);
                return defaultBalance;
            }
            try {
                return Integer.parseInt(resp.trim());
            } catch (NumberFormatException e) {
                updateBalance(sess, defaultBalance);
                return defaultBalance;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return defaultBalance;
        }
    }

    /**
     * Atualiza o saldo do utilizador no Realtime Database.
     */
    public void updateBalance(SessionManager.Session sess, int newBalance) {
        if (sess == null || sess.idToken == null || sess.idToken.isEmpty()
                || DATABASE_URL == null || DATABASE_URL.contains("YOUR-RTDATABASE-URL")) {
            return;
        }
        try {
            String path = "users/" + safe(sess.localId) + "/balance.json?auth="
                    + URLEncoder.encode(sess.idToken, StandardCharsets.UTF_8);
            String body = String.valueOf(newBalance);
            URL url = new URL(DATABASE_URL + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(out.length);
            conn.connect();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }
            conn.getResponseCode(); // força o envio
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Regista uma ronda de jogo no caminho:
     *   gameLogs/{localId}/{timestamp}.json
     */
    public void logGame(SessionManager.Session sess,
                        int oldBalance,
                        int newBalance,
                        int bet,
                        String resultText) {
        if (sess == null || sess.idToken == null || sess.idToken.isEmpty()
                || DATABASE_URL == null || DATABASE_URL.contains("YOUR-RTDATABASE-URL")) {
            return;
        }
        try {
            String path = "gameLogs/" + safe(sess.localId) + "/" + System.currentTimeMillis()
                    + ".json?auth=" + URLEncoder.encode(sess.idToken, StandardCharsets.UTF_8);

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"email\":\"").append(esc(sess.email)).append("\",");
            sb.append("\"oldBalance\":").append(oldBalance).append(",");
            sb.append("\"newBalance\":").append(newBalance).append(",");
            sb.append("\"bet\":").append(bet).append(",");
            sb.append("\"result\":\"").append(esc(resultText)).append("\"");
            sb.append("}");

            String body = sb.toString();

            URL url = new URL(DATABASE_URL + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(out.length);
            conn.connect();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }
            conn.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Torna ids seguros para serem usados em paths do Firebase.
     */
    private static String safe(String id) {
        if (id == null) return "";
        return id.replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace("/", "_");
    }
}
