# Le Bar'app frontend prototype

Le Bar'app is a high-fidelity Vue frontend prototype for a cocktail bar exam project. It demonstrates the client ordering flow and the barmaker management flow with functional mock data, local state, routing, tests, and a production Docker container.

## Technical stack

- Vue 3 with the Composition API and `<script setup lang="ts">`
- TypeScript
- Vite
- Vue Router
- Pinia
- Scoped/plain CSS
- Vitest with jsdom and Vue Test Utils
- Docker multi-stage build
- Nginx production server with history-mode route fallback

## Language conventions

- All UI content is in French.
- Source code, filenames, component names, variables, types, tests, and developer documentation are in English.
- The current version uses mock data only.

## Project structure

```text
src/
├── components/        Reusable common, client, and barmaker components
├── layouts/           Client and barmaker layout shells
├── mocks/             Typed French mock categories, cocktails, and orders
├── router/            Vue Router history-mode routes
├── services/          Local frontend abstractions, no network requests
├── stores/            Pinia stores for catalog, cart, orders, and barmaker state
├── types/             Shared TypeScript domain types
├── utils/             Validation and price helpers
└── views/             Shared, client, and barmaker screens
```

## Installation

Run frontend commands from this directory:

```bash
cd front-end
npm install
```

## Local development

```bash
cd front-end
npm run dev
```

Open the Vite URL displayed in the terminal.

The dev server also accepts the two role-specific loopback hostnames (allow-listed
in `vite.config.ts`, no wildcard):

- <http://client.localhost:5173> — client interface
- <http://barmaker.localhost:5173> — barmaker interface

## Docker startup

```bash
cd front-end
docker compose up --build
```

The frontend is served at:

```text
http://localhost:8080
```

The compose file contains only the frontend service. No backend or database is required.

## Production build

```bash
npm run build
```

The build runs `vue-tsc --noEmit` first, then generates `dist/` with Vite.

## Tests

```bash
npm run test
npm run type-check
```

Tests cover cart calculations and mutations, mock payment selection, order completion logic, routing, navigation, a Vue component, and validation utilities.

## Available routes

Routing is **host-aware**: route *names* are stable across modes while the paths
depend on the hostname (`src/router/routes.ts` builds the table from
`window.location.hostname`). See the root `README.md` for the full access model
(clean hostnames vs legacy vs LAN).

**Client hostname** (`client.localhost`):

- `/` — cocktail menu
- `/cocktails/:id` — cocktail details
- `/panier` — cart
- `/confirmation/:orderId` — order confirmation
- `/suivi`, `/suivi/:orderId` — order tracking

**Barmaker hostname** (`barmaker.localhost`):

- `/` — redirects to `/login` (no session) or `/orders` (authenticated)
- `/login` — barmaker login
- `/orders`, `/orders/:orderId` — order dashboard / details
- `/categories` — category management
- `/cocktails` — cocktail management (modal query for create/edit)

**Legacy hostname** (`localhost`, `127.0.0.1`, LAN IPs, unknown hosts) — the
historical paths are preserved unchanged:

- `/` — redirects to `/client/menu`
- `/client/**` — client flow (`menu`, `cocktails/:id`, `panier`,
  `confirmation/:orderId`, `suivi`, `suivi/:orderId`)
- `/bar/login` and `/bar/**` — barmaker login + authenticated workspace
- `/barmaker/**` — backward-compatibility redirects to the `/bar/**` paths
- Any unknown route displays a French 404 page

## Mock-data architecture

Typed mock objects live in `src/mocks/`. Pinia stores load data through local services in `src/services/`, which currently read and write local browser state only. Local storage is isolated in `src/services/localPersistence.ts` so it can be removed without rewriting views.

Orders include a mock payment method. The payment flow is simulated locally only: it never asks for card data and never contacts an external checkout provider.

## Backend integration approach

Backend integration is intentionally absent in this prototype. There are no HTTP requests, `fetch`, Axios clients, WebSockets, API URLs, backend environment variables, Vite proxy configuration, or Spring Boot integration.

When a Spring Boot backend is introduced, replace the local service implementations behind the existing store actions with API-backed implementations. Views and components should remain largely unchanged because they already depend on stores and typed domain objects rather than direct data access.
