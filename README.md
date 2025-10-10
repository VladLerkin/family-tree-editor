# Family Tree Editor — как запустить проект

Ниже — самый короткий путь, чтобы запустить приложение локально и (при желании) упаковать установщик.

Требования
- JDK 25 (обязательно). Проверьте: `java -version` и переменную `JAVA_HOME`.
- Интернет для докачки зависимостей Maven'ом.
- Ничего ставить дополнительно не нужно: в репозитории есть Maven Wrapper (`mvnw`, `mvnw.cmd`).

Быстрый старт (Windows)
1) Откройте терминал в корне проекта.
2) Выполните:
   PowerShell:
   .\mvnw.cmd "-Dprism.order=sw" javafx:run
   
   Альтернативы:
   - PowerShell (stop-parsing):
     .\mvnw.cmd --% -Dprism.order=sw javafx:run
   - CMD.exe:
     .\mvnw.cmd -Dprism.order=sw javafx:run
   
   Пояснения:
   - В PowerShell обязательно заключайте системные свойства -D в кавычки ("-Dимя=значение") или используйте `--%`. Иначе PowerShell разобьёт аргумент, и Maven выдаст ошибку вида: Unknown lifecycle phase '.order=sw'.
   - JavaFX-плагин запустит приложение (MainApplication).
   - Флаг `-Dprism.order=sw` включает софт‑рендеринг. Можно убрать для аппаратного ускорения.

Быстрый старт (macOS/Linux)
1) Откройте терминал в корне проекта.
2) Выполните:
   ./mvnw -Dprism.order=sw javafx:run

Запуск из IntelliJ IDEA
- Откройте проект (pom.xml) как Maven-проект.
- Создайте конфигурацию "Application":
  - Main class: `com.pedigree.app.MainApplication`
  - Use classpath of module: `family-tree-editor`
- Запустите конфигурацию. Если на машине несколько JDK, убедитесь, что выбрана JDK 25 (Project SDK и Run configuration JRE).

Сборка установщика
- Windows (MSI):
  .\mvnw.cmd -Pwindows clean package
  Готовый MSI будет в `target/dist/family-tree-editor-${project.version}-windows.msi`.

- macOS (DMG):
  mvn -Pmac clean package
  Готовый DMG будет в `target/dist/family-tree-editor-${project.version}-macos.dmg`.

- Linux (DEB, c ярлыком в меню):
  mvn -Plinux clean package
  Готовый пакет будет в `target/dist/family-tree-editor-${project.version}-linux-amd64.deb`.
  Требуется пакет fakeroot (используется jpackage). Установите его через пакетный менеджер: apt/dnf/pacman. Если fakeroot отсутствует, сборка не упадёт, но шаг упаковки DEB будет пропущен.
  Рекомендуется установить binutils (даёт `objcopy`) для более компактного рантайма. Если binutils отсутствует, сборка всё равно пройдёт: мы отключаем соответствующий плагин jlink по умолчанию.

Подробные инструкции
- Windows: `docs/BUILD_WIN.md`
- macOS: `docs/BUILD_MAC.md`
- Linux: `docs/BUILD_LINUX.md`

Типичные проблемы
- "Cannot find wrapperUrl in .mvn\\wrapper\\maven-wrapper.properties":
  Обновите репозиторий (git pull). Файл уже должен содержать и `distributionUrl`, и `wrapperUrl`. Вручную можно прописать:
  distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.8/apache-maven-3.9.8-bin.zip
  wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

- JavaFX/рендеринг не стартует:
  Попробуйте добавить `-Dprism.order=sw` к команде запуска.

Версии и окружение
- Проект собирается Maven'ом, Java 25, JavaFX 24.0.1.
- Точка входа: `com.pedigree.app.MainApplication`.
