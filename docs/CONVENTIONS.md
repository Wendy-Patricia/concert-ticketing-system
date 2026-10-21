# Conventions techniques — Concert Ticketing System

## 1. Technologies et versions

| Technologie | Convention |
|---|---|
| Java | Java 25 |
| Spring Boot | 4.1.1 |
| Build | Maven Wrapper |
| Base de données | MySQL |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Sécurité | Spring Security, HTTP Basic, BCrypt |
| Tests | JUnit 5, Mockito, Testcontainers |
| Documentation API | OpenAPI / Swagger UI |
| Conteneurisation | Docker Compose |
| Frontend (si possible) | Thymeleaf |

## 2. Organisation des packages

Package racine : `com.example.concertticketingsystem`

```text
com.example.concertticketingsystem
├── config
│   ├── SecurityConfig
│   └── ClockConfig
├── entity
│   ├── Artist
│   ├── Venue
│   ├── Concert
│   ├── Customer
│   ├── LoyaltyCard
│   └── TicketPurchase
├── repository
│   ├── ArtistRepository
│   ├── VenueRepository
│   ├── ConcertRepository
│   ├── CustomerRepository
│   ├── LoyaltyCardRepository
│   └── TicketPurchaseRepository
├── service
│   ├── ArtistService
│   ├── VenueService
│   ├── ConcertService
│   ├── CustomerService
│   ├── LoyaltyCardService
│   └── TicketPurchaseService
├── controller
│   ├── ArtistController
│   ├── VenueController
│   ├── ConcertController
│   ├── CustomerController
│   ├── LoyaltyCardController
│   └── TicketPurchaseController
├── dto
│   ├── request
│   └── response
└── exception
    ├── GlobalExceptionHandler
    ├── ResourceNotFoundException
    ├── BusinessConflictException
    └── ApiError
```

**Convention de nommage :**

- Classes Java : `PascalCase`
- Méthodes et attributs : `camelCase`
- Packages : minuscules
- Constantes : `UPPER_SNAKE_CASE`
- Interfaces Repository : `NomEntiteRepository`
- DTO : suffixes `Request` et `Response`
- Tests : suffixe `Test` ou `IT`

## 3. Identifiants et relations JPA

Toutes les entités utilisent des identifiants de type `Long`.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

Entités et relations :

| Entité | Relations |
|---|---|
| Artist | Un artiste possède plusieurs concerts |
| Venue | Une salle accueille plusieurs concerts |
| Concert | ManyToOne vers Artist et Venue |
| Customer | Adresse intégrée avec `@Embedded` |
| LoyaltyCard | ManyToOne vers Customer |
| TicketPurchase | ManyToOne vers Customer et Concert |

Les montants utilisent `BigDecimal`, jamais `double` ni `float`.

Les dates utilisent les types de `java.time`.

Un `Clock` injectable sera utilisé pour rendre les règles temporelles testables.

## 4. Conventions SQL et Flyway

Les tables et colonnes utilisent `snake_case`, en anglais.

| Entité Java | Table MySQL |
|---|---|
| Artist | `artists` |
| Venue | `venues` |
| Concert | `concerts` |
| Customer | `customers` |
| LoyaltyCard | `loyalty_cards` |
| TicketPurchase | `ticket_purchases` |

**Règles SQL :**

- Clés primaires : `id BIGINT AUTO_INCREMENT`.
- Clés étrangères : `artist_id`, `venue_id`, `customer_id`, `concert_id`.
- Prix : `DECIMAL(10,2)`.
- Contraintes `NOT NULL` sur les champs obligatoires.
- `UNIQUE` sur `artists.name`, `customers.email` et `ticket_purchases.code`.
- `UNIQUE(name, city)` pour les salles.
- Contraintes adaptées pour la capacité et la quantité.
- Index sur les foreign keys et les champs utilisés dans les recherches.

**Répartition Flyway :**

- Personne A : `V1__create_catalog.sql` — artists, venues, concerts.
- Personne B : `V2__create_ticketing.sql` — customers, loyalty_cards, ticket_purchases.
- Personne A : `V3__insert_demo_data.sql` — données de démonstration.

Les migrations déjà appliquées ne doivent pas être modifiées.

Configuration Hibernate :

`spring.jpa.hibernate.ddl-auto=validate`

## 5. DTO et validation

Tous les DTO sont des `record` Java immuables.

Exemple :

```java
public record ConcertRequest(
    @NotBlank String title,
    @NotNull Long artistId,
    @NotNull Long venueId,
    @NotNull @Future LocalDateTime dateTime,
    @NotNull @DecimalMin("0.01") BigDecimal unitPrice
) {}
```

Conventions :

- `Request` pour les données reçues.
- `Response` pour les données renvoyées.
- Utiliser `@Valid` nos endpoints.
- Não expor diretamente as entidades JPA.
- A lógica de negócio pertence aos services.
- O preço total nunca é recebido do cliente.

## 6. Contrat des URLs REST

Prefixo para a API: `/api`.

