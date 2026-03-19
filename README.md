# Kafka Order Processing Service

## 📌 Описание

Проект представляет собой **Spring Boot сервис** для обработки заказов с использованием **Apache Kafka**.

Реализует архитектуру, приближенную к production:
- асинхронная обработка событий
- идемпотентность
- валидация и обработка ошибок
- полноценные end-to-end тесты с Testcontainers

---

## ⚙️ Технологии

- Java 21
- Spring Boot
- Apache Kafka
- PostgreSQL
- JUnit 5
- Testcontainers
- Awaitility
- Gradle

---

## 🏗 Архитектура

```text
[REST API] → [Kafka Producer] → [orders.created topic]
                                         ↓
                                  [Kafka Consumer]
                                         ↓
                                   [Service Layer]
                                         ↓
                                    [PostgreSQL]
                                         ↓
            ┌────────────────────────────┴────────────────────────────┐
            ↓                                                         ↓
            [orders.processed topic]              [orders.failed topic]
```

---


---

## 🚀 Функциональность

### ✔ Обработка событий
- Чтение событий из `orders.created`
- Валидация входящих данных
- Сохранение валидных заказов в БД

### ✔ Идемпотентность
- Повторные события игнорируются по `eventId`
- Исключает дубли в БД и повторную публикацию

### ✔ Обработка ошибок
- Некорректные события отправляются в `orders.failed`
- Содержат причину ошибки

### ✔ Интеграция с Kafka
- Producer и Consumer настроены
- Используется JSON сериализация

### ✔ Логирование
- Логи содержат `eventId` и `orderId`
- Позволяет отследить полный жизненный цикл события

---

## 🧪 Тестирование

### 🔹 E2E тесты
- Kafka → Consumer → БД → Kafka
- Проверка полного асинхронного пайплайна

### 🔹 Интеграционные тесты
- REST → Kafka → БД → Kafka

### 🔹 Негативные сценарии
- Невалидный заказ (отсутствуют поля)
- Отрицательная сумма
- Неподдерживаемая валюта

### 🔹 Идемпотентность
- Один и тот же event обрабатывается только один раз

### 🔹 Инфраструктура
- Kafka и PostgreSQL поднимаются через **Testcontainers**
- Используются реальные сервисы (без моков)

---

## ▶️ Локальный запуск (Docker + приложение)

#### 1. Поднять инфраструктуру

```bash
docker compose up -d
```

Поднимаются:

- Kafka (port 9092)
- Zookeeper (port 2181)
- PostgreSQL (port 5433)

#### 2. Запустить приложение

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

#### 3. Проверить, что сервис работает

```bash
http://localhost:8080/actuator/health
```

Ожидаемо:

```bash
{"status":"UP"}
```

#### 3. Swagger

```bash
http://localhost:8080/swagger-ui.html
```

---

## ▶️ Как запустить тесты

```bash
./gradlew test
```

Требования:
- установлен и запущен Docker

---

## 📡 API

### Создание заказа

```bash
POST /api/orders
```

Пример запроса:

```bash
{
  "orderId": "ORD-123",
  "userId": "USER-100",
  "amount": 1500.00,
  "currency": "RUB"
}
```

Ожидаемо:

```bash
Order event published
```

## 📈 Monitoring (Prometheus)

Приложение предоставляет метрики через Spring Boot Actuator:

```bash
http://localhost:8080/actuator/prometheus
```

Пример метрик:

```bash
HTTP:
http_server_requests_seconds
Kafka:
spring_kafka_template_seconds
spring_kafka_listener_seconds
kafka_consumer_fetch_manager_records_consumed_total
БД:
hikaricp_connections
spring_data_repository_invocations_seconds
JVM:
jvm_memory_used_bytes
jvm_threads_live_threads
```

#### Демонстрация работы:

После вызова:

```bash
POST /api/orders
```

наблюдается:

- отправка события в Kafka
- обработка consumer'ом
- сохранение в БД
- рост метрик producer/consumer

---

## 📊 Kafka топики

| Топик            | Назначение                  |
| ---------------- | --------------------------- |
| orders.created   | входящие события заказов    |
| orders.processed | успешно обработанные заказы |
| orders.failed    | ошибки обработки            |

---

## 📁 Структура проекта

```text
controller     → REST API
listener       → Kafka consumer
service        → бизнес-логика
repository     → доступ к БД
entity         → JPA сущности
dto            → модели событий
support        → утилиты для тестов
```

---

## 💡 Ключевые особенности

- Использование Testcontainers для реалистичных тестов
- Покрытие happy-path и негативных сценариев
- Тестирование асинхронных процессов через Awaitility
- Реализация идемпотентной обработки событий
- Практическое использование Kafka

---

## 🔮 Возможные доработки

- Retry механизм
- Dead Letter Queue (DLQ)
- docker-compose для локального запуска
- CI (GitHub Actions)
- Swagger / OpenAPI
  
---

## ⭐ Зачем этот проект

- Проект демонстрирует навыки, необходимые для роли QA / AQA / SDET:
- тестирование backend сервисов
- работа с Kafka
- интеграционные и e2e тесты
- тестирование распределённых систем
  
---

## 👤 Автор

Андрей Кузнецов
QA / AQA Engineer
