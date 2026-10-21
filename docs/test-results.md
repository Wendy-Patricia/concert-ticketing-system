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
