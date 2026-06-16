# Slice 6 — Errors, empty & loading states + i18n polish

> Triage: ready-for-agent · Type: AFK
> Parent PRD: `docs/prd/0001-tandoor-recipe-import.md`

## What to build

Harden the Tandoor import flow's edge states and finalise localisation across the connection, browse, and resolve/import screens. Covers consistent error reporting (auth 403, offline/timeout, malformed URL), empty states (server with no recipes, no search matches), loading indicators during fetch and import, and i18n for all new user-facing strings (Crowdin-managed `Res.string.*`).

Where it fits, reuse the existing `RemoteFoodException`-style error model so Tandoor errors are presented consistently with other remote sources.

## Acceptance criteria

- [ ] Auth failures (403) are reported as an invalid-token/auth error distinct from reachability errors.
- [ ] Offline/timeout failures during browse or import are reported clearly with a retry path.
- [ ] Empty states are shown for a server with no recipes and for no-search-results.
- [ ] Loading indicators appear while recipes/details fetch and while an import runs.
- [ ] All new user-facing strings are externalised via `Res.string.*` (no hardcoded text).
- [ ] Error/empty/loading behaviour is verified across the connection, browse, and import/resolve screens.

## Blocked by

- Slice 2 — Browse & search recipes (`docs/issues/0002-browse-and-search-recipes.md`)
- Slice 3 — Import an auto-resolvable recipe (`docs/issues/0003-import-auto-resolvable-recipe.md`)
- Slice 4 — Interactive ingredient resolution (`docs/issues/0004-interactive-ingredient-resolution.md`)
