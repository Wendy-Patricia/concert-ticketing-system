# Préparation des migrations Flyway — INFRA-05

Ce répertoire correspond à `classpath:db/migration`. Il est conservé dans Git
par ce document. Aucun script SQL vide ne réserve artificiellement une version.

## Versions réservées

| Ordre | Fichier à créer | Responsable | Contenu |
|---|---|---|---|
| 1 | `V1__create_catalog.sql` (créée) | Personne A | `artists`, `venues`, `concerts`, contraintes et index du catalogue |
| 2 | `V2__create_ticketing.sql` (créée) | Personne B | `customers`, `loyalty_cards`, `ticket_purchases`, contraintes et index de billetterie |
| 3 | `V3__integrate_ticketing_constraints.sql` (créée) | Intégration develop | Précision du total, total positif, période de carte valide |

## Coordination avec la Personne B

Le contrat de répartition est défini dans [les conventions du projet](../../../../../docs/CONVENTIONS.md).
Avant de fusionner V2, la Personne B doit vérifier avec la Personne A les noms et
types des clés de V1, notamment `concerts.id BIGINT`, référencée par
`ticket_purchases.concert_id`. V2 crée d'abord les tables dont dépendent ses clés
étrangères. V1 doit être disponible avant toute validation de V2.

V1 et V2 sont intégrées sur develop et ne sont pas réécrites. L’ancienne
réservation V3 pour des données de démonstration ne correspondait à aucun script.
V3 est utilisée pour les corrections d’intégration ; les données de démonstration
devront prendre la prochaine version disponible.

## Règles communes

- Nommage : `V<version>__<description_en_snake_case>.sql`, avec deux underscores.
- Une seule migration par version ; ordre numérique V1, V2, puis V3.
- SQL UTF-8 ; tables et colonnes en anglais et en `snake_case`.
- Identifiants `BIGINT AUTO_INCREMENT`, prix catalogue `DECIMAL(10,2)`, total
  d’achat `DECIMAL(12,2)` après V3 ; contraintes
  et index conformes aux conventions du projet.
- Ne jamais modifier une migration déjà appliquée à une base partagée : créer
  une nouvelle version pour toute correction.
- Flyway utilise le datasource de Spring Boot et s'exécute avant la validation
  Hibernate. Ne pas ajouter `schema.sql` ou activer la génération DDL Hibernate.
- Les noms et checksums sont validés ; exécution hors ordre et baseline
  automatique désactivées. Aucun `clean` n'est autorisé par la configuration.

## Validation du catalogue et suite des migrations

V1 est désormais créée pour le catalogue. Elle définit les identifiants, les
colonnes, les unicités, les clés étrangères et les contraintes de capacité et de
prix. La date future est validée à l’écriture par Jakarta Validation (`@Future`) :
aucun `CHECK` dépendant de l’heure courante n’est ajouté à MySQL.

`CatalogPersistenceTests` utilise MySQL 8.4/Testcontainers et cette migration
réelle, avec Hibernate en mode `validate`. Les tests vérifient également la
présence de V1 dans `flyway_schema_history`. Les résultats sont consignés dans
`docs/test-results.md` et `docs/ai-usage-log.md` à la racine du projet.

`TicketingIntegrationTests` vérifie V1 → V2 → V3, les associations et les contraintes
SQL ; `TicketingConcurrencyTests` vérifie les transactions indépendantes sur
MySQL. Un second démarrage doit laisser les migrations appliquées intactes.
Ne pas modifier une version après son application à une base partagée.
