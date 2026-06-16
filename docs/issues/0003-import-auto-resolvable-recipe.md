# Slice 3 — Import an auto-resolvable recipe (happy path)

> Triage: ready-for-agent · Type: AFK
> Parent PRD: `docs/prd/0001-tandoor-recipe-import.md`
> Respects: `docs/adr/0001-tandoor-ingredient-resolution.md`

## What to build

The core tracer bullet: importing a chosen **Tandoor Recipe** that is fully auto-resolvable into a native FoodYou **Recipe**. Fetch detail via `GET /api/recipe/{id}/`, map it with `TandoorRecipeMapper` into an intermediate `TandoorRecipeDraft`, then run the non-interactive part of **Ingredient Resolution**:

- For each ingredient whose Tandoor food carries nutritional `properties` (keyed by `open_data_slug`: `property-calories | property-proteins | property-fats | property-carbohydrates`, defined per `properties_food_amount` + `properties_food_unit` = 100 g), auto-create a **Product** with source **Tandoor** and those macros (other nutrient fields left null).
- For each ingredient that has a gram/millilitre entry in `conversions[]` (or a g/ml `base_unit`), derive the **Measurement** (Gram/Milliliter). Note `conversions[].amount` is the total already scaled by the ingredient `amount`.

Then create the Recipe via the existing `CreateRecipeUseCase`, inside a single transaction. Metadata mapping: `name`→name; `servings`→servings (clamp ≥ 1); concatenated step instructions + `source_url`→note; `isLiquid`=false; keywords ignored. A minimal confirmation screen shows the mapped recipe before saving.

This slice also adds the new food source: `FoodSource.Type.Tandoor` and Room `FoodSourceType.Tandoor` (next integer constant, additive — **no DB migration**), updating the exhaustive converters and the source icon/label `when` blocks.

Ingredients that are NOT auto-resolvable (no properties and/or no g/ml conversion) are out of scope here and handled in Slice 4; for this slice, importing such a recipe may be blocked with a "needs resolution" indication.

## Acceptance criteria

- [ ] `FoodSource.Type.Tandoor` / `FoodSourceType.Tandoor` added; all exhaustive `when`/converter sites updated; no DB migration required.
- [ ] `TandoorRecipeMapper` converts recipe-detail JSON into a `TandoorRecipeDraft` (name, servings, note + source url, raw ingredients).
- [ ] Ingredients with `properties` auto-create Tandoor-source Products carrying the available macros.
- [ ] Ingredients with a g/ml conversion get the correct `Measurement`.
- [ ] A fully-auto-resolvable recipe imports into a native FoodYou Recipe via `CreateRecipeUseCase`, in one transaction (rollback on mid-import failure).
- [ ] The imported recipe is indistinguishable from a hand-created recipe (editable, measurable, trackable).
- [ ] Tests cover the mapper, unit→Measurement derivation, the auto-resolution pipeline, and the import use case (incl. transactional rollback), with fakes for repositories. Prior art: `CsvParserImplTest`.

## Blocked by

- Slice 2 — Browse & search recipes (`docs/issues/0002-browse-and-search-recipes.md`)
