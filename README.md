# Concert Ticketing System

## Installation rapide — Windows / PowerShell

Il te faut **Git, JDK 25 et Docker Desktop**. Ouvre Docker Desktop et attends que le moteur Linux soit actif. Tu n’as pas besoin d’installer Maven : le projet inclut le Maven Wrapper.

### 1. Récupérer le projet

```powershell
git clone --branch develop https://github.com/Wendy-Patricia/concert-ticketing-system.git
cd concert-ticketing-system
java -version
docker compose version
```

Si tu as déjà le projet, ouvre PowerShell dans le dossier contenant `pom.xml` et `compose.yaml`. La commande `java -version` doit indiquer Java 25.

### 2. Configurer la base de données

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Dans le fichier `.env`, renseigne `MYSQL_PASSWORD` et `MYSQL_ROOT_PASSWORD` avec deux mots de passe locaux différents. Conserve les autres options de l’exemple. Si le port 3306 est occupé, change `MYSQL_PORT=3307`.

Enregistre le fichier. Le `.env` est privé et ignoré par Git ; ne publie pas de mots de passe.

### 3. Démarrer MySQL

```powershell
docker compose up -d --wait --wait-timeout 180
docker compose ps
```

Le service `mysql` doit apparaître avec l’état **healthy**. Les données sont conservées dans un volume Docker.

### 4. Démarrer l’application

Spring Boot ne charge pas automatiquement le `.env`. Exécute ce bloc dans PowerShell, à la racine du projet, pour importer la configuration MySQL et démarrer l’application :

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

Garde ce terminal ouvert. Au premier démarrage, Maven télécharge les dépendances et Flyway applique automatiquement les migrations de la base de données.

### 5. Accéder et tester

| Page | Adresse |
|---|---|
| Swagger — consulter et tester l’API | http://localhost:8080/swagger-ui.html |
| État de l’application — doit indiquer `UP` | http://localhost:8080/actuator/health |
| Liste des concerts | http://localhost:8080/api/concerts |
| Inscription des clients | http://localhost:8080/clients/inscription |

Pour les endpoints protégés, utilise **Authorize** dans Swagger ou **Basic Auth** dans Postman :

| Rôle | Utilisateur | Mot de passe de démonstration |
|---|---|---|
| CLIENT | `client` | `ClientDemo123!` |
| PRODUCTEUR | `producteur` | `ProducteurDemo123!` |
| ADMIN | `admin` | `AdminDemo123!` |

Ces comptes sont destinés à la démonstration locale.

Pour exécuter tous les tests, ouvre un autre terminal dans le dossier du projet, avec Docker Desktop actif :

```powershell
.\mvnw.cmd clean verify
```

Le résultat attendu est **BUILD SUCCESS**. Les tests d’intégration utilisent un MySQL isolé via Testcontainers.

### Arrêter et résoudre les problèmes courants

- **Arrêter l’application :** `Ctrl+C` dans le terminal où elle s’exécute.
- **Arrêter MySQL en conservant les données :** `docker compose down`.
- **Docker indisponible :** démarre Docker Desktop et réessaie.
- **Port 3306 occupé :** utilise `MYSQL_PORT=3307` dans le `.env`, répète l’étape 3 puis l’étape 4.
- **Port 8080 occupé :** exécute `$env:SERVER_PORT='8081'` avant de démarrer l’application et utilise le port 8081 dans les adresses.
- **Accès MySQL refusé après modification des mots de passe :** un volume déjà initialisé conserve les identifiants d’origine ; réutilise ces identifiants dans le `.env`.

---

## Documentation technique

API REST de catalogue de concerts développée avec Java 25 et Spring Boot 4.1.1.
Le périmètre intégré couvre le catalogue, l’inscription des clients, les cartes
de fidélité et l’achat de billets, avec validation métier, persistance MySQL,
migrations Flyway, HTTP Basic et documentation OpenAPI interactive. L’intégration
des deux modules est réalisée sur la branche `develop`.

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
├── entity/        # Artist, Venue, Concert, Customer, Address, LoyaltyCard, TicketPurchase
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
| `V2__create_ticketing.sql` | Présente | Clients, fidélité et achats |
| `V3__integrate_ticketing_constraints.sql` | Présente | Totaux d’achat en DECIMAL(12,2), total positif et période de carte valide |

Une migration appliquée à une base partagée ne doit plus être modifiée : ajouter
une nouvelle version pour toute évolution. L’historique est enregistré dans
`flyway_schema_history`. Voir [les conventions des migrations](src/main/resources/db/migration/README.md).

