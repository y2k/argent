# Проверки

## Быстрые проверки на Y2K

### Java source-file launcher

Для локальных JVM-проверок нужны JDK 22+, Make, `ly2k` и установленные пакеты
из `LY2K_PACKAGES_DIR` (включая `make` 0.6.0 и runtime `prelude` 1.0.0).
Основной запуск из корня проекта:

```sh
make test
make check
```

`make test` генерирует актуальные Java-исходники и запускает их без Android SDK,
Gradle, Docker, Python и устройства. Для повторного запуска уже сгенерированных
исходников достаточно последней команды ниже. Полная последовательность:

```sh
mkdir -p .build/unit
ly2k --target eval < build-test.clj > .build/unit/Makefile
make -f .build/unit/Makefile build
java .build/unit/src/argent/checks/runner.java
```

Eval здесь только генерирует Make-правила. Тесты исполняются на JVM:
launcher сам находит зависимые Java-исходники и компилирует их в памяти.
Предварительные `.class`, отдельный `javac`, Android SDK и устройство не нужны.

Runner явно запускает семь групп из `test/argent/checks/`, без APK и устройства:

| Группа | Что проверяет |
| --- | --- |
| `logging.clj` | Реальный stderr: отложенность и вложенность операций, события очереди и QR |
| `registration.clj` | Регистрация, отложенность, композиция и подмена операций |
| `qr_request.clj` | Подготовка сканера, однократная доставка, отмена/ошибка, очистка и поздние callbacks |
| `qr_example.clj` | Передача исходного результата в уведомление |
| `qr_button.clj` | Вызов `:qr:pick`, listener и окружение нажатия в настоящем преобразовании узла |
| `counter.clj` / `test/argent/counter.clj` | Состояние, маршрутизация событий, изоляция экземпляров, аргументы и число вызовов эффектов чата |
| `event_pump.clj` | Настоящий проход очереди: handle → ack → render → wake, один элемент за проход, остановка на пустой очереди |

Используются обычные Y2K-функции и `support/check`, который выбрасывает
`AssertionError` с названием проверки независимо от Java-флага `-ea`.
Ошибка даёт ненулевой код. После каждой группы печатается `ok`, после всего
набора — `unit tests passed`. Логирование проверяется захватом настоящего stderr
с восстановлением потока даже при исключении. Новые тесты размещаются по пути
namespace в `test/argent/checks/` и явно добавляются в `runner.clj`.

В тестах чата записываются вызовы `:chat:append-message` и `:chat:render-message`. Заглушки не реализуют историю, лимит или отключение кнопок. Эти механизмы остаются в Android View-дереве.

`make check` запускает тот же набор тестов, затем `git diff --check` и `git diff --cached --check`.

## Проверки на физическом Android-устройстве

Для сборки нужен Docker с доступом к образу из `test/qr_component.sh`; для устройства — `adb` и `android` CLI. Выберите serial физического устройства из `adb devices -l`:

```sh
make build
android run --device DEVICE_SERIAL --apks .build/apk/app-debug.apk --activity 'argent.app$MainActivity'
adb -s DEVICE_SERIAL shell settings put system accelerometer_rotation 0
adb -s DEVICE_SERIAL shell settings put system user_rotation 0
python3 test/counter_device.py DEVICE_SERIAL
sh test/qr_component.sh DEVICE_SERIAL
```

`counter_device.py` проверяет реальные нажатия и burst-доставку, восстановление и очистку SQLite-очереди, recreation и единственного consumer, историю, отключение старых кнопок, прокрутку и лимит 15 сообщений. Начальная ориентация должна быть `0`: тест переключает её в `1` для пересоздания Activity. Скрипт перезаписывает очередь тестовыми событиями, перезапускает приложение и меняет ориентацию; запускайте на тестовом экземпляре приложения. Промежуточная арифметическая проверка одиночного decrement выполняется на JVM; реальные decrement и отрицательное значение остаются в device-suite.

`qr_component.sh` собирает и устанавливает приложение и instrumentation APK, проверяет обновление текста и listener настоящей кнопки, передачу результата и отключение старой кнопки. Это не проверка камеры: `:qr:pick` здесь подменён.

Дополнительно на устройстве проверьте:

1. «Сканировать QR» открывает камеру; читаемый QR возвращает исходный текст в уведомлении.
2. Закрытие сканера крестиком сообщает отмену, повторный запуск работает. «Назад» может дать ошибку SDK — она отображается отдельно от отмены.
3. После уничтожения Activity незавершённый запрос не доставляет результат старому владельцу; новый экземпляр продолжает работать.

