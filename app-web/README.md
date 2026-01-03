# Family Tree Editor - Web Version

Веб-версия приложения Family Tree Editor, созданная с использованием Kotlin/Wasm и Compose Multiplatform.

## Требования

- JDK 25 или выше
- Node.js и npm (устанавливаются автоматически через Gradle)

## Сборка

### Production сборка

Для создания production сборки выполните:

```bash
./gradlew :app-web:wasmJsBrowserDistribution
```

Результат будет находиться в `app-web/build/dist/wasmJs/productionExecutable/`

### Development сборка с автоматической перезагрузкой

Для запуска dev-сервера с hot reload:

```bash
./gradlew :app-web:wasmJsBrowserDevelopmentRun
```

Приложение будет доступно по адресу: http://localhost:8080

## Запуск собранной версии

После сборки production версии, вы можете запустить веб-сервер в директории с результатами:

```bash
cd app-web/build/dist/wasmJs/productionExecutable/
python3 -m http.server 8000
```

Затем откройте в браузере: http://localhost:8000

## Структура проекта

```
app-web/
├── build.gradle.kts              # Конфигурация сборки
├── src/
│   └── wasmJsMain/
│       ├── kotlin/
│       │   └── com/family/tree/web/
│       │       └── Main.kt       # Точка входа приложения
│       └── resources/
│           └── index.html        # HTML страница
└── README.md
```

## Особенности веб-версии

### Реализованные функции
- Основной UI приложения (просмотр и редактирование генеалогического дерева)
- Zoom и навигация по дереву
- Диалоги редактирования персон и семей
- AI настройки (хранение в памяти)
- **Работа с файлами через HTML5 File API**:
  - Открытие проектов (.json)
  - Сохранение проектов (.json)
  - Импорт .rel файлов
  - Импорт GEDCOM (.ged, .gedcom)
  - Экспорт GEDCOM
  - Экспорт SVG (текущий вид и fit to content)
  - Импорт AI текста (.txt, .pdf)

### Ограничения
- **Голосовой ввод**: Не поддерживается в веб-версии
- **PDF импорт**: Не поддерживается (требуется реализация PDF парсера для веба)
- **PNG экспорт**: Не реализован (требуется canvas rendering)
- **Хранилище настроек**: localStorage API пока не подключен
- **ZIP архивы**: .rel файлы читаются/записываются, но требуется дополнительная библиотека

### Технические детали
- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.9.3
- **Vite**: 5.4.11
- **Gradle**: 9.2.13 (с поддержкой wasmJs)
- **Ktor Client**: 3.0.3 (с поддержкой wasmJs)
- **Target**: wasmJs (WebAssembly для JavaScript)

## Разработка

При разработке веб-версии учитывайте:

1. Все платформенные реализации находятся в `wasmJsMain` source set
2. Для доступа к Web APIs используйте `kotlinx.browser` (когда будет поддержка wasmJs)
3. Файловые операции требуют использования HTML5 File API
4. Некоторые функции могут быть недоступны в браузере (например, системные диалоги)

## Отладка

Для отладки в браузере:
1. Откройте DevTools (F12)
2. Перейдите во вкладку Console для просмотра логов
3. Source maps включены в development сборке

## Производительность

Production сборка оптимизирована с помощью:
- Webpack минификация
- Binaryen оптимизация WASM
- Code splitting (где возможно)

Размер bundle:
- JavaScript: ~579 KB
- WASM (приложение): ~4.8 MB
- WASM (Skiko): ~8 MB

## Поддержка браузеров

Требуется современный браузер с поддержкой:
- WebAssembly
- ES6 modules
- Canvas API (для Compose rendering)

Рекомендуемые браузеры:
- Chrome/Edge 119+
- Firefox 120+
- Safari 17+

## Лицензия

Та же, что и основной проект Family Tree Editor.
