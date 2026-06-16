# PRD: Import recipes from Tandoor

> Status: ready-for-agent. Source design: `CONTEXT.md`, `docs/adr/0001-tandoor-ingredient-resolution.md`.
> Note: GitHub Issues are disabled on this repository, so this PRD is published here in-repo.

## Problem Statement

I keep my recipes in my self-hosted **Tandoor** instance, but FoodYou has no way to bring them in. Today I have to re-enter every recipe by hand — name, servings, and each ingredient with its weight and nutrition — which is tedious and error-prone. I want to pull a recipe I already maintain in Tandoor into FoodYou so I can track its nutrition without retyping it.

## Solution

FoodYou lets me connect to my Tandoor server once (server URL + API token), then **browse and search** my Tandoor recipes and import a chosen one as a native FoodYou **Recipe**.

Because a FoodYou **Recipe** is nutrition-first (every **Recipe Ingredient** must point at a real **Food** with **Nutrition Facts**, measured in the closed **Measurement** set) while a **Tandoor Recipe** is loosely-typed text, import runs an **Ingredient Resolution** step:

1. When a Tandoor food carries nutritional `properties`, FoodYou auto-creates a **Product** (source `Tandoor`) with those macros.
2. When the Tandoor ingredient has a gram/millilitre conversion, FoodYou derives its **Measurement** automatically.
3. Anything Tandoor cannot weigh (e.g. *piece*, *clove*, *can*, *pinch*, *tbsp* with no gram conversion) is surfaced to me to resolve — I link an existing FoodYou Food, search/create one, give it a weight, or (one tap) create an empty-nutrition Product to fill in later.

The import only completes once every ingredient is resolved, so the resulting recipe's nutrition is trustworthy.

## User Stories

1. As a FoodYou user, I want to open a "Connect to Tandoor" screen from settings, so that I can set up the integration in one place.
2. As a FoodYou user, I want to enter my Tandoor server URL, so that FoodYou knows which instance to talk to.
3. As a FoodYou user, I want to paste my Tandoor API token, so that FoodYou can authenticate as me.
4. As a FoodYou user, I want a "Test connection" action, so that I get immediate confirmation my URL and token are correct before relying on them.
5. As a FoodYou user, I want my server URL and token stored encrypted on-device, so that my credentials are protected like my other source credentials.
6. As a FoodYou user, I want to clear/disconnect my Tandoor connection, so that I can revoke access or switch servers.
7. As a FoodYou user, I want a clear error when my token is rejected, so that I understand the connection failed for auth reasons (the API returns 403) rather than thinking the server is down.
8. As a FoodYou user, I want a clear error when the server is unreachable or the URL is malformed, so that I can fix the address.
9. As a FoodYou user, I want to browse the recipes on my Tandoor server, so that I can choose which to import.
10. As a FoodYou user, I want a search box while browsing, so that I can quickly find a recipe by name instead of scrolling.
11. As a FoodYou user, I want the recipe list to page as I scroll, so that large collections load smoothly.
12. As a FoodYou user, I want to see each recipe's name (and image where available) in the list, so that I can recognise it at a glance.
13. As a FoodYou user, I want to tap a recipe to start importing it, so that I can pull in just the one I want.
14. As a FoodYou user, I want the recipe's name carried over, so that I don't have to retype it.
15. As a FoodYou user, I want the recipe's servings carried over (at least 1), so that per-serving nutrition is correct.
16. As a FoodYou user, I want the recipe's step instructions carried into the recipe note along with the original source URL, so that I keep the method and provenance.
17. As a FoodYou user, I want ingredients whose Tandoor food has nutritional properties to be created automatically as Products with their macros, so that I don't re-enter nutrition.
18. As a FoodYou user, I want ingredients that Tandoor can weigh in grams or millilitres to get their Measurement automatically, so that quantities are right without effort.
19. As a FoodYou user, I want ingredients Tandoor cannot weigh to be shown to me in a resolution step, so that nothing is silently guessed.
20. As a FoodYou user, I want to assign a gram or millilitre weight to an unresolvable ingredient, so that its contribution to nutrition is captured.
21. As a FoodYou user, I want to link an unresolved ingredient to an existing FoodYou Food, so that I reuse foods I already maintain.
22. As a FoodYou user, I want confident name matches against my existing foods to be auto-linked, so that resolution is faster.
23. As a FoodYou user, I want to search for and create a Food for an unresolved ingredient inline, so that I can resolve it without leaving the flow.
24. As a FoodYou user, I want a one-tap "create empty-nutrition product" escape hatch, so that I can finish an import now and fill in details later.
25. As a FoodYou user, I want the "Import" action disabled until every ingredient is resolved (or explicitly given the empty escape hatch), so that I don't accidentally save a recipe with wrong nutrition.
26. As a FoodYou user, I want imported Products tagged with a Tandoor source, so that I can tell where they came from.
27. As a FoodYou user, I want the imported recipe to behave exactly like a recipe I created by hand, so that I can edit, measure, and track it normally.
28. As a FoodYou user, I want a loading indicator while recipes and details fetch, so that I know the app is working.
29. As a FoodYou user, I want an empty state when my Tandoor server has no recipes (or no search matches), so that I'm not confused by a blank screen.
30. As a FoodYou user, I want the import to run in a single transaction, so that a failure midway doesn't leave half a recipe or orphan products.
31. As a FoodYou user, I want offline/timeout failures during browse or import to be reported clearly, so that I can retry.
32. As a FoodYou user, I want guidance on where to generate a Tandoor API token, so that I can complete setup without hunting through Tandoor's UI.

