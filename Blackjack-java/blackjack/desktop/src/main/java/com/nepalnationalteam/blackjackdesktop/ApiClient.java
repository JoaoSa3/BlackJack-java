package com.nepalnationalteam.blackjackdesktop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.Gson;

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
    private final Gson gson = new Gson();

    public ApiClient(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
    }

    /** Exceção personalizada para erros HTTP que inclui o status code. */
    public static class HttpException extends IOException {
        private final int statusCode;
        public HttpException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        public int getStatusCode() {
            return statusCode;
        }
    }

    /** GET /users/{id} */
    public UserProfile getUser(String id) throws IOException {
    String path = "/users/" + encode(id);
    String json;

    try {
        json = request("GET", path, null);
    } catch (HttpException ex) {
        // Trata o erro 404 (Não Encontrado) como um caso esperado: o utilizador não existe.
        if (ex.getStatusCode() == 404) {
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
        UserProfile userToCreate = new UserProfile(id, displayName, wallet);
        String body = gson.toJson(userToCreate);
        String json = request("POST", path, body);
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
        String body = gson.toJson(Map.of("wallet", wallet));
        request("PUT", path, body);
    }

    // ============================================================
    //                      HTTP helper
    // ============================================================

    private String request(String method, String path, String body) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000); // 5 segundos para conectar
        conn.setReadTimeout(10000); // 10 segundos para ler a resposta
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
            throw new HttpException("HTTP " + code + " from " + path + ": " + resp, code);
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
        return gson.fromJson(json, UserProfile.class);
    }

    // ============================================================
    //                 helpers para strings / ids
    // ============================================================

    private String encode(String id) {
        if (id == null) return "";
        // Usa o standard para URL encoding, garantindo compatibilidade com o servidor (encodeURIComponent)
        return URLEncoder.encode(id, StandardCharsets.UTF_8);
    }
}
