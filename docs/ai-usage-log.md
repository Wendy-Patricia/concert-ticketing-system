# Journal d’utilisation de l’IA

Date : 2026-10-21. Outil : (GPT-6 ; Instant).

Demande reçue (traduction en français) :

> Créer une configuration Docker Compose pour MySQL
>
> Définir l’image MySQL, le nom du service, le port, la base de données, un volume persistant et un contrôle de santé ; utiliser des variables d’environnement sans publier de secrets.
> Critère d’acceptation : docker compose up -d démarre MySQL et docker compose ps indique que le service est sain.

Les fichiers `compose.yaml` et `.env.example` ont été créés, et les fichiers
`.gitignore` et README ont été mis à jour. Des identifiants locaux ont été générés
dans le fichier `.env`, ignoré par Git. Les vérifications effectuées sont consignées
dans `test-results.md`.
Aucune modification manuelle de l’utilisateur n’a été effectuée pour cette tâche.

Sources consultées : [image officielle MySQL](https://hub.docker.com/_/mysql),
[services Compose](https://docs.docker.com/reference/compose-file/services/) et
[ordre de démarrage](https://docs.docker.com/compose/how-tos/startup-order/).

## INFRA-04 — Configuration Spring Boot

Date : 21 octobre 2026.

### 1. Objectif

Configurer `application.properties` pour connecter l’application à MySQL avec
des variables d’environnement, activer Flyway, imposer `ddl-auto=validate`,
configurer Actuator et définir une propriété propre à l’application. Aucun
identifiant réel ne doit être ajouté aux fichiers versionnés.

### 2. Démarche et raisonnement

Les dépendances MySQL, Flyway, JPA et Actuator sont déjà présentes dans `pom.xml`.
Le datasource utilise `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`
et `MYSQL_PASSWORD`. Les identifiants n’ont aucune valeur par défaut. Les
variables standard `SPRING_DATASOURCE_*` permettent également de remplacer
la configuration depuis l’environnement.

Flyway partage ce datasource et recherche les migrations dans
`classpath:db/migration`. La validation des noms et des migrations est activée ;
la baseline automatique, l’exécution hors ordre et le nettoyage sont désactivés.
Hibernate valide le schéma sans le créer ; l’initialisation par `schema.sql`
est désactivée pour conserver un seul mécanisme de gestion du schéma.

Actuator expose uniquement la santé via HTTP, avec les sondes de vie et de
disponibilité, sans détails internes. `APP_TIME_ZONE` configure `app.time-zone`
et le bean `Clock` prévu par les conventions. Sa valeur par défaut est
`Europe/Paris`. `SERVER_PORT` permet de remplacer le port HTTP 8080.

Le README précise que Spring Boot ne charge pas automatiquement le `.env` de
Compose : les variables doivent aussi être fournies au processus Java ou à l’IDE.

### 3. Documentation utilisée

- [Spring Boot 4.1.1 — Common Application Properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html) :
  propriétés du datasource, de Flyway, de JPA et d’Actuator.
- [Spring Boot — Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html) :
  sources de configuration, variables d’environnement et liaison des propriétés.
- [Spring Boot — Endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html) :
  exposition HTTP, santé et restrictions apportées par Spring Security.
- [Flyway — Versioned migrations](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html) :
  nommage, ordre des versions et immutabilité des migrations appliquées.
- [Conventions du projet](CONVENTIONS.md) : validation Hibernate, Flyway et bean `Clock`.

### 4. Utilisation de l’IA

Outil : Codex. Modèle : GPT-6. Niveau de raisonnement exact : non communiqué.
Prompt de cadrage final (traduction en français) :

```text
INFRA-04
Configurer application.properties


Définir le datasource à partir de variables d’environnement, configurer Flyway, ddl-auto=validate, Actuator et les propriétés spécifiques à l’application.
Critère d’acceptation : la configuration est reconnue par Spring Boot et ne contient aucun identifiant réel.
```

Un premier message incluait aussi INFRA-05 ; la préparation du répertoire de
migrations a été conservée, mais la validation SQL reste différée jusqu’à V1.
Les propositions retenues sont la configuration externalisée, le partage du
datasource avec Flyway, la validation du schéma et un `Clock` configurable.
Les tests utilisent des identifiants fictifs et ne lisent pas les secrets locaux.
Aucune modification manuelle de l’utilisateur n’a été observée pour INFRA-04.

### 5. Tests et vérifications

`mvn -B -Dtest=ApplicationConfigurationTests test` : trois tests exécutés,
aucun échec ni erreur. Les tests chargent réellement `application.properties`
avec le mécanisme de configuration Spring Boot, puis lient les propriétés aux
classes officielles `DataSourceProperties`, `FlywayProperties` et
`HibernateProperties`.

Ils vérifient la construction de l’URL MySQL, les identifiants fictifs, les options
Flyway, `ddl-auto=validate`, les propriétés d’exposition Actuator, le fuseau par
défaut, son remplacement et le rejet d’un fuseau invalide. Aucun accès à MySQL
n’est nécessaire pour ces tests de configuration.

La vérification complète `.\mvnw.cmd -B clean verify` a réussi : quatre tests,
aucun échec ni erreur, dont le test de contexte existant utilisant Testcontainers.
Le JAR produit a ensuite été lancé avec `java -jar
target/concert-ticketing-system-0.0.1-SNAPSHOT.jar --server.port=0`, avec les
variables locales fournies au processus. Une requête HTTP sans authentification
vers `/actuator/health` a renvoyé le statut 200 et `status: UP`. Le processus
temporaire a été arrêté après la vérification.

`git check-ignore .env` confirme l’exclusion du fichier local. La configuration
versionnée contient uniquement des références aux variables et les tests
contiennent uniquement des valeurs fictives.

### 6. Problèmes rencontrés

Le premier appel Maven dans l’environnement restreint ne pouvait pas créer le
répertoire de dépendances `C:\.m2\repository`. L’exécution avec l’autorisation
appropriée a permis de compiler et de lancer les tests.

Le test de fuseau invalide produit volontairement un avertissement de création
du bean `Clock` : il vérifie que l’application refuse cette configuration.
Aucune capture d’écran n’a été réalisée ; les résultats sont disponibles dans
les rapports Surefire générés dans `target/surefire-reports`.

Le test de contexte existant utilise `mysql:latest` et Flyway signale une version
MySQL plus récente que celles vérifiées par cette version de Flyway. Le test
a néanmoins réussi. Le JAR a également démarré contre le MySQL 8.4 de Compose.
Flyway signale l’absence de migrations, attendue avant la création de V1.
Des avertissements non bloquants concernent les dépôts Maven secondaires (HTTP
401), l’absence de templates Thymeleaf et le chargement dynamique de Mockito.

La première lecture du log pendant la vérification du JAR a rencontré un verrou
Windows. La lecture avec partage du fichier a permis de terminer la vérification.

### 7. Traçabilité

Fichiers principaux : `src/main/resources/application.properties`,
`src/main/java/com/example/concertticketingsystem/config/ClockConfig.java`,
`src/test/java/com/example/concertticketingsystem/ApplicationConfigurationTests.java`,
`.env.example` et `README.md`.

Commit de cette tâche : non créé. URL de Pull Request : aucune PR créée pendant
cette intervention. Les modifications restent locales pour revue.

### 8. Conclusion

La vérification complète a réussi avec quatre tests. Le JAR démarre et son
endpoint de santé répond HTTP 200 avec `UP`. Aucun secret réel n’est inscrit
dans la configuration versionnée.
La validation du futur schéma SQL et de ses entités dépend de la création de V1 ;
le démarrage actuel sans migration ne constitue pas une validation de ce schéma.

## Entité Artist

Date : 21 octobre 2026.

### 1. Objectif

Créer l’entité JPA `Artist` avec les champs `id`, `name` et `musicalGenre`,
un nom obligatoire et unique, et les constructeurs nécessaires à JPA et au code métier.

### 2. Démarche et raisonnement

La classe est placée dans le package `entity` et mappée sur la table `artists`.
L’identifiant est un `Long` généré avec `GenerationType.IDENTITY`, conformément
aux conventions. `name` porte `@NotBlank` et un mappage `nullable=false,
unique=true`. Les chaînes sont limitées à 255 caractères avec `@Size` et les
colonnes correspondantes. `musicalGenre` est facultatif, car seule l’obligation
du nom est demandée, et correspond à la colonne `musical_genre`.

Un constructeur sans argument protégé est réservé à JPA ; un constructeur public
accepte le nom et le genre musical. Les accesseurs permettent de consulter les
trois champs et de modifier le nom et le genre ; aucun setter ne modifie l’identifiant.
La création du schéma reste confiée à Flyway. Aucune migration partielle V1
ni modification de `ddl-auto=validate` n’a été ajoutée pour cette tâche.

### 3. Documentation utilisée

- [Jakarta Persistence — Introduction](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/persist/persistence-intro/persistence-intro.html) :
  exigences d’une entité et constructeur sans argument public ou protégé.
- [Jakarta Persistence — Column](https://jakarta.ee/specifications/platform/9/apidocs/jakarta/persistence/column) :
  options `nullable`, `unique` et définition du mappage des colonnes.
- [Jakarta Validation — NotBlank](https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/notblank) :
  rejet de `null` et des chaînes sans caractère autre qu’un espace blanc.
- [CONVENTIONS.md](CONVENTIONS.md) : package `entity`, identifiants `Long`,
  noms SQL et unicité de `artists.name`.

### 4. Utilisation de l’IA

Outil : Codex. Modèle : GPT-6. Moyenne.
Prompt (traduction en français) :

```text
Créer l’entité Artist


Champs id, name et musicalGenre ; le nom est obligatoire et unique. Ajouter le mappage JPA et les constructeurs appropriés.
```

Les propositions retenues sont les annotations Jakarta, le constructeur JPA
protégé, le constructeur métier et les tests de validation. Aucune modification
manuelle de l’utilisateur n’a été observée pour cette tâche.

### 5. Tests et vérifications

Les tests `ArtistTests` couvrent les noms nuls, vides et composés d’espaces blancs,
le genre facultatif et les limites de longueur 255/256 caractères. Les résultats
d’exécution sont consignés dans [test-results.md](test-results.md).
Les sept cas de `ArtistTests` passent. La suite complète exécute onze tests,
avec une erreur du test de contexte due à l’absence de la table `artists`.

### 6. Problèmes rencontrés

La commande `.\mvnw.cmd -B clean verify` ne peut pas supprimer le JAR existant,
car il est utilisé par un autre processus. Le processus n’a pas été arrêté.
La commande `.\mvnw.cmd -B test` a donc été lancée sans nettoyage.
La base de test ne dispose pas encore de la table `artists` : la validation
Hibernate dépend de la future migration V1. Aucune capture d’écran n’a été réalisée.

### 7. Traçabilité

Fichiers ajoutés : `src/main/java/com/example/concertticketingsystem/entity/Artist.java`
et `src/test/java/com/example/concertticketingsystem/entity/ArtistTests.java`.
Documentation actualisée dans `docs/ai-usage-log.md` et `docs/test-results.md`.
Commit de cette tâche : non créé. Pull Request : non créée.

### 8. Conclusion

L’entité et ses contraintes de validation sont définies. L’unicité est déclarée
dans le mappage JPA ; son application réelle par MySQL nécessite une contrainte
`UNIQUE(name)` dans V1. La validation du schéma et les tests de persistance
restent dépendants de cette migration.

## Entités Venue et Concert — persistance du catalogue

Date : 21 octobre 2026.

### 1. Objectif

Créer `Venue` et `Concert`, vérifier les règles métier et les relations JPA avec
MySQL/Testcontainers, puis exécuter `.\mvnw.cmd clean verify`. Cette intervention
complète le catalogue et résout la dépendance à V1 signalée dans la section Artist.

### 2. Démarche et raisonnement

`Venue` est mappée sur `venues`, avec un identifiant `Long` auto-incrémenté,
un nom et une ville obligatoires, une capacité `Integer` non nulle comprise entre
50 et 50 000 et une contrainte unique nommée sur `(name, city)`.

`Concert` est mappé sur `concerts`. Les relations `artist` et `venue` utilisent
`@ManyToOne(fetch=LAZY, optional=false)` et des colonnes de jointure non nulles.
Elles ne propagent pas les opérations de suppression vers l’artiste ou la salle.
Le titre est obligatoire, la date est un `LocalDateTime` non nul annoté `@Future`
et le prix est un `BigDecimal` non nul et strictement positif. Le mappage
`DECIMAL(10,2)` est accompagné de `@Digits(integer=8, fraction=2)` pour éviter
de laisser MySQL arrondir silencieusement un prix saisi avec trop de décimales.
Les deux classes possèdent un constructeur JPA protégé sans argument, un
constructeur métier public et des accesseurs, sans setter pour l’identifiant.

V1 n’existait pas dans la copie locale : `git ls-files '*migration*'` et la
recherche de scripts ne trouvaient que le README de réservation. Le fichier
`V1__create_catalog.sql` a donc été créé selon les conventions existantes pour
`artists`, `venues` et `concerts`. Aucune migration déjà appliquée n’a été modifiée.
V1 définit les colonnes, les contraintes uniques, les clés étrangères, les index,
les `CHECK` de capacité et de prix, ainsi que les noms obligatoires. La règle de
date future reste contrôlée par Jakarta Validation lors des écritures JPA,
sans `CHECK` SQL utilisant une fonction temporelle non déterministe.

Les tests utilisent cette migration réelle, avec `ddl-auto=validate`. Aucun
schéma de remplacement ni génération DDL Hibernate n’est utilisé. La configuration
Testcontainers est alignée sur `mysql:8.4`, comme Compose, au lieu de `mysql:latest`.
Les tests transactionnels sont annulés après chaque cas. Les tests de lecture
font un `flush` puis un `clear` pour relire depuis MySQL, y compris les associations.
Des insertions JDBC complémentaires vérifient les contraintes SQL sans passer
par la validation Java.

### 3. Documentation utilisée

- [Spring Boot — Testing Spring Boot Applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html) :
  tests de contexte et accès à une véritable base de données.
- [Jakarta Validation — contraintes intégrées](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/beanvalidation/bean-validation/bean-validation.html) :
  `@Min`, `@Max`, `@Positive` et `@Future`.
- [Jakarta Validation — Future](https://jakarta.ee/specifications/platform/11/apidocs/jakarta/validation/constraints/future) :
  prise en charge de `LocalDateTime`.
- [MySQL 8.4 — CHECK Constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html) :
  contraintes de capacité et de prix, application et limites des expressions.
- [MySQL 8.4 — FOREIGN KEY Constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-foreign-keys.html) :
  références obligatoires et intégrité des relations.
- [Conventions internes](CONVENTIONS.md) et README du répertoire `db/migration` :
  noms SQL, identifiants, montants et réservation de V1/V2/V3.

### 4. Utilisation de l’IA

Outil : Codex. Modèle : GPT-6. Niveau de raisonnement: instant.
Prompt reçu en français :

```text
Pour Venue, ajoute les attributs id, name, city et capacity. Le nom et la ville sont obligatoires, la capacité doit être comprise entre 50 et 50 000, et le couple (name, city) doit être unique.

Pour Concert, ajoute les attributs id, title, artist, venue, dateTime et unitPrice. Utilise @ManyToOne pour les relations avec Artist et Venue. La date doit être future et le prix strictement positif.

Respecte les conventions JPA et la migration Flyway V1 existante.

Ajoute des tests JUnit avec MySQL/Testcontainers pour vérifier la persistance, les relations et les contraintes : capacités invalides (49 et 50 001), salle dupliquée, prix invalide et associations manquantes.

Exécute .\mvnw.cmd clean verify et documente les résultats, les erreurs et les corrections pour le compte rendu.
```

Propositions retenues : contraintes Jakarta et SQL complémentaires, relations
obligatoires, chargement différé, précision monétaire explicite et tests sur
MySQL 8.4. La création de V1 remplace l’hypothèse initiale de son existence,
après vérification de l’espace de travail.

Aucune modification manuelle de l’utilisateur n’a été observée pendant cette
intervention. Une modification préexistante du `pom.xml` ajoutant TestNG a été
conservée ; les nouveaux tests utilisent JUnit Jupiter.

### 5. Tests et vérifications

La commande ciblée `.\mvnw.cmd -B -Dtest=CatalogPersistenceTests test` a exécuté
36 cas : 32 réussis et 4 échecs d’assertion sur le type d’exception des contraintes
`CHECK`. Les corrections sont expliquées ci-dessous. La commande complète
`.\mvnw.cmd clean verify` a été relancée après correction et libération du JAR.
Résultat final : `BUILD SUCCESS`, 47 tests exécutés, aucun échec ni erreur,
dont 36 cas pour `CatalogPersistenceTests`. Le détail est consigné dans
[test-results.md](test-results.md).

Le JAR a ensuite démarré hors IDE sur le MySQL 8.4 local et appliqué V1.
Les endpoints `/actuator/health`, `/actuator/health/liveness` et
`/actuator/health/readiness` ont tous répondu HTTP 200 avec `UP`. Le binaire
Java du JDK a été lancé directement, puis son arrêt a été confirmé pour éviter
de laisser un processus temporaire verrouiller le JAR.

La couverture comprend la migration V1, les identifiants générés, la relecture
des attributs et des relations, les bornes 50/50 000, le rejet de 49/50 001,
les noms et villes absents ou blancs, les doublons de salle et d’artiste, les
prix nuls, négatifs, nuls au sens numérique ou hors précision, les dates absentes
ou passées, les titres manquants, les associations nulles et les clés étrangères
inexistantes. Les cas SQL vérifient aussi les contraintes `CHECK` et `NOT NULL`.

### 6. Problèmes rencontrés et corrections

1. **JAR verrouillé** : le premier `clean verify` échouait au nettoyage. Deux
   processus Java issus des contrôles temporaires précédents, identifiés par le
   chemin du JAR et `--server.port=0`, étaient restés actifs. Leur arrêt ciblé a
   libéré le fichier ; aucun autre processus Java n’a été arrêté.
2. **Exception des `CHECK` MySQL** : le pilote renvoie le code 3819 avec SQLState
   `HY000`, traduit ici par Spring en `UncategorizedSQLException`. Les assertions
   attendaient `DataIntegrityViolationException`. Elles vérifient désormais une
   `DataAccessException`, le nom de la contrainte et la cause SQL avec le code 3819,
   ce qui confirme précisément le refus attendu sans dépendre de cette traduction.
3. **Version de test variable** : `mysql:latest` est remplacé par `mysql:8.4` pour
   tester la même version majeure que le service Compose.

Aucune capture d’écran n’a été réalisée. Les rapports Surefire contiennent les
résultats textuels. Les avertissements Maven liés aux dépôts secondaires et à la
version `RELEASE` de TestNG sont distincts des contraintes testées.

### 7. Traçabilité

Fichiers ajoutés : `Venue.java`, `Concert.java`, `CatalogPersistenceTests.java`
et `V1__create_catalog.sql`. Fichiers actualisés : configuration Testcontainers,
README des migrations, `docs/ai-usage-log.md` et `docs/test-results.md`.
L’entité `Artist` et ses tests préexistants sont conservés.
Commit de cette intervention : non créé. Pull Request : non créée.

### 8. Conclusion

Les entités et la migration du catalogue sont validées : 47 tests passent,
le JAR démarre et ses contrôles de santé sont `UP`. V2 reste réservée à la
Personne B et V3 aux données de démonstration. La date future est une contrainte
applicative lors des écritures JPA ; une insertion SQL directe ne vérifie pas
cette règle temporelle. V1 ne devra plus être modifiée après application sur
une base partagée.

## API REST du catalogue de concerts

Date : 21 octobre 2026.

### 1. Objectif

Implémenter les lectures publiques du catalogue et la création d’artistes, de
salles et de concerts, avec DTO records, validations, réponses HTTP cohérentes
et préparation des rôles CLIENT, PRODUCTEUR et ADMIN. Conserver la configuration
de sécurité existante et les fichiers de la Personne B.

### 2. Démarche et raisonnement

Les entités et V1 existent, mais aucun repository n’était présent dans cette
copie du projet. Trois interfaces JPA ont donc été ajoutées pour les besoins du
catalogue. La version Spring Boot reste 4.1.1 ; `java.version` passe de 17 à 25,
conformément à la demande et au JDK installé.

L’architecture suit `Controller → Service → Repository`. Les créations sont
transactionnelles ; les lectures convertissent les entités en DTO dans la
transaction. Les relations artiste/salle sont chargées avec une jointure ou un
graphe d’entités pour éviter une sérialisation JPA et des accès différés après
fermeture de la transaction.

Les filtres `du` et `au` utilisent des dates ISO inclusives ; `artiste` est
l’identifiant positif de l’artiste. Ils sont combinés par un ET et les résultats
sont triés par date puis identifiant. La borne de fin utilise la dernière
microseconde de la journée, compatible avec `DATETIME(6)`, pour éviter un arrondi
vers le lendemain. Une période inversée produit 400 ; une recherche sans
résultat produit une liste vide avec 200.

Les DTO de requête portent les contraintes Jakarta et sont reçus avec `@Valid`.
Les services vérifient l’existence des références et l’unicité ; les contraintes
MySQL restent la garantie finale, notamment en cas de créations concurrentes.
`saveAndFlush` permet de faire remonter ces erreurs dans le traitement HTTP.
Les réponses sont des records, avec des DTO imbriqués pour l’artiste et la salle.

`CatalogExceptionHandler` est un `@RestControllerAdvice` limité aux trois
controllers du catalogue pour ne pas modifier le traitement des futurs endpoints
de la Personne B. Le format d’erreur suit les conventions : `timestamp`, `status`,
`code`, `message`, `path`. Les erreurs de validation renvoient 400, les références
absentes 404 et les doublons/conflits d’intégrité 409, sans texte SQL.

Les annotations `@PreAuthorize` préparent les autorisations : lectures publiques,
créations d’artistes/concerts pour PRODUCTEUR ou ADMIN, salles pour ADMIN.
Aucune activation de sécurité de méthode ni modification de chaîne HTTP n’est
ajoutée dans l’application. La branche sécurité devra activer ces annotations et
autoriser les lectures publiques. La configuration dédiée aux tests de rôle est
une `@TestConfiguration`, sans effet sur l’application livrée.

### 3. Documentation utilisée

- [Spring Boot 4.1.1 — WebMvcTest](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/WebMvcTest.html) :
  tests MVC et configuration de MockMvc/sécurité.
- [Spring Boot 4.1.1 — AutoConfigureMockMvc](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/webmvc/test/autoconfigure/AutoConfigureMockMvc.html) :
  configuration des tests HTTP avec contexte applicatif.
- [Spring Boot — Testing Spring Boot Applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html) :
  tests de contexte et imports des annotations de test de Spring Boot 4.
- [Spring Security — Method Security](https://docs.spring.io/spring-security/reference/7.0/servlet/authorization/method-security.html) :
  `@PreAuthorize` et activation par `@EnableMethodSecurity`.
- [Conventions internes](CONVENTIONS.md) : routes en français, DTO records,
  séparation controllers/services, codes applicatifs et format des erreurs.

### 4. Utilisation de l’IA

Outil : Codex. Modèle : GPT-6. Niveau de raisonnement: instant.
Prompt initial reçu en français :

```text
implémente l'API REST du catalogue de concerts avec Java 25 et Spring Boot 4.1.1, en utilisant les entités et repositories déjà créés.

Crée les controllers, services et DTO records nécessaires pour gérer les artistes, les salles et les concerts.

Implémente les endpoints suivants :

GET /api/concerts : lister les concerts avec des filtres optionnels du, au et artiste.

GET /api/concerts/{id} : consulter les détails d'un concert.

POST /api/production/artistes : créer un artiste.

POST /api/production/concerts : créer un concert.

POST /api/admin/salles : créer une salle.

Ajoute les validations nécessaires : champs obligatoires, capacité des salles entre 50 et 50 000, prix strictement positif, date de concert future, références à un artiste et une salle existants et contraintes d'unicité.

Utilise @Valid, des DTO records et un @RestControllerAdvice pour gérer les erreurs avec des réponses HTTP cohérentes : 201 Created, 200 OK, 400 Bad Request, 404 Not Found et 409 Conflict lorsque cela est approprié.

Ajoute des tests JUnit et MockMvc pour vérifier les endpoints, les filtres, les validations et les erreurs HTTP. Prépare les endpoints pour les rôles CLIENT, PRODUCTEUR et ADMIN, sans modifier la configuration de sécurité qui sera développée dans une autre branche.

Respecte l'architecture existante, ne modifie pas le travail de la Personne B et évite les changements inutiles. Exécute .\mvnw.cmd clean verify, corrige les erreurs rencontrées et documente les tests, les bugs et les corrections pour le compte rendu. Ne déclare aucun test réussi sans l'avoir exécuté.
```

Le message a ensuite été renvoyé avec la même demande fonctionnelle et la
contrainte de ne pas modifier la configuration de sécurité.
Propositions retenues : repositories JPA minimaux, services transactionnels,
DTO records, advice limité au catalogue et tests séparant contrat métier et
préparation des rôles. Aucune modification manuelle de l’utilisateur n’a été
observée pendant cette intervention.

### 5. Tests et vérifications

`.\mvnw.cmd clean verify` a réellement été exécuté avec Java 25 : `BUILD SUCCESS`,
94 tests, aucun échec, aucune erreur, aucun test ignoré.
Les nouveaux tests sont `CatalogApiTests` (41 cas MockMvc avec MySQL réel) et
`CatalogRolePreparationTests` (6 cas MVC avec services simulés et sécurité de
méthode activée uniquement pour le test). Les 47 tests précédents passent aussi.

Les tests vérifient les cinq endpoints, les DTO et identifiants, l’en-tête
`Location` du concert créé, les filtres individuels et combinés, leurs bornes,
l’ordre des résultats, les listes vides, les validations et les réponses 400,
404 et 409. Les tests de rôle vérifient les accès public, CLIENT, PRODUCTEUR et
ADMIN, ainsi que 401/403 dans la configuration de test.
Le traitement d’un conflit SQL après le précontrôle est vérifié par une exception
simulée ; aucune course concurrente réelle n’a été exécutée pour cette API.

Le JAR a aussi démarré hors IDE contre le MySQL local : `/actuator/health` répond
HTTP 200 et `GET /api/concerts` renvoie HTTP 200 avec une liste JSON, en utilisant
l’authentification Basic actuelle. Un compte temporaire a été fourni uniquement
au processus, sans identifiant versionné. Le processus a été arrêté après le test.

### 6. Problèmes rencontrés et adaptations

- Les repositories annoncés n’existaient pas localement : les trois interfaces
  nécessaires ont été créées, sans modification des entités ni de V1.
- Le projet compilait encore pour Java 17 : seule la propriété Java du POM a été
  alignée sur Java 25. La dépendance TestNG préexistante est conservée.
- La précision de fin de journée a été alignée sur les microsecondes MySQL avant
  la vérification, et testée avec un concert à `23:59:59.999999` et un autre à
  minuit le lendemain. Aucun échec d’exécution n’a été observé sur ce point.
- La première vérification complète de cette intervention a réussi. Il n’y a donc
  pas d’échec de test à présenter comme un bug corrigé.
- Les avertissements non bloquants concernent les métadonnées de dépôts secondaires
  HTTP 401, la version `RELEASE` de TestNG, les templates Thymeleaf absents et
  l’agent Mockito. Le message du test `invalid-zone` est un refus volontaire déjà
  couvert par les tests de configuration.

Aucune capture d’écran n’a été réalisée. Les rapports Surefire et le journal de
compilation sont conservés sous `target`, ignoré par Git.

### 7. Traçabilité

Fichiers ajoutés dans `controller`, `service`, `dto`, `exception` et `repository`,
ainsi que `CatalogApiTests.java` et `CatalogRolePreparationTests.java`.
Fichiers mis à jour : `pom.xml` (Java 25), README et les deux journaux de suivi.
Aucun fichier d’entité, de migration ou de sécurité applicative n’a été modifié.
Commit de cette intervention : non créé. Pull Request : non créée.

### 8. Conclusion

Le contrat REST du catalogue est implémenté et validé par 94 tests exécutés.
L’intégration de la sécurité reste à effectuer dans sa branche : activation de la
sécurité de méthode, authentification et règles HTTP publiques/privées. Les tests
de préparation des rôles ne remplacent pas la validation de cette future
configuration. Les filtres et les exemples de requête sont décrits dans le README.

## Sécurité Spring Security, HTTP Basic et BCrypt

Date : 21 octobre 2026.

### 1. Objectif

Activer l’authentification HTTP Basic, les rôles CLIENT/PRODUCTEUR/ADMIN et les
comptes de démonstration, conserver une API stateless et retourner 401/403 de
manière cohérente. Préparer les routes de la Personne B sans modifier son code.

### 2. Démarche et raisonnement

`SecurityConfig` définit un `BCryptPasswordEncoder`, un `UserDetailsService` en
mémoire et une `SecurityFilterChain`. Les mots de passe de démonstration sont
encodés avant création des comptes ; le gestionnaire conserve uniquement les
empreintes BCrypt. Les comptes sont `client`, `producteur` et `admin`, avec un
seul rôle chacun. Les variables `DEMO_CLIENT_PASSWORD`, `DEMO_PRODUCTEUR_PASSWORD`
et `DEMO_ADMIN_PASSWORD` permettent de remplacer les valeurs publiques de démo.
Le tableau des identifiants et la procédure Postman sont dans le README.

La chaîne HTTP autorise les lectures du catalogue, l’inscription client, la santé
et Swagger sans authentification. Les achats sont réservés à CLIENT, les cartes
de fidélité à CLIENT ou ADMIN, la production à PRODUCTEUR ou ADMIN et les routes
`/api/admin/**` à ADMIN. Les autres routes sont refusées. ADMIN n’hérite pas
implicitement de CLIENT : ses autorisations supplémentaires sont explicites.
`@EnableMethodSecurity` active également les annotations existantes du catalogue.

`SessionCreationPolicy.STATELESS`, `NullSecurityContextRepository` et la
désactivation du cache de requêtes évitent de conserver l’authentification en
session. Le formulaire de connexion et le logout de session sont désactivés.
Les refus 401 fournissent le challenge `WWW-Authenticate: Basic` ; les refus 403
indiquent un rôle insuffisant. Les deux utilisent le DTO `ApiError`, sans fuite
d’identifiants ni de détails internes. Les redistributions d’erreur du conteneur
sont autorisées pour préserver les codes d’erreur applicatifs comme 404.

CSRF est désactivé pour l’API REST destinée aux clients explicites tels que
Postman, sans cookie d’authentification ni formulaire. Stateless seul ne protège
pas contre CSRF ; une utilisation future dans un navigateur envoyant
automatiquement des identifiants Basic nécessite de revoir ce choix.

### 3. Documentation utilisée

- [Spring Security — Basic Authentication](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/basic.html) :
  filtre Basic, `AuthenticationEntryPoint` et challenge HTTP.
- [Spring Security — In-Memory Authentication](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/in-memory.html) :
  utilisateurs en mémoire et stockage de mots de passe encodés.
- [Spring Security — Session Management](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/session-management.html) :
  HTTP Basic stateless et `SessionCreationPolicy.STATELESS`.
- [Spring Security — CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html) :
  comportement par défaut et choix adapté au mode de consommation de l’API.
- [Spring Security — CSRF et applications stateless](https://docs.spring.io/spring-security/reference/7.1-SNAPSHOT/features/exploits/csrf.html) :
  vulnérabilité possible des navigateurs utilisant HTTP Basic malgré l’absence de session.
- [Conventions du projet](CONVENTIONS.md) : rôles, routes et format d’erreur.

### 4. Utilisation de l’IA

Outil : Codex. Modèle : GPT-6. Niveau de raisonnement: instant.
Prompt reçu en français, renvoyé pendant l’intervention :

```text
implémente la sécurité de l'application Concert Ticketing System avec Spring Security, HTTP Basic et BCrypt, en respectant l'architecture existante.

Crée une configuration SecurityConfig avec trois comptes de démonstration ayant les rôles CLIENT, PRODUCTEUR et ADMIN. Les mots de passe doivent être encodés avec BCrypt et les identifiants de démonstration doivent être clairement documentés.

Configure les autorisations suivantes :

PUBLIC : GET /api/concerts, GET /api/concerts/{id}, POST /api/clients, /actuator/health et la documentation Swagger.

CLIENT : POST /api/achats et POST /api/clients/{id}/carte-fidelite.

PRODUCTEUR : POST /api/production/artistes et POST /api/production/concerts.

ADMIN : POST /api/admin/salles, ainsi que les opérations réservées à l'administration.

L'administrateur doit également pouvoir accéder aux fonctionnalités du producteur et à la création des cartes de fidélité.

Ajoute des tests automatisés avec JUnit, MockMvc et Spring Security Test pour vérifier qu'un utilisateur non authentifié reçoit 401 Unauthorized lorsqu'il accède à une ressource protégée, qu'un utilisateur authentifié sans le rôle requis reçoit 403 Forbidden, et qu'un utilisateur autorisé peut accéder aux endpoints correspondants.

Assure-toi que l'API reste stateless, que les erreurs d'authentification et d'autorisation retournent les bons codes HTTP et que la configuration HTTP Basic fonctionne avec Postman. Prends également en compte la configuration CSRF adaptée à cette API REST.

Ne modifie pas inutilement le code de la Personne B. Exécute .\mvnw.cmd clean verify, corrige les erreurs rencontrées et documente les tests, les bugs, les corrections
```

Propositions retenues : comptes mémoire BCrypt, droits explicites sans hiérarchie
globale, sécurité de méthode active, erreurs JSON 401/403 et absence de session.
Les adaptations de tests sont décrites ci-dessous. Aucune modification manuelle
de l’utilisateur n’a été observée pendant cette intervention.

### 5. Tests et vérifications

La sélection `SecurityConfigTests,CatalogRolePreparationTests,CatalogApiTests`
a été exécutée : 85 tests réussis. La commande `.\mvnw.cmd clean verify` a ensuite
réussi : 132 tests, aucun échec, aucune erreur, aucun test ignoré.

`SecurityConfigTests` contient 38 cas couvrant les routes publiques, 401 sans
identifiants, 401 pour des identifiants incorrects, 403 sans le rôle requis,
les accès autorisés, les droits supplémentaires d’ADMIN, l’absence de jeton
CSRF exigé, les empreintes BCrypt et l’absence de réutilisation de session.
Les tests des rôles du catalogue importent maintenant la vraie `SecurityConfig`.

Les routes métier de la Personne B et Swagger sont représentées par des sondes
de contrôleur exclusivement dans les tests : cela vérifie leurs règles HTTP,
sans prétendre que leurs fonctionnalités métier ou leur documentation existent
dans cette branche. Les comptes réels sont testés avec le postprocesseur
`httpBasic`, tandis que les tests de rôle du catalogue utilisent aussi
`@WithMockUser` pour isoler les décisions d’autorisation.

Le JAR a ensuite été démarré hors IDE et contrôlé avec de vraies requêtes HTTP :
lectures publiques 200, concert absent 404, écriture anonyme 401, mauvais mot de
passe 401 et CLIENT sur une création d’artiste 403. Avec les bons comptes
PRODUCTEUR et ADMIN, les écritures ont atteint la validation métier et renvoyé
400 pour le corps volontairement vide `{}`. Ce contrôle ne crée aucune donnée.
Les challenges Basic et l’absence de cookie ont été vérifiés ; le processus
temporaire a été arrêté et son arrêt confirmé. Les créations 201 sont couvertes
par les tests MockMvc décrits précédemment. L’interface Postman n’a pas été utilisée.

### 6. Problèmes rencontrés et adaptations

Aucun échec de test n’a été rencontré dans les deux exécutions de cette tâche.
L’activation de la sécurité de méthode a nécessité d’ajouter un utilisateur
ADMIN simulé aux tests API qui désactivent les filtres HTTP. Leur configuration
antérieure ne fournissait aucun utilisateur, car les annotations n’étaient pas
encore actives dans l’application. Cette adaptation a été faite avant exécution.

La configuration de sécurité permissive réservée aux tests des rôles a été
remplacée par l’import de la configuration réelle. Les avertissements Maven
préexistants (dépôts secondaires HTTP 401, TestNG `RELEASE`), Thymeleaf et
Mockito restent non bloquants. Le refus `invalid-zone` est volontaire dans le
test de configuration existant. Aucune capture d’écran n’a été réalisée.

### 7. Traçabilité

Fichiers ajoutés : `config/SecurityConfig.java` et `SecurityConfigTests.java`.
Fichiers adaptés : `CatalogApiTests.java`, `CatalogRolePreparationTests.java`,
README, `docs/ai-usage-log.md` et `docs/test-results.md`. Aucun controller, service,
repository, schéma ou fichier métier de la Personne B n’a été modifié.
Commit de cette tâche : non créé. Pull Request : non créée.

### 8. Conclusion

La sécurité HTTP et de méthode est active et les 132 tests passent. Les comptes
publics de démonstration sont documentés avec des variables de remplacement.
Les fonctionnalités clients/achats/cartes et l’implémentation Swagger restent
dépendantes de leur intégration ; seules leurs règles d’accès sont préparées ici.
La procédure Postman est documentée ; les contrôles réseau automatisés utilisent
des requêtes HTTP équivalentes, sans prétendre avoir exécuté l’interface Postman.

## Swagger/OpenAPI, Actuator et README technique

Date : 21 octobre 2026.

### 1. Objectif

Fournir une véritable interface Swagger et un document OpenAPI fidèle aux cinq
endpoints du catalogue, vérifier `/actuator/health`, compléter les tests JPA et
HTTP, revérifier la sécurité et rédiger un README technique en français.

### 2. Démarche et raisonnement

La sécurité autorisait déjà les chemins Swagger, mais aucune bibliothèque ne
servait la documentation. Le starter `springdoc-openapi-starter-webmvc-ui:3.1.1`
a été ajouté ; il correspond à la génération Spring Boot 4. `OpenApiConfig`
définit les informations de l’API et le schéma HTTP Basic. Les controllers portent
les tags, descriptions d’opération, paramètres et réponses HTTP ; les DTO portent
les descriptions, exemples et contraintes de schéma. Les lectures restent publiques
dans le document ; les créations déclarent le besoin d’authentification Basic.

Les filtres `du`, `au` et `artiste` décrivent des dates ISO inclusives et un
identifiant d’artiste. Les schémas précisent les champs obligatoires, les bornes
de capacité, le prix au centime, les références existantes et la date future.
Les réponses documentent les DTO, erreurs 400/401/403/404/409 et l’en-tête
`Location` du concert créé. Le type de réponse des controllers du catalogue
est explicitement `application/json`.

Le JSON OpenAPI est exposé sur `/v3/api-docs`, le YAML sur `/v3/api-docs.yaml`
et Swagger UI sur `/swagger-ui.html`. Le chemin YAML est ajouté aux lectures
publiques de `SecurityConfig`. La persistance des identifiants dans Swagger UI
est désactivée. `springdoc.show-actuator=true` inclut les endpoints de santé.
Actuator conserve uniquement `health` exposé, ses sondes et les détails masqués.

Le README a été réécrit autour du périmètre réellement disponible, sans présenter
les achats/fidélité de la Personne B comme implémentés. Il décrit l’architecture,
les prérequis, Docker, les variables, Flyway, les routes, Swagger, HTTP Basic et
les commandes de test. La procédure PowerShell reprend la configuration résolue
par Compose afin de respecter les mots de passe et le port local sans les afficher.

### 3. Documentation utilisée

- [springdoc-openapi 3.1.1](https://springdoc.org/) : installation Web MVC,
  informations OpenAPI/sécurité, configuration Swagger UI et support Actuator.
- [Spring Boot — Endpoints Actuator](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html) :
  exposition de santé, détails et sondes.
- Les conventions et fichiers existants du projet : V1, DTO records, erreurs HTTP,
  sécurité, tests MockMvc et JPA/Testcontainers.

### 4. Utilisation de l’IA

Outil : Codex. Modèle : GPT-6. Niveau de raisonnement exact non communiqué.
Prompt reçu en français :

```text
onfigure et vérifie Swagger/OpenAPI pour documenter les endpoints REST du catalogue (artistes, salles et concerts), ainsi que Spring Boot Actuator avec l'endpoint /actuator/health. Assure-toi que Swagger est accessible et que les réponses HTTP, les DTO et les paramètres de filtrage sont correctement documentés.

Complète les tests automatisés JUnit, MockMvc et JPA avec Testcontainers MySQL pour vérifier la persistance des entités, les contraintes SQL, les relations JPA, les endpoints GET et POST, les filtres, les validations et les réponses HTTP (200, 201, 400, 404 et 409). Vérifie également les tests de sécurité 401 et 403 déjà développés dans la branche précédente.

Rédige un README technique professionnel expliquant l'architecture, les technologies, les prérequis, la configuration Docker/MySQL, le démarrage de l'application, les migrations Flyway, les endpoints disponibles, l'accès à Swagger et les commandes pour exécuter les tests.
```

Propositions retenues : springdoc 3 compatible Boot 4, annotations du contrat
réel, véritable Swagger dans les tests et parcours sécurisé complet avec MySQL.
Aucune modification manuelle de l’utilisateur n’a été observée pendant cette tâche.

### 5. Tests et vérifications

La compilation avec le nouveau starter a réussi. Une première sélection de
`CatalogPersistenceTests,OpenApiAndHealthTests` a exécuté 57 cas, sans échec ni
erreur : 45 tests JPA/SQL et 12 tests de documentation, santé et parcours HTTP.
La vérification finale ajoute également un contrôle de présence de la santé dans
OpenAPI et des assertions sur les références de schémas JSON.
Les résultats de `.\mvnw.cmd clean verify` sont consignés dans
[test-results.md](test-results.md).

Les ajouts JPA couvrent les champs SQL obligatoires/vides des salles, l’interdiction
de supprimer un artiste ou une salle référencés et la conservation exacte des
prix aux bornes `0.01`/`99999999.99`. Les tests précédents de relations, unicités,
capacités, validations et clés étrangères sont conservés.

La nouvelle suite utilise MockMvc avec la vraie chaîne de sécurité et un MySQL
8.4/Testcontainers. Elle teste l’interface Swagger, ses ressources et sa configuration,
les formats JSON/YAML, les chemins, filtres, DTO, codes et droits documentés,
les véritables sondes de santé et les créations Basic PRODUCTEUR/ADMIN avec
lecture publique et doublons 409. Elle vérifie aussi 401/403/400/404 sans remplacer
les controllers par des sondes. Les tests de sécurité antérieurs sont conservés.

### 6. Problèmes rencontrés et adaptations

Swagger était autorisé mais absent : ajout du starter, sans création d’un faux
endpoint de documentation. Le YAML avait un chemin distinct des règles existantes :
il est désormais explicitement public. Le README précédent mélangeait plusieurs
langues et des étapes devenues obsolètes ; il a été remplacé par une procédure
technique en français cohérente avec la configuration actuelle.

La première sélection de tests a réussi. Les avertissements préexistants relatifs
aux dépôts secondaires, TestNG, Thymeleaf et Mockito restent distincts des
vérifications fonctionnelles. Aucune capture d’écran n’a été réalisée.

### 7. Traçabilité

Fichiers concernés : POM, `OpenApiConfig`, annotations des controllers et DTO,
configuration springdoc, règle publique YAML, tests JPA, `OpenApiAndHealthTests`,
README et journaux de suivi. V1 et les entités ne sont pas modifiées.
Le travail métier de la Personne B est conservé. Les modifications de sécurité
de l’intervention précédente restent présentes.
Commit de cette tâche : non créé. Pull Request : non créée.

### 8. Conclusion

La documentation Swagger est fournie par springdoc et vérifiée par des tests
sur les véritables ressources. Le README décrit les commandes et limites du
périmètre actuel. Les tests n’implémentent pas les fonctionnalités encore
réservées à la Personne B. Les résultats finaux et limites des vérifications
sont consignés dans `docs/test-results.md`.

## Intégration complète des modules A et B sur develop

Date : 21 octobre 2026. Branche vérifiée avant modification : `develop`.
Commit de départ : `d785b09` (fusion de `personne-b` dans `develop`).
L’état Git initial était propre. Aucune branche n’a été créée ni fusionnée.

### 1. Objectif

Intégrer le catalogue et la billetterie réels, conserver leurs fonctionnalités,
valider les migrations et la sécurité, empêcher la survente concurrente et
vérifier le JAR contre le MySQL démarré par Docker Compose.

### 2. Démarche et raisonnement

L’analyse a couvert les entités, DTO, repositories, services, controllers REST
et Thymeleaf, migrations, sécurité, POM et tests des deux modules. Les défauts
constatés étaient : gateway d’accès au concert sans implémentation, absence de
`findByIdForUpdate`, référence scalaire sans navigation JPA vers le concert,
gestion des erreurs limitée au catalogue, client absent traité comme un argument
invalide lors d’une demande de carte, formulaires refusés par la chaîne de sécurité,
validation Java désalignée de certaines longueurs V2 et précision du total
insuffisante pour six billets au prix maximal.

Les corrections préservent le contrat `ConcertAvailabilityGateway` de B et ajoutent
son adaptateur JPA. Celui-ci appelle le repository de A avec `PESSIMISTIC_WRITE`
et impose une transaction existante (`MANDATORY`). La transaction d’achat garde
ce verrou jusqu’au commit, compte les ventes ensuite et écrit l’achat atomiquement.
Le niveau `READ_COMMITTED` évite qu’un premier SELECT client, exécuté avant
l’attente du verrou, fige un ancien instantané `REPEATABLE_READ` pour le SUM.
Ce risque a été identifié par analyse ; il n’a pas été présenté comme un test
exécuté avant correction puisque le gateway manquait alors.

La création d’une carte verrouille le client et utilise aussi `READ_COMMITTED`,
pour sérialiser les demandes concurrentes. Le calcul original de B est conservé :
10 % avec une carte active, aucune remise sinon, arrondi `HALF_UP` à deux décimales,
quantité entre 1 et 6. Une carte expire à sa borne supérieure exclusive.
Un concert commencé est refusé lors de l’achat.

`TicketPurchase` conserve son constructeur et son identifiant de concert pour
compatibilité avec le contrat existant ; une association JPA en lecture sur la
même colonne `concert_id` permet de naviguer vers `Concert`. Il n’y a ni colonne
dupliquée ni réécriture du schéma pour cette relation. Les tests relisent le lien
avec le concert, sa salle et le client après `flush/clear`.

V1 et V2 sont conservées sans modification. La nouvelle V3 élargit le total en
`DECIMAL(12,2)` pour accepter six fois `99999999.99`, impose un total positif et une
période de carte strictement croissante. La réservation documentaire V3 pour des
données de démonstration n’avait pas de script ; les futures données devront
prendre une version ultérieure. `ddl-auto=validate` reste actif.

L’advice existant est étendu aux trois controllers REST de B, avec les mêmes
erreurs JSON et codes 400/404/409. Les constructeurs d’exception existants sont
préservés et complétés par des codes métier. Les DTO et colonnes Java sont alignés
sur les longueurs SQL de V2. Les services valident aussi les demandes venant
des formulaires, qui ne passent pas par le `@Valid` d’un controller REST.

La sécurité Basic/BCrypt et les rôles REST sont conservés. Les pages Thymeleaf
et leur CSS sont rendus accessibles selon les mêmes droits. Les formulaires
utilisent un jeton CSRF en cookie et un champ caché ; seuls les chemins REST
`/api/**` sont exemptés. Aucun contournement global de sécurité n’est introduit.
Les erreurs techniques des formulaires n’affichent plus de texte SQL.

La dépendance TestNG en scope compile/version `RELEASE` est retirée après recherche
confirmant l’absence d’import ou de test TestNG. Les tests existants restent JUnit
Jupiter. Aucun endpoint ni calcul métier utile n’est supprimé. Compose exige
désormais des mots de passe locaux non vides, sans publier leurs valeurs.

### 3. Documentation utilisée

- [Conventions du projet](CONVENTIONS.md), notamment contrat de verrouillage,
  routes, rôles, SQL, transactions et format des erreurs.
- [MySQL 8.4 — Locking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html) :
  durée du verrou et comportement après attente d’une transaction concurrente.
- [MySQL — Consistent Nonlocking Reads](https://dev.mysql.com/doc/refman/8.2/en/innodb-consistent-read.html) :
  instantanés de lecture et différence entre READ COMMITTED et REPEATABLE READ.
- [Spring Data JPA — Transactionality](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html) :
  frontières transactionnelles des services et repositories.
- Les sources et tests des deux modules présents dans la branche fusionnée.

### 4. Utilisation de l’IA

Outil : Codex. Modèle : GPT-6. Niveau de raisonnement exact non communiqué.
Prompt reçu en français :

```text
gis comme un développeur senior Java/Spring Boot et responsable de l'intégration technique du projet Concert Ticketing System.

Les développements de la Personne A (infrastructure, catalogue, API REST, sécurité et documentation) et de la Personne B (clients, cartes de fidélité, achats, calcul des prix et gestion de la capacité) sont maintenant réunis dans la branche actuelle develop.

Ta mission est de réaliser une intégration complète, professionnelle et fonctionnelle des deux parties, sans perdre de fonctionnalités ni supprimer du code utile.

Commence par analyser l'ensemble du projet et identifier les problèmes d'intégration : entités JPA, repositories, services, controllers, DTO, migrations Flyway, configuration Spring Security, dépendances Maven, routes API et transactions.

Vérifie et corrige particulièrement :

Base de données : compatibilité des migrations Flyway V1 et V2, relations entre Artist, Venue, Concert, Client, LoyaltyCard et TicketPurchase, contraintes SQL et configuration ddl-auto=validate.

API REST : fonctionnement des endpoints du catalogue, inscription des clients, création des cartes de fidélité et achat de billets.

Sécurité : cohérence des rôles CLIENT, PRODUCTEUR et ADMIN, authentification HTTP Basic avec BCrypt, accès public aux endpoints prévus et réponses 401/403.

Logique métier : calcul des prix avec ou sans carte de fidélité, validité des cartes, contrôle de la capacité des salles et prévention des achats concurrents dépassant les places disponibles.

Transactions : utilisation correcte de ConcertRepository.findByIdForUpdate() dans les achats, avec des transactions atomiques.

Docker : démarrage de MySQL avec Docker Compose, connexion Spring Boot, migrations Flyway et fonctionnement de l'application.

Tests : tests JPA, services, MockMvc, sécurité, Testcontainers MySQL et concurrence. Vérifie notamment le scénario de deux achats simultanés de quatre billets lorsqu'il ne reste que cinq places : un seul achat doit réussir.

Exécute les commandes nécessaires, notamment docker compose up -d et .\mvnw.cmd clean verify, puis corrige progressivement les erreurs jusqu'à obtenir un projet cohérent et fonctionnel.

Règles importantes : travaille uniquement sur la branche develop, sans créer ni fusionner d'autres branches. Avant toute modification importante, vérifie l'état Git et préserve les changements existants. Ne supprime pas de fonctionnalités pour faire passer les tests, ne désactive pas la sécurité, ne remplace pas MySQL par H2 et ne contourne pas les migrations Flyway. Privilégie les corrections minimales et justifie les modifications qui touchent aux deux parties
```

Propositions retenues : adaptateur du gateway, verrous sur concert/client,
transactions READ_COMMITTED, évolution SQL additive, erreurs REST communes et
intégration du frontend avec CSRF. Les corrections touchant les deux parties
raccordent leurs contrats existants plutôt que de remplacer une implémentation.
Aucune modification manuelle de l’utilisateur n’a été observée pendant cette tâche.

### 5. Tests et vérifications

Le bilan initial `.\mvnw.cmd clean verify` a réellement échoué : 163 tests,
100 erreurs de contexte liées au gateway absent. Après corrections, la sélection
des parcours intégrés, concurrence et cartes a réussi : 24 tests. La vérification
complète finale a réussi : 199 tests, aucun échec, aucune erreur, aucun test ignoré.

La suite comprend 25 cas REST/JPA/SQL/formulaires intégrés, 6 cas de concurrence
et verrouillage, 5 tests unitaires d’achat, les tests de prix/cartes/clients de B
et tous les tests catalogue, Swagger, Actuator et sécurité de A.
Le scénario de survente est répété trois fois dans la suite de concurrence :
45 places déjà vendues sur 50, deux demandes simultanées de 4, exactement un
succès, un refus `JAUGE_INSUFFISANTE`, 49 places vendues et un seul nouvel achat.
Les threads utilisent des transactions réelles indépendantes et des fixtures
commitées ; aucun mock ne remplace le verrou MySQL.

Le test de verrou maintient aussi une transaction détentrice ouverte et vérifie
qu’un achat attend sa libération. Un autre test confirme qu’une acquisition par
le gateway hors transaction est refusée. Les demandes simultanées de cartes
produisent une seule carte active. Les tests HTML vérifient le refus sans CSRF,
l’acceptation du vrai jeton/cookie rendu et les achats/cartes depuis les formulaires.

`docker compose up -d` et `docker compose ps` ont été exécutés : MySQL 8.4 est
`healthy`, publié localement sur 3307. Le JAR a ensuite démarré contre ce MySQL
et appliqué les migrations manquantes. Un parcours HTTP réel a créé artiste,
salle, concert, client, carte et achat : quatre billets à 50 avec carte donnent
180.00, une remise de 0.10 et 46 places restantes. Le doublon de carte renvoie
409 ; l’achat anonyme 401 ; l’achat PRODUCTEUR 403. Santé, Swagger et lecture
publique sont accessibles. Le processus temporaire a été arrêté et seuls les
enregistrements créés par ce contrôle ont été nettoyés par leurs identifiants.

### 6. Problèmes rencontrés et corrections

Le défaut initial de bean empêchait le démarrage de l’application et de ses tests
d’intégration. L’adaptateur et le verrou repository le corrigent. Les autres
défauts relevés par analyse ont été corrigés avant les tests ciblés : isolement
de la lecture de capacité, absence de verrou sur création de carte, précision
du total, 404 client absent, erreurs REST non uniformes et pages HTML bloquées.
Les exécutions ciblée et finale n’ont pas présenté d’échec d’assertion après ces
corrections. Aucun résultat de test non exécuté n’est annoncé comme réussi.

Les avertissements résiduels de dépôts Maven secondaires et de l’agent Mockito
sont non bloquants. Aucune capture d’écran n’a été réalisée. Les rapports réels
figurent dans `target/surefire-reports` et les journaux de vérification sous `target`.

### 7. Traçabilité

Travail uniquement sur develop, sans création/fusion de branche. Les contrôles
Git successifs et `git diff --check` ont été exécutés. Le diff de V1/V2 est vide.
Les modifications concernent les raccords de repository/service, validations,
relations de billetterie, sécurité des formulaires, advice, nouvelle V3, tests,
Compose, suppression de la dépendance inutilisée et documentation.
Les anciens tests sont conservés ; seul le mock du repository client des tests
de carte est aligné sur le nouvel appel verrouillé.
Commit de cette intervention : non créé. Pull Request : non créée.

### 8. Conclusion

L’intégration est fonctionnelle avec 199 tests passés et un contrôle HTTP réel du
JAR contre Compose. Aucun H2, schéma généré par Hibernate ni désactivation de
sécurité n’a été utilisé. Le calcul et les écrans des deux modules sont préservés.
MySQL est laissé actif ; le JAR de contrôle est arrêté. Les comptes restent des
identités de démonstration en mémoire, distinctes des clients métier ; une liaison
personnelle compte/client pour la production reste hors du contrat actuel.

## Instructions d’installation rapide — 21 octobre 2026

- Objectif : rendre l’installation locale compréhensible dans le README, en portugais.
- Outil : Codex ; modèle et niveau de raisonnement non exposés dans cette session.
- Prompt utilisateur original : « rapidamente faz um readme com as intrucoes clara de instalcao ».
- Traduction française du prompt : « Fais rapidement un README avec des instructions claires d’installation. »
- Proposition retenue : ajouter en tête du README les prérequis, le clonage de develop, la configuration privée du .env, le démarrage MySQL, l’import des variables dans PowerShell, le démarrage Spring Boot, les URL, les comptes de démonstration et les commandes de test et d’arrêt. La documentation technique existante est conservée.
- Vérification : git diff --check -- README.md exécuté, code de sortie 0. Aucun test applicatif relancé pour cette modification documentaire. Les commandes de démarrage sont documentées ; elles n’ont pas été réexécutées pendant cette intervention.
- Correction : aucune modification du code applicatif. Aucun commit ni aucune Pull Request créés pour cette intervention.
