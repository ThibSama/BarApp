# Le Bar'app — Backend

Application web de commande de cocktails et de suivi de leur préparation.

Ce dépôt contient le socle backend ainsi qu’un prototype frontend Vue 3 situé dans `frontend/`. Le backend couvre le schéma de base de données, les migrations Flyway, le modèle de persistance JPA et la première API publique de consultation de la carte (`GET /api/menu`).

> Le frontend dans `frontend/` est un prototype autonome basé sur des données mockées. Il ne consomme pas encore le backend Spring Boot.

---

## Sommaire

- [Prérequis](#prérequis)
- [Variables d'environnement](#variables-denvironnement)
- [Démarrer PostgreSQL](#démarrer-postgresql)
- [Lancer le backend](#lancer-le-backend)
- [Lancer les tests](#lancer-les-tests)
- [API](#api)
- [Migrations de base de données](#migrations-de-base-de-données)
- [Limitations](#limitations)

---

## Prérequis

| Outil          | Version utilisée / requise                                            |
|----------------|-----------------------------------------------------------------------|
| **Java (JDK)** | **21** ciblé. L'environnement de développement tourne sur **JDK 25 LTS** (voir [Limitations](#limitations)). |
| **Maven**      | Non requis globalement : utilisez le **Maven Wrapper** (`./mvnw`). Maven 3.9.x si vous préférez l'installation locale. |
| **Docker**     | Docker Engine + **Docker Compose v2** (`docker compose`).             |

La compilation cible le bytecode Java 21 (`maven.compiler.release=21`) et reste
exécutable sur un JDK 21 ou supérieur.

---

## Variables d'environnement

Toutes les variables ont des **valeurs par défaut locales sûres**. Copiez le
fichier d'exemple et adaptez si besoin :

```bash
cp .env.example .env
```

| Variable                 | Défaut local | Description                              |
|--------------------------|--------------|------------------------------------------|
| `DB_HOST`                | `localhost`  | Hôte PostgreSQL                          |
| `DB_PORT`                | `5432`       | Port PostgreSQL (hôte)                   |
| `DB_NAME`                | `barapp`     | Nom de la base                           |
| `DB_USER`                | `barapp`     | Utilisateur applicatif                   |
| `DB_PASSWORD`            | `barapp`     | Mot de passe (local uniquement)          |
| `SERVER_PORT`            | `8080`       | Port HTTP du backend                     |
| `SPRING_PROFILES_ACTIVE` | `local`      | Profil Spring actif                      |

> ⚠️ Les valeurs par défaut sont réservées au développement local. **Ne jamais**
> les utiliser en production et **ne jamais committer** de fichier `.env`
> (déjà ignoré dans `.gitignore`).

---

## Démarrer PostgreSQL

PostgreSQL est fourni via `docker-compose.yml` (service `postgres` uniquement,
image `postgres:16-alpine`, volume nommé, healthcheck) :

```bash
# depuis la racine du dépôt
docker compose up -d postgres

# vérifier l'état (doit passer "healthy")
docker compose ps
```

> Si le port `5432` est déjà occupé sur votre machine, lancez sur un autre port :
> ```bash
> DB_PORT=5433 docker compose up -d postgres
> ```
> et démarrez le backend avec la même variable (`DB_PORT=5433`).

Arrêt :

```bash
docker compose down        # conserve les données (volume)
docker compose down -v     # supprime aussi le volume de données
```

---

## Lancer le backend

```bash
cd backend
./mvnw spring-boot:run
```

Au démarrage, **Flyway** applique automatiquement les migrations puis **Hibernate
valide** le schéma (`ddl-auto=validate`, aucune modification de schéma par
Hibernate). L'application écoute sur <http://localhost:8080>.

Si vous avez démarré PostgreSQL sur un port personnalisé :

```bash
DB_PORT=5433 ./mvnw spring-boot:run
```

---

## Lancer les tests

```bash
cd backend
./mvnw test       # tests unitaires (couche web/service/mapper)
./mvnw verify     # tests unitaires + tests d'intégration Testcontainers + couverture JaCoCo
```

Les tests d'intégration (`*IT`) démarrent un **vrai PostgreSQL via
Testcontainers** (image `postgres:16-alpine`) — aucun repli sur H2. Docker doit
donc être disponible.

**Couverture (JaCoCo)** : `./mvnw clean verify` fusionne la couverture des tests
unitaires et d'intégration et génère le rapport dans
`backend/target/site/jacoco/index.html` (CSV/XML à côté). Seule la classe de
démarrage `BarAppApplication` est exclue (lanceur trivial).

> **Note environnement** : le démon Docker local (Engine 29) impose l'API
> Docker ≥ 1.40, alors que le client `docker-java` embarqué par Testcontainers
> négocie 1.32 par défaut. Le `pom.xml` fixe donc `-Dapi.version=1.43` pour les
> tests d'intégration (plugin Failsafe). Ajustez/retirez cette valeur sur un
> environnement doté d'un démon plus ancien.

---

## API

- **Base URL** : `http://localhost:8080`
- **Endpoints publics** (client anonyme) :
  - `GET /api/menu` — consultation de la carte (lecture seule) ;
  - `POST /api/orders` — création d'une commande depuis le panier ;
  - `GET /api/orders/{orderId}` — consultation / suivi d'une commande par son UUID.

### Contrat `GET /api/menu`

| Élément              | Valeur                                                            |
|----------------------|------------------------------------------------------------------|
| Méthode / chemin     | `GET /api/menu`                                                  |
| Codes de statut      | `200 OK` (toujours en cas de succès, y compris carte vide) ; `500` en cas d'erreur interne (sans fuite de stacktrace ni d'identifiants) |
| Carte vide           | `200 OK` avec `{ "categories": [] }`                            |
| Données exposées     | catégories actives → cocktails actifs → ingrédients actifs + tailles/prix actifs |
| Données **non** exposées | entités JPA, enregistrements inactifs, timestamps internes   |
| Tri                  | catégories par `displayOrder` puis nom ; cocktails par nom ; ingrédients par `displayOrder` d'association puis nom ; prix dans l'ordre `S, M, L` |

### Exemple

```bash
curl -i http://localhost:8080/api/menu
```

```json
{
  "categories": [
    {
      "id": 1,
      "name": "Classiques",
      "displayOrder": 1,
      "cocktails": [
        {
          "id": 1,
          "name": "Mojito",
          "description": "Rhum blanc, citron vert, menthe fraîche et eau gazeuse, le grand classique rafraîchissant.",
          "imageUrl": null,
          "ingredients": [
            { "id": 1, "name": "Rhum blanc", "quantityLabel": "4 cl", "displayOrder": 1 }
          ],
          "prices": [
            { "size": "S", "price": 8.50 },
            { "size": "M", "price": 10.50 },
            { "size": "L", "price": 12.50 }
          ]
        }
      ]
    }
  ]
}
```

### Contrat `POST /api/orders`

Crée une commande à partir d'un panier. **Chaque élément de `items` représente une
boisson physique** : un même cocktail/taille peut apparaître plusieurs fois et
génère alors un `OrderItem` distinct.

| Élément              | Valeur                                                            |
|----------------------|------------------------------------------------------------------|
| Méthode / chemin     | `POST /api/orders`                                               |
| Succès               | `201 Created` + en-tête `Location: /api/orders/{id}` + corps de suivi |
| Calcul des prix      | **exclusivement côté serveur** depuis le catalogue actif ; le client ne fournit jamais de prix, nom, total, statut ou numéro de séquence |
| Total                | somme exacte des prix unitaires en `BigDecimal` (précision conservée, jamais de `double`) |
| Snapshots            | `cocktailName` et `unitPrice` sont **figés** au moment de la commande |
| Statut initial       | commande `ORDERED`, chaque ligne `PREPARATION_INGREDIENTS`, dates de complétion `null` |

**Règles de validation de la requête** (sinon `400`) :

- `items` obligatoire, **1 à 50** éléments, aucun élément `null` ;
- `cocktailId` obligatoire et positif ;
- `size` obligatoire, valeurs acceptées `S`, `M`, `L` uniquement (toute autre
  valeur d'énumération est rejetée comme requête mal formée).

Requête :

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "items": [
      {"cocktailId": 1, "size": "M"},
      {"cocktailId": 1, "size": "M"},
      {"cocktailId": 3, "size": "S"}
    ]
  }'
```

Réponse `201 Created` (corps identique à celui de `GET /api/orders/{orderId}`) :

```json
{
  "id": "c91618ae-523c-4818-9749-9ae8151de02b",
  "publicCode": "WENZTF",
  "status": "ORDERED",
  "totalAmount": 30.50,
  "createdAt": "2026-06-29T09:47:30.267029Z",
  "completedAt": null,
  "items": [
    {
      "id": "28c314cd-9d04-4ae2-b837-822e3ab51e5f",
      "sequenceNumber": 1,
      "cocktailName": "Mojito",
      "size": "M",
      "unitPrice": 10.50,
      "preparationStatus": "PREPARATION_INGREDIENTS",
      "completedAt": null
    }
  ]
}
```

### Contrat `GET /api/orders/{orderId}`

| Élément              | Valeur                                                            |
|----------------------|------------------------------------------------------------------|
| Méthode / chemin     | `GET /api/orders/{orderId}` (le `{orderId}` est l'**UUID interne**) |
| Succès               | `200 OK`, même forme de réponse que la création                  |
| Tri                  | les lignes sont triées par `sequenceNumber`                      |
| UUID invalide        | `400 INVALID_IDENTIFIER`                                         |
| Commande inconnue    | `404 ORDER_NOT_FOUND`                                            |

```bash
curl -i http://localhost:8080/api/orders/c91618ae-523c-4818-9749-9ae8151de02b
```

#### UUID vs `publicCode`

- **`id` (UUID)** : référence de suivi **sécurisée et non prédictible**. C'est le
  seul identifiant à utiliser pour récupérer une commande.
- **`publicCode`** (6–8 caractères alphanumériques majuscules) : destiné à
  l'**affichage humain** uniquement (ex. annonce au comptoir). Il est unique en
  base mais ne doit **pas** servir de référence de suivi sécurisée à ce stade.

#### Comportement des snapshots

Les valeurs historiques d'une ligne (`cocktailName`, `size`, `unitPrice`) sont
figées à la création et **ne changent jamais**, même si le catalogue est ensuite
modifié (cocktail renommé, prix changé) ou le cocktail désactivé. Un test
d'intégration le prouve explicitement.

### Format d'erreur (toutes les erreurs API)

Toutes les erreurs gérées partagent une enveloppe stable et un `code`
machine-lisible ; les messages utilisateur sont en français.

```json
{
  "timestamp": "2026-06-29T09:48:46.344637481Z",
  "status": 409,
  "code": "PRICE_UNAVAILABLE",
  "message": "Le tarif de la taille L n'est pas disponible pour le cocktail demandé (id 2).",
  "path": "/api/orders",
  "fieldErrors": []
}
```

| Cas                                   | Statut | `code`                          |
|---------------------------------------|--------|---------------------------------|
| JSON mal formé / taille inconnue      | `400`  | `MALFORMED_REQUEST`             |
| Validation de champ (items, id…)      | `400`  | `VALIDATION_ERROR`              |
| UUID de suivi invalide                | `400`  | `INVALID_IDENTIFIER`            |
| Cocktail introuvable                  | `404`  | `COCKTAIL_NOT_FOUND`            |
| Commande introuvable                  | `404`  | `ORDER_NOT_FOUND`               |
| Cocktail inactif                      | `409`  | `COCKTAIL_UNAVAILABLE`          |
| Taille indisponible                   | `409`  | `SIZE_UNAVAILABLE`              |
| Prix (taille) inactif                 | `409`  | `PRICE_UNAVAILABLE`             |
| Échec de génération du code public    | `500`  | `PUBLIC_CODE_GENERATION_FAILED` |
| Erreur inattendue                     | `500`  | `INTERNAL_ERROR` (sans stacktrace, identifiants, SQL ni nom de classe interne) |

> Une commande invalide (au moins un item invalide) **annule toute la
> transaction** : aucune commande partielle n'est persistée.

---

## Migrations de base de données

- Les migrations Flyway vivent dans
  `backend/src/main/resources/db/migration/` :
  - `V1__create_schema.sql` — schéma (8 tables, contraintes, index, `pgcrypto`)
  - `V2__insert_demo_catalog.sql` — jeu de données de démonstration
- Elles sont **exécutées automatiquement au démarrage** de l'application et
  pendant les tests d'intégration. Hibernate ne crée/altère jamais le schéma
  (`ddl-auto=validate`).
- Une **copie de référence consolidée** (schéma + données de démo) est
  maintenue dans **[`database/init.sql`](database/init.sql)** pour la livraison
  d'examen. Ce fichier reflète le schéma effectif mais n'est **pas** utilisé par
  l'application au runtime (c'est Flyway qui fait foi).

### Jeu de données de démonstration

3 catégories actives (+1 inactive), 12 ingrédients actifs (+1 inactif),
5 cocktails actifs (+1 inactif), prix en `S/M/L` (avec un prix inactif de
démonstration). La table `app_user` est **volontairement vide** : aucune
authentification ni mot de passe en clair à ce stade.

---

## Limitations

- **Java 21 indisponible** sur la machine de développement : le projet **cible**
  le bytecode 21 mais **compile et s'exécute sur JDK 25 LTS**. Byte Buddy est
  activé en mode expérimental (`-Dnet.bytebuddy.experimental=true`) pour accepter
  la version de classe du JVM 25.
- **Frontend autonome** : le prototype Vue 3 dans `frontend/` utilise uniquement des données mockées et n’est pas encore connecté au backend Spring Boot.
- **Pas d'authentification backend** : Spring Security / BCrypt / JWT viendront plus tard.
  Les entités `AppUser` et l'enum de rôle existent uniquement pour la cohérence
  du schéma.
- **Pas de compte client** : les clients restent anonymes ; le suivi se fait via
  l'UUID de la commande.
- **Pas de paiement** et **pas d'annulation** de commande.
- **Pas de workflow de préparation** côté barmaker : les statuts de préparation
  existent mais aucune transition n'est exposée (pas d'endpoint `/api/bar/**`).
- **Aucun endpoint d'administration** (catégories/cocktails/ingrédients/prix).
