@echo off
cd /d "%~dp0"
if exist firebase.local.cmd call firebase.local.cmd
java -Djava.net.preferIPv4Stack=true -jar agristore-backend.jar
