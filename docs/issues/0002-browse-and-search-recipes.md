# Slice 2 — Browse & search Tandoor recipes

> Triage: ready-for-agent · Type: AFK
> Parent PRD: `docs/prd/0001-tandoor-recipe-import.md`

## What to build

A browse screen that lists the recipes on the user's connected Tandoor server and lets them search by name. The list pages as the user scrolls and shows each recipe's name (and image where available). Tapping a recipe begins import (handed off to Slice 3).

Backed by `TandoorRemoteDataSource.listRecipes(query, page, pageSize)` calling `GET /api/recipe/?query=<text>&page=<n>&page_size=<n>`, which returns a DRF page `{count, next, previous, results[]}`. The search box drives the `query` parameter.

## Acceptance criteria

- [ ] The screen lists recipes from the connected server with paging (loads more on scroll).
- [ ] A search box filters results by name via the `query` parameter.
- [ ] Each list item shows the recipe name (and image when present).
- [ ] Tapping a recipe navigates into the import flow with the selected recipe id.
- [ ] A loading state shows while fetching and an empty state shows when there are no recipes / no search matches.
- [ ] Browse failures (offline/timeout/403) are surfaced clearly with a retry path.
- [ ] Tests cover list/search request shape (query/page/page_size params) and DRF page parsing via Ktor `MockEngine`.

## Blocked by

- Slice 1 — Connect to Tandoor (`docs/issues/0001-connect-to-tandoor.md`)
