# RW Code — Android-приложение

Редактор кода для Rusted Warfare (WebView + ваш сайт из assets/index.html).
В приложении работает НАСТОЯЩАЯ синхронизация с Google-таблицей (запросы идут нативно,
в обход блокировки браузера) и сохранение файлов в «Загрузки/RWCode».
Таблица должна быть открыта «всем, у кого есть ссылка». ID таблицы — в assets/index.html (константа SHEET).

## Как получить APK с телефона (без компьютера)
1. Создайте репозиторий на github.com, загрузите в него ВСЕ файлы этой папки (включая скрытую .github).
2. Вкладка Actions → «Build APK» → Run workflow. Через ~3 минуты внизу страницы запуска — артефакт RWCode-debug-apk.
3. Скачайте, распакуйте zip, установите .apk (разрешите установку из неизвестных источников).

## Как собрать на ПК
Android Studio → Open → эта папка → Build → Build APK(s). Или: gradle assembleDebug.

## Структура
app/src/main/assets/index.html — весь редактор
app/src/main/java/.../MainActivity.java — WebView + мост Android.fetchUrl / Android.saveFile
.github/workflows/build.yml — облачная сборка
