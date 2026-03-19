# order-events-kafka-tests

[![TEST](https://github.com/andreigkuznetsov/order-events-kafka-tests/actions/workflows/test.yml/badge.svg?branch=main)](https://github.com/andreigkuznetsov/order-events-kafka-tests/actions/workflows/test.yml)

Integration and end-to-end tests for an event-driven order processing service built with Spring Boot, Apache Kafka, PostgreSQL, JUnit 5, and Testcontainers.

# Kafka Order Processing Service

## 📌 Описание

Проект представляет собой набор интеграционных и end-to-end тестов для сервиса обработки заказов, построенного на событийной архитектуре с использованием **Apache Kafka**.
Тесты проверяют полный жизненный цикл заказа: от создания события до его обработки и сохранения в базе данных. Для изоляции тестов и воспроизводимости окружения используются **Testcontainers (Kafka и PostgreSQL)**.
Внешние зависимости могут изолироваться с помощью mock-подхода (например, при unit-тестировании), однако в интеграционных и e2e тестах используются реальные сервисы через Testcontainers, что обеспечивает максимальную приближенность к production-среде.

Проект моделирует архитектуру, приближенную к production:
- асинхронная обработка событий
- идемпотентность
- retry и Dead Letter Queue (DLQ)
- валидация и обработка ошибок
- наблюдаемость через Prometheus
- end-to-end тесты с Testcontainers

---

## ⚙️ Технологии

- Java 21
- Spring Boot 3
- Apache Kafka
- PostgreSQL
- JUnit 5
- Testcontainers (Kafka, PostgreSQL)
- Awaitility
- Gradle
- Spring Actuator (Prometheus)
- GitHub Actions (CI)
- Mockito (для unit-тестирования и изоляции зависимостей)

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

## 🔄 Полный сценарий обработки заказа

1. Клиент отправляет POST /api/orders
2. Сервис публикует событие в Kafka (orders.created)
3. Consumer читает событие
4. Выполняется:
- валидация
- проверка идемпотентности
- сохранение в БД
5. Результат обработки:
- ✅ success → orders.processed
- ⚠ validation error → orders.failed
- 🔁 technical error → retry → orders.dlq

---

## 🚀 Функциональность

### ✔ Обработка событий
- Чтение событий из `orders.created`
- Валидация входящих данных
- Сохранение валидных заказов в БД

### ✔ Идемпотентность
- Повторные события игнорируются по `eventId`
- Исключает дубли в БД и повторную публикацию

### ✔ Retry + DLQ
- Технические ошибки → до 2 retry
- После исчерпания → orders.dlq

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

## 🔁 Retry & Dead Letter Queue (DLQ)
### Типы ошибок
#### 🟡 Business errors (валидация)
- невалидные события
- отправляются в orders.failed
- retry не выполняется
#### 🔴 Technical errors (временные сбои)
- выполняется до 2 retry
- после → orders.dlq

---

## 🧪 Тестирование

### 🔹 E2E тесты
- Kafka → Consumer → БД → Kafka
- Проверка полного асинхронного пайплайна

### 🔹 Интеграционные тесты
- REST → Kafka → БД → Kafka

### 🔹 Unit тесты
- Использование Mockito для изоляции зависимостей
- Проверка бизнес-логики без поднятия инфраструктуры

### 🔹 Негативные сценарии
- Невалидный заказ (отсутствуют поля)
- Отрицательная сумма
- Неподдерживаемая валюта

### 🔹 Идемпотентность
- Один и тот же event обрабатывается только один раз

### 🔹 Инфраструктура
- Kafka и PostgreSQL поднимаются через **Testcontainers**
- Используются реальные сервисы (без моков)

### 🔹 Retry / DLQ тест
- техническая ошибка → retries → DLQ
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
- установлен и запущен Docker (без поднятых контейнеров)

---

## 📡 API

### Создание заказа

```bash
POST /api/orders
```
### Ручное тестирование

✔ Пример успешного запроса:

```bash
{
  "orderId": "ORD-123",
  "userId": "USER-100",
  "amount": 1500.00,
  "currency": "RUB"
}
```

Ожидаемый ответ:

Status code: **202 Accepted**

```bash
Order event published
```
→ попадёт в orders.processed 

❌ Валидационная ошибка:


```bash
{
  "orderId": null,
  "userId": "USER-1",
  "amount": 1000,
  "currency": "RUB"
}
```

Ожидаемый ответ:

Status code: **400 Bad Request**

```bash
{
    "status": 400,
    "error": "Validation failed",
    "fields": {
        "orderId": "orderId must not be blank"
    }
}
```

→ попадёт в orders.failed

---


## 📈 Monitoring (Prometheus)

Приложение предоставляет метрики через Spring Boot Actuator:

```bash
http://localhost:8080/actuator/prometheus
```

Пример метрик:

| Метрика                                             | Описание             |
| --------------------------------------------------- | -------------------- |
| http_server_requests_seconds                        | HTTP latency         |
| spring_kafka_template_seconds                       | Kafka producer       |
| spring_kafka_listener_seconds                       | Kafka consumer       |
| kafka_consumer_fetch_manager_records_consumed_total | обработанные события |
| hikaricp_connections                                | пул соединений       |
| jvm_memory_used_bytes                               | использование памяти |

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
| orders.failed    | бизнес-ошибки               |
| orders.dlq       | технические ошибки          | 

---

## 📁 Структура проекта

```text
controller     → REST API
listener       → Kafka consumer
service        → бизнес-логика
repository     → доступ к БД
entity         → JPA сущности
dto            → события
config         → Kafka / DLQ / topics
support        → тестовые утилиты
```

---

## 🔄 CI (Continuous Integration)

В проекте используется GitHub Actions для запуска автоматических проверок при каждом push и pull request.

Workflow включает:
- настройку JDK 21
- кэширование зависимостей Gradle
- выполнение unit, integration и e2e тестов

Историю сборок можно посмотреть во вкладке Actions в репозитории.

---

## 💡 Ключевые особенности

- Использование Testcontainers для реалистичных тестов
- Покрытие happy-path и негативных сценариев
- retry + DLQ
- Тестирование асинхронных процессов через Awaitility
- Реализация идемпотентной обработки событий
- Практическое использование Kafka
- observability через Prometheus
- Автоматический запуск тестов через GitHub Actions (CI)
- Воспроизводимость тестового окружения без внешних зависимостей
- Комбинация подходов: реальные сервисы (Testcontainers) + mocking (unit-тесты)

---

## 🔮 Возможные доработки

- Добавление contract-тестирования (например, с использованием Pact)
- Расширение покрытия негативных сценариев
- Интеграция с системой отчетности (Allure Report)
- Добавление test coverage (JaCoCo)
- Параллельный запуск тестов
- Разделение тестов на уровни (unit / integration / e2e) с отдельными CI job
  
---

## ⭐ Цель проекта

Проект демонстрирует навыки, необходимые для роли QA / AQA / SDET:
- применение разных уровней тестирования (unit / integration / e2e)
- комбинирование mocking и real environment подходов
- тестирование backend-сервисов
- работа с Kafka и асинхронными процессами
- интеграционные и E2E тесты
- тестирование распределённых систем
- обработка ошибок и retry
- observability (метрики)
- использование Testcontainers для создания изолированного окружения
- применение mocking-подхода для контроля внешних зависимостей
- построение CI-процесса с использованием GitHub Actions
  
---

## 👤 Автор

Андрей Кузнецов
QA / AQA Engineer
