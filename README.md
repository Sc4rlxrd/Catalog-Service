# 📚 Catalog Service

> Microsserviço responsável pelo gerenciamento do catálogo de livros do **BookCommerce**, fornecendo validação de disponibilidade e preço para o fluxo de pedidos via eventos assíncronos.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-6DB33F?style=for-the-badge&logo=springboot)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)

---

## 📋 Sumário

- [Sobre](#-sobre)
- [Responsabilidades](#-responsabilidades)
- [Arquitetura Interna](#-arquitetura-interna)
- [Modelo de Dados](#-modelo-de-dados)
- [Endpoints](#-endpoints)
- [Fluxo de Eventos](#-fluxo-de-eventos)
- [Comunicação via RabbitMQ](#-comunicação-via-rabbitmq)
- [Segurança](#-segurança)
- [Rate Limiting](#-rate-limiting)
- [Tecnologias](#-tecnologias)
- [Como Rodar](#-como-rodar)

---

## 📖 Sobre

O **Catalog Service** gerencia o catálogo de livros do BookCommerce. Além de expor uma API REST para cadastro, consulta, busca e filtragem de livros, ele atua como **respondedor de validações** no fluxo orientado a eventos — consumindo solicitações do `order-service` e retornando disponibilidade e preço de cada livro com base no estoque real.

- **Porta:** `8081`
- **Banco de dados:** PostgreSQL (`books`)

---

## 🧠 Responsabilidades

- ✅ Cadastrar livros com validação de ISBN duplicado
- ✅ Listar livros com paginação
- ✅ Buscar livros por título ou autor (busca parcial, case-insensitive)
- ✅ Filtrar livros por faixa de preço
- ✅ Decrementar estoque após compra confirmada
- ✅ Consumir eventos de validação do `order-service` e retornar disponibilidade + preço

---

## 🏗️ Arquitetura Interna

### 📁 Estrutura de Pacotes

```
src/main/java/com/scarlxrd/catalog_service/
│
├── config/
│   ├── RateLimitInterceptor.java     # Interceptor de rate limiting (Bucket4j)
│   ├── WebConfig.java                # Registro de interceptors
│   └── rabbitmq/
│       ├── RabbitConfig.java         # Configuração de exchanges, queues e bindings
│       └── CatalogConsumer.java      # Consumidor de eventos do order-service
│
├── controller/
│   ├── BookController.java           # Endpoints REST
│   └── GlobalExceptionHandler.java   # Tratamento global de exceções
│
├── dto/
│   ├── CreateBookDTO.java            # Payload de criação de livro
│   ├── BookResponseDTO.java          # Resposta da API
│   ├── BookValidationRequest.java    # Evento recebido do order-service
│   └── BookValidatedEvent.java       # Evento publicado de volta ao order-service
│
├── entity/
│   └── Book.java                     # Entidade JPA
│
├── exception/
│   ├── BookAlreadyExistsException.java
│   ├── BookNotExistsException.java
│   ├── BusinessException.java
│   └── RateLimitException.java
│
├── mapper/
│   └── BookMapper.java               # Mapeamento DTO ↔ Entity (MapStruct)
│
├── repository/
│   └── BookRepository.java
│
└── service/
    └── BookService.java
```

---

## 🗃️ Modelo de Dados

### Entidade `Book`

| Campo        | Tipo           | Restrições               |
|--------------|----------------|--------------------------|
| `id`         | `UUID`         | PK, gerado automaticamente |
| `title`      | `String`       | Not null                 |
| `author`     | `String`       | Not null                 |
| `isbn`       | `String`       | Not null, único          |
| `price`      | `BigDecimal`   | Not null                 |
| `stock`      | `Integer`      | Not null                 |
| `createdAt`  | `LocalDateTime`| Not null, gerado no `@PrePersist` |

---

## 🚀 Endpoints

Base URL: `/books`

### Cadastrar Livro

**`POST`** `/books`

```json
// Request Body
{
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "978-0132350884",
  "price": 89.90,
  "stock": 50
}

// Response 201 Created
{
  "id": "uuid",
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "isbn": "978-0132350884",
  "price": 89.90,
  "stock": 50
}
```

> ⚠️ Retorna erro se o ISBN já existir no banco.

---

### Listar Livros (paginado)

**`GET`** `/books?page=0&size=10`

Paginação padrão: `page=0`, `size=10`.

---

### Buscar por Título

**`GET`** `/books/search/title?title=clean`

Busca parcial e case-insensitive.

---

### Buscar por Autor

**`GET`** `/books/search/author?author=robert`

Busca parcial e case-insensitive.

---

### Filtrar por Faixa de Preço

**`GET`** `/books/filter?min=20.00&max=100.00`

---

### Decrementar Estoque

**`PATCH`** `/books/{id}/stock/decrease?quantity=2`

- Lança erro se o estoque for insuficiente.
- Chamado internamente após confirmação de pedido.

---

### Buscar Favoritos do Usuário *(em desenvolvimento)*

**`GET`** `/books/my-favorites`

| Header         | Descrição                                       |
|----------------|-------------------------------------------------|
| `X-User-Email` | Injetado pelo `gateway-service` após validação JWT |

---

## 🔄 Fluxo de Eventos

### 1️⃣ Recebe solicitação de validação
Consome da fila `book.validate.queue` (publicada pelo `order-service`):

```json
{
  "orderId": "uuid",
  "bookId": "uuid",
  "quantity": 2
}
```

### 2️⃣ Processa a validação
- Busca o livro pelo `bookId`
- Verifica se `stock >= quantity`
- Obtém o preço atual (`BigDecimal`)

### 3️⃣ Publica o resultado
Publica na exchange `book.events` com routing key `book.validated`:

```json
{
  "orderId": "uuid",
  "bookId": "uuid",
  "isbn": "978-0132350884",
  "price": 89.90,
  "available": true,
  "quantity": 2
}
```

> ⚠️ Se `stock < quantity`, publica com `available = false` — o `order-service` cancelará o pedido automaticamente.

> ⚠️ Se o livro não for encontrado, lança `BookNotExistsException`.

---

## 📡 Comunicação via RabbitMQ

**Exchange:** `book.events`

| Routing Key      | Tipo     | Fila                    | Descrição                                         |
|------------------|----------|-------------------------|---------------------------------------------------|
| `book.validate`  | Consome  | `book.validate.queue`   | Recebe solicitação de validação do `order-service` |
| `book.validated` | Publica  | —                       | Retorna disponibilidade e preço ao `order-service` |

---

## 🔐 Segurança

Este serviço é **interno** ao BookCommerce e não deve ser exposto diretamente à internet. O endpoint `/books/my-favorites` recebe o e-mail do usuário autenticado via header `X-User-Email`, injetado pelo `gateway-service` após validação do JWT.

> ⚠️ **Nunca exponha este serviço diretamente à internet.** Todo acesso externo deve passar pelo `gateway-service`.

---

## ⚡ Rate Limiting

Implementado via **Bucket4j** com interceptor customizado (`RateLimitInterceptor`):

- Intercepta todas as requisições HTTP recebidas
- Protege os endpoints REST contra abuso
- Retorna `429 Too Many Requests` quando o limite é atingido

---

## ⚙️ Tecnologias

| Tecnologia        | Finalidade                          |
|-------------------|-------------------------------------|
| Java 21           | Linguagem principal                 |
| Spring Boot 4.0.3 | Framework base                      |
| Spring Data JPA   | Persistência e queries paginadas    |
| Spring Validation | Validação de DTOs (`@Valid`)        |
| PostgreSQL        | Banco de dados relacional           |
| Flyway            | Migrations do banco                 |
| RabbitMQ (AMQP)   | Mensageria assíncrona               |
| MapStruct         | Mapeamento DTO ↔ Entity             |
| Lombok            | Redução de boilerplate              |
| Bucket4j          | Rate limiting                       |
| Virtual Threads   | Concorrência leve (Java 21)         |

---

## 🐳 Como Rodar

### Pré-requisitos

- Java 21+
- Maven
- PostgreSQL na porta `5433` com banco `books`
- RabbitMQ na porta `5672`

### Subir a infra com Docker

```bash
cd docker
docker-compose up -d
```

### Executar o serviço

```bash
./mvnw spring-boot:run
```

### Configurações (`application.yaml`)

| Propriedade                   | Valor padrão                              |
|-------------------------------|-------------------------------------------|
| `server.port`                 | `8081`                                    |
| `spring.datasource.url`       | `jdbc:postgresql://localhost:5433/books`  |
| `spring.datasource.username`  | `postgres`                                |
| `spring.rabbitmq.host`        | `localhost`                               |
| `spring.rabbitmq.port`        | `5672`                                    |
| `spring.rabbitmq.username`    | `book_user`                               |

> 💡 Em produção, utilize variáveis de ambiente para não expor credenciais no `application.yaml`.

---

> Parte da arquitetura de microserviços do **BookCommerce** — integrado com `order-service` e `gateway-service`.