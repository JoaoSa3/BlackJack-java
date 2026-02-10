/**
 * Users API - agora ligada diretamente ao Firebase Realtime Database.
 *
 * Esta API expõe endpoints simples de utilizadores, pensados para serem
 * testados com o Postman:
 *
 *   - GET    /users/:id          -> lê um utilizador
 *   - GET    /users              -> lista todos os utilizadores
 *   - POST   /users              -> cria um utilizador
 *   - PUT    /users/:id/wallet   -> atualiza apenas o saldo (wallet)
 *   - DELETE /users/:id          -> remove um utilizador
 *
 * Internamente, cada pedido faz uma chamada HTTP REST ao Realtime Database,
 * usando a DATABASE_URL definida em:
 *
 *   ../NepalNationalTeamBlackjack_Casino_Maven_SERVICEACCOUNT/src/main/resources/serviceAccountKey.json
 *
 * O ficheiro serviceAccountKey.json deve ter o formato:
 *   { "apiKey": "...", "databaseUrl": "https://xxxxxx-default-rtdb.europe-west1.firebasedatabase.app/" }
 */

const express = require("express");
const cors = require("cors");
const fs = require("fs");
const path = require("path");
const http = require("http");
const https = require("https");

const app = express();
const PORT = 3000;

// ---------------------------------------------------------
//   Carregar configuração do Firebase (databaseUrl)
// ---------------------------------------------------------
const CONFIG_PATH = path.join(__dirname, "../desktop/src/main/resources/serviceAccountKey.json");

let DATABASE_URL = null;

try {
  const raw = fs.readFileSync(CONFIG_PATH, "utf8");
  const cfg = JSON.parse(raw);
  if (!cfg.databaseUrl) {
    throw new Error("databaseUrl não encontrado em serviceAccountKey.json");
  }
  DATABASE_URL = cfg.databaseUrl.endsWith("/")
    ? cfg.databaseUrl
    : cfg.databaseUrl + "/";
  console.log("[Firebase] DATABASE_URL =", DATABASE_URL);
} catch (err) {
  console.error(
    "[Firebase] Erro a carregar serviceAccountKey.json:",
    err.message
  );
  console.error("Caminho esperado:", CONFIG_PATH);
  console.error(
    "Garante que o ficheiro existe e contém o campo databaseUrl correto."
  );
  process.exit(1);
}

// ---------------------------------------------------------
//   Helper para fazer pedidos ao Realtime Database
// ---------------------------------------------------------
/**
 * Faz um pedido HTTP ao Firebase Realtime Database.
 *
 * @param {"GET"|"PUT"|"POST"|"PATCH"|"DELETE"} method
 * @param {string} path Ex: "users/uid123.json"
 * @param {object|null} body Dados a enviar (serão serializados em JSON)
 * @returns {Promise<any>} Resposta JSON já convertida (ou null)
 */
function firebaseRequest(method, path, body) {
  return new Promise((resolve, reject) => {
    const url = new URL(DATABASE_URL + path);
    const isHttps = url.protocol === "https:";
    const lib = isHttps ? https : http;

    const options = {
      method,
      headers: {}
    };

    let payload = null;
    if (body != null) {
      payload = JSON.stringify(body);
      options.headers["Content-Type"] = "application/json; charset=utf-8";
      options.headers["Content-Length"] = Buffer.byteLength(payload);
    }

    const req = lib.request(url, options, (res) => {
      let data = "";
      res.on("data", (chunk) => {
        data += chunk.toString("utf8");
      });
      res.on("end", () => {
        if (res.statusCode >= 400) {
          return reject(
            new Error(`Firebase ${res.statusCode}: ${data || "<sem corpo>"}`)
          );
        }
        if (!data) {
          return resolve(null);
        }
        try {
          const json = JSON.parse(data);
          resolve(json);
        } catch (e) {
          // Caso o Firebase devolva algo que não seja JSON (raro)
          resolve(data);
        }
      });
    });

    req.on("error", (err) => {
      reject(err);
    });

    if (payload) {
      req.write(payload);
    }
    req.end();
  });
}

// ---------------------------------------------------------
//   Middleware base
// ---------------------------------------------------------
app.use(cors());
app.use(express.json());

// Health-check simples (opcional)
app.get("/", (req, res) => {
  res.json({ ok: true, message: "Users API ligada ao Firebase" });
});

