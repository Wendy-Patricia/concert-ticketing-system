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
