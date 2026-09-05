# Food Calorie Scanner

The Discovery tab contains a **Food calorie scanner** entry. Users can take a camera photo or
choose an image from the gallery, then send it to Spoonacular for an estimated category,
calories, confidence range, protein, carbohydrates, and fat.

## API key setup

Add the following entry to the project-root `local.properties` file:

```properties
SPOONACULAR_API_KEY=your_api_key_here
```

Alternatively, expose `SPOONACULAR_API_KEY` as an environment variable before building. The
environment variable takes precedence over `local.properties`.

`local.properties` is ignored by Git. Never commit the real key. The current direct API call is
appropriate for a prototype, but `BuildConfig` values can still be extracted from an APK. Route
the Spoonacular request through a backend before distributing a production build.

## Spoonacular usage

The scanner calls `POST https://api.spoonacular.com/food/images/analyze` with a JPEG in the
multipart field named `file`. Spoonacular currently charges 8 quota points for each call.
Nutrition is an estimate based on similar recipes, so the UI shows the API's 95% calorie range
and an estimation disclaimer.

## History persistence

Every successful analysis is saved to the `food_logs` table (`FoodScanViewModel` calls
`SaveFoodLogUseCase` right after `AnalyzeFoodImageUseCase` succeeds) — category, calorie/macro
values, and the timestamp only; the source photo itself is never persisted, matching the
`form_check_results` convention. "View food log history" on `FoodScanScreen` opens
`FoodLogHistoryScreen`, a plain list of past scans. This history feeds the TDEE weight forecast —
see `docs/weight-forecast-feature.md`.
