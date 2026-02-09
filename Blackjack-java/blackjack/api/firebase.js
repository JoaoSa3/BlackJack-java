const http = require("http");
const https = require("https");

const DATABASE_URL = process.env.DATABASE_URL;

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
      headers: {},
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
          const error = new Error(`Firebase ${res.statusCode}: ${data || "<sem corpo>"}`);
          error.statusCode = res.statusCode;
          return reject(error);
        }
        if (!data || data.trim() === 'null') {
          return resolve(null);
        }
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          resolve(data); // Fallback for non-json response
        }
      });
    });

    req.on("error", reject);

    if (payload) req.write(payload);
    req.end();
  });
}

module.exports = { firebaseRequest };