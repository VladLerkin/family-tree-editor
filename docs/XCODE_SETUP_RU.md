# Решение проблемы "No such module 'FamilyTreeApp'" в Xcode

## Проблема
При попытке импортировать модуль в Swift файле:
```swift
import FamilyTreeApp
```

Xcode выдает ошибку: **"No such module 'FamilyTreeApp'"**

## Причина
Для работы с Kotlin Multiplatform фреймворком в iOS необходимо создать Xcode проект-обертку, который будет использовать собранный фреймворк.

## Быстрое решение

### Шаг 1: Соберите фреймворк
```bash
cd /Users/yav/IdeaProjects/rel
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
```

Фреймворк будет создан в:
```
app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework
```

### Шаг 2: Создайте новый Xcode проект

1. Откройте Xcode
2. **File → New → Project**
3. Выберите **iOS → App**
4. Настройки проекта:
   - Product Name: `FamilyTreeWrapper` (или любое другое имя)
   - Interface: **SwiftUI**
   - Language: **Swift**
   - Сохраните вне папки проекта (например, в `ios-wrapper`)

### Шаг 3: Добавьте фреймворк в Xcode проект

1. В Xcode, выберите ваш проект в Project Navigator
2. Выберите **target** вашего приложения
3. Перейдите на вкладку **General**
4. Прокрутите до **Frameworks, Libraries, and Embedded Content**
5. Нажмите **+** (плюс)
6. Нажмите **Add Other... → Add Files...**
7. Перейдите в:
   ```
   /Users/yav/IdeaProjects/rel/app-ios/build/bin/iosSimulatorArm64/debugFramework/
   ```
8. Выберите `FamilyTreeApp.framework`
9. **Важно:** Измените значение справа на **Embed & Sign**

### Шаг 4: Настройте Framework Search Paths

1. В настройках target, перейдите на вкладку **Build Settings**
2. Найдите **Framework Search Paths**
3. Добавьте путь:
   ```
   /Users/yav/IdeaProjects/rel/app-ios/build/bin/iosSimulatorArm64/debugFramework
   ```
4. Убедитесь, что стоит галочка **recursive** (рекурсивно)

### Шаг 5: Создайте Swift UI

В вашем основном Swift файле (например, `ContentView.swift`):

```swift
import SwiftUI
import FamilyTreeApp  // Теперь этот импорт должен работать

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
```

### Шаг 6: Соберите и запустите

1. В Xcode: **Product → Clean Build Folder** (⇧⌘K)
2. Выберите iOS Simulator (например, iPhone 15 Pro)
3. **Product → Build** (⌘B)
4. **Product → Run** (⌘R)

## Если ошибка все еще возникает

### Проверка 1: Фреймворк существует?
```bash
ls -la /Users/yav/IdeaProjects/rel/app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework
```

Если нет - пересоберите:
```bash
cd /Users/yav/IdeaProjects/rel
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
```

### Проверка 2: Правильная архитектура?

Проверьте архитектуру вашего Mac:
```bash
uname -m
```

- Если `arm64` (Apple Silicon M1/M2/M3) → используйте `iosSimulatorArm64`
- Если `x86_64` (Intel Mac) → используйте `iosX64`:
  ```bash
  ./gradlew :app-ios:linkDebugFrameworkIosX64
  ```
  И измените путь в Xcode на:
  ```
  /Users/yav/IdeaProjects/rel/app-ios/build/bin/iosX64/debugFramework
  ```

### Проверка 3: Очистка Xcode

```bash
# Закройте Xcode полностью, затем:
rm -rf ~/Library/Developer/Xcode/DerivedData/*
```

Затем откройте Xcode снова и пересоберите.

### Проверка 4: Правильное имя модуля

В Swift файлах используйте **точное** имя:
```swift
import FamilyTreeApp  // ✅ Правильно
```

НЕ используйте:
```swift
import app_ios      // ❌ Неправильно
import AppIos       // ❌ Неправильно  
import FamilyTree   // ❌ Неправильно
```

