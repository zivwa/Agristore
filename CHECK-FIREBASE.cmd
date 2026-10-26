@echo off
cd /d "%~dp0agristore-backend"
if exist firebase.local.cmd call firebase.local.cmd
java -Djava.net.preferIPv4Stack=true -jar agristore-backend.jar --check-firebase
