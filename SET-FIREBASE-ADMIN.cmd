@echo off
if "%~1"=="" (
 echo Usage: SET-FIREBASE-ADMIN.cmd FIREBASE_USER_UID
 echo This explicitly grants administrator access to that Firebase user.
 exit /b 1
)
cd /d "%~dp0agristore-backend"
if exist firebase.local.cmd call firebase.local.cmd
java -Djava.net.preferIPv4Stack=true -jar agristore-backend.jar --grant-admin "%~1"
