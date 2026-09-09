# UPS Tray Monitor

Иконка в системном трее Windows с текущим зарядом батареи ИБП **SVC PTS-3KLN-LCD**.
Опционально — безопасное выключение ПК, когда ИБП работает от батареи и заряд упал ниже
уставки.

## Требования

- JDK 21 (в проекте настроен Gradle toolchain на 21)
- ИБП подключён по USB, в системе виден как COM-порт (по умолчанию `COM4`)

## Сборка и запуск

```powershell
# запуск из исходников
.\gradlew.bat run

# проверка связи с ИБП (читает holding-регистры 1..80 по Modbus ASCII, COM4)
.\gradlew.bat modbusPollTest

# собрать дистрибутив (скрипты + jar + зависимости в build/install)
.\gradlew.bat installDist
```

## Нативный `.exe` (jpackage + org.beryx.runtime)

```powershell
.\gradlew.bat jpackageImage
```

Результат - папка `build\jpackage\ups-tray-monitor\` с урезанным JRE и
launcher'ом без консоли. Копируется на целевую машину, запуск - `ups-tray-monitor.exe`.
Обновление - заменить jar-ы в подпапке `app\`.

Список JDK-модулей для урезанного рантайма задан в `build.gradle.kts` (блок `runtime`);
пересмотреть его можно через `.\gradlew.bat suggestModules`.

## Настройки

Файл `settings.properties` создаётся рядом с рабочим каталогом при первом запуске:

| Ключ | Смысл | По умолчанию |
|---|---|---|
| `comPort` | COM-порт ИБП | `COM4` |
| `baudRate` | скорость порта | `9600` |
| `pollIntervalSeconds` | период опроса | `2` |
| `shutdownThresholdPercent` | порог заряда для выключения | `20` |
| `shutdownDelaySeconds` | задержка `shutdown /s /t` | `60` |

Те же поля доступны через пункт меню трея **Настройки**.

## Автозапуск при входе в Windows

Автозапуск в код не встроен. Чтобы приложение стартовало при входе в систему:

1. Нажмите `Win + R`, введите `shell:startup`, `Enter` - откроется папка автозагрузки.
2. Создайте в ней ярлык на `.exe` (после jpackage) или на батник вида
   `gradlew.bat run` в каталоге проекта.
