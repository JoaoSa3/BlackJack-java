package com.nepalnationalteam.blackjackdesktop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Cliente simples para a Users API.
 *
 * A ideia é ter uma API REST (por exemplo Node + Firebase) com endpoints do género:
 *
 *   GET  /users/{id}
 *   POST /users
 *   PUT  /users/{id}/wallet
 *
 * Esta classe não depende de nenhuma biblioteca externa de JSON. O parsing é feito
 * com métodos muito simples (extractString, extractInt) suficientes para o formato
 * de resposta esperado.
 *
 * Se a API não estiver a correr, estes métodos vão lançar IOException – o resto da
 * aplicação (mesa de blackjack) continua a funcionar porque o saldo "real"
 * é mantido no Firebase diretamente via FirebaseService.
 */
public class ApiClient {

    private final String baseUrl;

    public ApiClient(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
    }

    /** GET /users/{id} */
    public UserProfile getUser(String id) throws IOException {
    String path = "/users/" + encode(id);
    String json;

    try {
        // Try to call the API normally
        json = request("GET", path, null);
    } catch (IOException ex) {
        String msg = ex.getMessage();
        // If the server replies with HTTP 404 "user not found"
        // we treat it as "no user yet" => return null
        if (msg != null && msg.startsWith("HTTP 404")) {
            return null;
        }
        // Any other error (500, 400, etc.) is real and should propagate
        throw ex;
    }

    if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
        return null;
    }

    return parseUser(json);

}

    /** Cria utilizador com carteira inicial. */
    public UserProfile createUser(String id, String displayName, int wallet) throws IOException {
        String path = "/users";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(esc(id)).append("\",");
        sb.append("\"displayName\":\"").append(esc(displayName)).append("\",");
        sb.append("\"wallet\":").append(wallet);
        sb.append("}");
        String json = request("POST", path, sb.toString());
        return parseUser(json);
    }

    /**
     * Tenta obter o utilizador; se não existir, cria com carteira default.
     */
    public UserProfile getOrCreateUser(String id, String displayName, int defaultWallet) throws IOException {
        UserProfile u = getUser(id);
        if (u == null) {
            u = createUser(id, displayName, defaultWallet);
        }
        return u;
    }

    /** Atualiza apenas a carteira (wallet) do utilizador. */
    public void setWallet(String id, int wallet) throws IOException {
        String path = "/users/" + encode(id) + "/wallet";
        String body = "{\"wallet\":" + wallet + "}";
        request("PUT", path, body);
    }

    // ============================================================
    //                      HTTP helper
    // ============================================================

    private String request(String method, String path, String body) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");

        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(out.length);
            conn.connect();
            try (OutputStream os = conn.getOutputStream()) {
                os.write(out);
            }
        } else {
            conn.connect();
        }

        int code = conn.getResponseCode();
        InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
        String resp = readAll(is);

        if (code >= 400) {
            throw new IOException("HTTP " + code + " from " + path + ": " + resp);
        }
        return resp;
    }

    private String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    // ============================================================
    //                      JSON helpers simples
    // ============================================================

    private UserProfile parseUser(String json) {
        String id = extractString(json, "id");
        String displayName = extractString(json, "displayName");
        int wallet = extractInt(json, "wallet", 0);
        return new UserProfile(id, displayName, wallet);
    }

    private String extractString(String json, String key) {
        if (json == null) return null;
        String k = "\"" + key + "\":";
        int idx = json.indexOf(k);
        if (idx < 0) return null;
        int start = json.indexOf('"', idx + k.length());
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        String raw = json.substring(start + 1, end);
        return raw.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private int extractInt(String json, String key, int defaultValue) {
        if (json == null) return defaultValue;
        String k = "\"" + key + "\":";
        int idx = json.indexOf(k);
        if (idx < 0) return defaultValue;
        int start = idx + k.length();
        int end = start;
        while (end < json.length() && "0123456789-".indexOf(json.charAt(end)) >= 0) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ============================================================
    //                 helpers para strings / ids
    // ============================================================

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String encode(String id) {
        if (id == null) return "";
        // id seguro para paths (sem caracteres problemáticos)
        return id.replace(" ", "_")
                 .replace("/", "_")
                 .replace("?", "_")
                 .replace("#", "_");
    }
}
