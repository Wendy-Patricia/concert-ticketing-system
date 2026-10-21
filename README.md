# Concert Ticketing System

API REST de catalogue de concerts développée avec Java 25 et Spring Boot 4.1.1.
Le périmètre disponible couvre les artistes, les salles et les concerts, avec
validation métier, persistance MySQL, migrations Flyway, HTTP Basic et documentation
OpenAPI interactive. Les fonctionnalités clients, achats et fidélité sont réservées
à la Personne B ; leurs règles d’accès sont préparées dans la sécurité.

## Technologies et prérequis

| Composant | Version ou rôle |
|---|---|
| Java | JDK 25 |
| Spring Boot | 4.1.1 |
| Spring MVC / Spring Data JPA | API REST et persistance Hibernate |
| Jakarta Validation | Validation des DTO et des entités |
| MySQL | 8.4, image Docker officielle |
| Flyway | Gestion du schéma SQL, version gérée par Spring Boot |
| Spring Security | HTTP Basic, BCrypt, autorisations HTTP et de méthode |
| springdoc-openapi | 3.1.1, OpenAPI et Swagger UI |
| Spring Boot Actuator | Santé et sondes de disponibilité |
| JUnit Jupiter / MockMvc / Spring Security Test | Tests automatisés |
| Testcontainers | Véritable MySQL 8.4 pour les tests de persistance et d’intégration |
| Maven Wrapper | Construction reproductible, sans installation Maven séparée |

Installer un JDK 25 et Docker Desktop avec le moteur de conteneurs Linux actif.
Le premier lancement nécessite un accès réseau pour les dépendances Maven et les
images Docker. Les commandes ci-dessous sont prévues pour PowerShell, depuis la
racine du dépôt. Sur Linux/macOS, utiliser `./mvnw` au lieu de `.\mvnw.cmd` et exporter
les mêmes variables d’environnement.

```powershell
java -version
docker version
docker compose version
```

## Architecture

```text
Requête HTTP
  → Spring Security (HTTP Basic et rôles)
  → Controller (@Valid, codes HTTP)
  → Service (@Transactional, règles métier)
  → Repository Spring Data JPA
  → MySQL (schéma créé par Flyway)
```

Les requêtes et réponses utilisent des `record` Java dans `dto` ; les entités JPA
ne sont pas exposées directement. Les services convertissent les entités en DTO
pendant la transaction ; les relations artiste/salle sont chargées avec le concert.
`CatalogExceptionHandler` centralise les erreurs du catalogue au format `ApiError`.

```text
src/main/java/com/example/concertticketingsystem/
├── config/        # Clock, sécurité et OpenAPI
├── controller/    # Endpoints REST et advice du catalogue
├── dto/           # Records de requête, réponse et erreur
├── entity/        # Artist, Venue, Concert
├── exception/     # Erreurs métier du catalogue
├── repository/    # Accès JPA
└── service/       # Logique métier transactionnelle
src/main/resources/db/migration/   # Migrations Flyway versionnées
src/test/java/                    # Tests JUnit, MVC, sécurité et MySQL
```

## Configuration Docker et MySQL

Créer le fichier local de configuration, puis renseigner deux mots de passe
différents et non vides :

```powershell
Copy-Item .env.example .env
notepad .env
```

| Variable | Valeur par défaut ou usage |
|---|---|
| `MYSQL_DATABASE` | `concert_ticketing` |
| `MYSQL_USER` | `concert_app` |
| `MYSQL_PASSWORD` | Mot de passe local du compte applicatif, à renseigner |
| `MYSQL_ROOT_PASSWORD` | Mot de passe d’initialisation MySQL, à renseigner |
| `MYSQL_PORT` | `3306` ; choisir par exemple `3307` si 3306 est indisponible |
| `MYSQL_HOST` | `127.0.0.1` pour l’application lancée sur le poste |

`.env` est ignoré par Git ; `.env.example` ne contient pas de secret réel.
Le service `mysql` publie son port uniquement sur `127.0.0.1`. Le volume nommé
`mysql_data` conserve les données. Le healthcheck exécute `SELECT 1` avec le compte
applicatif dans la base configurée.

```powershell
docker compose up -d --wait --wait-timeout 180
docker compose ps
```

Le service doit afficher `healthy`. Pour arrêter MySQL en conservant les données :

```powershell
docker compose down
```

Les variables d’initialisation MySQL ne s’appliquent qu’à un volume vide. Modifier
les mots de passe dans `.env` ne change pas ceux d’une base déjà initialisée.

## Démarrage de l’application

Spring Boot ne charge pas automatiquement le `.env` de Docker Compose. Dans le
terminal qui lancera Java, reprendre la configuration résolue par Compose sans
l’afficher, y compris la véritable valeur du port publié :

```powershell
$compose = docker compose config --format json | ConvertFrom-Json
$mysql = $compose.services.mysql
$env:MYSQL_HOST = '127.0.0.1'
$env:MYSQL_PORT = [string]$mysql.ports[0].published
$env:MYSQL_DATABASE = [string]$mysql.environment.MYSQL_DATABASE
$env:MYSQL_USER = [string]$mysql.environment.MYSQL_USER
$env:MYSQL_PASSWORD = [string]$mysql.environment.MYSQL_PASSWORD

.\mvnw.cmd spring-boot:run
```

