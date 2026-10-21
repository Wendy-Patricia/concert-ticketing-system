# Validacao do Docker Compose MySQL

Data: 2026-10-21.

- `docker compose config --quiet`: passou.
- `git check-ignore .env`: confirmou que as credenciais locais ficam ignoradas.
- `docker compose up -d --wait --wait-timeout 180`: passou, servico `mysql` saudavel.
- `docker compose ps`: `concert-ticketing-system-mysql-1`, imagem `mysql:8.4`,
  estado `Up About a minute (healthy)`, porta `127.0.0.1:3307->3306/tcp`.
- `git diff --check`: passou.

A primeira tentativa na porta 3306 falhou porque o sistema recusou o bind.
Foi usado `MYSQL_PORT=3307` apenas no `.env` local; o exemplo mantem 3306.
O contentor ficou em execucao. A persistencia foi configurada com um volume
nomeado; nao foi realizado um ciclo de escrita e recriacao para a testar.

## INFRA-04 — Configuration Spring Boot (21 octobre 2026)

| Vérification | Résultat attendu | Résultat obtenu |
|---|---|---|
| `mvn -B -Dtest=ApplicationConfigurationTests test` | Chargement et liaison des propriétés | 3 tests réussis, 0 échec, 0 erreur. |
| `.\mvnw.cmd -B clean verify` | Compilation, tests et création du JAR | `BUILD SUCCESS` ; 4 tests réussis, 0 échec, 0 erreur. |
| `java -jar target/concert-ticketing-system-0.0.1-SNAPSHOT.jar --server.port=0`, avec les variables locales exportées | Démarrage hors IDE contre MySQL 8.4 | Application démarrée ; processus temporaire arrêté après le contrôle. |
| Requête HTTP sans authentification vers `/actuator/health` sur le port attribué | HTTP 200, état `UP`, aucun détail interne | HTTP 200 ; état `UP` avec groupes `liveness` et `readiness`. |
| `git check-ignore .env` | Secrets locaux ignorés | `.env` confirmé comme ignoré. |
| `git diff --check` | Absence d’erreurs d’espacement | Code de sortie 0 ; avertissements LF/CRLF uniquement. |

