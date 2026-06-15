# MathUp Android

Образовательное приложение для подготовки к ОГЭ/ЕГЭ по математике на Android. Игровой формат с персонажами-математиками, системой прогресса и офлайн-доступом.

[![Google Play](https://img.shields.io/badge/Google%20Play-Download-green?logo=google-play)](https://play.google.com/store/apps/details?id=com.feofanova.mathup)

## ✨ Возможности

- База задач по всем темам ОГЭ и ЕГЭ
- Персонажи-математики с уникальными историями
- Статистика и система достижений
- Полностью офлайн — Room Database
- Material Design 3

## 🛠 Стек

- **Kotlin**
- **Jetpack Compose** — UI
- **Room** — локальная БД
- **Hilt** — Dependency Injection
- **Firebase** — аналитика и Crashlytics
- Архитектура: Clean Architecture (Domain / Data / UI)

## 📱 Скриншоты

> Добавь 2–3 скриншота из Google Play

## 🚀 Сборка

```bash
git clone https://github.com/FeofanovIvan/mathup-android.git
cd mathup-android
./gradlew assembleDebug
```

Требования: Android Studio Hedgehog+, minSdk 26

> ⚠️ `google-services.json` и `*.jks` исключены из репозитория.

## 📄 Лицензия

MIT © Ivan Feofanov