| Méthode | URL | Accès |
|---|---|---|
| GET | `/api/concerts` | PUBLIC |
| GET | `/api/concerts/{id}` | PUBLIC |
| POST | `/api/clients` | PUBLIC |
| POST | `/api/production/artistes` | PRODUCTEUR / ADMIN |
| POST | `/api/production/concerts` | PRODUCTEUR / ADMIN |
| POST | `/api/admin/salles` | ADMIN |
| POST | `/api/clients/{id}/carte-fidelite` | CLIENT / ADMIN |
| POST | `/api/achats` | CLIENT |
| GET | `/actuator/health` | PUBLIC |
| GET | `/v3/api-docs` | PUBLIC |
| GET | `/swagger-ui.html` | PUBLIC |

Os caminhos REST permanecem em francês para respeitar exatamente o enunciado.

Os filtros de concertos são `du`, `au` e `artiste`.

## 7. Conventions HTTP et codes d'erreur

| Situation | HTTP | Code applicatif |
|---|---|---|
| Création réussie | 201 | — |
| Lecture réussie | 200 | — |
| Données invalides | 400 | `VALIDATION_ECHOUEE` |
| Concert inexistant | 404 | `CONCERT_INTROUVABLE` |
| Client inexistant | 404 | `CLIENT_INTROUVABLE` |
| Ressource inexistante | 404 | `RESSOURCE_INTROUVABLE` |
| Jauge insuffisante | 409 | `JAUGE_INSUFFISANTE` |
| Ressource déjà existante | 409 | `RESSOURCE_EXISTANTE` |
| Carte déjà active | 409 | `CARTE_DEJA_ACTIVE` |
| Authentification absente | 401 | Spring Security |
| Accès interdit | 403 | Spring Security |

Os códigos adicionais aos exemplos do enunciado são convenções internas do binómio.

Formato de resposta de erro:

```json
{
  "timestamp": "2026-10-21T09:00:00Z",
  "status": 409,
  "code": "JAUGE_INSUFFISANTE",
  "message": "Il ne reste que 2 places pour ce concert",
  "path": "/api/achats"
}
```

O tratamento dos erros deve ser centralizado com `@RestControllerAdvice`, sem expor stack traces ou detalhes SQL.

## 8. Architecture et responsabilités

Arquitetura adotada:

`Controller → Service → Repository → MySQL`

**Controller**

- Recebe pedidos HTTP.
- Valida os DTO.
- Chama o Service.
- Devolve respostas HTTP.
- Não executa SQL nem cálculos de negócio.

**Service**

- Implementa as regras de negócio.
- Gere as transações com `@Transactional`.
- Calcula preços e descontos.
- Verifica a disponibilidade de bilhetes.
- Lança exceções específicas.

**Repository**

- Estende `JpaRepository<Entity, Long>`.
- Executa consultas à base de dados.
- Implementa filtros e consultas específicas.
- Disponibiliza o bloqueio pessimista para compras.

**Entity**

- Representa os dados persistidos.
- Define relações e constraints JPA.
- Não é exposta diretamente através dos endpoints REST.

## 9. Contrat de concurrence

Le verrouillage des achats repose sur la base MySQL.

Personne A fournit dans `ConcertRepository` :

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Concert c WHERE c.id = :id")
Optional<Concert> findByIdForUpdate(
    @Param("id") Long id
);
```

Personne B appelle cette méthode dans une transaction `@Transactional`.

Le nombre de places restantes est défini par :

`venue.capacity - SUM(ticket_purchases.quantity)`

Le verrou doit être obtenu avant le calcul et l'enregistrement de l'achat.

Une vente ne peut jamais dépasser la capacité de la salle.

## 10. Travail parallèle et Git

Branches :

- `main` : code intégré et validé.
- `personne-a` : catalogue, infrastructure, sécurité.
- `personne-b` : clients, fidélité, achats.

Chaque personne développe et teste ses fonctionnalités sur sa propre branche.

Les changements concernant les fichiers communs sont annoncés avant intégration.

Les modifications sont fusionnées via Pull Request après vérification.

Format des commits :

- `feat: add concert entity`
- `feat: implement concert API`
- `fix: correct venue capacity validation`
- `test: add concert repository tests`
- `docs: update technical report`
- `chore: configure mysql docker compose`

## 11. Compte rendu et traçabilité

Chaque développeur documente :

- Les tâches réellement effectuées.
- Les choix techniques et leurs justifications.
- Les ressources documentaires consultées.
- Les outils IA utilisés, avec modèle et niveau si connus.
- Les prompts exacts et les modifications manuelles.
- Les tests, résultats attendus et résultats observés.
- Les bugs, captures d'écran et corrections.
- Les commits et Pull Requests.
- Les limites et les fonctionnalités non réalisées.

Arborescence :

```text
docs/
├── CONVENTIONS.md
├── compte-rendu.md
├── ai-usage-log.md
├── test-results.md
└── screenshots/
```

**Validation finale :**

`.\mvnw.cmd clean verify`

Les tests d'intégration MySQL exigent Docker démarré. Le JAR doit également être vérifié en dehors de l'IDE.

Les conventions sont adoptées après validation par les deux membres du binôme.