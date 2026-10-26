@echo off
cd /d "%~dp0"
call mvn -B test package
