# Context & Glossary

This document defines the canonical domain language for FoodYou. It is a glossary,
not a specification — it contains no implementation details.

## Core food domain

- **Food** — anything with nutrition facts that can be measured and tracked. A Food is
  either a **Product** or a **Recipe**. Every Food exposes `NutritionFacts` (per 100 g/ml).
- **Product** — a single food item with its own nutrition facts. Has a **Food Source**.
- **Recipe** — a Food composed of **Recipe Ingredients**, with a number of **servings**,
  an optional note, and a liquid/solid flag. Its nutrition facts are derived from its
  ingredients. A Recipe may not be saved unless every ingredient resolves to an existing
  Food.
- **Recipe Ingredient** — a pairing of a **Food** with a **Measurement**. It is not free
  text; it always points at a concrete, nutrition-bearing Food.
- **Measurement** — a typed quantity of a Food. The set is closed: Gram, Milliliter,
  Serving, Package, Ounce, FluidOunce. There is no notion of "cups", "tablespoons", or
  "pieces" in FoodYou.
- **Food Source** — the provenance of a Product: `User`, `OpenFoodFacts`, `USDA`,
  `SwissFoodCompositionDatabase`, and (new) **`Tandoor`**.

## Tandoor integration

- **Tandoor** — a self-hosted, third-party recipe manager (https://tandoor.dev). Each
  user runs their own instance, identified by a **Tandoor Server URL**.
- **Tandoor Connection** — the pairing of a Tandoor Server URL with a personal **API
  Token**, stored encrypted on-device. A user has at most one Tandoor Connection.
- **Tandoor Recipe** — a recipe as modelled by Tandoor: loosely-typed text. It has steps,
  and each step has ingredients expressed as `{ amount, unit, food-name, note }`. Tandoor
  units are free-form and Tandoor food items usually carry no nutrition data. A Tandoor
  Recipe is *not* a FoodYou Recipe; it must be imported and bridged.
- **Tandoor Import** — the act of turning a chosen Tandoor Recipe into a FoodYou Recipe.
- **Ingredient Resolution** — the bridging step of Tandoor Import: deciding, for each
  Tandoor ingredient, which FoodYou Food it maps to and with which Measurement. An
  ingredient is **resolved** when it points at a concrete Food; otherwise it is
  **unresolved** and blocks the import until the user acts on it.
- **Tandoor Food Property** — optional nutritional data Tandoor may hold for one of its
  food items. When present, it is a source for Ingredient Resolution.
