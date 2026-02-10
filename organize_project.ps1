# 1. Remover a pasta src/ (versão antiga)
Write-Host "Removendo a pasta src/ (versão antiga)..." -ForegroundColor Yellow
if (Test-Path "src") {
    Remove-Item -Recurse -Force "src"
    Write-Host "✓ Pasta src/ removida com sucesso." -ForegroundColor Green
} else {
    Write-Host "✓ Pasta src/ não encontrada. Nada para remover." -ForegroundColor Green
}

# 2. Atualizar o README.md
Write-Host "Atualizando o README.md..." -ForegroundColor Yellow
$readmePath = "README.md"
$updatedReadmeContent = @"
# Nepal National Team Blackjack – Desktop App + Users API

This project contains:

- A **Java Swing desktop Blackjack game** that talks directly to **Firebase Realtime Database**.
- A small **Users REST API (Node + Express)** used only to demonstrate the 4 basic HTTP operations (GET, POST, PUT, DELETE) on a `/users` collection via Postman.

The desktop app can run **without** the local API (it uses Firebase directly).  
The local API is only for showing HTTP requests with Postman.

---

## Project structure

```
blackjack/
  desktop/         # Main Java desktop app (Maven)
    src/main/java/com/nepalnationalteam/blackjackdesktop/
    src/main/resources/serviceAccountKey.json
    pom.xml
    user_session.json
    target/
  api/             # Users API (Node.js + Express)
    server.js
    package.json
    node_modules/
  postman/         # Postman collection for testing the API
    BlackjackUsersAPI.postman_collection.json
doc/
  requirements.md
  blackjack_diagram.md
  diagram_short_version.png
  diagram_complete_version.png
  Black Jack Game Sprint 2.pptx
```

---

## How to run

### Desktop App (Java/Maven)

1. Navigate to the `blackjack/desktop` folder:
   ```sh
   cd blackjack/desktop
   ```

2. Compile and run the JAR:
   ```sh
   mvn clean package
   java -jar target/blackjack-casino-desktop-1.0-SNAPSHOT.jar
   ```

### Users API (Node.js)

1. Navigate to the `blackjack/api` folder:
   ```sh
   cd blackjack/api
   ```

2. Install dependencies and start the server:
   ```sh
   npm install
   npm start
   ```

The API will be available at `http://localhost:3000`.

### Test with Postman

Import the Postman collection located at:
[`blackjack/postman/BlackjackUsersAPI.postman_collection.json`](blackjack/postman/BlackjackUsersAPI.postman_collection.json).

---

## Notes

- Only the Maven version of the desktop app is maintained.
- User balance is saved in Firebase Realtime Database.
- The user session file is located at:
  [`blackjack/desktop/user_session.json`](blackjack/desktop/user_session.json).
"@

Set-Content -Path $readmePath -Value $updatedReadmeContent -Force
Write-Host "✓ README.md atualizado com sucesso." -ForegroundColor Green

# 3. Adicionar alterações ao Git
Write-Host "Adicionando alterações ao Git..." -ForegroundColor Yellow
git add .
Write-Host "✓ Alterações adicionadas." -ForegroundColor Green

# 4. Criar o commit
Write-Host "Criando commit..." -ForegroundColor Yellow
git commit -m "Organização do projeto: remoção de versões antigas, atualização do README e padronização para Maven."
Write-Host "✓ Commit criado com sucesso." -ForegroundColor Green

# 5. Fazer o push para o repositório remoto
Write-Host "Enviando alterações para o repositório remoto..." -ForegroundColor Yellow
git push origin main
Write-Host "✓ Alterações enviadas para o GitHub." -ForegroundColor Green

Write-Host "`nProcesso concluído com sucesso! 🎉" -ForegroundColor Green