# Communication Service (Rent Platform)

## Overview

`communication-service` — микросервис чатов арендной платформы.

Отвечает за:

- чаты между арендатором и владельцем (привязаны к товару)
- WebSocket-доставку сообщений в реальном времени (STOMP)
- системные уведомления о смене статуса сделки
- счётчики непрочитанных сообщений
- разделение чатов на «Я сдаю» и «Я арендую»
- интеграцию с catalog-service и user-service

---

## Tech Stack

- Java 21
- Spring Boot
- Spring Security (JWT Resource Server)
- Spring WebSocket + STOMP
- Spring Data JPA
- PostgreSQL
- Flyway
- MapStruct
- RestClient
- Swagger (OpenAPI)

---

## Ports

| Service      | Port |
|-------------|------|
| Gateway     | 8080 |
| Communication | 8084 |
| Catalog     | 8082 |
| User        | 8081 |
| Deal-Payment | 8083 |

---

## Base URL

Через gateway:

* /api/chats
* /api/internal/chats
* WebSocket: ws://localhost:8084/ws

---

## Domain Model

### Chat

- id (UUID)
- itemId — объявление
- ownerId — владелец (сдаёт)
- renterId — арендатор (арендует)

Один товар — один чат для каждой пары (owner + renter).

### Message

- chatId
- senderId
- text
- messageType: `USER` / `SYSTEM` / `DEAL_STATUS` / `PAYMENT_STATUS`
- systemPayload (JSONB) — данные для системных сообщений

### MessageRead

- messageId
- userId
- readAt

Фиксирует факт прочтения сообщения пользователем.

---

## API

### Chat Endpoints

### Создать или получить чат

#### POST /api/chats?itemId={itemId}

Создаёт чат между арендатором и владельцем. Повторный вызов возвращает существующий.

---
### Список чатов:

#### GET /api/chats?role=RENTER — «Я арендую»
#### GET /api/chats?role=OWNER — «Я сдаю»

Сортировка: по времени последнего сообщения (сверху новые).

---

### История сообщений

#### GET /api/chats/{chatId}/messages?before={datetime}&limit=50

Курсорная пагинация. Без `before` — последние 50 сообщений.

---
### Отправить сообщение

#### POST /api/chats/{chatId}/messages

```json
{ "text": "Здравствуйте!" }
```

---

### Отметить прочитанным

#### POST /api/chats/{chatId}/read

Помечает все сообщения в чате как прочитанные текущим пользователем.

---

### WebSocket
#### Подключение: ws://localhost:8084/ws через STOMP

#### Подписка: /topic/chat/{chatId}

#### Отправка: /app/chat/{chatId}/send

```json
{"text": "Привет!"}
```

---

### Internal API

Системное сообщение о смене статуса сделки

#### POST /api/internal/chats/deal-status

```json
{
"itemId": "UUID",
"dealId": "UUID",
"status": "CONFIRMED"
}
```
Вызывается из deal-payment-service при изменении статуса сделки.

--- 

### WebSocket Flow

1. Фронтенд подключается: ws://localhost:8084/ws (STOMP)
2. Подписывается: SUBSCRIBE /topic/chat/{chatId}
3. Отправляет: SEND /app/chat/{chatId}/send
4. Получает: MESSAGE /topic/chat/{chatId}

## Error Handling

### 400
* cannot create chat with yourself

* validation errors

### 403
* access denied (not chat participant)

### 404
* chat not found

### 500
* internal server error
---
### Run

#### Build:
```bash
./gradlew build -x test
```
---
### Docker
```bash
docker compose up --build
```

## Environment Variables

| Переменная               | Описание                  | По умолчанию |
|--------------------------|--------------------------|-------------|
| PG_HOST                  | PostgreSQL хост           | localhost   |
| PG_PORT                  | PostgreSQL порт           | 5433        |
| PG_DATABASE              | Имя БД                   | comm_db     |
| PG_USER                  | Пользователь БД           | postgres    |
| PG_PASSWORD              | Пароль БД                | 12345       |

---

## Notes

- Чат привязан к товару, только между арендатором и владельцем
- Системные сообщения создаются при смене статуса сделки
- Счётчик непрочитанных обновляется при `markRead`
- WebSocket доставляет сообщения мгновенно
- REST-эндпоинты для истории и офлайн-доступа