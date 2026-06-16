# Slice 4 — Interactive ingredient resolution

> Triage: ready-for-agent · Type: HITL (novel UX — design review of the resolve screen)
> Parent PRD: `docs/prd/0001-tandoor-recipe-import.md`
> Respects: `docs/adr/0001-tandoor-ingredient-resolution.md`

## What to build

Extend the import flow to handle ingredients that the auto-pipeline (Slice 3) cannot resolve — primarily ingredients Tandoor cannot weigh (units like *piece*, *clove*, *can*, *pinch*, *tbsp* with no gram conversion), which Tandoor reports in `recipe.food_properties[*].food_values[*].missing_conversion`, and ingredients whose food has no nutritional `properties`.

A **resolve screen** lists every ingredient with its resolution status. For each unresolved ingredient the user can:

- assign a gram/millilitre **weight**,
- **link** it to an existing FoodYou Food,
- **search/create** a Food inline, or
- one-tap **create an empty-nutrition Product** (the explicit escape hatch).

The **Import** action is disabled until every ingredient is resolved (auto or manual). Because the resolution step is primarily about *weighing* rather than nutrition-matching, the UI should optimise for quickly assigning weights.

> HITL: the resolve-screen interaction is the novel UX in this feature and warrants a design review before/at implementation.

## Acceptance criteria

- [ ] Ingredients flagged by `missing_conversion` (or lacking properties) appear as unresolved with a clear reason.
- [ ] The user can resolve an ingredient by assigning a g/ml weight, linking an existing Food, searching/creating a Food, or creating an empty-nutrition Product.
- [ ] Import is blocked while any ingredient is unresolved; enabling it requires full resolution (escape hatch counts as resolved).
- [ ] A resolved recipe imports correctly via the Slice 3 path (Products created as needed; single transaction).
- [ ] Tests cover the resolution use case for the unresolved/blocked cases and the empty-product escape hatch, with a fake food-search repository.

## Blocked by

- Slice 3 — Import an auto-resolvable recipe (`docs/issues/0003-import-auto-resolvable-recipe.md`)
