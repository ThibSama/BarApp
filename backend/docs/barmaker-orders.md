# API de traitement des commandes (barmaker)

> **Documentation des endpoints protégés de préparation des commandes.**
> Couvre la file d'attente des commandes, le détail d'une commande, la
> progression des étapes de préparation et la synchronisation automatique du
> statut global de la commande.

Voir aussi [`security.md`](./security.md) pour l'authentification, le cycle de
vie des jetons JWT et la politique CORS.

---

## 1. Authentification

Toutes les routes ci-dessous vivent sous `/api/bar/**` et exigent un barmaker
authentifié possédant le rôle **`ROLE_BARMAKER`**.

```http
Authorization: Bearer <jwt>
```

Le jeton n'est accepté **que** via l'en-tête `Authorization` (jamais en
paramètre d'URL, cookie, corps ou fragment).

| Cas                                   | Réponse |
|---------------------------------------|---------|
| Aucun jeton                           | `401 AUTHENTICATION_REQUIRED` |
| Jeton invalide / expiré               | `401 INVALID_TOKEN` / `401 TOKEN_EXPIRED` |
| Authentifié sans le rôle `BARMAKER`   | `403 ACCESS_DENIED` |
| `ROLE_BARMAKER`                       | autorisé |

---

## 2. Modèle de préparation

Chaque cocktail commandé (`order_item`) progresse séquentiellement à travers
quatre états (`PreparationStatus`) :

```text
PREPARATION_INGREDIENTS -> ASSEMBLY -> DRESSING -> COMPLETED
```

- Un nouvel élément démarre à `PREPARATION_INGREDIENTS`.
- Une transition avance **d'exactement une étape** ; aucune étape ne peut être
  sautée ni inversée.
- `completedAt` n'est renseigné que lorsque l'élément atteint `COMPLETED` ; il
  reste `null` dans tous les autres états.

Le statut global de la commande (`OrderStatus`) est recalculé automatiquement à
chaque progression :

| Condition                                              | Statut commande |
|--------------------------------------------------------|-----------------|
| Aucun élément n'a encore progressé                     | `ORDERED`       |
| Au moins un élément a progressé, tous non terminés     | `IN_PROGRESS`   |
| Tous les éléments sont `COMPLETED`                     | `COMPLETED`     |

Lorsqu'une commande passe `COMPLETED`, son `completedAt` est renseigné ; une
commande non terminée a toujours `completedAt = null`. Une commande terminée ne
régresse jamais.

---

## 3. `GET /api/bar/orders`

File d'attente des commandes (résumés compacts).

**Paramètre de requête :**

| Nom         | Type    | Défaut  | Description |
|-------------|---------|---------|-------------|
| `completed` | boolean | `false` | `false` → commandes actives (`ORDERED` + `IN_PROGRESS`), triées de la plus ancienne à la plus récente (`createdAt`). `true` → commandes `COMPLETED`, de la plus récemment terminée à la plus ancienne (`completedAt`). |

**Requête :**

```http
GET /api/bar/orders?completed=false
Authorization: Bearer <jwt>
```

**Réponse 200 OK :**

```json
[
  {
    "id": "7fdbd20d-9e3a-4b7a-b807-82765d60432f",
    "publicCode": "ABC234",
    "status": "IN_PROGRESS",
    "totalAmount": 20.00,
    "createdAt": "2026-06-30T10:00:00Z",
    "completedAt": null,
    "itemCount": 2,
    "completedItemCount": 1
  }
]
```

Une file vide renvoie `200 OK` avec `[]`. Les compteurs `itemCount` /
`completedItemCount` sont agrégés côté PostgreSQL (aucune entité d'élément n'est
chargée, pas de requête N+1).

---

## 4. `GET /api/bar/orders/{orderId}`

Détail complet d'une commande, éléments triés par `sequenceNumber`.

**Requête :**

```http
GET /api/bar/orders/7fdbd20d-9e3a-4b7a-b807-82765d60432f
Authorization: Bearer <jwt>
```

**Réponse 200 OK :**

```json
{
  "id": "7fdbd20d-9e3a-4b7a-b807-82765d60432f",
  "publicCode": "ABC234",
  "status": "IN_PROGRESS",
  "totalAmount": 20.00,
  "createdAt": "2026-06-30T10:00:00Z",
  "completedAt": null,
  "items": [
    {
      "id": "0b1d2c3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e",
      "sequenceNumber": 1,
      "cocktailName": "Mojito",
      "size": "M",
      "unitPrice": 10.50,
      "preparationStatus": "ASSEMBLY",
      "completedAt": null
    }
  ]
}
```

`cocktailName` et `unitPrice` sont des **instantanés historiques** figés à la
commande (jamais relus depuis le catalogue courant).

**Erreurs :**

| HTTP | Code                 | Cas |
|------|----------------------|-----|
| 404  | `ORDER_NOT_FOUND`    | Aucune commande pour cet identifiant |
| 400  | `INVALID_IDENTIFIER` | UUID mal formé |

---

## 5. `PATCH /api/bar/order-items/{itemId}/next-step`

Fait progresser un cocktail commandé d'**exactement une étape** et recalcule le
statut de la commande parente. Aucun corps de requête n'est nécessaire.

La transition et le recalcul du statut parent ont lieu dans **une seule
transaction**, sous un verrou pessimiste sur la commande concernée (les
modifications concurrentes sur une même commande sont sérialisées).

**Requête :**

```http
PATCH /api/bar/order-items/0b1d2c3e-4f5a-6b7c-8d9e-0f1a2b3c4d5e/next-step
Authorization: Bearer <jwt>
```

**Réponse 200 OK :** la **commande parente complète et rafraîchie** (même forme
que `GET /api/bar/orders/{orderId}`), pour que l'interface puisse rafraîchir le
détail immédiatement.

**Erreurs :**

| HTTP | Code                             | Cas |
|------|----------------------------------|-----|
| 404  | `ORDER_ITEM_NOT_FOUND`           | Aucun élément pour cet identifiant |
| 409  | `INVALID_PREPARATION_TRANSITION` | L'élément est déjà `COMPLETED` (aucune étape suivante) |
| 400  | `INVALID_IDENTIFIER`             | UUID mal formé |

Toutes les erreurs utilisent l'enveloppe homogène `ApiErrorResponse`
(`timestamp`, `status`, `code`, `message`, `path`, `fieldErrors`) avec un
message en français.

---

## 6. Exemple complet (curl)

```bash
BASE=http://localhost:8080

# 1. Authentification barmaker
TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"barmaker","password":"barapp-demo-2024"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

# 2. Création anonyme d'une commande (deux boissons)
ORDER=$(curl -s -X POST "$BASE/api/orders" \
  -H 'Content-Type: application/json' \
  -d '{"items":[{"cocktailId":1,"size":"M"},{"cocktailId":3,"size":"S"}]}')
ORDER_ID=$(echo "$ORDER"  | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

# 3. File d'attente active
curl -s "$BASE/api/bar/orders?completed=false" -H "Authorization: Bearer $TOKEN"

# 4. Détail de la commande
curl -s "$BASE/api/bar/orders/$ORDER_ID" -H "Authorization: Bearer $TOKEN"

# 5. Faire progresser un élément d'une étape (à répéter jusqu'à COMPLETED)
ITEM_ID=$(echo "$ORDER" | sed -n 's/.*"items":\[{"id":"\([^"]*\)".*/\1/p')
curl -s -X PATCH "$BASE/api/bar/order-items/$ITEM_ID/next-step" \
  -H "Authorization: Bearer $TOKEN"

# 6. Une fois tous les éléments terminés : suivi public et historique
curl -s "$BASE/api/orders/$ORDER_ID"
curl -s "$BASE/api/bar/orders?completed=true" -H "Authorization: Bearer $TOKEN"
```
