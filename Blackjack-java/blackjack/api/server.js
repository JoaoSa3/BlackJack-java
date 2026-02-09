/**
 * Users API - ligada ao Firebase Realtime Database via variáveis de ambiente.
 *
 * Esta API expõe endpoints simples de utilizadores, pensados para serem
 * testados com o Postman:
 *
 *   - GET    /users/:id
 *   - GET    /users
 *   - POST   /users
 *   - PUT    /users/:id/wallet
 *   - DELETE /users/:id
 *
 * Internamente, cada pedido faz uma chamada HTTP REST ao Realtime Database,
 * usando a DATABASE_URL definida num ficheiro de configuração `.env`.
 *
 * Para configurar, crie um ficheiro chamado `.env` nesta pasta (`/api`)
 * com o seguinte conteúdo, substituindo pelo seu URL do Firebase:
 *   DATABASE_URL="https://xxxxxx-default-rtdb.europe-west1.firebasedatabase.app/"
 */

const express = require("express");
const cors = require("cors");
require('dotenv').config();
const { firebaseRequest } = require('./firebase');

const app = express();
const PORT = 3000;

// ---------------------------------------------------------
//   Carregar configuração do Firebase (databaseUrl)
// ---------------------------------------------------------
let DATABASE_URL = process.env.DATABASE_URL;

if (!DATABASE_URL) {
  console.error(
    "[Firebase] Erro: A variável de ambiente DATABASE_URL não está definida."
  );
  console.error(
    "Crie um ficheiro .env na pasta /api ou defina a variável de ambiente no seu sistema."
  );
  process.exit(1);
}
if (!DATABASE_URL.endsWith('/')) {
  DATABASE_URL += '/';
}
console.log("[Firebase] DATABASE_URL =", DATABASE_URL);

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
app.get("/users", async (req, res, next) => {
  try {
    const data = await firebaseRequest("GET", "users.json", null);
    res.json(data || {});
  } catch (err) {
    next(err);
  }
});

// GET /users/:id -> devolve um utilizador específico
app.get("/users/:id", async (req, res, next) => {
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
    next(err);
  }
});

// POST /users -> cria um novo utilizador
app.post("/users", async (req, res, next) => {
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
    next(err);
  }
});

// PUT /users/:id/wallet -> atualiza apenas o saldo do utilizador
// (compatível com o ApiClient usado na aplicação Java)
app.put("/users/:id/wallet", async (req, res, next) => {
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
    next(err);
  }
});

// DELETE /users/:id -> remove um utilizador (quarto tipo de request)
app.delete("/users/:id", async (req, res, next) => {
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
    next(err);
  }
});

// ---------------------------------------------------------
//   Middleware de tratamento de erros (deve ser o último)
// ---------------------------------------------------------
app.use((err, req, res, next) => {
  // O status code pode vir do erro (ex: de uma falha no firebaseRequest)
  // ou ser um 500 genérico.
  const statusCode = err.statusCode || 500;

  console.error(`[${req.method} ${req.path}] Error ${statusCode}: ${err.message}`);

  res.status(statusCode).json({
    error: {
      message: err.message || 'Ocorreu um erro interno no servidor.',
    }
  });
});

// ---------------------------------------------------------
//   Arrancar servidor
// ---------------------------------------------------------

// Esta verificação impede que o servidor arranque quando o ficheiro é importado por testes
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Users API a correr em http://localhost:${PORT}`);
  });
}

// Exportar a app para ser usada nos testes
module.exports = app;
