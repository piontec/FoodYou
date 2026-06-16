# Slice 5 — Fuzzy auto-link to existing foods

> Triage: ready-for-agent · Type: AFK
> Parent PRD: `docs/prd/0001-tandoor-recipe-import.md`
> Respects: `docs/adr/0001-tandoor-ingredient-resolution.md`

## What to build

Speed up **Ingredient Resolution** by automatically linking a Tandoor ingredient to an existing FoodYou **Food** when its name is a confident match against the local food database. Confident matches are pre-linked (and clearly shown as auto-linked, so the user can override); ambiguous or low-confidence ones remain unresolved for manual handling (Slice 4).

This reduces manual effort on recipes whose ingredients the user already maintains as foods.

## Acceptance criteria

- [ ] During resolution, ingredient names are matched against existing foods and confident matches are auto-linked.
- [ ] Auto-linked ingredients are visibly marked and can be overridden by the user.
- [ ] Non-confident matches stay unresolved and fall through to manual resolution.
- [ ] Matching is conservative enough to avoid wrong links (no silent incorrect matches).
- [ ] Tests cover the matching behaviour (confident → linked, ambiguous → unresolved) with a fake food-search repository.

## Blocked by

- Slice 4 — Interactive ingredient resolution (`docs/issues/0004-interactive-ingredient-resolution.md`)
