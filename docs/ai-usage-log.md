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

Outil : Codex. Modèle : GPT-6. Niveau de raisonnement exact non communiqué.
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
