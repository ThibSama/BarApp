# Tests backend — Le Bar'app

> **Comment exécuter la suite de tests et comment l'infrastructure d'intégration
> est conçue.** Voir aussi [`security.md`](./security.md) et
> [`barmaker-orders.md`](./barmaker-orders.md).

---

## 1. Commande standard

```bash
cd backend
./mvnw clean verify
```

Cette unique commande :

- exécute les tests unitaires et contrôleur (Surefire) ;
- exécute les tests d'intégration PostgreSQL (Failsafe, classes `*IT`) ;
- fusionne automatiquement les données de couverture (`jacoco.exec` +
  `jacoco-it.exec` → `jacoco-merged.exec`) ;
- génère le rapport de couverture combiné.

Aucune exécution classe par classe ni fusion JaCoCo manuelle n'est nécessaire.

Rapport de couverture :

```text
backend/target/site/jacoco/index.html
```

---

## 2. Pré-requis Docker

Les tests d'intégration utilisent **Testcontainers** avec une vraie image
`postgres:16-alpine` (aucun repli H2).

Le démon Docker local doit être démarré. Le client docker-java embarqué négocie
une version d'API ; le `pom.xml` épingle la version d'API au niveau du plugin
Failsafe pour rester compatible avec le démon local :

```xml
<argLine>@{failsafeArgLine} -Dnet.bytebuddy.experimental=true -Dapi.version=1.44</argLine>
```

Ajuster cette valeur si le démon local impose une version minimale différente
(Docker Engine 29 exige au minimum `1.44`).

---

## 3. Cycle de vie du conteneur PostgreSQL (singleton)

`AbstractPostgresIntegrationTest` démarre **un seul** conteneur PostgreSQL,
partagé par toutes les classes `*IT` pour toute la durée de la JVM de test :

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static { POSTGRES.start(); }   // démarré une seule fois
}
```

Le conteneur est **volontairement** géré par un initialiseur statique plutôt
que par `@Testcontainers`/`@Container`. Avec la gestion JUnit, le conteneur
statique est arrêté dans le `afterAll` de chaque classe puis redémarré sur un
**nouveau port aléatoire** pour la classe suivante ; or toutes les classes `*IT`
partagent la même configuration `@SpringBootTest`, donc Spring **réutilise un
contexte applicatif mis en cache** dont la source de données `@ServiceConnection`
pointe encore sur l'ancien port → HikariCP n'obtient plus de connexion et expire
au bout de ~30 s.

En démarrant le conteneur une fois et en ne l'arrêtant jamais entre les classes,
le port mappé (et donc le contexte mis en cache) reste valide pour toute la
campagne Failsafe. Le conteneur est récupéré par le *resource reaper* (Ryuk) de
Testcontainers et à l'arrêt de la JVM.

> Conséquence : les tests d'intégration partagent une base persistante au sein
> d'une exécution. Ils sont donc écrits pour être **indépendants de l'ordre**
> (identifiants UUID générés, assertions sur des deltas de comptage, données de
> catalogue de test sous une catégorie masquée). Flyway applique ses migrations
> une seule fois au démarrage du contexte.

---

## 4. Décompte attendu

| Suite                          | Tests |
|--------------------------------|-------|
| Surefire (unitaires/contrôleur) | 71    |
| Failsafe (intégration `*IT`)    | 78    |
| **Total**                      | **149** |

Couverture JaCoCo fusionnée attendue : **lignes ≈ 97 %**, branches ≈ 78 %
(seuil minimal exigé : lignes > 85 %).