V1 et V2 sont conservées sans modification. V3 remplace la réservation documentaire
de données de démonstration qui n’avait aucun script. Les futures données de
démonstration devront utiliser une nouvelle version disponible. Le total d’achat
utilise `DECIMAL(12,2)` pour accepter jusqu’à six billets au prix catalogue maximal.

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
| POST | `/api/clients` | Inscription d’un client et adresse | Public |
| POST | `/api/clients/{id}/carte-fidelite` | Création d’une carte valable un an | CLIENT ou ADMIN |
| POST | `/api/achats` | Achat de 1 à 6 billets | CLIENT |

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

## Clients, fidélité et achats

Inscription d’un client, sans authentification :

```json
{"name":"Alice","email":"alice@example.com","street":"Rue de Paris","city":"Nancy"}
```

L’email est unique. Une carte se crée par POST `/api/clients/{id}/carte-fidelite`,
sans corps JSON, avec le rôle CLIENT ou ADMIN. Elle est active à partir du jour
d’achat inclus et jusqu’à sa date d’expiration exclusive, un an plus tard.
Une deuxième carte active renvoie 409 `CARTE_DEJA_ACTIVE` ; une carte expirée
peut être renouvelée. Les créations concurrentes sont sérialisées par un verrou
sur le client.

Achat de billets avec HTTP Basic et le rôle CLIENT :

```json
{"concertId":1,"customerId":1,"quantity":2}
```

Le serveur calcule le total : `prix unitaire × quantité`, moins 10 % si une carte
est active, puis arrondi à deux décimales avec `HALF_UP`. Le prix et la remise ne
sont pas acceptés comme des montants à imposer par le client. La réponse 201
contient `code`, `quantity`, `unitPrice`, `discountRate`, `totalAmount` et
`remainingSeats`. Une référence absente produit 404 ; un concert commencé ou
des places insuffisantes produisent 409. Un achat refusé n’est pas enregistré.

Chaque achat utilise une transaction atomique `READ_COMMITTED`. Le gateway JPA
appelle `ConcertRepository.findByIdForUpdate()` avec `PESSIMISTIC_WRITE`, puis le
service calcule `venue.capacity - SUM(ticket_purchases.quantity)` et enregistre
l’achat avant de libérer le verrou au commit. `READ_COMMITTED` permet de voir les
ventes validées après l’attente du verrou, sans réutiliser un ancien instantané
MySQL. Le test de deux achats simultanés de quatre billets avec cinq places
restantes vérifie qu’un seul réussit et qu’une place reste disponible.

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
POST `/api/clients` est public. Les comptes de démonstration restent distincts des
clients métier créés par cette route ; ils fournissent des rôles de démonstration,
sans association personnelle entre identifiant Basic et numéro de client.
Les chemins non répertoriés sont refusés.

L’API utilise HTTP Basic sans session, sans formulaire de connexion et sans cache
d’authentification entre requêtes. Les erreurs 401 comportent `WWW-Authenticate`.
CSRF est désactivé pour les clients REST explicites, sans authentification par
cookie sur `/api/**`. Les formulaires HTML sont protégés par un jeton CSRF
stocké dans un cookie, sans session d’authentification. Les formulaires Thymeleaf
incluent ce jeton automatiquement.
Utiliser HTTPS hors du poste local.

### Interface Thymeleaf préservée

- `/clients/inscription` : inscription publique.
- `/fidelite` : formulaire réservé à CLIENT ou ADMIN.
- `/billets/acheter` : formulaire réservé à CLIENT.
- `/css/style.css` : feuille de style publique.

Le navigateur demande les identifiants HTTP Basic pour les pages protégées.
Les POST de formulaire nécessitent le jeton CSRF de la page, contrairement aux
POST JSON de l’API. Les erreurs affichées dans les formulaires ne révèlent pas de SQL.

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
appliquent V1, V2 et V3 ; ils n’utilisent pas les données du volume Compose. Il n’est pas
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

# Parcours intégrés clients/cartes/achats, SQL, sécurité et formulaires
.\mvnw.cmd "-Dtest=TicketingIntegrationTests" test

# Transactions indépendantes, verrous MySQL et achats simultanés
.\mvnw.cmd "-Dtest=TicketingConcurrencyTests" test
```

Les tests JPA relisent les données après `flush`/`clear` et vérifient aussi les
contraintes par SQL direct. Les tests API combinent MockMvc et les véritables
services/repositories. Les tests de sécurité vérifient les règles HTTP et de
méthode ; les parcours de billetterie sont maintenant testés avec leurs véritables
controllers et services, en complément des sondes de sécurité. La suite OpenAPI/Actuator utilise la véritable chaîne de sécurité,
les ressources Swagger et MySQL/Testcontainers.


## Références techniques

- [Conventions du projet](docs/CONVENTIONS.md).
- [Image officielle MySQL](https://hub.docker.com/_/mysql).
- [springdoc-openapi : installation et configuration](https://springdoc.org/).
- [Spring Boot Actuator : endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html).
- [Spring Security : HTTP Basic](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/basic.html).