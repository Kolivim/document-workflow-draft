# Document Workflow Service

Backend-сервис по работе c документами.
Документы создаются, переводятся по статусам, по изменениям статуса ведётся история.

Дополнительно предусмотрена утилита для массового создания документов и фоновая обработка документов пачками

## Содержание

- [Стек технологий](#стек-технологий)
- [Быстрый старт](#быстрый-старт)
- [API Endpoints](#api-endpoints)
- [Конфигурация приложения](#конфигурация-приложения)
- [Схема базы данных](#схема-базы-данных)
- [Утилита массового создания документов](#утилита-массового-создания-документов)
- [Фоновая обработка](#фоновая-обработка)
- [Логирование](#логирование)
- [Тестирование](#тестирование)
- [Swagger UI](#swagger-ui)
- [Масштабирование и опциональные улучшения](#масштабирование-и-опциональные-улучшения)

## Стек технологий
Java 17 + Spring Boot 3

PostgreSQL 17

JPA/Hibernate

Liquibase

Maven

Docker Compose


## Быстрый старт

## Для запуска приложения используйте следующие команды

mvn clean package

java -jar target/document-worlflow-1.0-SNAPSHOT.jar


## Запуск PostgreSQL, через docker-compose
docker-compose up -d

# Остановка БД
docker-compose downdoc_workflow

# Альтернативный вариант через Docker, создание и запуск контейнера с БД
docker run -it -d ^
--name doc_wf ^
-e POSTGRES_PASSWORD=docWf ^
-e POSTGRES_USER=docWf ^
-e POSTGRES_DB=doc_workflow ^
-p 5432:5432 ^
postgres:17.6


## API endpoints

Базовый URL: `http://localhost:8080/api/v1/document`

### Основной контроллер (DocumentController)

| Метод  | Endpoint                   | Описание                                                |
|--------|----------------------------|---------------------------------------------------------|
| `POST` | `/`                        | Создать документ в статусе DRAFT                        |
| `GET`  | `/{id}`                    | Получить документ по ID                                 |
| `POST` | `/filter`                  | Поиск с фильтрацией                                     |
| `POST` | `/advancedFilter`          | Расширенный поиск                                       |
| `POST` | `/documents/noFoundIds`    | Получить документы, с информацией о ненайденных         |
| `POST` | `/documents/extendedPage`  | Получить документы, расширенная страница с заголовками  |
| `PUT`  | `/submit`                  | Отправить документы на согласование (DRAFT → SUBMITTED) |
| `PUT`  | `/approve`                 | Утвердить документы (SUBMITTED → APPROVED)              |

### Контроллер конкурентного утверждения (DocumentConcurrentController)

| Метод | Endpoint      | Описание                                 |
|-------|---------------|------------------------------------------|
| `PUT` | `/concurrent` | Тест конкурентного утверждения документа |


## Конфигурация приложения

Основные настройки в `application.yml`:

| Параметр                        | Описание                               | Значение по умолчанию                  |
|---------------------------------|----------------------------------------|----------------------------------------|
| `app.batch.size`                | Размер пачки для воркеров (batchSize)  | 10                                     |
| `app.batch.submit.fixed-delay`  | Интервал запуска SUBMIT-worker'а (мс)  | 100000                                 |
| `app.batch.approve.fixed-delay` | Интервал запуска APPROVE-worker'а (мс) | 100000                                 |
| `api.base-url`                  | Базовый URL для доступа к API сервиса  | http://localhost:8080/api/v1/document  |


## Утилита массового создания документов

### Конфигурация утилиты

Утилита поддерживает два способа настройки:

1. Через аргументы командной строки

| Параметр                | Описание                             | Пример                                            |
|-------------------------|--------------------------------------|---------------------------------------------------|
| `--generate` или `-g`   | Запустить генератор                  | `--generate`                                      |
| `--standalone` или `-s` | Завершить приложение после генерации | `--standalone`                                    |
| `--number=N`            | Количество документов в пачке        | `--number=500`                                    |
| `--count=N`             | Количество пачек, по умолчанию = 1   | `--count=1`                                       |
| `--author=NAME`         | Автор документов                     | `--author=test-user`                              |
| `--api-url=URL`         | URL API сервиса                      | `--api-url=http://localhost:8080/api/v1/document` |

2. Через встроенный файл generator.properties в ресурсах, расположен: src/main/resources/generator.properties

# Значения по умолчанию
# Количество документов в одной пачке (N согласно ТЗ)
number=50
# Количество пачек (по умолчанию = 1)
count=1
# Автор создаваемых документов
author=test-user
# Статус новых документов (всегда DRAFT)
status=DRAFT
# URL API сервиса для создания документов
api-url=http://localhost:8080/api/documents


# Для вызова выполните команду в консоли(CMD), открытой в каталоге проекта:

# Для Windows (корректное отображение Unicode)
chcp 65001

java -jar target/document-workflow-1.0-SNAPSHOT.jar --generate --standalone --server.port=0 ^
--app.workers.enabled=false ^
--number=100                ^
--count=1                   ^
--author=console-user       ^
--api-url=http://localhost:8080/api/v1/document


## Фоновая обработка

В сервисе работают два фоновых процесса для автоматической обработки документов. 

Настройки worker'ов задаются в application.yml:
app:
  batch:
    size: 10
    submit:
      fixed-delay: 100000
    approve:
      fixed-delay: 100000


1. SUBMIT-worker
   Что делает: Отправляет документы на согласование, DRAFT → SUBMITTED
   Период запуска: Каждые 100 000 мс (100 секунд)
   Размер пачки: 10 документов (настраивается через app.batch.size)

   Логика работы:
   Находит документы в статусе DRAFT
   Отправляет их пачками через API /submit
   Частичные ошибки не останавливают обработку
   Логирует прогресс и время выполнения

2. APPROVE-worker
   Что делает: Автоматически утверждает документы (SUBMITTED → APPROVED)
   Период запуска: Каждые 100 000 мс (100 секунд)
   Размер пачки: 10 документов (настраивается через app.batch.size)

   Логика работы:
   Находит документы в статусе SUBMITTED
   Отправляет их на утверждение через API /approve
   При успехе создаёт запись в реестре утверждений
   Частичные ошибки не останавливают обработк


## Тестирование

В проекте реализованы тесты, покрывающие ключевые сценарии:

Happy-path: создание, submit, approve одного документа

Пакетный submit с частичными результатами

Пакетный approve с частичными результатами

Откат approve при ошибке записи в регистр


## Swagger UI 
Документация API доступна по адресу:
http://localhost:8080/swagger-ui/index.html#


## Масштабирование и опциональные улучшения

### При масштабировании системы для уверенной обработки одним запросом 5000+ id

В текущей реализации пакетная обработка поддерживает до 1000 Id на запрос. 
Если потребуется обрабатывать запросы с **5000+ Id**, можно внести следующие изменения:

#### 1. Переход на асинхронную обработку HTTP-контроллеров

Синхронная обработка 5000+ документов займет слишком много времени (блокировка HTTP-соединения, таймауты)
Предоставит следующие преимущества: отсутствие таймаутов HTTP-соединения, отслеживание клиентом прогресса, 
возможность показывать частичные результаты, по мере обработки.

#### 2. Оптимизация запросов к БД

Запрос 'WHERE id IN (...)' с 5000+ параметрами может работать медленно, что можно решить несколькими путями: 
 - **Пакетная загрузка (Batch Loading)** - разбиение на несколько запросов по 500-1000 Id 
 - **Временная таблица** - вставка полученных из контроллера Id во временную таблицу и JOIN с ней имеющихся в 
существующих таблицах данных

#### 3. Оптимизация JPA/Hibernate

Оптимизация через массовое обновление с saveAll и batch_size, пачками по +/- 1000 штук, т.к. при обработке запросов 
с **5000+ Id**, текущая реализация (последовательная обработка каждого документа) станет слишком медленной. 
Оптимальным решением будет массовое обновление с использованием `saveAll` и настройкой `batch_size`.

#### 4. Оптимизация индексов

Добавление дополнительных индексов под высоковостребованные запросы, в т.ч. на статус-автора и дате создания
(т.к. по ней выполняется сортировка)

#### 5. Настройка пула соединений

Увеличение максимального количества соединений в пуле для обработки пиковых нагрузок

#### 6. Добавление Circuit Breaker и Retry

При обработке 5000+ Id в одном запросе нагрузка на БД резко возрастает. 
Если БД начнет перегружаться или временно упадет, все запросы клиентов начнут падать с ошибками. 
Circuit Breaker и Retry защищают систему от каскадных сбоев, 
**Retry** — автоматически повторит запрос при временных ошибках (таймаут, deadlock). 
Например, если БД перегружена, через 1 секунду попробует снова.
**Circuit Breaker** — отключает вызовы, если БД слишком долго не отвечает, что даёт ей возможность восстановиться. 
Когда БД снова заработает, автоматически включает обратно.

#### 7. Добавить шардирование таблицы документов по id

Шардирование по id можно рассмотреть только с учётом того что запросы между шардами без id сильно замедлятся и будут 
фактически выполнятся в каждом шарде отдельно и потом результаты сливаться в единый, например поиск по автору. 
Скорее уместно когда количество записей в таблице перешагнёт рубеж 100млн


### Масштабирование системы в виде выделения Реестра Утверждений в отдельную систему

В текущей реализации реестр утверждений находится в той же БД, что и основные данные. Это простое и надежное решение 
для большинства задач. Однако при росте нагрузки либо архитектурных изменениях можно рассмотреть вынос реестра 
в отдельный сервис.


#### Сравнение подходов

| Характеристика           | Текущая реализация (одна БД)   | Потенциальная архитектура (HTTP + Kafka)                                 |
|--------------------------|--------------------------------|--------------------------------------------------------------------------|
| **Время ответа клиенту** | 15-40 мс (одна транзакция)     | 30-60 мс (транзакция + HTTP до реестра)                                  |
| **Надежность**           | ACID, сильная согласованность  | Конечная согласованность + риск рассинхронизации (требуются компенсации) |
| **Пиковые нагрузки**     | БД может не справиться         | Kafka буферизует и сглаживает всплески, защищая БД                       |
| **Отказоустойчивость**   | При падении БД — всё падает    | При падении БД реестра — запросы буферизуются в Kafka                    |
| **Сложность**            | Низкая                         | Средняя / Высокая                                                        |


#### Потенциальная архитектура: Отдельный HTTP-сервис для реестра утверждений, с внутренней буферизацией через Kafka

Основной сервис документов Document Service общается с реестром **синхронно через HTTP**, но внутри сервиса реестра 
используется **Kafka для буферизации и гарантированной доставки**. Это дает сочетание простоты интеграции и 
повышенной надежности.


                     ┌─────────────────┐     HTTP     ┌─────────────────────────────────────┐
                     │  Document       │─────────────▶│      Registry Service (HTTP)        │
                     │  Service        │              │                                     │
                     └─────────────────┘              │  ┌─────────┐    ┌───────────────┐   │
                                                      │  │  Kafka  │───▶│  БД реестра   │   │
                                                      │  │  topic  │    └───────────────┘   │
                                                      │  └─────────┘                        │
                                                      └─────────────────────────────────────┘

#### Детально:

##### 1. **Document Service** в одной локальной транзакции:
   
   Обновляет статус документа на APPROVED

   Сохраняет запись в историю

   Коммитит транзакцию (документ считается утвержденным)

##### 2. **Document Service** отправляет **синхронный HTTP-запрос** на утверждение в Registry Service

##### 3. **Registry Service**:

   **Сразу возвращает HTTP 202 Accepted** (не ждет записи в БД реестра)
   
   Публикует событие во **внутренний Kafka топик** approval-register

##### 4. **Kafka внутри сервиса**:

   Хранит событие на диске
   
   Реплицирует для отказоустойчивости
   
   Гарантирует, что событие не потеряется даже при падении сервиса

##### 5. **Внутренний консьюмер** (в том же Registry Service):

   Читает события из Kafka

   Пытается сохранить запись в БД реестра

   При успехе — фиксирует offset (ручное подтверждение)

   При временной ошибке — **retry** (Kafka автоматически переотправит)

   При невосстановимой ошибке — отправляет в **Dead Letter Queue (DLQ)**


#### **Обработка ошибок и согласованность**

Если реестр не сможет сохранить запись (например, ошибка БД), срабатывает **паттерн компенсации**:

   Консьюмер обнаруживает проблему

   Отправляет компенсирующее событие

   Специальный слушатель откатывает статус документа обратно на SUBMITTED


Что касается согласованности с Document Service:

Между утверждением документа и записью в реестр существует окно несогласованности (секунды или десятки секунд), 
когда документ числится утвержденным, но в реестре его еще нет. Это плата за асинхронность и отказоустойчивость.

**Преимущества подхода**:
**Буферизация пиков** — Kafka сглаживает нагрузку на БД реестра
**Отказоустойчивость** — при падении Registry Service события не теряются
**Слабая связанность** — сервисы знают только о формате событий, а не друг о друге
**Масштабирование** — горизонтальное через партиции Kafka

**Компромиссы**:
**Время ответа** (порядка 30-60 мс) — больше, чем у имеющейся реализации с таблицей внутри одной БД
**Согласованность** — сильная заменяется на конечную с компенсациями
**Сложность** — появляются новые компоненты (Kafka, консьюмеры, DLQ, компенсации)


#### Резюмируя:

Предлагаемая архитектура с HTTP + Kafka **не дает выигрыша в скорости или простоте** по сравнению с текущей реализацией. 
Ее смысл — в **повышении отказоустойчивости, возможности переживать пиковые нагрузки и независимом масштабировании**. 

Для большинства проектов текущее решение (все в одной БД) является оптимальным. 
Описанный же подход — это вариант развития системы, когда таких возможностей перестанет хватать.


## Схема базы данных

### `documents` — основная таблица документов

| Поле          | Тип                        | Ограничения                             | Описание                                                                                      |
|---------------|----------------------------|-----------------------------------------|-----------------------------------------------------------------------------------------------|
| `id`          | `BIGINT`(autoIncrement)    | `PRIMARY KEY`                           | **Идентификатор** (уникальный номер, генерируется БД при создании), Surrogate Key             |
| `inner_id`    | `VARCHAR(255)`             | `NOT NULL`, **`UNIQUE`**                | **Внутренний номер документа** (Задаётся пользователем при создании, уникальный), Natural Key |
| `author`      | `VARCHAR(255)`             | `NOT NULL`                              | Автор документа                                                                               |
| `name`        | `VARCHAR(255)`             | `NOT NULL`                              | Название документа                                                                            |
| `status`      | `VARCHAR(50)`              | `NOT NULL`, `DEFAULT 'DRAFT'`           | Статус документа: DRAFT, SUBMITTED, APPROVED                                                  |
| `create_date` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Дата создания документа                                                                       |
| `update_date` | `TIMESTAMP WITH TIME ZONE` | `DEFAULT CURRENT_TIMESTAMP`             | Дата последнего обновления документа                                                          |

**Индексы:**
- `idx_documents_author` — для быстрого поиска по автору
- Уникальный индекс по `inner_id` (автоматически через UNIQUE constraint)

> **Значения поля `status`:** только `DRAFT`, `SUBMITTED`, `APPROVED`. 
> Любые другие значения будут отклонены на уровне приложения.


### `history` — история событий, совершенных с документами

| Поле          | Тип                        | Ограничения                    | Описание                        |
|---------------|----------------------------|--------------------------------|---------------------------------|
| `id`          | `BIGINT`(autoIncrement)    | `PRIMARY KEY`                  | Идентификатор записи истории    |
| `author`      | `VARCHAR`                  | `NOT NULL`                     | Инициатор события               |
| `date`        | `TIMESTAMP WITH TIME ZONE` | `NOT NULL`                     | Дата и время события            |
| `action`      | `VARCHAR`                  | `NOT NULL`                     | Событие: `SUBMIT`, `APPROVE`    |
| `comment`     | `VARCHAR`                  | —                              | Комментарий (может быть пустым) |
| `document_id` | `BIGINT`                   | `FOREIGN KEY` → `documents.id` | Ссылка на документ              |

**Связи:**
- Внешний ключ `fk_document_id` на `documents(id)`
- При удалении документа каскадного удаления истории **нет** (история хранится всегда)


### `register` — реестр утвержденных документов

| Поле  | Тип                     | Ограничения                                   | Описание                                                        |
|-------|-------------------------|-----------------------------------------------|-----------------------------------------------------------------|
| `id`  | `BIGINT`(autoIncrement) | `PRIMARY KEY`, `FOREIGN KEY` → `documents.id` | Идентификатор документа (он же ссылка на утвержденный документ) |

**Связи:**
- Внешний ключ `fk_register_documents` на `documents(id)`
- `id` одновременно является и первичным ключом, и внешним ключом

> **Примечание:** Реестр хранит только идентификаторы утвержденных документов. 
> Детальная информация об утверждении (кто и когда утвердил) содержится в 
> таблице `history` (записи с `action = 'APPROVE'`), то соответствует принципу DRY (Don't Repeat Yourself) и 
> позволяет избежать избыточности данных.



## Логирование

Логирование доступно: 

При запуске в IDE — в консоли

При запуске через java -jar — в консоли

В Docker: docker logs doc_wf -f

Также логи приложения записываются в директорию logs/ в корне проекта: logs/log


# Что логируется
1. Обработка документов, в т.ч. от создания в статусе DRAFT до дальнейшего движения по статусам
   2026-02-26T23:44:20.299+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1249, author: processApproveBatch, comment: 2026-02-26T23:44:20.200335300+03:00[Europe/Moscow]
   2026-02-26T23:44:20.306+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-8] r.k.d.w.s.impl.DocumentServiceImpl       : Конец транзакции для документа с Id: 1249, документ успешно утверждён
   2026-02-26T23:44:20.307+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ с Id: 2836 успешно обработан, статус изменен с DRAFT на SUBMITTED
   2026-02-26T23:44:20.307+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 2836 обработан со статусом: SUCCESS
   2026-02-26T23:44:20.307+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 8 из 10, с Id: 2836 отправлен на утверждение (SUBMITTED) со статусом: SUCCESS
   2026-02-26T23:44:20.307+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 2837, author: processSubmitBatch, comment: 2026-02-26T23:44:20.199790600+03:00[Europe/Moscow]
   2026-02-26T23:44:20.308+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-8] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 10 из 10, с Id: 1249 утверждён (APPROVE) со статусом: SUCCESS
   2026-02-26T23:44:20.308+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-8] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 10, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1240, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=1241, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=1242, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=1243, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=1244, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=1245, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=1246, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=1247, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=1248, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=1249, operationStatus=SUCCESS)]
   2026-02-26T23:44:20.310+03:00  INFO 27700 --- [document-workflow] [   doc-worker-1] r.k.d.workflow.client.DocumentApiClient  : Получен ответ от /approve API : statusCode = 200 OK
   2026-02-26T23:44:20.310+03:00  INFO 27700 --- [document-workflow] [   doc-worker-1] r.k.d.workflow.client.DocumentApiClient  : Документы успешно отправлены на утверждение (Approve) в количестве 10
   2026-02-26T23:44:20.310+03:00  INFO 27700 --- [document-workflow] [   doc-worker-1] r.k.d.w.s.DocumentProcessingService      : Успешно отправлено на утверждение (Approve) 10 документов, статус Approve успешно присвоен документам с Id: [1240, 1241, 1242, 1243, 1244, 1245, 1246, 1247, 1248, 1249]
   2026-02-26T23:44:20.310+03:00  INFO 27700 --- [document-workflow] [   doc-worker-1] r.k.d.w.s.DocumentProcessingService      : Завершена обработка пачки документов на утверждение (APPROVE), в которой всего 10 документов, из них успешно обработано 10, осталось/ошибок 0
   2026-02-26T23:44:20.311+03:00  INFO 27700 --- [document-workflow] [   doc-worker-1] r.k.d.workflow.worker.ApproveWorker      : endMethod
   2026-02-26T23:44:20.318+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ с Id: 2837 успешно обработан, статус изменен с DRAFT на SUBMITTED
   2026-02-26T23:44:20.318+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 2837 обработан со статусом: SUCCESS
   2026-02-26T23:44:20.318+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 9 из 10, с Id: 2837 отправлен на утверждение (SUBMITTED) со статусом: SUCCESS
   2026-02-26T23:44:20.318+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 2838, author: processSubmitBatch, comment: 2026-02-26T23:44:20.199790600+03:00[Europe/Moscow]
   2026-02-26T23:44:20.331+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ с Id: 2838 успешно обработан, статус изменен с DRAFT на SUBMITTED
   2026-02-26T23:44:20.331+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 2838 обработан со статусом: SUCCESS
   2026-02-26T23:44:20.331+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 10 из 10, с Id: 2838 отправлен на утверждение (SUBMITTED) со статусом: SUCCESS
   2026-02-26T23:44:20.331+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-7] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершена отправка на утверждение (SUBMITTED) полученного списка размером: 10, submitDocumentDtoList: [DocumentSubmitResponseDto(id=2829, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=2830, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=2831, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=2832, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=2833, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=2834, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=2835, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=2836, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=2837, operationStatus=SUCCESS), DocumentSubmitResponseDto(id=2838, operationStatus=SUCCESS)]
   2026-02-26T23:44:20.333+03:00  INFO 27700 --- [document-workflow] [   doc-worker-2] r.k.d.workflow.client.DocumentApiClient  : Получен ответ от /submit API: statusCode = 200 OK
   2026-02-26T23:44:20.333+03:00  INFO 27700 --- [document-workflow] [   doc-worker-2] r.k.d.workflow.client.DocumentApiClient  : Документы успешно отправлены на согласование (SUBMITTED), обработано: 10
   2026-02-26T23:44:20.333+03:00  INFO 27700 --- [document-workflow] [   doc-worker-2] r.k.d.w.s.DocumentProcessingService      : Успешно отправлено на согласование 10 документов статус SUBMITTED присвоен документам с Id: [2829, 2830, 2831, 2832, 2833, 2834, 2835, 2836, 2837, 2838]
   2026-02-26T23:44:20.333+03:00  INFO 27700 --- [document-workflow] [   doc-worker-2] r.k.d.w.s.DocumentProcessingService      : Завершена отправка пачки документов на утверждение (SUBMITTED), в которой всего 10 документов, из них успешно обработано 10, осталось/ошибок 0
   2026-02-26T23:44:20.334+03:00  INFO 27700 --- [document-workflow] [   doc-worker-2] r.k.d.workflow.worker.SubmitWorker       : Отправлено на SUBMIT 10 документов за 155 мс
   2026-02-26T23:44:20.334+03:00  INFO 27700 --- [document-workflow] [   doc-worker-2] r.k.d.workflow.worker.SubmitWorker       : endMethod
   2026-02-26T23:44:43.465+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentDto: DocumentDto(id=null, innerId=4c559924-ffc6-44ae-a05a-e1a4e9014115, author=I'm author, name=MyDoc, description=Описание, status=null, createDate=2020-01-28T23:59:59Z, updateDate=null, historySet=null, register=null)
   2026-02-26T23:44:43.468+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-9] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, к возврату documentDto: DocumentDto(id=null, innerId=4c559924-ffc6-44ae-a05a-e1a4e9014115, author=I'm author, name=MyDoc, description=Описание, status=null, createDate=2020-01-28T23:59:59Z, updateDate=null, historySet=null, register=null)


2. Фоновая обработка:

   2026-02-26T22:54:55.297+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.workflow.worker.ApproveWorker      : startMethod
   2026-02-26T22:54:55.304+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.w.s.DocumentProcessingService      : startMethod поиск документов со статусом SUBMITTED, для отправки на утверждение, размер пакета batchSize: 10
   2026-02-26T22:54:55.413+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.w.s.DocumentProcessingService      : Найдено для Утверждения (Approve) 10 документов со статусом SUBMITTED
   2026-02-26T22:54:55.413+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.workflow.client.DocumentApiClient  : startMethod, к утверждению (APPROVE)  request: DocumentsRequestDto(ids=[1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009], author=processApproveBatch, comment=2026-02-26T22:54:55.413910100+03:00[Europe/Moscow])
   2026-02-26T22:54:55.413+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.workflow.client.DocumentApiClient  : Отправка запроса (APPROVED) на URL: http://localhost:8080/api/v1/document/approve для 10 документов
   2026-02-26T22:54:55.808+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.workflow.client.DocumentApiClient  : Получен ответ от /approve API : statusCode = 200 OK
   2026-02-26T22:54:55.808+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.workflow.client.DocumentApiClient  : Документы успешно отправлены на утверждение (Approve) в количестве 10
   2026-02-26T22:54:55.809+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.w.s.DocumentProcessingService      : Успешно отправлено на утверждение (Approve) 10 документов, статус Approve успешно присвоен документам с Id: [1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009]
   2026-02-26T22:54:55.809+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.w.s.DocumentProcessingService      : Завершена обработка пачки документов на утверждение (APPROVE), в которой всего 10 документов, из них успешно обработано 10, осталось/ошибок 0
   2026-02-26T22:54:55.811+03:00  INFO 35680 --- [document-workflow] [   doc-worker-1] r.k.d.workflow.worker.ApproveWorker      : endMethod


4. Утилита генерации:

   2026-02-26T22:04:52.003+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  : ╔══════════════════════════════════════════════════════════════╗
   2026-02-26T22:04:52.004+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  : ║         УТИЛИТА МАССОВОГО СОЗДАНИЯ ДОКУМЕНТОВ                                       ║
   2026-02-26T22:04:52.004+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  : ╚══════════════════════════════════════════════════════════════╝
   2026-02-26T22:04:52.004+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  :  Получены следующие параметры генерации Документов:
   2026-02-26T22:04:52.004+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  :  📊 Количество пачек для создания: 1 (по умолчанию = 1)
   2026-02-26T22:04:52.004+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  :  📦 Количество документов для создания, в 1 пачке: 50
   2026-02-26T22:04:52.004+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  :  👤 Автор: test-user
   2026-02-26T22:04:52.004+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  :  📌 Статус: DRAFT
   2026-02-26T22:04:52.004+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  :  🌐 API URL: http://localhost:8080/api/v1/document
   2026-02-26T22:04:52.006+03:00 ERROR 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.GeneratorProperties  : endMethod
   2026-02-26T22:04:52.006+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Получено для создания следующее количество документов: 50
   2026-02-26T22:04:52.719+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : startMethod, получено для создания следующее количество документов: 50
   2026-02-26T22:04:52.720+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : startMethod, пачка 1 из 1 (для создания 50 документов)
   2026-02-26T22:04:52.720+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : startMethod, подготовка создания документа 1 из 50
   2026-02-26T22:04:52.948+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Создан Документ 1/50 - с Id: 3479 за 228 мс
   2026-02-26T22:04:52.948+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod, отправлен запрос на создание документа 1 из 50
   2026-02-26T22:04:52.949+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : startMethod, подготовка создания документа 2 из 50
   2026-02-26T22:04:52.959+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Создан Документ 2/50 - с Id: 3480 за 10 мс
   2026-02-26T22:04:52.959+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod, отправлен запрос на создание документа 2 из 50
   2026-02-26T22:04:52.960+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : startMethod, подготовка создания документа 3 из 50
   2026-02-26T22:04:52.967+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Создан Документ 3/50 - с Id: 3481 за 7 мс
   2026-02-26T22:04:53.043+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Создан Документ 10/50 - с Id: 3488 за 9 мс
   2026-02-26T22:04:53.045+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Прогресс: для документа 10 из 50 составляет 20% | Прошло: 326 мс | Прогнозируемое оставшееся время ~ 1304 мс
   2026-02-26T22:04:53.045+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod, отправлен запрос на создание документа 10 из 50
   2026-02-26T22:04:53.431+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Создан Документ 50/50 - с Id: 3528 за 8 мс
   2026-02-26T22:04:53.432+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Прогресс: для документа 50 из 50 составляет 100% | Прошло: 713 мс | Прогнозируемое оставшееся время ~ 0 мс
   2026-02-26T22:04:53.432+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod, отправлен запрос на создание документа 50 из 50
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod, создание Документов пачки 1 из 1 завершено за 713 мс, создано Документов: 50
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : startMethod
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ╔══════════════════════════════════════════════════════════════╗
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ║                     ГЕНЕРАЦИЯ ЗАВЕРШЕНА                                             ║
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ╚══════════════════════════════════════════════════════════════╝
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ⏱️  Время выполнения: 714 мс (0 сек)
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ✅ Успешно создано: 50
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ❌ Ошибок: 0
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Процент успеха: 100.0 %
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  :
   2026-02-26T22:04:53.433+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : 📋 Созданные ID документов (первые 20):
   2026-02-26T22:04:53.435+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  :    3479, 3480, 3481, 3482, 3483, 3484, 3485, 3486, 3487, 3488, 3489, 3490, 3491, 3492, 3493, 3494, 3495, 3496, 3497, 3498
   2026-02-26T22:04:53.435+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  :    ... и еще 30 документов
   2026-02-26T22:04:53.435+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  :
   2026-02-26T22:04:53.435+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Всего создано документов: 50
   2026-02-26T22:04:53.436+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  :
   2026-02-26T22:04:53.436+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Среднее время на документ: 14 мс
   2026-02-26T22:04:53.436+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod
   2026-02-26T22:04:53.436+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod
   2026-02-26T22:04:53.436+03:00  INFO 15216 --- [document-workflow] [           main] r.k.d.w.u.generator.DocumentGenerator    : ✅ Генерация завершена, выход из утилиты массового создания Документов

   Или

   2026-02-26T21:56:04.114+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod, отправлен запрос на создание документа 49 из 50
   2026-02-26T21:56:04.114+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : startMethod, подготовка создания документа 50 из 50
   2026-02-26T21:56:04.116+03:00 ERROR 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Ошибка создания документа 50 RestClientException: I/O error on POST request for "http://localhost:8080/api/v1/document": null
   2026-02-26T21:56:04.116+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod, отправлен запрос на создание документа 50 из 50
   2026-02-26T21:56:04.116+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : endMethod, создание Документов пачки 1 из 1 завершено за 334 мс, создано Документов: 50
   2026-02-26T21:56:04.116+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : startMethod
   2026-02-26T21:56:04.117+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ╔══════════════════════════════════════════════════════════════╗
   2026-02-26T21:56:04.117+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ║                     ГЕНЕРАЦИЯ ЗАВЕРШЕНА                      ║
   2026-02-26T21:56:04.117+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ╚══════════════════════════════════════════════════════════════╝
   2026-02-26T21:56:04.117+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ⏱️  Время выполнения: 335 мс (0 сек)
   2026-02-26T21:56:04.117+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ✅ Успешно создано: 0
   2026-02-26T21:56:04.117+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : ❌ Ошибок: 50
   2026-02-26T21:56:04.117+03:00  INFO 16328 --- [document-workflow] [           main] r.k.d.w.util.generator.GeneratorService  : Процент успеха: 0.0 %


5. Конкурентное утверждение:
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [nio-8080-exec-4] .k.d.w.s.i.DocumentConcurrentServiceImpl : Начало работы 10 шт Threads c 5 шт попыток в каждом
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 1 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 3 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 2 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 4 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 5 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 7 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 8 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 9 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 10 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 6 начало выполнения попытки APPROVE attemptNumber: 1
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 1, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 7, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 10, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 4, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 5, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 6, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 8, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 9, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 1, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 2, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 6, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.153+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 3, comment=попытка номер: 1), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 9, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 1
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 8, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 1
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 7, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 4, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 1
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 10, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 5, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 1
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 3, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 1
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 2, comment:попытка номер: 1, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 1
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 1
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 1
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 1
   2026-02-26T23:59:20.154+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 1
   2026-02-26T23:59:20.155+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 1
   2026-02-26T23:59:20.155+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 1
   2026-02-26T23:59:20.155+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 1
   2026-02-26T23:59:20.155+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 1
   2026-02-26T23:59:20.155+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 1
   2026-02-26T23:59:20.155+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 1
   2026-02-26T23:59:20.155+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 1
   2026-02-26T23:59:20.155+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 1
   2026-02-26T23:59:20.155+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 1
   2026-02-26T23:59:20.167+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Конец транзакции для документа с Id: 1464, документ успешно утверждён
   2026-02-26T23:59:20.182+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: SUCCESS
   2026-02-26T23:59:20.182+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 1
   2026-02-26T23:59:20.182+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=SUCCESS)]
   2026-02-26T23:59:20.184+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 6 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.184+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 6, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.184+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 6, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.184+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 2
   2026-02-26T23:59:20.184+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.186+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.187+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 2
   2026-02-26T23:59:20.187+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.187+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 8 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.187+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 8, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.187+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 8, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.187+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 2
   2026-02-26T23:59:20.188+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.188+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 2
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 1 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 2 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 1, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 2, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 1, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 2
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 2, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 2
   2026-02-26T23:59:20.189+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 2
   2026-02-26T23:59:20.190+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.191+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.191+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.191+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.191+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 4 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.191+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 2
   2026-02-26T23:59:20.192+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 4, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.192+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 4, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.192+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 2
   2026-02-26T23:59:20.192+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.192+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.192+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.192+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 2
   2026-02-26T23:59:20.192+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 6 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.192+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 6, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.193+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 6, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.193+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 3
   2026-02-26T23:59:20.194+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.194+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.194+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.194+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.194+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 3
   2026-02-26T23:59:20.194+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 7 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.194+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 7, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.194+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 7, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.195+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 2
   2026-02-26T23:59:20.195+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.195+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.195+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 2
   2026-02-26T23:59:20.195+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.195+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.195+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.195+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 8 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.195+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 2 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.196+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 8, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.196+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 2, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.196+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 8, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.196+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 3
   2026-02-26T23:59:20.196+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 2, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.196+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 3
   2026-02-26T23:59:20.196+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 3
   2026-02-26T23:59:20.197+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.197+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.197+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 3
   2026-02-26T23:59:20.197+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.197+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.197+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 5 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.197+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 5, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.197+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 5, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.197+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 2
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 2
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 4 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 1 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 4, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 1, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 1, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 4, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 3
   2026-02-26T23:59:20.198+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 3
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 3
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 3
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 6 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 10 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 10, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 6, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.199+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 10, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.200+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 2
   2026-02-26T23:59:20.200+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 6, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.200+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 4
   2026-02-26T23:59:20.200+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 2
   2026-02-26T23:59:20.200+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.200+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.200+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.201+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.201+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 7 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 4
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 9 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 8 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 8, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 7, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 9, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 8, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 7, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 4
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 3
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 9, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 2
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 3
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 4
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 2 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 2
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 2, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.202+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 2, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.203+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 4
   2026-02-26T23:59:20.203+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.203+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.203+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 4
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 5 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 5, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 5, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 3
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 3 начало выполнения попытки APPROVE attemptNumber: 2
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 3
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 3, comment=попытка номер: 2), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 3, comment:попытка номер: 2, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.204+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 2
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 2
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 1 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 4 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 1, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 4, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 1, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 4
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 4, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.205+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 4
   2026-02-26T23:59:20.206+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 4
   2026-02-26T23:59:20.206+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.206+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 4
   2026-02-26T23:59:20.206+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.206+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 10 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.206+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 10, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.206+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 10, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.206+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 3
   2026-02-26T23:59:20.207+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.207+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.207+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.207+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.208+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.208+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.208+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.208+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.208+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 3
   2026-02-26T23:59:20.208+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.208+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.208+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.208+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 8 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 6 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 9 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 7 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 7, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 9, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 8, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 7, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 6, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 9, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 6, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 3
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 4
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 8, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 5
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 5
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 6, comment: попытка номер: 5
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 4
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 3
   2026-02-26T23:59:20.209+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.211+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.211+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.211+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 8, comment: попытка номер: 5
   2026-02-26T23:59:20.211+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.211+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.211+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.211+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.211+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 2 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.211+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 2, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.212+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 2, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.212+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 5
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 2, comment: попытка номер: 5
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 3 начало выполнения попытки APPROVE attemptNumber: 3
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 4 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 4, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 3, comment=попытка номер: 3), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 4, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 5
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 5 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 1 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 3, comment:попытка номер: 3, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 3
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 5, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 4, comment: попытка номер: 5
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 3
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 5, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 4
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 4
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 1, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 1, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.213+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 5
   2026-02-26T23:59:20.215+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.215+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.215+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.215+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 1, comment: попытка номер: 5
   2026-02-26T23:59:20.216+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 9 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 7 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-6] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 10 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 10, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 9, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 10, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 9, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 4
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 4
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 7, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 7, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 5
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 7, comment: попытка номер: 5
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 4
   2026-02-26T23:59:20.217+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 4
   2026-02-26T23:59:20.218+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.218+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-8] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.218+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.218+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.218+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.218+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.219+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.219+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.219+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.219+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-4] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.219+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.219+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-2] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.219+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.219+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 3 начало выполнения попытки APPROVE attemptNumber: 4
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 5 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 5, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 3, comment=попытка номер: 4), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 5, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 5
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 3, comment:попытка номер: 4, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 4
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 5, comment: попытка номер: 5
   2026-02-26T23:59:20.220+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 4
   2026-02-26T23:59:20.222+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.222+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.222+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.223+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.223+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.223+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.224+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-1] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.224+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.224+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-7] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.224+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 9 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.224+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 9, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.224+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.224+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 9, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.224+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 5
   2026-02-26T23:59:20.224+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 9, comment: попытка номер: 5
   2026-02-26T23:59:20.225+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.225+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.225+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 10 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.225+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 10, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.225+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 10, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.225+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 5
   2026-02-26T23:59:20.225+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 10, comment: попытка номер: 5
   2026-02-26T23:59:20.225+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.225+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.226+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.226+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.226+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-5] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.226+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.226+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] .k.d.w.s.i.DocumentConcurrentServiceImpl : В Thread с threadNumber: 3 начало выполнения попытки APPROVE attemptNumber: 5
   2026-02-26T23:59:20.226+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentsRequestDto: DocumentsRequestDto(ids=[1464], author=concurrentApprove threadNumber: 3, comment=попытка номер: 5), pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.227+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, начало утверждения (APPROVED) полученного списка размером: 1, documentsRequestDto: [1464], author: concurrentApprove threadNumber: 3, comment:попытка номер: 5, pageable: Page request [number: 0, size 1, sort: UNSORTED]
   2026-02-26T23:59:20.227+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 5
   2026-02-26T23:59:20.227+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Начало транзакции для обработки документа c Id: 1464, author: concurrentApprove threadNumber: 3, comment: попытка номер: 5
   2026-02-26T23:59:20.228+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.230+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.230+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-9] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.230+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.231+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.231+03:00  INFO 32884 --- [document-workflow] [ool-2-thread-10] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.232+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Конфликт при обновлении статуса документа 1464: APPROVED
   2026-02-26T23:59:20.233+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : Документ 1 из 1, с Id: 1464 утверждение (перевод в статус APPROVE) завершено со статусом операции OperationStatus: CONFLICT
   2026-02-26T23:59:20.233+03:00  INFO 32884 --- [document-workflow] [pool-2-thread-3] r.k.d.w.s.impl.DocumentServiceImpl       : endMethod, завершено утверждение (APPROVED) полученного списка размером: 1, к возврату submitDocumentDtoList: [DocumentSubmitResponseDto(id=1464, operationStatus=CONFLICT)]
   2026-02-26T23:59:20.233+03:00  INFO 32884 --- [document-workflow] [nio-8080-exec-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, id: 1464
   2026-02-26T23:59:20.235+03:00  INFO 32884 --- [document-workflow] [nio-8080-exec-4] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, documentId: 1464
   2026-02-26T23:59:20.238+03:00  INFO 32884 --- [document-workflow] [nio-8080-exec-4] .k.d.w.s.i.DocumentConcurrentServiceImpl : endMethod, количество записей Success: 1, Conflict: 49, Error: 0, Итоговый статус: APPROVED, количество записей в Реестре: 1, isSuccessWorkApprove: true


   2026-02-26T23:52:34.567+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-8] r.k.d.w.s.impl.DocumentServiceImpl       : startMethod, id: 1462
   2026-02-26T23:52:34.571+03:00  INFO 27700 --- [document-workflow] [nio-8080-exec-8] .k.d.w.s.i.DocumentConcurrentServiceImpl : Не корректный статус документа с documentId: 1462


## Контакты и вопросы
По вопросам использования, развития и изменения проекта обращайтесь к разработчику