Les tests ciblés vérifient le datasource, Flyway, Hibernate, les propriétés
Actuator et le bean `Clock`, dont le rejet d’un fuseau invalide. L’échec de
création du bean dans ce dernier cas est volontaire.
Le test de contexte existant utilise `mysql:latest`, avec un avertissement de
compatibilité Flyway ; le contrôle du JAR utilise la version 8.4 de Compose.
Aucune migration SQL n’existe encore : leur validation réelle reste à faire
après V1. Détails dans [le journal d’utilisation de l’IA](ai-usage-log.md#infra-04--configuration-spring-boot).

## Entité Artist — 21 octobre 2026

| Vérification | Résultat attendu | Résultat obtenu |
|---|---|---|
| `.\mvnw.cmd -B clean verify` | Nettoyage, compilation et validation complète | Arrêt au nettoyage : le JAR existant est verrouillé par un autre processus. |
| `.\mvnw.cmd -B test` — `ArtistTests` | Rejet des noms invalides et des chaînes trop longues ; acceptation du genre facultatif et des longueurs limites | 7 tests réussis, 0 échec, 0 erreur. |
| Même commande — `ApplicationConfigurationTests` | Configuration toujours reconnue | 3 tests réussis, 0 échec, 0 erreur. |
| Même commande — `ConcertTicketingSystemApplicationTests` | Contexte compatible avec le schéma SQL | 1 erreur : `Schema validation: missing table [artists]`. V1 n’existe pas encore. |

Bilan : compilation réussie ; 11 tests exécutés, 10 réussis et 1 erreur liée au
schéma absent. La suite complète ne passe donc pas encore. La migration V1 devra
créer `artists` avec ses colonnes et une contrainte `UNIQUE(name)`. L’unicité est
déclarée dans le mappage JPA mais n’a pas encore été testée dans MySQL.
`ddl-auto=validate` est conservé. Les sorties sont enregistrées dans les fichiers
ignorés `target/artist-clean-verification.log`, `target/artist-tests.log` et les
rapports Surefire. Aucune capture d’écran n’a été réalisée.

## Venue, Concert et V1 — 21 octobre 2026

Cette validation résout l’erreur historique `missing table [artists]` décrite
dans la section précédente. V1 était absente de la copie locale et a été créée
avec le nom réservé `V1__create_catalog.sql` pour les trois tables du catalogue.
Testcontainers utilise désormais `mysql:8.4`, comme Compose.

| Commande ou vérification | Résultat attendu | Résultat obtenu |
|---|---|---|
| Premier `.\mvnw.cmd clean verify` | Nettoyage et vérification complète | Échec du nettoyage : JAR verrouillé. Deux anciens processus temporaires ont été identifiés et arrêtés de manière ciblée. |
| `.\mvnw.cmd -B -Dtest=CatalogPersistenceTests test`, première tentative | Persistance et contraintes du catalogue | 36 cas, 32 réussis, 4 échecs d’assertion sur la traduction Spring de l’erreur CHECK MySQL 3819. |
| `.\mvnw.cmd clean verify`, après corrections | Migration réelle, validation Hibernate, tests et création du JAR | `BUILD SUCCESS` : 47 tests réussis, 0 échec, 0 erreur, 0 test ignoré. |
| Démarrage du JAR avec variables locales et `--server.port=0` | Application reconnue par Spring Boot, V1 appliquée à MySQL 8.4 | Démarrage réussi ; processus temporaire arrêté et arrêt confirmé. |
| Requêtes HTTP sans authentification vers les trois endpoints Actuator | HTTP 200 et `UP` | `/actuator/health`, `/actuator/health/liveness` et `/actuator/health/readiness` : HTTP 200 / `UP`. |
| `git diff --check` | Absence d’erreurs d’espacement | Code de sortie 0 ; avertissements LF/CRLF uniquement. |

### Répartition de la vérification finale

| Classe JUnit | Cas exécutés | Échecs | Erreurs |
|---|---:|---:|---:|
| `ApplicationConfigurationTests` | 3 | 0 | 0 |
| `CatalogPersistenceTests` | 36 | 0 | 0 |
| `ConcertTicketingSystemApplicationTests` | 1 | 0 | 0 |
| `ArtistTests` | 7 | 0 | 0 |
| **Total** | **47** | **0** | **0** |

### Couverture et corrections

- Flyway : V1 présente et réussie dans `flyway_schema_history` ; Hibernate reste
  en mode `validate`, sans création de schéma pour les tests.
- Persistance : identifiants générés, relecture de `Venue` et `Concert` après
  `flush`/`clear`, références vers l’artiste et la salle.
- Salle : capacités 50 et 50 000 acceptées ; 49, 50 001 et `null` refusées ; noms
  et villes nuls ou blancs refusés ; doublon `(name, city)` refusé par MySQL ;
  même nom dans deux villes et deux noms dans une ville acceptés.
- Concert : prix `null`, zéro, négatif, trop précis ou hors `DECIMAL(10,2)` refusé ;
  titre absent ou blanc, date absente ou passée et associations nulles refusés.
- SQL direct : `CHECK` de capacité/prix, `NOT NULL` et clés étrangères testés
  indépendamment de Jakarta Validation. Les références inexistantes sont refusées.
- Artiste : unicité du nom vérifiée cette fois dans MySQL.
- Correction des quatre premières assertions : les violations `CHECK` sont
  traduites ici en `UncategorizedSQLException`. Les assertions vérifient une
  `DataAccessException`, le nom exact de la contrainte et le code SQL 3819.
  Les données invalides étaient déjà correctement refusées par MySQL.

Les avertissements non bloquants concernent notamment la dépendance TestNG
préexistante avec version `RELEASE`, des métadonnées de dépôts secondaires (HTTP
401), l’absence de templates Thymeleaf et l’agent Mockito. Ils n’empêchent pas
la réussite de la vérification finale.

Les rapports sont dans `target/surefire-reports`, le journal final dans
`target/catalog-clean-verify.log` et les journaux de démarrage dans
`target/catalog-startup*.log` (fichiers ignorés par Git). Aucune capture d’écran
n’a été réalisée. Le journal d’utilisation de l’IA, les références et le prompt
en français figurent dans [ai-usage-log.md](ai-usage-log.md).

Limites : la date future est contrôlée par Jakarta Validation à l’écriture JPA,
pas par une insertion SQL directe. V2/V3 restent hors du périmètre de cette tâche.

## API REST du catalogue — 21 octobre 2026

### Commandes réellement exécutées

| Vérification | Résultat attendu | Résultat obtenu |
|---|---|---|
| `.\mvnw.cmd clean verify` | Compilation Java 25, tests et création du JAR | `BUILD SUCCESS`, 94 tests exécutés, 0 échec, 0 erreur, 0 test ignoré. |
| Lancement temporaire du JAR hors IDE avec variables locales et `--server.port=0` | Démarrage avec V1 et les nouveaux repositories/services/controllers | Démarrage réussi ; processus arrêté après les contrôles. |
| Requête `/actuator/health` sur le JAR | HTTP 200 | HTTP 200. |
| Requête `GET /api/concerts` sur le JAR avec compte Basic temporaire | HTTP 200 et tableau JSON | HTTP 200 et liste JSON. Aucun compte ni secret ajouté aux fichiers versionnés. |
| `git diff --check` | Absence d’erreurs d’espacement | Code de sortie 0. |

### Répartition des tests

| Classe | Tests | Échecs | Erreurs | Ignorés |
|---|---:|---:|---:|---:|
| `ApplicationConfigurationTests` | 3 | 0 | 0 | 0 |
| `CatalogApiTests` | 41 | 0 | 0 | 0 |
| `CatalogPersistenceTests` | 36 | 0 | 0 | 0 |
| `CatalogRolePreparationTests` | 6 | 0 | 0 | 0 |
| `ConcertTicketingSystemApplicationTests` | 1 | 0 | 0 | 0 |
| `ArtistTests` | 7 | 0 | 0 | 0 |
| **Total** | **94** | **0** | **0** | **0** |

### Couverture de l’API

- Créations 201 d’artiste, de salle et de concert ; identifiants générés et DTO
  imbriqués ; `Location` du concert ; lecture du détail avec 200.
- Liste vide, tri chronologique, filtres `du`, `au`, `artiste` séparés et combinés,
  bornes inclusives de journée à la microseconde et exclusion du lendemain.
- 400 : noms obligatoires, capacités 49/50 001/null, prix zéro/négatif/null/hors
  précision, date passée, champs et associations manquants, JSON mal formé,
  identifiants invalides, dates de filtre invalides et période inversée.
- 404 : concert inexistant, artiste inconnu et salle inconnue.
- 409 : artiste et salle dupliqués ; même salle dans une autre ville acceptée ;
  conflit d’intégrité simulé avec vérification de l’absence de SQL dans la réponse.
- Rôles dans une configuration de test uniquement : lecture publique autorisée,
  CLIENT sans droit de création, PRODUCTEUR autorisé sur artistes/concerts,
  ADMIN autorisé sur les trois créations, anonyme refusé en écriture.

Les 41 tests API utilisent MockMvc avec les filtres HTTP désactivés, les vrais
services/repositories et MySQL 8.4/Testcontainers. Les 6 tests de préparation des
rôles utilisent `@WebMvcTest`, des services simulés et une sécurité de méthode
activée dans une `@TestConfiguration`. Ils ne valident pas la future configuration
de sécurité de l’application, qui reste inchangée.

Cette section décrit la validation historique de l’API avant l’activation de la
sécurité. La validation de la véritable configuration est présentée ci-dessous.

### Observations et adaptations

Aucun échec de test n’a été rencontré pendant cette intervention. Les repositories
absents ont été ajoutés, la cible de compilation a été alignée de Java 17 à Java
25 et la borne supérieure des dates a été limitée à la précision de V1. Les
annotations `@PreAuthorize` sont présentes, mais leur activation et les règles
HTTP publiques restent à intégrer dans la branche sécurité.

Rapports : `target/surefire-reports`. Journal de vérification :
`target/api-clean-verify.log`. Journaux du JAR : `target/api-startup*.log`.
Les limites et avertissements non bloquants sont détaillés dans
[ai-usage-log.md](ai-usage-log.md). Aucune capture d’écran n’a été réalisée.

## Sécurité HTTP Basic, BCrypt et rôles — 21 octobre 2026

### Exécutions réelles

| Commande | Résultat attendu | Résultat obtenu |
|---|---|---|
| `.\mvnw.cmd -B "-Dtest=SecurityConfigTests,CatalogRolePreparationTests,CatalogApiTests" test` | Sécurité et API compatibles | `BUILD SUCCESS`, 85 tests, 0 échec, 0 erreur. |
| `.\mvnw.cmd clean verify` | Compilation, ensemble des tests et JAR | `BUILD SUCCESS`, 132 tests, 0 échec, 0 erreur, 0 test ignoré. |

| Classe JUnit | Tests | Échecs | Erreurs |
|---|---:|---:|---:|
| `ApplicationConfigurationTests` | 3 | 0 | 0 |
| `CatalogApiTests` | 41 | 0 | 0 |
| `CatalogPersistenceTests` | 36 | 0 | 0 |
| `CatalogRolePreparationTests` | 6 | 0 | 0 |
| `ConcertTicketingSystemApplicationTests` | 1 | 0 | 0 |
| `ArtistTests` | 7 | 0 | 0 |
| `SecurityConfigTests` | 38 | 0 | 0 |
| **Total** | **132** | **0** | **0** |

### Couverture de la sécurité

- Routes publiques accessibles sans identifiants, y compris POST `/api/clients`.
- Routes protégées sans identifiants : 401 avec challenge Basic et erreur JSON.
- Mot de passe incorrect ou compte inconnu : 401.
- Compte valide sans le bon rôle : 403, sans challenge trompeur de réauthentification.
- CLIENT autorisé sur achats/cartes ; PRODUCTEUR autorisé sur artistes/concerts ;
  ADMIN autorisé sur administration, production et cartes de fidélité.
- ADMIN refusé sur les achats réservés à CLIENT ; les rôles restent distincts.
- POST autorisés sans jeton CSRF, authentification Basic réelle avec les comptes
  de démonstration et mots de passe stockés sous forme BCrypt.
- Aucun cookie de session, aucune session créée, authentification absente à la
  requête suivante et contexte d’authentification d’une session entrante ignoré.
- Routes non répertoriées refusées, absence de redirection vers un formulaire HTML.

`SecurityConfigTests` utilise des sondes HTTP de test pour les routes de la
Personne B et Swagger. Ces tests valident les règles d’accès sans implémenter
leurs controllers métier. `CatalogRolePreparationTests` utilise désormais la
vraie `SecurityConfig` avec les controllers du catalogue et des services simulés.
`CatalogApiTests` conserve ses tests métier avec filtres désactivés et un ADMIN
simulé, nécessaire depuis l’activation de `@PreAuthorize`.

### Erreurs et corrections

Aucun échec de test n’a été observé pendant cette intervention. Les adaptations
préventives ont consisté à fournir l’utilisateur ADMIN aux tests métier et à
remplacer l’ancienne configuration de sécurité de test par la vraie configuration.
Les avertissements non bloquants préexistants sont inchangés. Aucun test n’est
déclaré réussi sans exécution.

Les comptes et la procédure Postman sont documentés dans le README. La décision
CSRF concerne une API sans cookie destinée aux clients explicites ; l’absence de
session seule ne garantit pas une protection CSRF pour un navigateur HTTP Basic.
Les rapports et journaux sont sous `target/surefire-reports`,
`target/security-tests.log` et `target/security-clean-verify.log`.

### Contrôle réseau du JAR hors IDE

Le JAR généré par `clean verify` a été lancé avec les variables locales et un
port temporaire. Les requêtes HTTP réelles suivantes ont été exécutées :

| Requête | Compte | Résultat obtenu |
|---|---|---|
| GET `/api/concerts` | Aucun | 200 |
| GET `/actuator/health` | Aucun | 200 |
| GET `/api/concerts/9223372036854775807` | Aucun | 404 |
| POST `/api/production/artistes` | Aucun | 401 |
| POST `/api/admin/salles` | admin, mauvais mot de passe | 401 |
| POST `/api/production/artistes` | client | 403 |
| POST `/api/production/artistes`, corps `{}` | producteur | 400 : sécurité franchie, validation métier refusée |
| POST `/api/admin/salles`, corps `{}` | admin | 400 : sécurité franchie, validation métier refusée |

Les réponses 401 contiennent le challenge Basic et aucune réponse contrôlée ne
crée de cookie de session. Aucun enregistrement métier n’a été ajouté. Le processus
temporaire a été arrêté après le contrôle. Les journaux du démarrage sont
`target/security-startup.log` et `target/security-startup-error.log`.
La procédure Postman est documentée ; son interface n’a pas été exécutée.

## Intégration A/B sur develop — 21 octobre 2026

État initial : branche `develop`, commit `d785b09`, espace de travail Git propre.
Aucune branche n’a été créée ni fusionnée pendant l’intervention.

| Exécution | Résultat réellement obtenu |
|---|---|
| `docker compose up -d` puis `docker compose ps` | MySQL 8.4 en cours d’exécution, `healthy`, port `127.0.0.1:3307`. |
| `.\mvnw.cmd clean verify`, bilan avant correction | Échec : 163 tests exécutés, 100 erreurs de contexte ; `ConcertAvailabilityGateway` sans bean d’implémentation. |
| `.\mvnw.cmd -B "-Dtest=TicketingIntegrationTests,TicketingConcurrencyTests,LoyaltyCardServiceTest" test` | `BUILD SUCCESS` : 24 tests, 0 échec, 0 erreur. |
| `.\mvnw.cmd clean verify`, vérification finale | `BUILD SUCCESS` : 199 tests, 0 échec, 0 erreur, 0 test ignoré. |
| `git diff --check` | Code de sortie 0. |
| Diff Git sur les scripts V1 et V2 | Aucun changement ; V3 ajoutée pour les ajustements d’intégration. |

### Répartition finale

| Classe | Tests | Échecs | Erreurs |
|---|---:|---:|---:|
| `ApplicationConfigurationTests` | 3 | 0 | 0 |
| `CatalogApiTests` | 41 | 0 | 0 |
| `CatalogPersistenceTests` | 45 | 0 | 0 |
| `CatalogRolePreparationTests` | 6 | 0 | 0 |
| `ConcertTicketingSystemApplicationTests` | 1 | 0 | 0 |
| `ArtistTests` | 7 | 0 | 0 |
| `OpenApiAndHealthTests` | 13 | 0 | 0 |
| `SecurityConfigTests` | 38 | 0 | 0 |
| `CustomerServiceTest` | 2 | 0 | 0 |
| `LoyaltyCardServiceTest` | 3 | 0 | 0 |
| `TicketPricingServiceTest` | 4 | 0 | 0 |
| `TicketPurchaseServiceTest` | 5 | 0 | 0 |
| `TicketingConcurrencyTests` | 6 | 0 | 0 |
| `TicketingIntegrationTests` | 25 | 0 | 0 |
| **Total** | **199** | **0** | **0** |

### Concurrence MySQL et atomicité

Le scénario exigé est exécuté trois fois dans `TicketingConcurrencyTests` :

- Salle de 50 places ; achats existants totalisant 45 places, committés avant les threads.
- Deux threads démarrés avec une barrière, demandant chacun quatre billets.
- Exactement un achat validé et un refus `BusinessConflictException` avec code `JAUGE_INSUFFISANTE`.
- Total SQL final : 49 places vendues ; neuf lignes d’achat au lieu de huit.
- Une place restante et aucune écriture du second achat refusé.

Les autres cas vérifient une seule carte active après deux créations simultanées,
l’attente réelle d’un verrou détenu par une autre transaction et l’interdiction
du gateway hors transaction. Il s’agit de MySQL/Testcontainers et de transactions
réelles, sans mock ni transaction de test globale autour des threads.

### Parcours intégrés et SQL

Les 25 cas REST/JPA/formulaires valident V1 → V2 → V3 avec `ddl-auto=validate`,
l’adresse embarquée, les relations achat/client/concert/salle après relecture,
l’inscription publique et les doublons d’email, la création et le renouvellement
des cartes, leur date d’expiration exclusive et les cartes futures non actives.

Les achats sont vérifiés avec et sans remise, jusqu’à épuisement exact de capacité,
avec refus sans écriture supplémentaire. Le total maximal de six billets
`599999999.94` est persisté sans débordement. Les concerts commencés sont refusés.
Les cas SQL directs vérifient unicité d’email, clés étrangères, quantités 0/7,
totaux zéro/négatifs et période de carte invalide.

Les codes REST 201/400/404/409 et de sécurité 401/403 sont vérifiés avec les vrais
controllers, services et filtres. Les formulaires Thymeleaf restent fonctionnels :
CSS public, création client, carte et achat, refus sans CSRF et acceptation du
véritable couple jeton/cookie rendu par la page.

### Contrôle du JAR sur le MySQL Compose

Le JAR issu du build final a été lancé hors IDE sur un port temporaire avec la
configuration résolue de Compose. Migrations et connexion MySQL ont réussi.

| Requête réelle | Résultat |
|---|---|
| GET santé, JSON OpenAPI et Swagger UI | 200 ; santé `UP`, route `/api/achats` présente dans OpenAPI. |
| POST artiste PRODUCTEUR, salle ADMIN et concert PRODUCTEUR | 201 pour les trois créations. |
| POST inscription client sans compte Basic | 201. |
| POST carte avec ADMIN | 201. |
| POST achat CLIENT, quatre billets à 50 avec carte active | 201 ; total `180.00`, remise `0.10`, places restantes `46`. |
| Deuxième demande de carte active | 409. |
| Achat sans authentification | 401. |
| Achat avec PRODUCTEUR | 403. |
| Lecture publique du concert créé | 200. |

Le processus temporaire a été arrêté et seules ses lignes de test ont été
nettoyées, par leurs identifiants/codes connus et dans une transaction SQL.
MySQL Compose reste actif. Aucun enregistrement antérieur n’a été supprimé.

### Corrections justifiées et limites

Le gateway manquant et le repository verrouillé raccordent les contrats A/B.
L’isolation READ_COMMITTED évite un SUM basé sur un instantané antérieur à l’attente
du verrou. Le client est verrouillé pour les cartes concurrentes. V3 élargit le
total et ajoute des CHECK sans réécrire V1/V2. Les erreurs B sont intégrées au
format commun et les formulaires sont protégés par CSRF, avec API stateless.
La dépendance TestNG inutilisée a été supprimée ; tous les tests restent JUnit.

Les limites de démonstration des comptes en mémoire et de leur absence de liaison
personnelle à un client métier sont documentées. Les messages du test de fuseau
invalide sont attendus ; les avertissements Maven secondaires/Mockito sont non
bloquants. Aucune capture d’écran n’a été réalisée.

Journaux : `target/integration-baseline.log`, `target/integration-targeted.log`,
`target/integration-clean-verify.log`, `target/integration-startup*.log` et
rapports `target/surefire-reports`.
