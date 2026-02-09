@echo off
echo ==== Starting Users API on port 3000 ====
cd /d "%~dp0api"
IF NOT EXIST "node_modules" (
    echo First run: installing dependencies...
    npm install
)
start "Users API" cmd /k "npm start"

echo ==== Starting Java desktop app ====
cd /d "%~dp0desktop"
mvn clean package
if exist "target\blackjack-casino-desktop-1.0-SNAPSHOT.jar" (
    java -jar target\blackjack-casino-desktop-1.0-SNAPSHOT.jar
) else (
    echo ERRO: JAR nao encontrado em target\blackjack-casino-desktop-1.0-SNAPSHOT.jar
    pause
)
