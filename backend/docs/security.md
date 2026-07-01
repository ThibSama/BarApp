# Sécurité — Le Bar'app

> **Documentation de sécurité du backend Spring Boot.**
> Couvre l'authentification barmaker (Spring Security + BCrypt + JWT), les
> endpoints, les variables d'environnement, le cycle de vie des jetons, les
> routes publiques/protégées, le comportement des comptes inactifs, CORS et les
> limitations actuelles.

---

## 1. Identifiants de démonstration (développement uniquement)

> ⚠️ **AVERTISSEMENT** — Ces identifiants sont **uniquement destinés au
> développement local et aux tests**. Ne jamais les utiliser en production.

| Champ       | Valeur              |
|-------------|---------------------|
| `username`  | `barmaker`          |
| `password`  | `barapp-demo-2024`  |
| `role`      | `BARMAKER`          |
| `active`    | `true`              |

Le mot de passe est stocké sous forme de **hash BCrypt** (force 10) dans la
migration Flyway `V3__insert_demo_barmaker.sql`. Le mot de passe en clair
n'apparaît **jamais** dans le code source, les migrations ou la base de données.

---

## 2. Endpoints d'authentification

### `POST /api/auth/login`

Authentifie un barmaker et émet un jeton d'accès JWT.

**Requête :**

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "barmaker",
  "password": "barapp-demo-2024"
}
```

**Réponse 200 OK :**

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "barmaker",
    "displayName": "Barman principal",
    "role": "BARMAKER"
  }
}
```

**Erreurs :**

| HTTP | Code                  | Cas                                    |
|------|-----------------------|----------------------------------------|
| 400  | `VALIDATION_ERROR`    | Nom d'utilisateur vide ou manquant      |
| 400  | `VALIDATION_ERROR`    | Mot de passe vide ou manquant          |
| 400  | `MALFORMED_REQUEST`   | JSON mal formé                         |
| 401  | `INVALID_CREDENTIALS` | Utilisateur inconnu                    |
| 401  | `INVALID_CREDENTIALS` | Mot de passe incorrect                 |
| 401  | `INVALID_CREDENTIALS` | Compte inactif                         |

### `GET /api/auth/me`

Retourne le profil du barmaker authentifié. Recharge l'utilisateur depuis
PostgreSQL et vérifie qu'il existe toujours, qu'il est actif et qu'il possède
le rôle attendu.

**Requête :**

```http
GET /api/auth/me
Authorization: Bearer <jwt>
```

**Réponse 200 OK :**

```json
{
  "id": 1,
  "username": "barmaker",
  "displayName": "Barman principal",
  "role": "BARMAKER"
}
```

---

## 3. Utilisation du jeton Bearer

Toutes les routes protégées (`/api/bar/**`, `/api/auth/me`) exigent un jeton
JWT valide dans l'en-tête `Authorization` :

```http
Authorization: Bearer <jeton-jwt>
```

Le jeton **n'est accepté que** depuis cet en-tête. Les jetons passés via :

- paramètres d'URL (`?access_token=...`) → **ignorés** ;
- cookies → **non supportés** ;
- corps de requête → **non supportés** ;
- fragments d'URL → **non supportés**.

---

## 4. Variables d'environnement

| Variable                       | Description                              | Défaut (local)                              |
|--------------------------------|------------------------------------------|---------------------------------------------|
| `APP_JWT_SECRET`               | Secret HMAC (≥ 256 bits / 32 octets)     | Secret de développement (ne pas utiliser en prod) |
| `APP_JWT_ISSUER`               | Émetteur (`iss`) et validateur de jeton  | `le-barapp`                                 |
| `APP_JWT_EXPIRATION_SECONDS`   | Durée de vie du jeton d'accès (secondes)  | `3600`                                      |
| `APP_CORS_ALLOWED_ORIGINS`      | Origines CORS autorisées (séparées par `,`) | `http://localhost:5173`                  |

> ⚠️ En production, `APP_JWT_SECRET` **doit** être fourni via une variable
> d'environnement ou un gestionnaire de secrets. Aucun secret par défaut n'est
> fourni dans la configuration de base (`application.yml`). Le profil
> `application-local.yml` fournit un secret de développement uniquement pour
> le développement local. En l'absence de `APP_JWT_SECRET`, l'application
> refuse de démarrer avec une erreur de configuration claire.

