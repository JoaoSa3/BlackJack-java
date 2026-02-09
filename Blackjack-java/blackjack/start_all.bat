@echo off
echo.
echo =================================================
echo  Blackjack Project - Full Stack Starter
echo =================================================
echo.

echo [1/4] A iniciar o servidor da API Node.js...
cd api

REM Instala as dependencias apenas se a pasta node_modules nao existir
if not exist "node_modules" (
    echo      A instalar dependencias (npm install)...
    call npm install
)

start "Blackjack API Server" npm start
echo.
echo [2/4] Servidor da API iniciado numa nova janela.
echo [3/4] A compilar a aplicacao Java Desktop (Maven)...
cd ../desktop
call mvn clean package
echo.
echo [4/4] A iniciar o jogo de Blackjack...
java -jar target/blackjackdesktop-1.0-SNAPSHOT.jar