// ---------------------------------------------------------
//   Endpoints de utilizadores
// ---------------------------------------------------------

// GET /users -> devolve todos os utilizadores
app.get("/users", async (req, res) => {
  try {
    const data = await firebaseRequest("GET", "users.json", null);
    res.json(data || {});
  } catch (err) {
    console.error("GET /users erro:", err.message);
    res.status(500).json({ error: "Falha ao ler utilizadores", details: err.message });
  }
});

// GET /users/:id -> devolve um utilizador específico
app.get("/users/:id", async (req, res) => {
  const id = req.params.id;
  try {
    const data = await firebaseRequest(
      "GET",
      `users/${encodeURIComponent(id)}.json`,
      null
    );
    if (data === null || data === undefined) {
      return res.status(404).json({ error: "User not found" });
    }
    // Garante que o campo id existe também no objeto
    if (!data.id) {
      data.id = id;
    }
    res.json(data);
  } catch (err) {
    console.error(`GET /users/${id} erro:`, err.message);
    res
      .status(500)
      .json({ error: "Falha ao ler utilizador", details: err.message });
  }
});

// POST /users -> cria um novo utilizador
app.post("/users", async (req, res) => {
  const { id, displayName, wallet } = req.body || {};

  if (!id || typeof id !== "string" || !id.trim()) {
    return res
      .status(400)
      .json({ error: "Campo 'id' é obrigatório (string não vazia)" });
  }

  try {
    // Verificar se já existe
    const existing = await firebaseRequest(
      "GET",
      `users/${encodeURIComponent(id)}.json`,
      null
    );
    if (existing && existing !== null) {
      return res.status(409).json({ error: "User already exists" });
    }

    const w = Number(wallet);
    const user = {
      id,
      displayName: displayName || "",
      wallet: Number.isFinite(w) ? w : 0
    };

    await firebaseRequest(
      "PUT",
      `users/${encodeURIComponent(id)}.json`,
      user
    );

    return res.status(201).json(user);
  } catch (err) {
    console.error("POST /users erro:", err.message);
    res
      .status(500)
      .json({ error: "Falha ao criar utilizador", details: err.message });
  }
});

// PUT /users/:id/wallet -> atualiza apenas o saldo do utilizador
// (compatível com o ApiClient usado na aplicação Java)
app.put("/users/:id/wallet", async (req, res) => {
  const id = req.params.id;
  const { wallet } = req.body || {};

  if (wallet === undefined || wallet === null) {
    return res.status(400).json({ error: "Campo 'wallet' é obrigatório" });
  }

  const w = Number(wallet);
  if (!Number.isFinite(w)) {
    return res.status(400).json({ error: "Invalid wallet value" });
  }

  try {
    // Verificar se o utilizador existe
    const existing = await firebaseRequest(
      "GET",
      `users/${encodeURIComponent(id)}.json`,
      null
    );
    if (!existing) {
      return res.status(404).json({ error: "User not found" });
    }

    // Atualizar apenas o campo wallet usando PATCH
    await firebaseRequest(
      "PATCH",
      `users/${encodeURIComponent(id)}.json`,
      { wallet: w }
    );

    const updated = { ...(existing || {}), wallet: w, id };
    return res.json(updated);
  } catch (err) {
    console.error(`PUT /users/${id}/wallet erro:`, err.message);
    res
      .status(500)
      .json({ error: "Falha ao atualizar wallet", details: err.message });
  }
});

// DELETE /users/:id -> remove um utilizador (quarto tipo de request)
app.delete("/users/:id", async (req, res) => {
  const id = req.params.id;
  try {
    // Verifica se existe antes de apagar (para responder 404 corretamente)
    const existing = await firebaseRequest(
      "GET",
      `users/${encodeURIComponent(id)}.json`,
      null
    );
    if (!existing) {
      return res.status(404).json({ error: "User not found" });
    }

    await firebaseRequest(
      "DELETE",
      `users/${encodeURIComponent(id)}.json`,
      null
    );

    return res.status(204).send(); // Sem conteúdo
  } catch (err) {
    console.error(`DELETE /users/${id} erro:`, err.message);
    res
      .status(500)
      .json({ error: "Falha ao apagar utilizador", details: err.message });
  }
});

// ---------------------------------------------------------
//   Arrancar servidor
// ---------------------------------------------------------
app.listen(PORT, () => {
  console.log(`Users API a correr em http://localhost:${PORT}`);
});
