# Инструкции по отладке импорта .rel файлов на Android TV

## Подготовка

1. **Включите режим разработчика на Android TV:**
   - Откройте Настройки → О системе
   - Нажмите на "Сборка" 7 раз подряд
   - Вернитесь в Настройки → Параметры разработчика
   - Включите "Отладка по USB"

2. **Подключите Android TV к компьютеру:**
   ```bash
   # Найдите IP-адрес вашего Android TV в Настройки → Сеть
   adb connect <IP_ADDRESS>:5555
   
   # Проверьте подключение
   adb devices
   ```

## Установка и запуск приложения

1. **Соберите и установите debug версию:**
   ```bash
   cd /Users/vlad/IdeaProjects/family-tree-editor
   ./gradlew :app-android:installDebug
   ```

2. **Запустите приложение на TV:**
   ```bash
   adb shell am start -n com.family.tree.android/.MainActivity
   ```

## Сбор логов во время импорта

1. **Запустите мониторинг логов:**
   ```bash
   adb logcat -c  # Очистить старые логи
   adb logcat | grep -E "DEBUG_LOG|RelImporter|OutOfMemory|AndroidRuntime"
   ```

2. **В приложении на TV:**
   - Откройте меню (кнопка Menu на пульте)
   - Выберите "Import .rel file"
   - Выберите большой .rel файл для импорта
   - Наблюдайте за прогрессом и возможными ошибками

3. **Логи будут показывать:**
   - `[DEBUG_LOG] RelImporter.importFromBytes: START - file size=...` - начало импорта
   - `[DEBUG_LOG] RelImporter.importFromBytes: Stripping zero bytes...` - обработка данных
   - `[DEBUG_LOG] RelImporter.importFromBytes: Decoding to UTF-8 string...` - декодирование
   - `[DEBUG_LOG] RelImporter.importFromBytes: Parsing bundle from UTF-8...` - парсинг
   - `[DEBUG_LOG] RelImporter.importFromBytes: Building model from parsed data...` - построение модели
   - `[DEBUG_LOG] RelImporter.importFromBytes: SUCCESS - Created project with...` - успех
   - ИЛИ `[DEBUG_LOG] RelImporter.importFromBytes: FATAL - OutOfMemoryError...` - ошибка памяти

## Сохранение полных логов

Для сохранения всех логов в файл:
```bash
adb logcat -d > tv_import_logs_$(date +%Y%m%d_%H%M%S).txt
```

## Мониторинг памяти

Для отслеживания использования памяти во время импорта:
```bash
# В отдельном терминале
watch -n 1 'adb shell dumpsys meminfo com.family.tree.android | grep -A 10 "App Summary"'
```

## Что делать при ошибке OutOfMemory

Если появляется ошибка "Out of memory while importing .rel file":

1. **Проверьте размер файла:**
   - Файлы > 50 MB могут вызывать проблемы на TV
   - Попробуйте файл меньшего размера

2. **Проверьте доступную память:**
   ```bash
   adb shell dumpsys meminfo com.family.tree.android
   ```

3. **Закройте другие приложения на TV** перед импортом

4. **Перезагрузите TV** если память фрагментирована

## Тестовые сценарии

1. **Малый файл (< 1 MB):** Должен импортироваться быстро без проблем
2. **Средний файл (1-10 MB):** Должен показывать прогресс и успешно импортироваться
3. **Большой файл (10-50 MB):** Может занять время, но должен завершиться успешно
4. **Очень большой файл (> 50 MB):** Может вызвать OutOfMemoryError - это ожидаемое поведение

## Отправка логов разработчику

После тестирования отправьте:
1. Файл с логами (tv_import_logs_*.txt)
2. Размер тестового .rel файла
3. Модель Android TV устройства
4. Результат: успех или ошибка
