# MathUp Android

MathUp — Android-приложение для подготовки к ОГЭ и ЕГЭ по математике. Приложение объединяет тематическую подготовку, пошаговое решение заданий, экзаменационный режим, справочные материалы, формулы, видео, статистику, игровой раздел и собственную математическую клавиатуру.

Основной фокус проекта — работа с учебным контентом в offline-first формате: данные загружаются из bundled assets или удалённого JSON-источника, сохраняются в локальные Room-базы и дальше используются приложением как основной источник истины.

## Возможности

- Подготовка по темам и блокам заданий.
- Пошаговое решение задач с сохранением прогресса.
- Экзаменационный режим с восстановлением незавершённой сессии.
- Проверка ответов через математический ввод.
- Собственная математическая клавиатура.
- Черновик для вычислений и набросков.
- Разделы со справочными материалами, формулами и видео.
- Поддержка профилей ОГЭ, базового и профильного ЕГЭ.
- Локальная статистика по заданиям и экзаменам.
- Игровой раздел с персонажами.
- Синхронизация учебного контента из Firebase Storage / Firestore.
- Работа с локальными данными после первичной загрузки.

## Технологии

| Область | Используется |
| --- | --- |
| Язык | Kotlin |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Асинхронность | Kotlin Coroutines |
| Состояние | ViewModel, Compose State, UiState |
| Локальное хранение | Room, DataStore Preferences, EncryptedSharedPreferences |
| Синхронизация | Firebase Storage, Firestore, JSON import, локальный sync metadata |
| Backend-сервисы | Firebase Auth, Firestore, Storage, Realtime Database, Crashlytics |
| Сборка | Gradle Kotlin DSL, KSP, R8, resource shrinking |

## Архитектура

Проект постепенно приведён к более поддерживаемой архитектуре без полной переписки MVP. Основные слои:

```text
app/src/main/java/com/feofanova/mathup/
  ui/
    components/      переиспользуемые Compose-компоненты
    navigation/      Navigation Compose graph
    screens/         экраны, ViewModel и экранные компоненты
  domain/
    repository/      контракты репозиториев
  data/
    local/           Room DAO, entity и базы учебного контента
    stats/           Room-база статистики и экзаменационных сессий
    characters/      данные игрового раздела
    remote/          модели JSON-контрактов
    repository/      Room/Firebase реализации репозиториев и sync
  sync/              use case для первичной загрузки и обновления контента
  sound/             SoundPlayer и настройки звука
```

Базовый поток данных:

```text
Firebase / assets JSON
        -> sync/use case
        -> Room
        -> Repository
        -> ViewModel
        -> Compose UI
```

Room используется как локальный источник истины для учебного контента, статистики и экзаменационных сессий.

## Архитектурные улучшения

- Первичная загрузка bundled-контента вынесена из `MainActivity` в `InitialContentSyncUseCase`.
- Проверка версий контента и обновление баз вынесены из UI в `ContentUpdateChecker` и `ContentUpdateUseCase`.
- Добавлена модель `SyncMetadata` для хранения версий локального контента.
- Добавлены репозиторные контракты:
  - `TaskRepository`
  - `ExamRepository`
- DAO-вызовы подготовки и экзамена вынесены из ViewModel в Room-реализации репозиториев:
  - `RoomTaskRepository`
  - `RoomExamRepository`
- `TaskViewModel` и `ExamViewModel` переведены на единый immutable `UiState`.
- Подготовка и экзамен разбиты на более мелкие Compose-компоненты.
- Навигация по заданиям экзамена вынесена в `ExamTaskNavigator` и покрыта unit-тестами.
- Добавлены Room-индексы для FK-полей:
  - `TaskEntity.blockOwnerID`
  - `StepEntity.taskOwnerID`
  - `FormulaEntity.blockOwnerID`
- Добавлены миграции для основных баз контента.
- Удалён неиспользуемый дублирующий `AppDatabase`.
- Убраны временные debug-логи и отладочные комментарии из основного кода.
- Звуковые эффекты вынесены в общий UI-компонент, чтобы экраны не зависели друг от друга напрямую.

## Offline-first и синхронизация

Приложение может импортировать контент из assets при первом запуске, а затем проверять актуальные версии удалённых баз. После загрузки данные сохраняются в Room и доступны без постоянного подключения к сети.

Синхронизация разделена на несколько частей:

- `InitialContentSyncUseCase` — первичная загрузка bundled-контента.
- `ContentUpdateChecker` — проверка удалённых версий.
- `ContentUpdateUseCase` — запуск обновления нужной базы.
- `DataSyncManager` и `GameDataSyncManager` — импорт JSON-данных в локальные базы.

## Структура экранов экзамена

Экзаменационный экран разделён на отдельные файлы:

- `ExamScreen.kt` — координация состояния экрана.
- `ExamControls.kt` — вкладки, таймер и боковое меню.
- `ExamContent.kt` — контент задания и инструкция по клавиатуре.
- `ExamResults.kt` — проверка ответов, итоговый диалог и HTML-отображение результата.
- `ExamViewModel.kt` — состояние экзамена и действия пользователя.

## Сборка

Debug-сборка:

```bash
./gradlew assembleDebug
```

Unit-тесты:

```bash
./gradlew testDebugUnitTest
```

Release-сборка требует локальных файлов конфигурации Firebase и signing config.

## Локальные и секретные файлы

В репозиторий не должны попадать:

- `google-services.json`
- `local.properties`
- `keystore.properties`
- `keystore/`
- `*.jks`
- release APK/AAB outputs

Для release signing можно использовать `keystore.properties.example` как шаблон.

## Дальнейшее развитие

- Ввести `AppContainer` или Hilt для централизованного DI.
- Заменить Room entity в контрактах репозиториев на domain-модели.
- Перенести фоновые обновления контента в WorkManager.
- Расширить unit-тесты для экзаменационных сценариев и проверки ответов.
- Уменьшить дублирование portrait/landscape-разметки в экзаменационном режиме.