L’application écoute par défaut sur `http://localhost:8080`. `Ctrl+C` l’arrête.
Les mêmes variables peuvent être renseignées dans la configuration d’exécution
de l’IDE. `MYSQL_ROOT_PASSWORD` n’est pas utilisé par l’application.

| Variable facultative | Effet |
|---|---|
| `SPRING_DATASOURCE_URL` | Remplace l’URL JDBC complète |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Remplacent les identifiants du datasource |
| `SERVER_PORT` | Port HTTP, 8080 par défaut |
| `APP_TIME_ZONE` | Fuseau du bean `Clock`, `Europe/Paris` par défaut |

Pour construire puis lancer le JAR, avec les variables exportées dans le même
terminal :

```powershell
.\mvnw.cmd clean verify
java -jar .\target\concert-ticketing-system-0.0.1-SNAPSHOT.jar
```

Arrêter le JAR avant une reconstruction qui doit nettoyer `target`.

## Migrations Flyway et modèle de données

Flyway est activé sur `classpath:db/migration`, avant la validation Hibernate.
`spring.jpa.hibernate.ddl-auto=validate` vérifie les tables sans les créer.
L’initialisation par `schema.sql`, la baseline automatique, l’exécution hors ordre
et le nettoyage Flyway sont désactivés.

| Version | État | Responsabilité |
|---|---|---|
| `V1__create_catalog.sql` | Présente | Catalogue : `artists`, `venues`, `concerts`, contraintes et index |
| `V2__create_ticketing.sql` | Réservée | Personne B : clients, fidélité et achats |
| `V3__insert_demo_data.sql` | Réservée | Données de démonstration |

Une migration appliquée à une base partagée ne doit plus être modifiée : ajouter
une nouvelle version pour toute évolution. L’historique est enregistré dans
`flyway_schema_history`. Voir [les conventions des migrations](src/main/resources/db/migration/README.md).

- `Artist` : `id` généré, nom obligatoire et unique, genre musical facultatif.
- `Venue` : nom et ville obligatoires, couple `(name, city)` unique, capacité
  entre 50 et 50 000 inclus.
- `Concert` : titre obligatoire, relations `ManyToOne` vers un artiste et une salle
  existants, date future, prix positif stocké en `DECIMAL(10,2)`.

Les contraintes SQL protègent les unicités, les capacités, les prix, les champs
obligatoires et les clés étrangères. La date future est contrôlée par Jakarta
Validation lors des écritures JPA ; elle n’est pas vérifiée par un `CHECK` SQL
utilisant l’heure courante.

## Endpoints du catalogue

| Méthode | URL | Résultat | Accès |
|---|---|---|---|
| GET | `/api/concerts` | Liste triée par date puis identifiant | Public |
| GET | `/api/concerts/{id}` | Détail avec artiste et salle | Public |
| POST | `/api/production/artistes` | Création d’un artiste | PRODUCTEUR ou ADMIN |
| POST | `/api/production/concerts` | Création d’un concert | PRODUCTEUR ou ADMIN |
| POST | `/api/admin/salles` | Création d’une salle | ADMIN |

Les filtres de liste sont facultatifs et combinables par ET :

| Paramètre | Type | Sémantique |
|---|---|---|
| `du` | Date ISO `YYYY-MM-DD` | Début de journée inclusif |
| `au` | Date ISO `YYYY-MM-DD` | Fin de journée inclusive, à la microseconde |
| `artiste` | Entier positif | Identifiant de l’artiste |

Exemple : `/api/concerts?du=2027-01-01&au=2027-12-31&artiste=1`.
Une période inversée renvoie 400. Une recherche valide sans résultat renvoie `[]`
avec 200. La liste n’est pas paginée dans le périmètre actuel.

Corps JSON de création :

```json
{"name":"Nina Simone","musicalGenre":"Jazz"}
```

```json
{"name":"Olympia","city":"Paris","capacity":500}
```

```json
{"title":"Jazz Night","artistId":1,"venueId":1,"dateTime":"2027-06-15T20:30:00","unitPrice":42.50}
```

Utiliser les identifiants renvoyés par les créations précédentes et une date
future au moment de l’appel. Le prix accepte au plus deux décimales et huit
chiffres entiers. Les créations répondent 201 ; le concert fournit aussi
`Location: /api/concerts/{id}`.

| Code HTTP | Signification |
|---|---|
| 200 | Lecture réussie |
| 201 | Ressource créée |
| 400 | JSON, paramètre ou validation invalide |
| 401 | Authentification absente ou incorrecte |
| 403 | Compte sans le rôle requis |
| 404 | Concert ou référence artiste/salle introuvable |
| 409 | Doublon ou conflit d’intégrité |

Les erreurs contiennent `timestamp`, `status`, `code`, `message` et `path`, sans
SQL ni trace interne. Exemple de code applicatif : `CONCERT_INTROUVABLE`.

## Sécurité et comptes de démonstration