## Альтернативный способ: Использование IntelliJ IDEA

Если у вас установлен IntelliJ IDEA Ultimate:

1. Установите плагин **Kotlin Multiplatform Mobile**:
   - IntelliJ IDEA → Settings → Plugins
   - Найдите "Kotlin Multiplatform Mobile"
   - Установите и перезапустите IDE

2. Откройте проект в IntelliJ IDEA

3. **Run → Edit Configurations...**

4. Нажмите **+** → выберите **Kotlin Mobile**

5. Настройте:
   - Execute target: **app-ios**
   - Выберите iOS Simulator

6. Нажмите **Run** (▶️)

IntelliJ IDEA автоматически создаст Xcode проект и запустит приложение.

## Дополнительная документация

Подробная документация на английском языке:
- Полное руководство: `docs/BUILD_IOS.md`
- Решение проблем: `docs/iOS_TROUBLESHOOTING.md`

## Структура проекта

```
Project Root:
├── app-ios/              # iOS модуль
│   ├── build.gradle.kts  # baseName = "FamilyTreeApp"
│   └── src/iosMain/kotlin/com/family/tree/ios/main.kt
├── core/                 # Общая бизнес-логика
└── ui/                   # Общий UI (Compose Multiplatform)
```

Фреймворк `FamilyTreeApp` экспортирует функцию `MainViewController()`, которая создает iOS view controller с Compose UI.

## Почему проект не соответствует стандартному шаблону?

**Важно:** Этот проект отличается от стандартных шаблонов Kotlin Multiplatform.

### Стандартный шаблон KMP (один модуль "shared")

В типичных KMP-проектах конфигурация выглядит так:

```kotlin
// shared/build.gradle.kts
kotlin {
    androidTarget()
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"  // Имя фреймворка задается здесь
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            // Все зависимости в одном месте
        }
    }
}
```

В стандартных шаблонах **iOS targets и конфигурация фреймворка находятся в одном модуле**.

### Этот проект (мультимодульная архитектура)

```
Project Root:
├── core/build.gradle.kts      # Объявляет iosX64(), iosArm64(), iosSimulatorArm64()
├── ui/build.gradle.kts        # Объявляет iosX64(), iosArm64(), iosSimulatorArm64()
└── app-ios/build.gradle.kts   # Конфигурация фреймворка: baseName = "FamilyTreeApp"
```

### В чем разница и почему?

**Почему проект устроен иначе:**
- **Разделение ответственности:** `core` (бизнес-логика) и `ui` (Compose UI) — это переиспользуемые библиотеки
- **Каждый модуль объявляет iOS targets** для компиляции под iOS
- **Только `app-ios` создает фреймворк**, который объединяет все модули для Xcode
- **Лучшая масштабируемость:** большие проекты выигрывают от разделения на модули

**Оба подхода правильные:**
- ✅ Один модуль "shared" — проще, подходит для небольших проектов (стандартный шаблон)
- ✅ Несколько модулей — организованнее, лучше для средних и больших проектов (этот проект)

**Главный вывод:** Имя `baseName = "FamilyTreeApp"` в файле `app-ios/build.gradle.kts` определяет имя фреймворка, которое вы используете в Swift/Xcode, независимо от количества модулей в проекте.

### Как проверить конфигурацию в вашем проекте

Откройте файл `app-ios/build.gradle.kts` и найдите:

```kotlin
kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "FamilyTreeApp"  // ← Это имя используется в import
            isStatic = true
        }
    }
}
```

**Именно значение `baseName` определяет имя модуля для импорта в Swift:**
```swift
import FamilyTreeApp  // Имя берется из baseName
```

## Резюме

✅ Фреймворк собран правильно (`baseName = "FamilyTreeApp"`)  
✅ Фреймворк находится по пути `app-ios/build/bin/.../FamilyTreeApp.framework`  
✅ Но для использования **нужен Xcode проект-обертка**  
✅ Следуйте инструкциям выше для создания Xcode проекта
