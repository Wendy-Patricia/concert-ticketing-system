# concert-ticketing-system
Professional concert ticketing system built with Java 25, Spring Boot, MySQL, and Docker. Features a secure REST API, ticket management, loyalty discounts, and concurrency-safe booking.

## MySQL com Docker Compose

Requisitos: Docker Engine/Docker Desktop iniciado e Docker Compose.

1. Copie `.env.example` para `.env` (`Copy-Item .env.example .env` no PowerShell).
2. Defina `MYSQL_PASSWORD` e `MYSQL_ROOT_PASSWORD` com palavras-passe diferentes no `.env`.
   O ficheiro fica excluido do Git; o exemplo versionado nao contem segredos.
3. Inicie e verifique o servico:

```sh
docker compose up -d
docker compose ps
```

O servico `mysql` usa a imagem oficial `mysql:8.4`. Aguarde ate a coluna
`STATUS` apresentar `healthy`. Para aguardar automaticamente:

```sh
docker compose up -d --wait --wait-timeout 180
docker compose ps
```

O healthcheck executa `SELECT 1` por TCP com o utilizador da aplicacao na base
configurada. Por omissao, a ligacao local e `127.0.0.1:3306`, a base e
`concert_ticketing` e o utilizador e `concert_app`. A porta pode ser alterada
com `MYSQL_PORT` no `.env` se 3306 ja estiver ocupada.

Os dados persistem no volume `mysql_data` entre reinicios e apos
`docker compose down`. As variaveis de inicializacao da imagem MySQL so sao
aplicadas quando o volume esta vazio; alterar o `.env` nao altera as credenciais
de uma base ja existente.

Para parar os servicos preservando os dados:

```sh
docker compose down
```

Referencias: [imagem oficial MySQL](https://hub.docker.com/_/mysql) e
[healthcheck do Compose](https://docs.docker.com/reference/compose-file/services/#healthcheck).

## Configuration Spring Boot et Flyway

Spring Boot ne charge pas automatiquement le fichier `.env` utilisé par Compose.
Exportez les variables dans le processus Java ou configurez-les dans votre IDE :
`MYSQL_USER`, `MYSQL_PASSWORD`, ainsi que `MYSQL_HOST`, `MYSQL_PORT` et
`MYSQL_DATABASE` si les valeurs par défaut ne conviennent pas. Utilisez le port
publié par Compose pour une application lancée sur le poste (3307 sur le poste
de validation). Le mot de passe root est réservé à l'initialisation MySQL.

Vous pouvez aussi fournir directement `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` et `SPRING_DATASOURCE_PASSWORD` depuis l'environnement.
Ne placez aucun identifiant réel dans les fichiers versionnés ou les arguments
de commande. `SERVER_PORT` vaut 8080 par défaut ; `APP_TIME_ZONE` vaut
`Europe/Paris` et configure le bean `Clock` utilisé pour les futures règles temporelles.

Flyway est activé dans `classpath:db/migration`. Hibernate utilise
`ddl-auto=validate` et ne crée pas de tables. La réservation des versions et le
contrat avec la Personne B sont décrits dans
[le guide des migrations](src/main/resources/db/migration/README.md).
V1 crée désormais les tables `artists`, `venues` et `concerts`. Les tests
`CatalogPersistenceTests` vérifient la migration, les relations et les contraintes
sur MySQL 8.4/Testcontainers, avec Hibernate en mode `validate`.

Actuator expose `/actuator/health` et les sondes `/actuator/health/liveness` et
`/actuator/health/readiness`, sans détails internes. La vérification de santé
globale inclut la connexion à la base. Les trois endpoints ont été vérifiés avec
le JAR et le schéma V1 : HTTP 200 et état `UP`.
