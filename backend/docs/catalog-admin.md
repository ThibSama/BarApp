# Gestion du catalogue barmaker (catégories & cocktails)

API protégée (`/api/bar/**`, `ROLE_BARMAKER` obligatoire, JWT) pour le CRUD des
catégories et des cocktails (tailles & prix). Voir le contrat complet, les
exemples de requêtes/réponses et le tableau des erreurs dans le
[README](../../README.md#api-de-gestion-du-catalogue-barmaker).

## Routes

| Méthode | Route | Comportement |
|---------|-------|--------------|
| `GET`    | `/api/bar/categories`              | liste (actives **et** inactives), triée `displayOrder, name, id` |
| `POST`   | `/api/bar/categories`              | création — `201` + `Location` |
| `PUT`    | `/api/bar/categories/{id}`         | mise à jour (incl. réactivation `active:true`) |
| `DELETE` | `/api/bar/categories/{id}`         | **désactivation logique** — `204` |
| `GET`    | `/api/bar/cocktails`               | liste (actifs **et** inactifs) |
| `GET`    | `/api/bar/cocktails/{id}`          | détail complet |
| `POST`   | `/api/bar/cocktails`               | création — `201` + `Location` |
| `PUT`    | `/api/bar/cocktails/{id}`          | mise à jour (détails, catégorie, ingrédients, prix) |
| `DELETE` | `/api/bar/cocktails/{id}`          | **désactivation logique** — `204` |
| `GET`    | `/api/bar/ingredients`             | liste (actifs **et** inactifs), triée `active desc, name, id` |
| `GET`    | `/api/bar/ingredients/{id}`        | détail |
| `POST`   | `/api/bar/ingredients`             | création — `201` + `Location` |
| `PUT`    | `/api/bar/ingredients/{id}`        | mise à jour (renommage + activation/désactivation) |
| `DELETE` | `/api/bar/ingredients/{id}`        | **désactivation logique** — `204` |

## Désactivation logique (« suppression »)

`DELETE` ne supprime **jamais** physiquement une catégorie, un cocktail, un
ingrédient ou un prix : la ligne passe `active = false`. Conséquences :

- l'historique des commandes (snapshots `cocktailName` / `unitPrice`) est
  intégralement préservé ;
- la ressource disparaît de `GET /api/menu` (public) mais reste visible dans la
  gestion ;
- la réactivation se fait via `PUT` avec `active: true`.

## Stratégie de persistance (agrégat cocktail)

Stratégie **B** retenue : *repositories enfants explicites + remplacement
delete/insert*, plutôt que des cascades JPA. Tout se fait dans **une seule
transaction** (`@Transactional`) :

- **Ingrédients** : les associations existantes sont chargées, supprimées
  (`deleteAll`) puis `flush`, et le nouvel ensemble déterministe est inséré via
  `EntityManager.persist` (la clé composite `@MapsId` est assignée, ce qui
  éviterait le chemin `merge` de `save`). Un ingrédient réutilisé peut donc
  reprendre la même clé `(cocktail_id, ingredient_id)`.
- **Prix** : *upsert* par taille — un unique prix actif par S/M/L est maintenu en
  place sous la contrainte `UNIQUE (cocktail_id, size)`.
- **Ingrédients partagés** : résolus **insensiblement à la casse**
  (`findByNameIgnoreCase`), réutilisés entre cocktails, jamais dupliqués, et
  réactivés s'ils étaient inactifs. Aucun ingrédient global n'est supprimé lors
  de l'édition d'un cocktail.

## Validation

- `400` (bean validation + règles métier) : nom vide, `displayOrder` < 0, moins
  d'un ingrédient, prix ≠ 3, prix ≤ 0, noms d'ingrédients dupliqués
  (insensible à la casse), tailles dupliquées / S-M-L incomplet
  (`INVALID_CATALOG_REQUEST`).
- `404` : `CATEGORY_NOT_FOUND`, `COCKTAIL_NOT_FOUND`.
- `409` : `CATEGORY_ALREADY_EXISTS`, `COCKTAIL_ALREADY_EXISTS` (unicité
  insensible à la casse), `CATEGORY_INACTIVE` (rattachement à une catégorie
  inactive).

## Ingrédients (CRUD autonome)

Modèle minimal (`id`, `name`, `active`) — aucune description, unité, stock ni
prix n'existe, donc aucun de ces champs n'est inventé. `DELETE` = désactivation
logique idempotente : la ligne et les associations `cocktail_ingredient` sont
conservées ; un ingrédient inactif est filtré de la composition affichée dans
`GET /api/menu` (règle de menu existante) mais le cocktail reste exposé. La
résolution insensible à la casse (`IngredientRepository.findByNameIgnoreCase`)
est **partagée** avec l'agrégat cocktail : pas de second mécanisme de résolution.
Erreurs : `404 INGREDIENT_NOT_FOUND`, `409 INGREDIENT_ALREADY_EXISTS`.

## Robustesse du `publicCode`

La création de commande régénère un `publicCode` (format inchangé) avec **reprise
bornée par transaction** lorsqu'un `INSERT` perd la course d'unicité de la
contrainte base `customer_order_public_code_key` ; seule cette collision est
rejouée, et après épuisement une erreur contrôlée `500
PUBLIC_CODE_GENERATION_FAILED` est renvoyée. Voir le
[README](../../README.md#unicité-du-publiccode-anti-collision).

## Migrations

Les colonnes `category.description` et `cocktail.short_description` proviennent de
**V4**. Le référentiel d'ingrédients (`ingredient.active`, index unique
`uk_ingredient_name_lower`) existe déjà depuis **V1**, donc le CRUD autonome des
ingrédients n'a **nécessité aucune migration** (**pas de V6**).

> Intégration frontend et suppression des mocks : **passe ultérieure**. Ces API
> ne sont pas encore consommées par le front.
