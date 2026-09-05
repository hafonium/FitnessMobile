# TDEE Weight Forecast — Implementation Notes

The Weight screen (Report tab → Weight) projects future body weight from a transparent TDEE
(Total Daily Energy Expenditure) formula fed entirely by data already collected elsewhere in the
app — weight logs, age/height/gender, completed workouts, runs, and the Food Calorie Scanner's
history. This is deliberately **not** a trained ML model: with a single local user and a handful
of manually-entered data points, a physics-based formula is more accurate and far more explainable
than anything a model could learn from this little data. Read this before touching
`GetWeightForecastUseCase`, `WeightViewModel`, `WeightLineChart`'s `forecastRecords` param, or the
`food_logs` / `users.ageYears` columns.

This started life as its own "Weight Curve" tab under Discovery, with a dedicated `DiscoveryViewModel`.
It was later folded entirely into the real Weight screen instead — the same forecast overlaid on
the chart the user already tracks their weight on, plus its own explanatory summary card — and the
Discovery tab/`DiscoveryViewModel` were deleted. `DiscoveryScreen` is back to being a pure static
dispatcher; nothing forecast-related lives there anymore.

## Why not a trained model

This came out of a broader discussion of what AI/ML features the app's data could realistically
support. The blockers for a personalized ML model were: (1) each user has only a device's worth of
manually-logged weight points — far too sparse a time series to train anything meaningful, and (2)
the app never recorded calorie *intake* at all before this feature, only calories burned. A formula
that plugs real numbers into Mifflin-St Jeor is both more accurate with this little data and fully
auditable — the summary card shows every number that went into it.

## Two prerequisites this feature added

- **`users.ageYears`** (`UserEntity`, nullable `Int`): BMR needs age. Editable from its own `AgeCard`
  panel on the Weight screen (same shape as `CurrentWeightCard` — big value + a centered
  "Add age"/"Edit age" button), not folded into `BmiCard`. Reuses the existing `DecimalInputDialog`
  rather than adding a new dialog type, rounding to `Int` on save. `UpdateAgeUseCase` validates
  5..120, mirroring `RecordWeightUseCase`'s height/weight constants.
- **`food_logs` table** (`FoodLogEntity`/`FoodLogDao`): every successful Food Calorie Scanner
  analysis is now saved automatically (`FoodScanViewModel` calls `SaveFoodLogUseCase` right after
  `AnalyzeFoodImageUseCase` succeeds) — category, calorie/macro values, timestamp. The source photo
  is never persisted (same convention as `form_check_results`). See docs/food-calorie-scanner.md.

Both shipped in `AppDatabase` version 10 via an explicit `MIGRATION_9_10` (not a destructive wipe)
so existing weight/workout/run history survives the upgrade.

## The formula (`GetWeightForecastUseCase`)

1. **BMR** (Mifflin-St Jeor): `10*weightKg + 6.25*heightCm - 5*age + s`, where `s` is `+5` for
   `MALE`, `-161` for `FEMALE`, and `-78` (the average of the two) for `OTHER`/`PREFER_NOT_TO_SAY`/
   unset — `WeightForecast.usedNeutralGenderConstant` flags this so the UI can disclose it.
2. **TDEE** = `BMR * 1.2` (a sedentary daily-living baseline, since BMR alone is resting energy
   expenditure only) `+ avgDailyExerciseKcal`. The exercise term is **measured, not guessed**: the
   sum of `workout_sessions.caloriesBurned` (via `WorkoutSessionRepository.observeCompletedSessions()`,
   filtered to `endedAt` in the trailing 14 days) plus `run_sessions.calories`
   (`RunningRepository.observeFinishedSessions()`, filtered to `startedAt` in the same window),
   divided by 14.
3. **Average daily intake** = the trailing 14 days' `food_logs.caloriesKcal`, averaged **per
   distinct calendar day that has at least one log** (not a flat ÷14). The scanner logs individual
   dishes, not a full diary, so intake is a real (likely under-) estimate only on days the user
   actually scanned something — diluting by every day in the window would understate it further.
4. **Net daily balance** = avg intake − TDEE.
5. **Projection**: every 7 days out to 90, `weight(d) = currentWeightKg + netBalance*d / 7700`
   (7700 kcal ≈ 1 kg of body fat — the standard constant most fitness apps use).

### Guardrails — no silent bad estimate

`WeightForecast.hasEnoughData` gates everything above; when false, `missingReason` explains what's
missing and `ForecastSummaryCard` (Weight screen) shows that message instead of numbers:
- No current weight log, or no height, or no age → *"Add your age, height and a weight entry
  first."* (No extra CTA needed for this one — the same screen already has the Record/Edit height/
  Edit age actions right above it.)