## Implementation Decisions

Design is recorded in `CONTEXT.md` (domain glossary) and `docs/adr/0001-tandoor-ingredient-resolution.md`. The API contract below was **verified against a live Tandoor instance on 2026-06-15**.

### Scope (v1)
- **Browse & pick with search.** Connect to a server, list/search recipes, import one at a time. Single-recipe-by-id falls out for free.
- **Out:** bulk/sync-all import; importing Tandoor keywords; writing back to Tandoor.

### Connection & auth
- **Token only.** `Authorization: Bearer <token>`. Verified: valid token → 200; wrong/missing token → **403** (treat 403 as an auth failure, not 401).
- Store **server URL + API token** encrypted via the existing `MasterCrypto` + DataStore pattern, in a new `TandoorCredentialsRepository` mirroring `OpenFoodFactsCredentialsRepositoryImpl` (`store` / `clear` / `hasCredentials` / `load`). At most one Tandoor connection.
- A dedicated connection settings screen (enter URL + token, Test connection, Disconnect), reached from the existing settings/database area.

### Remote source (mirrors the OpenFoodFacts pattern)
- New infra package `food/infrastructure/tandoor`: a Koin `TandoorModule`, an own Ktor `HttpClient` (timeouts + JSON `ignoreUnknownKeys = true` + bearer auth from credentials), a `TandoorRemoteDataSource`, `@Serializable` DTOs under `model/`, and a `TandoorRecipeMapper`.
- **Endpoints (verified):**
  - List/search: `GET /api/recipe/?query=<text>&page=<n>&page_size=<n>` → DRF page `{count, next, previous, results[]}`. `query` powers the search box.
  - Detail: `GET /api/recipe/{id}/` → `name, description, servings, servings_text, source_url, keywords[], steps[]`, plus `nutrition` and `food_properties`.
  - Property catalogue (optional): `GET /api/property-type/`.
- **Ingredient shape (verified):** each `steps[].ingredients[]` has `amount`, `unit{name, base_unit, open_data_slug}`, `food{name, properties[], properties_food_amount, properties_food_unit}`, `conversions[]`, `note`, `no_amount`.

### New food source
- Add `FoodSource.Type.Tandoor` and Room `FoodSourceType.Tandoor` (next integer constant, `= 4`). **No DB migration** — `FoodSourceType` is persisted as an int, so this is additive. Update the exhaustive `toDomain`/`toEntity` converters and the `FoodSource.Type` icon/label `when` blocks (compiler will flag every site).

