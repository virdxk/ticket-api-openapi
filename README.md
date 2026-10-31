# Ticket, Booking and Person API

Комплект содержит:

- `ticket-service.yaml` — спецификацию OpenAPI 3.0.3 первого сервиса `/tickets` (коллекция Ticket).
- `booking-service.yaml` — спецификацию второго сервиса `/booking`: CRUD бронирований,
  продажа VIP-копии и отмена бронирований человека.
- `person-service.yaml` — спецификацию третьего сервиса `/persons` (коллекция Person).
- `index.html` — интерактивную документацию Swagger UI со списком всех трёх спецификаций.
- `swagger-ui/` — локальная копия Swagger UI 5.27.1, документация открывается без CDN.

Развёрнутая документация: <https://se.ifmo.ru/~s412939/>.

## Просмотр Swagger UI

Из корня репозитория запустите любой статический HTTP-сервер, например:

```bash
python3 -m http.server 8081
```

После этого откройте <http://localhost:8081/>. Запуск `index.html` напрямую через
`file://` может не загрузить спецификации из-за ограничений браузера.

## Сборка и запуск

### Сборка сервисов

```bash
mvn clean package
```

WAR-файлы собираются в `ticket-service/target/ticket-service.war`,
`person-service/target/person-service.war` и `booking-service/target/booking-service.war`.
Библиотеки внутрь WAR не входят: `deploy/deploy.sh` копирует их через
`dependency:copy-dependencies` в `target/lib/`, а Tomcat подхватывает их через
`shared.loader`.

### Переменные окружения

Все три сервиса читают подключение к PostgreSQL из окружения:

- `DB_URL` — JDBC-URL, например `jdbc:postgresql://pg:5432/studs`;
- `DB_USER` — имя пользователя;
- `DB_PASSWORD` — пароль.

Booking Service обращается к двум другим сервисам по HTTPS и проверяет их
сертификаты по truststore `$HOME/soa/tls/truststore.p12` (пароль `soa-lab2-tls`).
Связка keystore/truststore/server.crt создаётся и продлевается скриптом
`deploy/helios.sh`; для локального запуска Booking Service её нужно положить по
тому же пути. Адреса соседних сервисов можно переопределить переменными
`TICKET_SERVICE_URL` и `PERSON_SERVICE_URL` (по умолчанию
`https://localhost:41390` и `https://localhost:41391`).

### Локальный запуск

Сервисы собраны как WAR с Tomcat в scope `provided`, поэтому локально их
запускают через Spring Boot-плагин Maven. Сначала один раз соберите `common`
в локальный репозиторий, затем поднимайте каждый сервис:

```bash
mvn clean install -DskipTests
DB_URL=... DB_USER=... DB_PASSWORD=... mvn -pl ticket-service spring-boot:run
```

Аналогично для `person-service` и `booking-service`. При одновременном запуске
нескольких сервисов задайте каждому свой порт через `SERVER_PORT`.

### Деплой на helios

```bash
sh deploy/deploy.sh
```

Скрипт собирает сервисы и клиент (при наличии `client/package-lock.json` —
через `npm ci`; клиент собирается с `VITE_API_BASE`, по умолчанию
`https://se.ifmo.ru/~s412939`), копирует артефакты на helios и запускает там
`deploy/helios.sh`. Он останавливает прежние инстансы Tomcat (с ограничением по
времени), ставит новые версии рядом и переключается на них, сохраняя каталог
прежнего инстанса с меткой времени и логи, после чего до минуты ждёт ответа
каждого сервиса по HTTP; при неудаче печатает хвост лога и завершается с
ошибкой.

### Клиент

```bash
cd client
npm ci
npm run dev    # dev-сервер Vite
npm run build  # статическая сборка в client/dist
```

По умолчанию клиент обращается к API по относительным путям того же origin
(`/ticket-service`, `/person-service`, `/booking-service`); dev-сервер
проксирует эти префиксы на `VITE_API_TARGET` (по умолчанию
`https://se.ifmo.ru`). Иной базовый URL API задаётся через `VITE_API_BASE`:

```bash
VITE_API_BASE=https://se.ifmo.ru/~s412939 npm run build
```

## Принятые решения

- Ticket Service и Person Service друг о друге не знают. Связь человека с билетом
  хранит только Booking Service в виде бронирования.
- Нумерация страниц начинается с 1, размер страницы ограничен 100 элементами.
- Фильтр списка — повторяемый параметр `filter=<поле><оператор><значение>`,
  операторы `=`, `!=`, `>`, `>=`, `<`, `<=`, `~` (содержит подстроку). Фильтры
  объединяются через `AND`, например `?filter=price>=100&filter=name~концерт`.
- `sort` повторяется для многоуровневой сортировки, например
  `?sort=price,desc&sort=name,asc`.
- Обновление выполняется методом `PATCH` в формате JSON Merge Patch: передаются
  только изменяемые поля, `null` очищает nullable-поле.
- Ошибка возвращается объектом `Error` с полями `message` и `time`. В ответе `422`
  добавляется массив `details` с нарушениями по полям JSON запроса.
- Коды ошибок: `400` — некорректный запрос, `404` — ресурс не найден, `422` —
  нарушение ограничений модели, `500` — внутренняя ошибка (во всех операциях),
  `503` — недоступен Ticket Service или Person Service (только в Booking Service).
- Продажа VIP-копии исходного билета с `price=null` возвращает `409`, поскольку
  удвоить отсутствующую цену невозможно.