---

## 5. Cycle de vie des jetons JWT

- **Algorithme de signature :** HS256 (HMAC-SHA256)
- **Claims obligatoires :** `sub`, `userId`, `role`, `iat`, `exp`, `iss`
- **Validation :** signature, expiration, émetteur, algorithme supporté
- **Durée de vie par défaut :** 3600 secondes (1 heure)
- **Refresh token :** non implémenté (voir limitations)

---

## 6. Routes publiques et protégées

### Routes publiques (sans authentification)

| Méthode | Route                      |
|---------|----------------------------|
| `POST`  | `/api/auth/login`          |
| `GET`   | `/api/menu`                |
| `POST`  | `/api/orders`              |
| `GET`   | `/api/orders/{orderId}`    |

### Routes authentifiées

| Méthode | Route            | Rôle requis  |
|---------|------------------|---------------|
| `GET`   | `/api/auth/me`   | Authentifié   |

### Routes protégées (personnel)

Deux rôles authentifiés existent : `BARMAKER` et `MANAGER`. Un `MANAGER` est un
barmaker élevé qui conserve tout l'accès barmaker ; ce n'est **pas** une
hiérarchie Spring — chaque matcher liste explicitement les rôles autorisés.

| Méthode | Route              | Rôle requis                        |
|---------|--------------------|------------------------------------|
| `*`     | `/api/bar/users/**`| `ROLE_MANAGER`                     |
| `*`     | `/api/bar/**`      | `ROLE_BARMAKER` **ou** `ROLE_MANAGER` |

> L'ordre est significatif : le matcher `/api/bar/users/**` (manager) est déclaré
> **avant** le matcher large `/api/bar/**`, sinon un barmaker accéderait à la
> gestion du personnel.

Un barmaker authentifié appelant `/api/bar/users/**` reçoit `403 ACCESS_DENIED` ;
un appel anonyme reçoit `401 AUTHENTICATION_REQUIRED`. Les autorités étant
rechargées depuis PostgreSQL à chaque requête, une revendication `role` falsifiée
dans le JWT ne peut pas élever les privilèges.

Toutes les autres routes sont **authentifiées par défaut** (secure default).

---

## 7. Comportement des comptes inactifs

- Un compte inactif (`active = false`) **ne peut pas se connecter**.
- Un jeton émis pour un compte ensuite désactivé est **immédiatement rejeté**
  sur toutes les routes authentifiées (`/api/auth/me`, `/api/bar/**`) — pas
  seulement à l'expiration du jeton. Chaque requête authentifiée recharge
  l'utilisateur depuis PostgreSQL et vérifie `active = true`.
- Tous ces cas retournent `401 INVALID_TOKEN` (le jeton n'est plus valide)
  ou `401 INVALID_CREDENTIALS` (pour le login) sans révéler quelle
  vérification a échoué.

---

## 8. CORS

- **Origines autorisées :** uniquement celles configurées via
  `APP_CORS_ALLOWED_ORIGINS` (pas de wildcard `*`).
- **Méthodes autorisées :** `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- **En-têtes autorisés :** `Authorization`, `Content-Type`.
- **En-tête exposé :** `Location`.
- **`allowCredentials` :** `false`.

---

## 9. Limitations actuelles

- **Pas de refresh token** — seule l'émission de jetons d'accès est implémentée.
- **Pas de révocation de jeton** — un jeton valide reste utilisable jusqu'à son
  expiration. Cependant, la désactivation de l'utilisateur dans PostgreSQL
  bloque immédiatement toutes les requêtes authentifiées car l'utilisateur est
  rechargé à chaque requête.
- **Pas de logout côté serveur** — l'authentification est stateless.
- **Identifiants de démonstration** — le compte `barmaker` est inséré via Flyway
  pour le développement et les tests ; il ne doit pas être présent en production.
- **Secret JWT** — aucun secret par défaut n'est fourni dans la configuration de
  base. Le profil local fournit un secret de développement. En production,
  `APP_JWT_SECRET` doit être fourni via une variable d'environnement.
