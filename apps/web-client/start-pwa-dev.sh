#!/bin/bash
echo "🚀 Запуск мессенджера Connect в режиме разработки PWA"

# 1. Автоматическое подключение к телефону по Wi-Fi (порт замените на актуальный)
echo "📱 Подключаюсь к Android..."
adb connect 192.168.1.43:38997

# 2. Очистка старых и создание актуальных reverse-портов
adb reverse --remove-all
adb reverse tcp:5174 tcp:5174
adb reverse tcp:8080 tcp:8080
echo "🔗 Порты 5174 (Vue) и 8080 (Бэкенд) успешно проброшены по Wi-Fi!"

# 3. Запуск превью сервера
echo "💻 Запускаю Vue dev сервер..."
pnpm run build && pnpm run preview
