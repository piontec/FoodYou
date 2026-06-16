# Tandoor ingredients are bridged by resolution, not silently auto-created

## Status

accepted

## Context

FoodYou's `Recipe` is nutrition-first: `CreateRecipeUseCase` rejects any recipe whose
ingredients don't all resolve to an existing `Food` carrying `NutritionFacts`. Tandoor
recipes are the opposite — loosely-typed text where each ingredient is a food *name* plus
a free-form amount/unit, usually with **no nutrition data**. Importing therefore requires
bridging a strongly-typed model onto a loosely-typed one.

## Decision

We import a Tandoor recipe through an explicit **Ingredient Resolution** step rather than
silently fabricating data. For each Tandoor ingredient we resolve a FoodYou `Food` in this
priority order:

1. **Tandoor Food Property** — if the Tandoor food carries nutritional properties, create a
   `Product` (source `Tandoor`) from them.
2. **Match existing FoodYou Food** — fuzzy-match the ingredient name against the local food
   database and auto-link confident matches.
3. **Interactive resolution** — anything still unresolved is surfaced to the user, who
   links an existing Food, searches/creates one, or (one-tap escape hatch) auto-creates an
   **empty-nutrition** `Product` they can fill in later.

The import cannot complete while any ingredient is unresolved, except via the explicit
empty-nutrition escape hatch.

## Considered options

- **Auto-create empty placeholder Products for every ingredient.** Rejected as the default:
  it always "succeeds" but silently fills the user's food database with zero-nutrition
  products and produces a recipe whose nutrition facts are wrong without warning —
  poisoning the core value of the app. Kept only as an explicit, user-chosen fallback.
- **Import as a text/note-only artifact (not a real Recipe).** Rejected: delivers almost
  no nutrition value and creates a second-class kind of recipe that doesn't fit the domain.

## Consequences

- Import is interactive and may require user effort per unmatched ingredient; it is not a
  silent one-shot. This is the deliberate price of keeping recipe nutrition trustworthy.
- A new `FoodSource.Type.Tandoor` (and matching Room `FoodSourceType` value) is introduced.
  This is additive — `FoodSourceType` is persisted as an integer, so no schema migration is
  required, only a new constant.
- Tandoor units that don't map onto FoodYou's closed `Measurement` set (e.g. cups, pieces)
  must be handled during resolution, not silently dropped.

## Validation (verified against a live instance, 2026-06-15)

The live API confirmed and sharpened this design:

- **Nutrition is reliably available**, not the hard part: each Tandoor food carries
  `properties[]` (per `properties_food_amount`/`properties_food_unit`) with canonical
  `open_data_slug`s (calories/proteins/fats/carbohydrates). So step 1 (create a `Product`
  from Tandoor properties) works for most ingredients — though a given instance may define
  only macros, leaving other FoodYou nutrient fields null.
- **The genuinely hard part is unit→weight conversion.** Tandoor cannot weigh ingredients
  given in piece/clove/can/pinch/tbsp units when no gram conversion exists, and reports
  exactly these in `recipe.food_properties[*].food_values[*].missing_conversion`. This is
  the precise set Ingredient Resolution must ask the user to weigh; everything with a
  `g`/`ml` conversion auto-resolves. Resolution is therefore primarily a *weighing* step,
  and only secondarily a nutrition-matching step.
