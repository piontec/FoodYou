# Slice 1 — Connect to Tandoor (settings + encrypted credentials + test connection)

> Triage: ready-for-agent · Type: AFK
> Parent PRD: `docs/prd/0001-tandoor-recipe-import.md`

## What to build

An end-to-end path that lets a user connect FoodYou to their self-hosted **Tandoor** server. From a new connection screen in the settings/database area, the user enters their **Tandoor Server URL** and **API Token**, runs a **Test connection** action, and saves or disconnects.

This slice establishes the foundations the later slices build on: a new `food/infrastructure/tandoor` package, a Koin module, its own Ktor `HttpClient` (timeouts, JSON `ignoreUnknownKeys = true`, bearer auth), a `TandoorRemoteDataSource` with a lightweight connectivity check, and a `TandoorCredentialsRepository` that stores the URL + token encrypted via the existing `MasterCrypto` + DataStore pattern (mirroring `OpenFoodFactsCredentialsRepositoryImpl`).

Test connection issues `GET /api/recipe/?page_size=1` with `Authorization: Bearer <token>`: 200 → success; **403 → invalid token**; network/timeout/bad-URL → reachability error. At most one Tandoor connection exists at a time.

## Acceptance criteria

- [ ] A connection screen is reachable from settings; it accepts a server URL and API token.
- [ ] "Test connection" reports success on valid credentials, an auth-specific error on 403, and a reachability error on bad URL/offline/timeout.
- [ ] Credentials are stored encrypted (via `MasterCrypto`) and can be loaded and cleared; `hasCredentials` reflects state.
- [ ] A "Disconnect"/clear action removes stored credentials.
- [ ] Short guidance tells the user where to generate a `tda_...` token in Tandoor.
- [ ] `TandoorRemoteDataSource` and its module are wired via Koin; the Ktor client uses bearer auth, timeouts, and lenient JSON.
- [ ] Tests cover the data source request shape (bearer header) and 200/403/error mapping using Ktor `MockEngine`, and the credentials repository round-trip (store → load → clear).

## Blocked by

None — can start immediately.
