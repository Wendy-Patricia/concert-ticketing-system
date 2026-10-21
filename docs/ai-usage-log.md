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
Prompt de cadrage final exact :

```text
INFRA-04
Configurar application.properties


Definir datasource por variáveis de ambiente, Flyway, ddl-auto=validate, Actuator e propriedades específicas da aplicação.
Critério de aceitação: A configuração é reconhecida pelo Spring Boot e não contém credenciais reais.
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
