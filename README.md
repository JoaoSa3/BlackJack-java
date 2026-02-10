# Blackjack – Desktop App + Users API

A Java Swing Blackjack game with Firebase integration and a Node.js REST API for user management.

---

## Features

✅ **Blackjack Game** – Play against the dealer with Hit, Stand, Double Down  
✅ **User Authentication** – Register and login with Firebase  
✅ **Real-time Wallet** – Balance synced to Firebase Realtime Database  
✅ **REST API** – Full CRUD operations on users via Postman  
✅ **Session Management** – Persistent user sessions  

---

## Project Structure

```
blackjack/
├── desktop/              # Java Swing desktop app (Maven)
│   ├── src/main/java/com/nepalnationalteam/blackjackdesktop/
│   ├── src/main/resources/serviceAccountKey.json
│   ├── pom.xml
│   └── user_session.json
├── api/                  # REST API (Node.js + Express)
│   ├── server.js
│   └── package.json
└── postman/              # Postman collection
    └── BlackjackUsersAPI.postman_collection.json

doc/
├── requirements.md
└── blackjack_diagram.md
```

---

## How to Run

### Desktop App (Java/Maven)

**Requirements:** Java 17+, Maven 3.6+

```sh
cd blackjack/desktop
mvn clean package
java -jar target/blackjack-casino-desktop-1.0-SNAPSHOT.jar
```

### Users API (Node.js)

**Requirements:** Node.js 14+

```sh
cd blackjack/api
npm install
npm start
```

API runs on `http://localhost:3000`.

---

## Test with Postman

Import the collection: `blackjack/postman/BlackjackUsersAPI.postman_collection.json`

Endpoints:
- `GET /users` – List all users
- `GET /users/:id` – Get user by ID
- `POST /users` – Create new user
- `PUT /users/:id/wallet` – Update wallet balance
- `DELETE /users/:id` – Delete user

---

## Firebase Configuration

Add your Firebase credentials to:
```
blackjack/desktop/src/main/resources/serviceAccountKey.json
```

```json
{
  "apiKey": "YOUR_API_KEY",
  "databaseUrl": "https://your-project.firebasedatabase.app/"
}
```

---

## Notes

- Desktop app works independently (Firebase-based)
- API is optional (for testing/demos)
- User session saved in `blackjack/desktop/user_session.json`
- User balance persisted in Firebase Realtime Database