# Pedigree Chart Editor — как запустить проект

Ниже — самый короткий путь, чтобы запустить приложение локально и (при желании) упаковать установщик.

Требования
- JDK 24 (обязательно). Проверьте: `java -version` и переменную `JAVA_HOME`.
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
  - Use classpath of module: `pedigree-editor`
- Запустите конфигурацию. Если на машине несколько JDK, убедитесь, что выбрана JDK 24 (Project SDK и Run configuration JRE).

Сборка установщика
- Windows (MSI):
  .\mvnw.cmd -Pwindows clean package
  Готовый MSI будет в `target/dist/Pedigree Chart Editor-${project.version}.msi`.

- macOS (DMG):
  mvn -Pmac clean package
  Готовый DMG будет в `target/dist/Pedigree Chart Editor-${project.version}.dmg`.

Подробные инструкции
- Windows: `docs/BUILD_WIN.md`
- macOS: `docs/BUILD_MAC.md`

Типичные проблемы
- "Cannot find wrapperUrl in .mvn\\wrapper\\maven-wrapper.properties":
  Обновите репозиторий (git pull). Файл уже должен содержать и `distributionUrl`, и `wrapperUrl`. Вручную можно прописать:
  distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.8/apache-maven-3.9.8-bin.zip
  wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

- JavaFX/рендеринг не стартует:
  Попробуйте добавить `-Dprism.order=sw` к команде запуска.

Версии и окружение
- Проект собирается Maven'ом, Java 24, JavaFX 21.0.3.
- Точка входа: `com.pedigree.app.MainApplication`.
