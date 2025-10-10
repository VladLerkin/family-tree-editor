# GitHub Actions: Автоматическая сборка релизов

## Обзор

Проект настроен для автоматической сборки релизов через GitHub Actions. Workflow файл находится по адресу: `.github/workflows/release.yml`

## Когда срабатывает сборка

1. **При создании тега версии**: Когда вы пушите тег, начинающийся с `v` (например, `v1.2.0`)
   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```

2. **Вручную**: Через интерфейс GitHub в разделе Actions → Build and Release → Run workflow

## Что происходит при сборке

Workflow запускает три параллельные задачи для создания дистрибутивов:

### 1. macOS (DMG)
- Использует runner: `macos-latest`
- Устанавливает JDK 25 (Temurin)
- Собирает DMG с помощью Maven профиля `-Pmac`
- Результат: `Family Tree Editor-{version}.dmg`
- **Предустановлено**: Все необходимые инструменты (Xcode CLI, hdiutil)

### 2. Windows (MSI)
- Использует runner: `windows-latest`
- Устанавливает JDK 25 (Temurin)
- Собирает MSI с помощью Maven профиля `-Pwindows`
- Результат: `Family Tree Editor-{version}.msi`
- **Предустановлено**: WiX Toolset v3.x (уже есть на GitHub runners)

### 3. Linux (DEB)
- Использует runner: `ubuntu-latest`
- Устанавливает JDK 25 (Temurin)
- **Автоматически устанавливает**: `fakeroot` и `binutils` (требуются для jpackage)
- Собирает DEB с помощью Maven профиля `-Plinux`
- Результат: `family-tree-editor_{version}_amd64.deb`

### 4. Создание релиза
- Запускается только при пуше тега (не при ручном запуске)
- Скачивает все артефакты со всех трёх платформ
- Создаёт GitHub Release с:
  - Автоматически сгенерированными release notes
  - Всеми собранными файлами (DMG, MSI, DEB)
  - Не черновик (draft: false)
  - Не pre-release

## Что нужно для работы

### В репозитории GitHub:

1. **Разрешения (Permissions)**: 
   - Workflow уже настроен с правильными разрешениями (`contents: write`) на уровне всего workflow
   - GitHub автоматически предоставляет `GITHUB_TOKEN` для создания релизов

2. **Секреты**: 
   - Не требуются! `GITHUB_TOKEN` создаётся автоматически

3. **Настройки репозитория**:
   - В Settings → Actions → General → Workflow permissions должно быть:
     - ✓ "Read and write permissions" (или)
     - ✓ "Read repository contents and packages permissions" + включён "Allow GitHub Actions to create and approve pull requests"

### На вашей локальной машине (для создания тега):

```bash
# 1. Убедитесь, что код закоммичен
git add .
git commit -m "Release version 1.2.0"

# 2. Создайте и запушьте тег
git tag v1.2.0
git push origin main
git push origin v1.2.0

# Workflow запустится автоматически!
```

## Проверка статуса сборки

1. Перейдите в ваш репозиторий на GitHub
2. Откройте вкладку **Actions**
3. Найдите запуск "Build and Release"
4. Откройте его для просмотра логов каждой задачи

## Возможные проблемы и решения

### ❌ Проблема: JDK 25 недоступен
**Решение**: GitHub Actions использует Temurin (Eclipse Adoptium), который поддерживает JDK 25. Если возникнут проблемы, можно изменить на `oracle` или `zulu` distribution.

### ❌ Проблема: Windows сборка не находит WiX
**Решение**: WiX Toolset v3.x предустановлен на `windows-latest` runners. Если всё же возникнут проблемы, можно добавить явную установку:
```yaml
- name: Install WiX (if needed)
  run: choco install wixtoolset -y
```

### ❌ Проблема: Linux DEB не создаётся
**Решение**: Workflow уже настроен на установку `fakeroot` и `binutils`. Если DEB не создастся, workflow продолжит работу (`if-no-files-found: warn`), но релиз будет без Linux пакета.

### ❌ Проблема: Релиз не создаётся
**Причина**: Workflow создаёт релиз только при пуше тега, начинающегося с `v`

**Решение**: Проверьте, что вы запушили тег правильно:
```bash
git tag -l  # Проверить локальные теги
git ls-remote --tags origin  # Проверить теги на GitHub
```

### ❌ Проблема: Ошибка разрешений при создании релиза
**Решение**: 
1. Перейдите в Settings → Actions → General
2. В разделе "Workflow permissions" выберите "Read and write permissions"
3. Нажмите "Save"

## Ручной запуск (без создания релиза)

Если хотите просто протестировать сборку без создания релиза:

1. Перейдите в Actions → Build and Release
2. Нажмите "Run workflow"
3. Выберите ветку
4. Нажмите "Run workflow"

Это создаст артефакты, но не создаст GitHub Release (релиз создаётся только при тегах).

## Структура артефактов

После успешной сборки в разделе Artifacts будут доступны:
- `macos-dmg` - содержит DMG файл
- `windows-msi` - содержит MSI файл
- `linux-deb` - содержит DEB файл (если сборка успешна)

Артефакты хранятся 90 дней (по умолчанию для GitHub).

## Итоговый чеклист для первого релиза

- [ ] Код закоммичен и запушен в main/master
- [ ] В Settings → Actions → General разрешения установлены на "Read and write permissions"
- [ ] Создан и запушен тег версии (например, `v1.2.0`)
- [ ] Workflow запустился автоматически (проверить во вкладке Actions)
- [ ] Все три задачи (macOS, Windows, Linux) завершились успешно
- [ ] GitHub Release создан автоматически с прикреплёнными файлами

## Дополнительно

### Версионирование
Версия берётся из `pom.xml` (`<version>1.2.0-SNAPSHOT</version>`). 
- Для инсталляторов используется только numeric версия (1.2.0)
- Суффикс `-SNAPSHOT` автоматически удаляется build-helper-maven-plugin
- Если major версия = 0, автоматически заменяется на 1 (требование macOS/Windows)

### Кэширование
Workflow использует кэширование Maven dependencies (`cache: 'maven'`), что ускоряет повторные сборки.

### Параллелизм
Все три платформы собираются параллельно, что сокращает общее время сборки примерно до 10-15 минут (вместо 30-45 минут последовательной сборки).