JVM-набор проверяет алгоритмы и их Java-target, а Android-сборка — SDK-связи.
QR-запрос используется из общего с приложением `argent.vendor.qr_request`;
камера и другие Android-адаптеры не загружаются локальными тестами.
Проверки на устройстве подтверждают платформенное поведение; вызов эффекта в
unit-тесте их не заменяет. Результаты автоматических device-проверок сохраняются
в `.build/counter-device-check/` и `.build/qr-component-result.txt`.

## Проверка JVM-переноса 2026-10-23

- `make test` и `make check`: прошли все семь групп на JDK 27.
- Чистый `make check` прошёл с ограниченным `PATH`, содержащим только
  `make`, `git`, `java`, `ly2k`, `mkdir`, `cp`; Android-инструменты, Python
  и `javac` недоступны. `.class` на диске не создаются.
- Временное нарушение ожидания, ошибка Java-компиляции и отказ генератора
  дали ненулевой код `make test`; после восстановления набор прошёл.
  Ошибка внутри захвата stderr сохранила видимую диагностику после `finally`.
- `make build`: APK собран; существующее предупреждение unchecked operations
  в Android-адаптере `qr_image.java` сохраняется.
- APK установлен и запущен на физическом Pixel 9a (`57051JEBF21519`).
  Нажатие Increment изменило 0 на 1. Реальный Google Code Scanner открылся;
  закрытие крестиком дало `qr.result status=cancelled` и вызов уведомления.
  Повторное открытие работает.
- При запуске Activity с `--activity-no-history` открытие сканера вызвало
  `wm_on_destroy_called`, `qr.stop` и `app.stop`. Поздняя отмена не вызвала
  доставку результата или уведомление. Логи: `.build/jvm-device-lifecycle.log`
  и `.build/jvm-device-destroy.log`. Затем выполнен обычный запуск приложения.
- `sh test/qr_component.sh 57051JEBF21519`: `OK (1 test)`;
  результат в `.build/qr-component-result.txt`.
- Независимое read-only ревью: блокирующих замечаний нет, сохранение всех
  прежних assertions и ожиданий stderr подтверждено.
- Пользователь подтвердил успешное чтение реального QR и отображение
  исходного текста в уведомлении на Pixel 9a.

## Проверка рефакторинга 2026-10-22

- `make check`: все шесть групп прошли.
- `make build`: APK собран; компилятор сообщил о существующих unchecked operations в `qr_image.java`.
- APK установлен и запущен на физическом Xiaomi 21051182G (`abf51f11`).
- `counter_device.py`: `device checks passed`, включая SQLite, recreation, историю и лимит 15. Первый запуск не прошёл проверку recreation, потому что планшет уже был в ориентации `1`; после установки исходной ориентации `0` полный сценарий прошёл. Сообщение MIUI об отсутствующем `theme_compatibility.xml` не помешало получению UI-дерева и проверкам.
- Независимое read-only ревью не выявило замечаний; все шесть eval-групп повторно прошли.
- `sh test/qr_component.sh abf51f11`: `OK (1 test)`.
- Реальный Google Code Scanner открылся; закрытие крестиком показало «Сканирование отменено» (`.build/qr-refactor-cancel.png`), повторное открытие работает.
- Успешное чтение физического QR подтверждено пользователем: создан и открыт `.build/test-qr.png` с текстом `Argent QR test 123`, приложение показало результат.
- Lifecycle smoke-проверка выполнена запуском Activity с `--activity-no-history`: после открытия сканера Android вызвал `onDestroy` (событие `wm_on_destroy_called` в `.build/qr-destroy-events.txt`), MainActivity исчезла из стека, процесс приложения остался жив. Закрытие сканера вернуло на launcher без наблюдаемого уведомления старого экземпляра (`.build/qr-destroy-cancel.png`); обычный повторный запуск дал 0, нажатие Increment — 1, ошибок AndroidRuntime нет. Точные проверки подавления поздних callbacks остаются в `qr_image.eval`.
- Команда lifecycle-проверки: `adb -s DEVICE_SERIAL shell 'am start -W -f 0x10000000 --activity-clear-task --activity-no-history -n "im.y2k.argent/argent.app\$MainActivity"'`. Флаги `--activity-*` должны идти после `-f`. Первоначальная попытка через `settings put global always_finish_activities 1` не уничтожила Activity; настройка возвращена в исходное значение `0`.
