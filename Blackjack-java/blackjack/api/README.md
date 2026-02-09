# Blackjack Desktop - Nepal National Team Project

Este é um projeto full-stack de um jogo de Blackjack (21), desenvolvido por **João** para demonstrar competências em **Java (Swing)**, **Node.js** e integração com **Firebase Realtime Database**.

O sistema simula uma mesa de casino onde o utilizador joga contra o Dealer e bots automatizados, com persistência de saldo e histórico de jogadas na nuvem.

## 🚀 Funcionalidades

- **Cliente Desktop (Java):** Interface gráfica rica usando Swing, com renderização personalizada de cartas e mesa.
- **API Backend (Node.js):** API REST que gere utilizadores e comunica de forma segura com o Firebase.
- **Persistência de Dados:** O saldo (wallet) e os logs de jogo são guardados em tempo real no Firebase.
- **Bots Inteligentes:** 3 bots jogam automaticamente na mesa seguindo regras básicas de Blackjack.

## 🛠️ Tecnologias Usadas

- **Frontend/Desktop:** Java 17+, Maven, Swing.
- **Backend:** Node.js, Express.
- **Base de Dados:** Firebase Realtime Database.

## 📂 Estrutura do Projeto

```text
/api        -> Servidor Node.js (Endpoints REST para gestão de users)
/desktop    -> Aplicação Java Maven (Lógica do jogo e UI)
```

## ⚙️ Como Executar

### Pré-requisitos
1. **Java JDK 17** ou superior instalado.
2. **Maven** instalado e configurado no PATH.
3. **Node.js** instalado.

### Instalação e Arranque Rápido

O projeto inclui um script automatizado para Windows. Basta executar o ficheiro na raiz desta pasta:

`start_all.bat`

Este script irá:
1. Instalar as dependências da API (`npm install`) se necessário.
2. Iniciar o servidor local na porta 3000.
3. Compilar o projeto Java (`mvn clean package`).
4. Abrir a janela do jogo.

### Execução Manual

**0. Configurar a API (apenas na primeira vez):**

Antes de iniciar a API, é necessário configurar a ligação ao Firebase.
1. Crie um ficheiro chamado `.env` na pasta `/api`.
2. Adicione a seguinte linha a esse ficheiro, substituindo pelo URL da sua Realtime Database:

```
DATABASE_URL="https://nepal-national-team-default-rtdb.europe-west1.firebasedatabase.app/"
```

**1. Iniciar a API:**
```bash
cd api
npm install
npm start
```

**2. Iniciar o Jogo (noutro terminal):**
```bash
cd desktop
mvn clean package
java -jar target/blackjackdesktop-1.0-SNAPSHOT.jar
```

---
Desenvolvido por **João**.