- Zero `food_logs` rows in the trailing 14-day window → *"Log a few meals with the Food Calorie
  Scanner to estimate your intake."* (Computing TDEE without any intake data would silently assume
  0 kcal eaten, producing an absurd starvation curve — this guard exists specifically to prevent
  that.) This one does get a "Scan food" button, since the Food Scanner isn't otherwise reachable
  from Weight — `WeightScreen` takes an `onOpenFoodScanner` callback for it.

## UI (all on the Weight screen — Report tab → Weight)

- `WeightAnalyticsCard`'s chart overlays the forecast on the user's real weight-trend chart via
  `WeightLineChart`'s `forecastRecords` param (default empty, so any other call site is
  unaffected): a dashed continuation series from the last actual point, with the min/max scale
  computed over both series so the projection never clips off the top/bottom. `forecastLineColor`
  defaults to `StreakRed` (the app's existing red accent) rather than a lighter shade of the actual
  line's blue (`BrandBlueLight`), so the two series read as clearly distinct colors — a small
  "Actual" (blue) / "Projected" (red) legend sits under the chart.
- `WeightLineChart` also gained `showAxisLabels` (default false, so the small Report preview chart
  is unchanged): when on, it draws the weight value (kg) next to each gridline and a handful of
  evenly-spaced dates along the bottom, reading directly off each plotted `WeightRecord.loggedAt` —
  no separate date param needed, since the forecast points are already `WeightRecord`s. This
  replaced `WeightAnalyticsCard`'s previous hand-rolled "current month" label + per-record
  day-of-month row, which didn't know about a forecast series and broke across a month boundary.
- A new `ForecastSummaryCard` (below the existing `AgeCard`) shows BMR/TDEE/avg intake/net balance,
  the neutral-gender disclosure when applicable, and a standing "rough estimate, not medical
  advice" line — or the missing-data guard message above when `hasEnoughData` is false.
- `WeightViewModel` depends on `GetWeightForecastUseCase` alongside its existing weight use cases.

## Bug fixed: chart points must be positioned by real elapsed time, not by list index

`WeightLineChart` originally spaced every point evenly by its index in `records + forecastRecords`
— so a user recording every 5 days and a forecast stepping every 7 days both rendered as equal-width
slots, and multiple same-day re-weighings (re-weighing, fixing a typo, just testing) each got their
own index, bunching up as extra, visually-squeezed points near the same spot instead of coinciding.
Fixed two ways:
- `GetWeightDashboardUseCase` now collapses `profile.records` to the latest entry per calendar day
  (`collapseToLatestPerDay()`) before computing anything — one point per day, period.
- `WeightLineChart` now positions every point (actual and forecast alike) by its real
  `WeightRecord.loggedAt` timestamp relative to the full time span (`xForTime`), not by index. Axis
  date labels sample evenly-spaced *times* across that span instead of evenly-spaced indices, for
  the same reason.

## Bug fixed: exercise and intake must average over the same set of days

`GetWeightForecastUseCase` originally averaged exercise calories over a flat 14-day denominator
(`(workoutCalories + runCalories) / 14`) while intake was averaged only over the days the scanner
was actually used (as intended — see the formula section above). Mixing those two denominators
meant TDEE and intake weren't answering the same question: TDEE reflected "a typical day over the
last two weeks" while intake reflected "a day you happened to scan food" — if those two sets of
days differ (e.g. the user only scanned meals on a couple of unusually active or unusually
sedentary days), the resulting net balance was quietly biased in either direction.

Fixed by computing both from the exact same `loggedDayStarts` (the calendar days that have at
least one `food_logs` row in the window): `caloriesBurnedOnDay(dayStart)` and
`caloriesEatenOnDay(dayStart)` both bucket their source data by real day boundaries
(`startOfDay`/`dayStart until dayStart + DAY_MILLIS`), and `avgDailyExerciseKcal`/
`avgDailyIntakeKcal` are each averaged over that identical list of days. The 14-day window itself
was also switched from a raw `now - 14*24h` slide to whole calendar days (`todayStart` back through
13 days ago) for the same reason — a floating window clips a sliver off "today" and tacks an
uneven sliver onto a 15th day depending on what time of day it happens to be.

## Limitations

- Single-dish photo scanning means avg intake is very likely an underestimate of true intake unless
  every meal is scanned — the UI and this doc both say so.
- The 1.2 sedentary multiplier and 7700 kcal/kg constant are the same simplifying assumptions every
  consumer fitness app makes; they are not individually calibrated.
- Not medical advice — this is an transparent estimate for a single local user, not a clinical tool.
