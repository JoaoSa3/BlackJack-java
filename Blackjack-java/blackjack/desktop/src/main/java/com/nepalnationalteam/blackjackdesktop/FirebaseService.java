
package com.nepalnationalteam.blackjackdesktop;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class FirebaseService {
    public static final String API_KEY;
    public static final String DATABASE_URL;

    private static final Gson gson = new Gson();

    static {
        // Carrega a configuração a partir do ficheiro de resources.
        // Se o ficheiro ou as chaves não existirem, a aplicação não deve arrancar.
        try (InputStream is = FirebaseService.class.getResourceAsStream("/serviceAccountKey.json")) {
            if (is == null) {
                throw new IllegalStateException("Ficheiro de configuração 'serviceAccountKey.json' não encontrado nos resources.");
            }

            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Map<String, String> config = gson.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            API_KEY = config.get("apiKey");
            String dbUrl = config.get("databaseUrl");

            if (API_KEY == null || API_KEY.isEmpty() || dbUrl == null || dbUrl.isEmpty()) {
                throw new IllegalStateException("'apiKey' ou 'databaseUrl' em falta no ficheiro 'serviceAccountKey.json'.");
            }

            DATABASE_URL = dbUrl.endsWith("/") ? dbUrl : dbUrl + "/";

        } catch (IOException e) {
            // Envolve a exceção para indicar claramente que a inicialização falhou.
            throw new RuntimeException("Falha crítica ao carregar a configuração do Firebase.", e);
        }
    }

    private static HttpURLConnection createConnection(String urlStr, String method, int connectTimeout, int readTimeout) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        return conn;
    }

    private static String httpWithBody(String method, String urlStr, String body) throws IOException {
        HttpURLConnection conn = createConnection(urlStr, method, 5000, 10000);
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
        String payload = gson.toJson(Map.of(
                "email", email,
                "password", password,
                "returnSecureToken", true
        ));
        String resp = post(endpoint, payload);
        return gson.fromJson(resp, AuthResult.class);
    }

    public AuthResult signUp(String email, String password, String displayName) throws IOException {
        String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + API_KEY;
        String payload = gson.toJson(Map.of("email", email, "password", password, "returnSecureToken", true));
        String resp = post(endpoint, payload);
        AuthResult r = gson.fromJson(resp, AuthResult.class);
        r.displayName = displayName;

        if (DATABASE_URL.startsWith("http")) {
            String userPath = DATABASE_URL + "users/" + r.localId + ".json?auth=" + r.idToken;
            String body = gson.toJson(Map.of("email", email, "displayName", displayName, "createdAt", System.currentTimeMillis()));
            put(userPath, body);
        }
        return r;
    }

    public String refreshIdToken(String refreshToken) throws IOException {
        String endpoint = "https://securetoken.googleapis.com/v1/token?key=" + API_KEY;
        String payload = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        HttpURLConnection conn = createConnection(endpoint, "POST", 5000, 10000);
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
        Map<String, String> result = gson.fromJson(resp, new TypeToken<Map<String, String>>() {}.getType());
        return result.get("id_token");
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
            HttpURLConnection conn = createConnection(DATABASE_URL + path, "GET", 5000, 10000);
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
            HttpURLConnection conn = createConnection(DATABASE_URL + path, "PUT", 5000, 5000);
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

            Map<String, Object> logData = Map.of(
                "email", sess.email,
                "oldBalance", oldBalance,
                "newBalance", newBalance,
                "bet", bet,
                "result", resultText
            );

            String body = gson.toJson(logData);
            HttpURLConnection conn = createConnection(DATABASE_URL + path, "PUT", 5000, 5000);
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