| Identifiant | Mot de passe public de démonstration | Rôle |
|---|---|---|
| `client` | `ClientDemo123!` | CLIENT |
| `producteur` | `ProducteurDemo123!` | PRODUCTEUR |
| `admin` | `AdminDemo123!` | ADMIN |

Les mots de passe sont encodés avec BCrypt avant stockage en mémoire. Les comptes
sont recréés au démarrage et restent indépendants des clients métier. Les variables
`DEMO_CLIENT_PASSWORD`, `DEMO_PRODUCTEUR_PASSWORD`, `DEMO_ADMIN_PASSWORD` remplacent
ces valeurs pour un environnement distinct de la démonstration locale.

CLIENT dispose des achats et cartes de fidélité. ADMIN dispose de l’administration,
des droits du producteur et de la création de cartes, sans hériter du droit d’achat.
POST `/api/clients` est public. Ces routes métier restent à intégrer avec la Personne B.
Les chemins non répertoriés sont refusés.

L’API utilise HTTP Basic sans session, sans formulaire de connexion et sans cache
d’authentification entre requêtes. Les erreurs 401 comportent `WWW-Authenticate`.
CSRF est désactivé pour les clients REST explicites, sans authentification par
cookie. Stateless seul ne protège pas contre CSRF dans un navigateur qui envoie
Basic automatiquement : revoir ce choix si ce mode d’utilisation est introduit.
Utiliser HTTPS hors du poste local.

## Swagger / OpenAPI et Postman

| Ressource | URL locale |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| OpenAPI YAML | http://localhost:8080/v3/api-docs.yaml |
| Configuration de Swagger UI | http://localhost:8080/v3/api-docs/swagger-config |

La documentation est accessible sans authentification. Les tags Artistes, Salles
et Concerts présentent les DTO, contraintes, filtres, réponses HTTP et rôles.
Dans Swagger UI, cliquer sur **Authorize**, saisir un compte Basic, puis utiliser
**Try it out** sur une création. Les identifiants ne sont pas persistés entre
rechargements. Les lectures publiques restent accessibles sans compte.

Dans Postman, sélectionner **Authorization → Basic Auth**, puis **Body → raw → JSON**
pour les POST. Avec `producteur`, créer un artiste ou un concert ; avec `admin`,
créer une salle. Un POST artiste avec `client` renvoie 403 ; sans identifiants,
401. Un nom d’artiste déjà utilisé renvoie 409.

## Actuator et santé

```powershell
curl.exe -i http://localhost:8080/actuator/health
```

L’endpoint est public. Quand l’application et MySQL sont disponibles, il répond
200 avec `status: UP`. Un état de santé dégradé peut produire 503. Les détails
internes sont masqués ; seul l’endpoint `health` est exposé via Actuator.
Les sondes publiques sont `/actuator/health/liveness` et
`/actuator/health/readiness`. La santé globale inclut le datasource MySQL ; la sonde
readiness reflète la disponibilité applicative et ne constitue pas, à elle seule,
un contrôle complet de la base. Les endpoints exposés de santé sont inclus dans
la description OpenAPI.

## Tests et vérification

Docker doit être actif. Les tests Testcontainers démarrent un MySQL 8.4 isolé et
appliquent V1 ; ils n’utilisent pas les données du volume Compose. Il n’est pas
nécessaire de démarrer manuellement le service Compose pour ces tests.

```powershell
# Vérification complète et génération du JAR
.\mvnw.cmd clean verify

# Persistance, contraintes SQL et relations
.\mvnw.cmd "-Dtest=CatalogPersistenceTests" test

# Endpoints, filtres, validations et erreurs métier
.\mvnw.cmd "-Dtest=CatalogApiTests" test

# HTTP Basic, BCrypt, rôles, 401/403 et absence de session
.\mvnw.cmd "-Dtest=SecurityConfigTests,CatalogRolePreparationTests" test

# Swagger réel, contrat OpenAPI, Actuator et parcours HTTP sécurisé avec MySQL
.\mvnw.cmd "-Dtest=OpenApiAndHealthTests" test
```

Les tests JPA relisent les données après `flush`/`clear` et vérifient aussi les
contraintes par SQL direct. Les tests API combinent MockMvc et les véritables
services/repositories. Les tests de sécurité vérifient les règles HTTP et de
méthode ; les routes réservées à la Personne B utilisent uniquement des sondes
de test. La suite OpenAPI/Actuator utilise la véritable chaîne de sécurité,
les ressources Swagger et MySQL/Testcontainers.

Les rapports sont générés dans `target/surefire-reports`. Les résultats réellement
observés, les problèmes et les corrections sont consignés dans
[docs/test-results.md](docs/test-results.md). Le journal des outils IA et des prompts
se trouve dans [docs/ai-usage-log.md](docs/ai-usage-log.md).

## Références techniques

- [Conventions du projet](docs/CONVENTIONS.md).
- [Image officielle MySQL](https://hub.docker.com/_/mysql).
- [springdoc-openapi : installation et configuration](https://springdoc.org/).
- [Spring Boot Actuator : endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html).
- [Spring Security : HTTP Basic](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/basic.html).
