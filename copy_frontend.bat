@echo off
rd /s /q vscode-extension/dist
mkdir vscode-extension/dist
robocopy frontend/dist vscode-extension/dist /e