# Le Bar'app — Backend

Application web de commande de cocktails et de suivi de leur préparation.

Ce dépôt contient le socle backend ainsi qu’un prototype frontend Vue 3 situé dans `frontend/`. Le backend couvre le schéma de base de données, les migrations Flyway, le modèle de persistance JPA et la première API publique de consultation de la carte (`GET /api/menu`).

> Le frontend dans `frontend/` consomme désormais **réellement** le backend Spring Boot : la carte client (`GET /api/menu`), la création et le suivi de commande (`POST`/`GET /api/orders`), ainsi que la gestion barmaker du catalogue (`/api/bar/categories`, `/api/bar/cocktails`, `/api/bar/ingredients`). Les mocks de production ont été supprimés ; seules des *fixtures de test* subsistent sous `frontend/src/test/`.

---

## Sommaire

- [Prérequis](#prérequis)
- [Variables d'environnement](#variables-denvironnement)
- [Stack Docker complète](#stack-docker-complète)
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

| Variable                     | Défaut local             | Description                                                        |
|------------------------------|--------------------------|--------------------------------------------------------------------|
| `DB_NAME`                    | `barapp`                 | Nom de la base                                                     |
| `DB_USER`                    | `barapp`                 | Utilisateur applicatif                                            |
| `DB_PASSWORD`                | `barapp`                 | Mot de passe (local uniquement)                                  |
| `DB_HOST_PORT`               | `5433`                   | Port PostgreSQL **mappé sur l'hôte** (outils hôte uniquement)     |
| `DB_HOST` / `DB_PORT`        | `localhost` / `5433`     | Cible PostgreSQL pour le backend lancé **hors Docker**            |
| `API_HOST_PORT`              | `8080`                   | Port **hôte** de l'API (`localhost:8080` → conteneur `:8080`)      |
| `FRONTEND_PORT`              | `8081`                   | Port **hôte** du frontend (`localhost:8081` → Nginx `:80`)        |
| `SERVER_PORT`                | `8080`                   | Port HTTP du backend lancé hors Docker                            |
| `SPRING_PROFILES_ACTIVE`     | `local`                  | Profil Spring actif                                              |
| `APP_JWT_SECRET`             | *(dev-only)*             | Secret HMAC JWT (≥ 256 bits) — à remplacer hors développement     |
| `APP_JWT_ISSUER`             | `le-barapp`              | Émetteur (`iss`) des jetons                                       |
| `APP_JWT_EXPIRATION_SECONDS` | `3600`                   | Durée de vie du jeton d'accès (secondes)                          |
| `APP_CORS_ALLOWED_ORIGINS`   | `http://localhost:8081`  | Origines CORS autorisées (séparées par `,`, pas de wildcard)      |

**Signification des ports** (à ne pas confondre) :

| Port | Où | Rôle |
|------|----|------|
| `5432` | interne au réseau Compose | port PostgreSQL utilisé par l'API (`postgres:5432`) |
| `DB_HOST_PORT` (`5433`) | hôte | accès PostgreSQL pour les outils de l'hôte |
| `8080` | interne au conteneur API | port d'écoute Spring Boot |
| `API_HOST_PORT` (`8080`) | hôte | accès direct à l'API (debug / tests) |
| `FRONTEND_PORT` (`8081`) | hôte | accès au frontend (et à l'API via le proxy `/api`) |

> ⚠️ Les valeurs par défaut sont réservées au développement local. **Ne jamais**
> les utiliser en production et **ne jamais committer** de fichier `.env`
> (déjà ignoré dans `.gitignore`). En particulier, `APP_JWT_SECRET` a une valeur
> de développement explicite : remplacez-la pour tout usage hors local.

---

## Stack Docker complète

L'ensemble du stack (`postgres` + `api` + `frontend`) se lance en **une seule
commande** depuis la racine du dépôt :

```bash
cp .env.example .env          # (optionnel) sinon les défauts s'appliquent
docker compose up -d --build
```

Chaîne de démarrage orchestrée par les healthchecks :

```text
postgres (healthy)  ->  api (healthy, après migrations Flyway + validation Hibernate)  ->  frontend
```

### Services et URLs

| Service    | Conteneur          | URL hôte                  | Rôle                                   |
|------------|--------------------|---------------------------|----------------------------------------|
| `postgres` | `barapp-postgres`  | `localhost:5433` (outils) | Base de données                        |
| `api`      | `barapp-api`       | <http://localhost:8080>   | API Spring Boot (`/api/**`)            |
| `frontend` | `barapp-frontend`  | <http://localhost:8081>   | Frontend Nginx + **proxy `/api`**      |

L'API est joignable de deux manières :

- **directe** (debug / tests) : `http://localhost:8080/api/menu` ;
- **via le proxy Nginx du frontend** (même origine, recommandé pour le
  navigateur) : `http://localhost:8081/api/menu` → transmis à `api:8080`.

### Vérifier la santé et les logs

```bash
docker compose ps                       # postgres/api healthy, frontend up
docker compose logs --no-color api      # connexion PostgreSQL, Flyway, démarrage
```

### Smoke tests

```bash
curl -i http://localhost:8080/api/menu        # API directe
curl -i http://localhost:8081/api/menu        # API via le proxy frontend
curl -i http://localhost:8081/                # page frontend
```

### Arrêt (sans perte de données)

```bash
docker compose stop          # arrête les conteneurs, conserve le volume
docker compose down          # supprime les conteneurs, CONSERVE le volume de données
```

> 🛑 **Réinitialisation destructive** (supprime aussi les données PostgreSQL) —
> à n'utiliser qu'en connaissance de cause :
> ```bash
> docker compose down -v
> ```

### Accès depuis un autre appareil (même réseau)

Récupérez l'adresse LAN de la machine hôte :

```bash
hostname -I
```

Puis ouvrez `http://<IP_LAN>:8081` depuis l'autre appareil. Le navigateur n'a
besoin que du frontend : les appels `/api` sont relayés côté serveur vers `api`
par Nginx (aucun nom interne Docker n'est exposé au navigateur). Pensez à ajouter
l'origine LAN à `APP_CORS_ALLOWED_ORIGINS` si vous appelez l'API en direct depuis
ce navigateur.

> ✅ **Les écrans Vue consomment réellement l'API** via le proxy `/api` :
> carte client, panier (IDs/tailles/prix réels), saisie obligatoire du numéro de
> table puis du mode de paiement, création de commande, confirmation et suivi
> temps réel (polling ~2,5 s), et gestion barmaker du catalogue. Aucune donnée
> mockée de production ne subsiste.

---

## Démarrer PostgreSQL

> Pour le développement **backend hors Docker** uniquement. Le stack complet
> ci-dessus démarre déjà PostgreSQL.

PostgreSQL est fourni via `docker-compose.yml` (service `postgres` uniquement,
image `postgres:16-alpine`, volume nommé, healthcheck) :

```bash
# depuis la racine du dépôt
docker compose up -d postgres

# vérifier l'état (doit passer "healthy")
docker compose ps
```

> Le port PostgreSQL exposé sur l'hôte est `DB_HOST_PORT` (défaut `5433`). Pour
> en changer :
> ```bash
> DB_HOST_PORT=5544 docker compose up -d postgres
> ```
> et démarrez le backend hors Docker avec `DB_PORT=5544`.

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

> **Note environnement** : le démon Docker local (Engine 29) impose une version
> minimale d'API Docker (≥ **1.44**), alors que le client `docker-java` embarqué
> par Testcontainers négocie une version plus ancienne par défaut. Le `pom.xml`
> fixe donc `-Dapi.version=1.44` pour les tests d'intégration (plugin Failsafe).
> Ajustez cette valeur selon la version minimale imposée par votre démon local.
> Les tests d'intégration utilisent un **conteneur PostgreSQL singleton** partagé
> (voir `backend/docs/testing.md`).

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
  valeur d'énumération est rejetée comme requête mal formée) ;
- `tableNumber` obligatoire, entier **1 à 999** ;
- `paymentMethod` obligatoire, valeurs acceptées `CASH_AT_COUNTER`,
  `CARD_AT_COUNTER`, `CARD_IN_APP`, `APPLE_PAY`, `GOOGLE_PAY` (ajouté par la
  migration **V4**, voir plus bas).

Requête :

```bash
curl -i -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "items": [
      {"cocktailId": 1, "size": "M"},
      {"cocktailId": 1, "size": "M"},
      {"cocktailId": 3, "size": "S"}
    ],
    "tableNumber": 12,
    "paymentMethod": "CARD_IN_APP"
  }'
```

Réponse `201 Created` (corps identique à celui de `GET /api/orders/{orderId}`) :

```json
{
  "id": "c91618ae-523c-4818-9749-9ae8151de02b",
  "publicCode": "WENZTF",
  "status": "ORDERED",
  "totalAmount": 30.50,
  "tableNumber": 12,
  "paymentMethod": "CARD_IN_APP",
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

`tableNumber` et `paymentMethod` sont renvoyés tels quels pour les écrans de
confirmation et de suivi. Le résumé barmaker (`GET /api/bar/orders`) expose
également `tableNumber`, et le détail barmaker (`GET /api/bar/orders/{id}`)
expose `tableNumber` et `paymentMethod`.

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

#### Unicité du `publicCode` (anti-collision)

Le `publicCode` (6 caractères alphanumériques majuscules, alphabet sans
caractères ambigus) est garanti unique par la **contrainte d'unicité base de
données** `customer_order_public_code_key`, qui fait autorité. La création de
commande est protégée à deux niveaux : un pré-contrôle applicatif peu coûteux et,
surtout, une **reprise bornée** (jusqu'à 5 tentatives) — chaque tentative dans sa
**propre transaction** — qui régénère un nouveau code uniquement si l'`INSERT`
perd la course d'unicité du `public_code`. Toute autre erreur de persistance
n'est **jamais** rejouée, et après épuisement des tentatives une erreur contrôlée
`500 PUBLIC_CODE_GENERATION_FAILED` est renvoyée (jamais un 500 incontrôlé). Le
format, la longueur, l'alphabet et la contrainte d'unicité sont inchangés ; aucun
UUID n'est exposé comme code public.

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
| Catégorie introuvable                 | `404`  | `CATEGORY_NOT_FOUND`            |
| Ingrédient introuvable                | `404`  | `INGREDIENT_NOT_FOUND`          |
| Commande introuvable                  | `404`  | `ORDER_NOT_FOUND`               |
| Nom de catégorie déjà utilisé (insensible à la casse) | `409` | `CATEGORY_ALREADY_EXISTS` |
| Nom de cocktail déjà utilisé dans la catégorie | `409` | `COCKTAIL_ALREADY_EXISTS`     |
| Nom d'ingrédient déjà utilisé (insensible à la casse) | `409` | `INGREDIENT_ALREADY_EXISTS` |
| Catégorie cible inactive (rattachement cocktail) | `409` | `CATEGORY_INACTIVE`          |
| Données de catalogue invalides (ingrédients/prix) | `400` | `INVALID_CATALOG_REQUEST`   |
| Authentification requise / jeton invalide | `401` | `AUTHENTICATION_REQUIRED` / `INVALID_TOKEN` |
| Rôle insuffisant                      | `403`  | `ACCESS_DENIED`                 |
| Cocktail inactif                      | `409`  | `COCKTAIL_UNAVAILABLE`          |
| Taille indisponible                   | `409`  | `SIZE_UNAVAILABLE`              |
| Prix (taille) inactif                 | `409`  | `PRICE_UNAVAILABLE`             |
| Échec de génération du code public    | `500`  | `PUBLIC_CODE_GENERATION_FAILED` |
| Erreur inattendue                     | `500`  | `INTERNAL_ERROR` (sans stacktrace, identifiants, SQL ni nom de classe interne) |

> Une commande invalide (au moins un item invalide) **annule toute la
> transaction** : aucune commande partielle n'est persistée.

---

## Authentification (barmaker)

L'API barmaker est protégée par **JWT** (Spring Security, resource server
stateless). Les routes `/api/bar/**` exigent le rôle `ROLE_BARMAKER` ; `GET
/api/menu`, `POST /api/orders` et `GET /api/orders/{id}` restent publics.

**Identifiants de démonstration** (développement / démo uniquement, créés par les
migrations V3/V5 — jamais en production) :

- `username` : `barmaker`
- `password` : `barmaker-test`

```bash
# 1. Obtenir un jeton
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"barmaker","password":"barmaker-test"}'
# -> { "accessToken": "...", "tokenType": "Bearer", "expiresIn": 3600, "user": {...} }

# 2. Appeler une route protégée
curl -s http://localhost:8080/api/bar/categories \
  -H "Authorization: Bearer <accessToken>"
```

Un appel non authentifié à `/api/bar/**` renvoie `401 AUTHENTICATION_REQUIRED` ;
un jeton valide sans le rôle requis renvoie `403 ACCESS_DENIED`.

---

## API de gestion du catalogue (barmaker)

Routes protégées (`ROLE_BARMAKER` obligatoire) pour gérer catégories et
cocktails. Les entités JPA ne sont jamais exposées ; seules des DTO transitent.

> **« Suppression » = désactivation logique.** `DELETE` ne supprime jamais
> physiquement une catégorie, un cocktail, un ingrédient ou un prix : il passe
> `active = false`. Les données (et les snapshots de commandes) sont conservées,
> la ligne disparaît de `GET /api/menu` mais reste visible dans la gestion, et
> une réactivation est possible via `PUT` (`active: true`).

### Catégories

| Méthode | Route | Succès | Erreurs |
|---------|-------|--------|---------|
| `GET`    | `/api/bar/categories`               | `200` (actives **et** inactives) | `401`/`403` |
| `POST`   | `/api/bar/categories`               | `201` + `Location`               | `400`, `409 CATEGORY_ALREADY_EXISTS` |
| `PUT`    | `/api/bar/categories/{categoryId}`  | `200`                            | `400`, `404`, `409` |
| `DELETE` | `/api/bar/categories/{categoryId}`  | `204` (désactivation)            | `404` |

Requête (création/mise à jour) :

```json
{
  "name": "Classiques",
  "description": "Les grands classiques du bar.",
  "displayOrder": 1,
  "active": true
}
```

Règles : `name` obligatoire et **trimmé**, ≤ 100 caractères, unique
(insensible à la casse) ; `description` optionnelle (≤ 255, V4), une chaîne vide
est stockée `null` ; `displayOrder` ≥ 0 ; `active` par défaut `true` à la
création. Tri de la liste : `displayOrder`, puis `name`, puis `id`.

Réponse :

```json
{ "id": 1, "name": "Classiques", "description": "Les grands classiques du bar.", "displayOrder": 1, "active": true }
```

### Cocktails (tailles & prix)

| Méthode | Route | Succès | Erreurs |
|---------|-------|--------|---------|
| `GET`    | `/api/bar/cocktails`               | `200` (actifs **et** inactifs) | `401`/`403` |
| `GET`    | `/api/bar/cocktails/{cocktailId}`  | `200`                          | `404` |
| `POST`   | `/api/bar/cocktails`               | `201` + `Location`             | `400`, `404 CATEGORY_NOT_FOUND`, `409` |
| `PUT`    | `/api/bar/cocktails/{cocktailId}`  | `200`                          | `400`, `404`, `409` |
| `DELETE` | `/api/bar/cocktails/{cocktailId}`  | `204` (désactivation)          | `404` |

Requête (création/mise à jour) :

```json
{
  "categoryId": 1,
  "name": "Mojito",
  "description": "Cocktail frais à base de rhum.",
  "shortDescription": "Rhum, menthe et citron vert.",
  "imageUrl": "https://example.test/mojito.jpg",
  "active": true,
  "ingredients": [
    { "name": "Rhum blanc", "quantityLabel": "5 cl", "displayOrder": 1 },
    { "name": "Menthe",     "quantityLabel": "8 feuilles", "displayOrder": 2 }
  ],
  "prices": [
    { "size": "S", "price": 7.50 },
    { "size": "M", "price": 9.00 },
    { "size": "L", "price": 11.00 }
  ]
}
```

Règles principales :

- `categoryId` obligatoire, la catégorie doit **exister et être active** (sinon
  `404 CATEGORY_NOT_FOUND` ou `409 CATEGORY_INACTIVE`) ;
- `name` obligatoire, trimmé, ≤ 150, **unique (insensible à la casse) dans la
  catégorie** ; le même nom est autorisé dans une autre catégorie ;
- `description` obligatoire ; `shortDescription` optionnelle (≤ 255, colonne V4
  `short_description`) ; `imageUrl` optionnelle ;
- **au moins un ingrédient** ; noms trimmés, **uniques (insensible à la casse)
  dans la requête** ; `quantityLabel` optionnel ; `displayOrder` ≥ 0 ; les
  ingrédients sont renvoyés triés par `displayOrder` ;
- **exactement trois prix : un S, un M, un L**, chacun `> 0` (`BigDecimal`) ;
- `active` par défaut `true` à la création.

Les **ingrédients sont réutilisés** de façon transactionnelle et insensible à la
casse (un même ingrédient est partagé entre cocktails, jamais dupliqué ; un
ingrédient désactivé est réactivé s'il est réutilisé). À la mise à jour, les
ingrédients sont **remplacés** (suppression/réinsertion déterministe) et les prix
sont **réécrits en place** (un seul prix actif par taille). Toutes ces opérations
ont lieu dans **une seule transaction**.

La réponse de gestion contient l'`id`, `categoryId`, `categoryName`, `name`,
`description`, `shortDescription`, `imageUrl`, `active`, les ingrédients ordonnés
et les prix S/M/L — soit toutes les données nécessaires au futur formulaire Vue.

> `GET /api/menu` (public) continue de n'exposer **que** les catégories,
> cocktails, ingrédients et prix **actifs** : son comportement est inchangé.

### Ingrédients (gestion autonome)

API autonome pour administrer le référentiel d'ingrédients partagé entre
cocktails. Le modèle d'ingrédient ne porte que `id`, `name` et `active` :
aucune description, unité, quantité, allergène, stock ni prix n'existe, donc rien
de tel n'est exposé.

| Méthode | Route | Succès | Erreurs |
|---------|-------|--------|---------|
| `GET`    | `/api/bar/ingredients`              | `200` (actifs **et** inactifs) | `401`/`403` |
| `GET`    | `/api/bar/ingredients/{id}`         | `200`                          | `404 INGREDIENT_NOT_FOUND` |
| `POST`   | `/api/bar/ingredients`              | `201` + `Location`             | `400`, `409 INGREDIENT_ALREADY_EXISTS` |
| `PUT`    | `/api/bar/ingredients/{id}`         | `200`                          | `400`, `404`, `409` |
| `DELETE` | `/api/bar/ingredients/{id}`         | `204` (désactivation logique)  | `404` |

Requête (création/mise à jour) :

```json
{ "name": "Menthe fraîche", "active": true }
```

Règles : `name` obligatoire et **trimmé**, ≤ 120 caractères, **unique
(insensible à la casse)** via vérification en base ; `active` par défaut `true` à
la création. La liste de gestion est triée **actifs d'abord**, puis par nom
(insensible à la casse), puis par `id`.

Réponse :

```json
{ "id": 3, "name": "Menthe fraîche", "active": true }
```

**Désactivation logique & réutilisation par les cocktails.** `DELETE` passe
`active=false` (idempotent) sans jamais supprimer la ligne ni les associations
`cocktail_ingredient` : l'historique est préservé. Un ingrédient inactif
**disparaît de la composition affichée** d'un cocktail dans `GET /api/menu`
(règle de menu déjà établie et testée), mais le cocktail reste exposé. La
création/édition d'un cocktail **réutilise** un ingrédient existant de façon
insensible à la casse (résolution centralisée `findByNameIgnoreCase`, partagée
avec ce CRUD), ne crée jamais de doublon, et **réactive** un ingrédient inactif
lorsqu'il est réutilisé.

---

## Migrations de base de données

- Les migrations Flyway vivent dans
  `backend/src/main/resources/db/migration/` :
  - `V1__create_schema.sql` — schéma (8 tables, contraintes, index, `pgcrypto`)
  - `V2__insert_demo_catalog.sql` — jeu de données de démonstration
  - `V3__insert_demo_barmaker.sql` — compte barmaker de démonstration (hash BCrypt)
  - `V4__add_order_table_payment_and_catalog_descriptions.sql` — ajout (additif,
    sans perte de données) de `customer_order.table_number` (1..999),
    `customer_order.payment_method` (set contraint), `category.description` et
    `cocktail.short_description`
  - `V5__update_demo_barmaker_password.sql` — rotation du mot de passe de démo
    vers `barmaker-test` (idempotent : `UPDATE` puis `INSERT … ON CONFLICT`)
- **Aucune migration appliquée (V1→V5) n'est modifiée.** Cette passe n'a pas
  nécessité de nouvelle migration : les colonnes V4 (`description`,
  `short_description`) couvrent déjà les besoins de gestion du catalogue, donc
  **pas de V6**.
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
démonstration). La table `app_user` contient **un compte barmaker de
démonstration** (`barmaker` / `barmaker-test`, mot de passe stocké en hash
BCrypt, jamais en clair).

---

## Limitations

- **Java 21 indisponible** sur la machine de développement : le projet **cible**
  le bytecode 21 mais **compile et s'exécute sur JDK 25 LTS**. Byte Buddy est
  activé en mode expérimental (`-Dnet.bytebuddy.experimental=true`) pour accepter
  la version de classe du JVM 25.
- **Frontend connecté** : le frontend Vue 3 consomme désormais le backend pour
  tous les écrans de production (carte client réelle, panier avec IDs/tailles/
  prix réels, numéro de table obligatoire avant le paiement, création de commande
  `POST /api/orders`, confirmation et suivi rechargés depuis `GET /api/orders/{id}`,
  gestion barmaker des catégories/cocktails/ingrédients). Les **mocks de
  production ont été supprimés** ; seules des fixtures de test demeurent sous
  `frontend/src/test/fixtures/`. En cas d'indisponibilité de l'API, les écrans
  affichent un état de chargement/erreur/réessai — **jamais** de fausse donnée.
- **Pas de compte client** : les clients restent anonymes, **sans
  authentification** ; seul l'espace barmaker est authentifié (JWT). Le suivi de
  commande se fait via l'UUID de la commande.
- **Paiement non encaissé** : `paymentMethod` est enregistré avec la commande
  mais aucun encaissement réel n'est effectué ; pas d'annulation de commande.
- La « suppression » de catégories/cocktails est une **désactivation logique** :
  aucune donnée n'est supprimée physiquement (historique des commandes préservé).