### Mapping & resolution (ADR-0001)
- `TandoorRecipeMapper` converts the detail DTO into an intermediate `TandoorRecipeDraft` (raw ingredients, not yet Foods).
- `ResolveTandoorIngredientsUseCase` produces a per-ingredient resolution state and applies the pipeline: **Tandoor food `properties` → Product with macros** → **derive Measurement from a g/ml entry in `conversions[]`** → **interactive resolve** for the rest.
- **Nutrition is reliably present** per food via `properties[]` keyed by canonical `open_data_slug` (`property-calories | property-proteins | property-fats | property-carbohydrates`), defined per `properties_food_amount` + `properties_food_unit` (100 g). A given instance may define **only macros** — other FoodYou nutrient fields are left null.
- **The hard part is unit→weight, not nutrition.** Ingredients Tandoor cannot weigh (no g/ml conversion) are reported exactly in `recipe.food_properties[*].food_values[*].missing_conversion` (`base_unit` → `g`); that set is precisely what the resolution UI must ask the user to weigh. `conversions[].amount` is already scaled by the ingredient `amount` (total quantity, not per-unit).
- `ImportTandoorRecipeUseCase` creates any needed Tandoor-source Products, then delegates to the existing `CreateRecipeUseCase`, **within one transaction**. Metadata mapping: `name`→name; `servings`→servings (clamp ≥ 1); concatenated step instructions + `source_url`→note; `isLiquid`=false; keywords ignored.

### UI
- Settings connection screen; browse screen (search box + paged list); resolve screen (per-ingredient status with link / search-create / weigh / empty-product actions; "Import" gated on full resolution). Wire routes into `FoodYouAppNavHost`. All new strings via `Res.string.*` (Crowdin-managed).

## Testing Decisions

Good tests assert **external behaviour** (inputs → outputs / state), never implementation details. Prior art: pure-logic tests in `app/src/commonTest` such as `CsvParserImplTest` and `RfcCsvParserTest`.

- **`TandoorRecipeMapper` (highest-value seam):** feed captured Tandoor recipe-detail JSON (including the verified `"Marry me" Orzo` sample with mixed units) and assert the resulting `TandoorRecipeDraft` — name, servings, note/source-url composition, and each raw ingredient's amount/unit/properties. Pure function, no network.
- **Unit→Measurement derivation:** table-driven tests over `conversions[]` / `base_unit` — g/ml present → correct `Gram`/`Milliliter`; absent → flagged unresolved. Cover the `missing_conversion` cases (`szt`, `ząbek`, `puszka`, `szczypta`, `łyżka`).
- **`ResolveTandoorIngredientsUseCase`:** with a fake food-search repository, assert the pipeline — property-bearing food → auto Product with macros; confident name match → auto-link; everything else → unresolved with reason; empty-nutrition escape hatch yields a resolvable ingredient.
- **`ImportTandoorRecipeUseCase`:** with fakes for product/recipe repositories and a test `TransactionProvider`, assert a fully-resolved draft creates the expected Products + Recipe and that an unresolved ingredient blocks import; assert transactional rollback on mid-import failure.
- **`TandoorRemoteDataSource`:** use Ktor `MockEngine` to assert request shape (bearer header, `query`/`page`/`page_size` params) and parsing of the DRF page and detail payloads, including 403 → auth error mapping.

UI screens are not the test seam; logic is pushed down into the mapper and use cases so it can be tested headlessly.

## Out of Scope

- Bulk / "import all" / continuous sync of the whole Tandoor collection.
- Writing or editing recipes back in Tandoor.
- Importing Tandoor keywords, ratings, images-as-media, or step ordering beyond note text.
- Username/password (Allauth/OIDC) login flows — token only.
- Importing nutrients beyond what a Tandoor instance exposes (e.g. vitamins/minerals/fiber when absent).
- Auto-creating gram conversions in Tandoor on the user's behalf.

## Further Notes

- Verified live (2026-06-15) against a real instance: Bearer auth, DRF pagination, `query` search, per-food `properties` macros, and the `missing_conversion` mechanism all behave as described above.
- Because nutrition is usually present but weight often isn't, **Ingredient Resolution is primarily a *weighing* step and only secondarily a nutrition-matching step** — the inverse of the initial assumption; the resolution UI should optimise for quickly assigning weights.
- Java/Gradle note for implementers: the repo targets the Android/KMP toolchain; building requires a compatible JDK (the wrapper uses Gradle 8.13) and the Android SDK at `sdk.dir`.
