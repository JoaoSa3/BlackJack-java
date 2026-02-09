# Nepal National Team Blackjack – Desktop App + Users API

This project contains:

- A **Java Swing desktop Blackjack game** that talks directly to **Firebase Realtime Database**.
- A small **Users REST API (Node + Express)** used only to demonstrate the 4 basic HTTP operations (GET, POST, PUT, DELETE) on a `/users` collection via Postman.

The desktop app can run **without** the local API (it uses Firebase directly).  
The local API is only for showing HTTP requests with Postman.

---

## 1. Project structure (root folder)

Root folder (e.g. `iso-2025-proyect-nepal-national-team-main/`):

- **`README.md`**  
  This file: project description, structure, and how to run things.

- **`blackjack/`**  
  Main ISO project folder. Contains:
  - The Node Users API (`blackjack/api`)
  - The Maven-based desktop app (`blackjack/desktop`)
  - The Postman collection (`blackjack/postman`)
  - A helper start script (`blackjack/start_all.bat`)

- **`src/`**  
  Original / non-Maven version of the desktop app (kept for reference).

- **`doc/`**  
  Documentation, requirements, diagrams and slides.

---

## 2. The `blackjack/` folder

### 2.1. Helper files

- **`blackjack/start_all.bat`**  
  Windows script that:
  1. Goes into `blackjack/api`, installs Node dependencies (if missing) and starts the Users API on port **3000**.
  2. Goes into `blackjack/desktop`, runs `mvn clean package`, and then runs the generated desktop JAR.

- **`blackjack/user_session.json`**  
  Small JSON file used to remember the last logged user/session (email, display name, etc.).

---

### 2.2. Users API – `blackjack/api/`

This folder contains the **Node.js + Express REST API** for `/users`.

- **`blackjack/api/package.json`**  
  Node project manifest:
  - Declares project name (`users-api`).
  - Declares dependencies: `express`, `cors`.
  - Defines the script:
    - `"start": "node server.js"` → used by `npm start`.

- **`blackjack/api/server.js`**  
  **Main Express server** for the Users API:
  - Starts a server on `http://localhost:3000`.
  - Reads the Firebase Realtime Database URL from  
    `../desktop/src/main/resources/serviceAccountKey.json` (field `databaseUrl`).
  - Exposes REST endpoints for users:
    - `GET /users/:id` – read a single user.
    - `GET /users` – list all users.
    - `POST /users` – create a new user.
    - `PUT /users/:id/wallet` – update only the user’s wallet balance.
    - `DELETE /users/:id` – delete a user.
  - All operations forward the request to **Firebase Realtime Database** via HTTP (no local DB).

- **`blackjack/api/users.json`**  
  Example JSON structure for a user.  
  It’s just a sample file; the real API logic uses Firebase, not this file, as storage.

- **`blackjack/api/node_modules/`**  
  Auto-generated folder with all Node dependencies installed by `npm install`.  
  You don’t edit this by hand.

---

### 2.3. Desktop app (Maven) – `blackjack/desktop/`

This is the **main Java desktop app**, managed by Maven.

- **`blackjack/desktop/pom.xml`**  
  Maven configuration:
  - Defines `groupId`, `artifactId`, version.
  - Sets Java version (17).
  - Configures `maven-jar-plugin` so that the final JAR is executable with:
    - `mainClass` = `com.nepalnationalteam.blackjackdesktop.StartScreenFrame`.

- **`blackjack/desktop/src/main/java/com/nepalnationalteam/blackjackdesktop/`**  
  Main Java source code for the desktop client:

  - **`StartScreenFrame.java`**  
    Start screen window:
    - Shows project title.
    - Buttons to go to Login, Register, etc.

  - **`LoginFrame.java`**  
    Login window:
    - Email and password fields.
    - Calls `FirebaseService` to authenticate the user in Firebase.

  - **`RegisterFrame.java`**  
    Registration window:
    - Fields for email, password, confirm password, display name, and ID.
    - Creates the user in Firebase (auth + data in Realtime DB).

  - **`LobbyFrame.java`**  
    Lobby screen:
    - Shows user info (display name, wallet).
    - Buttons to go to the Blackjack table, reload wallet, logout, etc.

  - **`GameFrame.java`**  
    Main **Blackjack table**:
    - Swing GUI with the green table background.
    - Shows player and dealer cards.
    - Buttons: “Hit”, “Stand”, “Double”, “New game”, etc.
    - Uses `Deck`, `Hand`, and `Card` classes for game logic.

  - **`Card.java`**  
    Represents a **single card** (rank, suit, value and optionally image).

  - **`Deck.java`**  
    Represents the **deck of cards**:
    - Creates the deck.
    - Shuffles.
    - Deals cards to players.

  - **`Hand.java`**  
    Represents the **hand** of a player or the dealer:
    - Stores the cards in that hand.
    - Calculates the Blackjack score (with proper Ace handling).

  - **`FirebaseService.java`**  
    Wrapper for all HTTP calls to **Firebase**:
    - Uses `apiKey` and `databaseUrl` from `serviceAccountKey.json`.
    - Handles login, register, reading/writing user data and wallet.

  - **`SessionManager.java`**  
    Manages the **current user session**:
    - Stores logged user information.
    - Saves/loads session data from `user_session.json`.

- **`blackjack/desktop/src/main/resources/`**

  - **`serviceAccountKey.json`**  
    Configuration file with at least:
    ```json
    {
      "apiKey": "YOUR_FIREBASE_API_KEY",
      "databaseUrl": "https://your-project-default-rtdb.europe-west1.firebasedatabase.app/"
    }
    ```
    - `apiKey` is used by the desktop client for Firebase auth.
    - `databaseUrl` is used by both the desktop app and the Node API to reach your Realtime DB.

  - **`table_green.png`**  
    Green table background image used in the Blackjack UI.

- **`blackjack/desktop/user_session.json`**  
  Session file used by the Maven version of the app to remember the last logged user.

- **`blackjack/desktop/target/`**  
  Maven build output:
  - Contains the compiled JAR, e.g.  
    `blackjack-casino-desktop-1.0-SNAPSHOT.jar`.

---

### 2.4. Postman collection – `blackjack/postman/`

- **`BlackjackUsersAPI.postman_collection.json`**  
  Postman collection prepared to test the Users API:
  - `GET /users/:id`
  - `GET /users`
  - `POST /users`
  - `PUT /users/:id/wallet`
  - `DELETE /users/:id`
  Base URL: `http://localhost:3000`.

---

## 3. `src/` – legacy / non-Maven version

- **`src/com/nepalnationalteam/blackjack/`**  
  Original non-Maven version of the desktop app.
  Includes, among others:

  - **`Main.java`**  
    Simple `main` method that launches the start screen.  
    This version is kept only for reference; the real execution for the project uses the Maven version in `blackjack/desktop`.

- **`src/com/nepalnationalteam/blackjackdesktop/`**  
  Alternative package structure similar to the Maven one.  
  Also kept as support / legacy code.

---

## 4. `doc/` folder

- **`doc/requirements.md`**  
  Functional and non-functional requirements of the Blackjack project.

- **`doc/blackjack_diagram.md`**  
  Text explanation of the class diagram and responsibilities.

- **`doc/diagram_short_version.png` / `doc/diagram_complete_version.png`**  
  UML class diagrams (short and complete versions).

- **`doc/Black Jack Game Sprint 2.pptx`**  
  PowerPoint presentation used to describe the project (Sprint 2).

---

## 5. How to run the Users API

> ⚠️ You **need Node.js and npm** installed on the machine to run the API.  
> On a computer without Node, you **cannot** start the API there.  
> In that cas
