# Running Feature — Android Production Implementation Spec

## 1. Objective

Implement a production-ready **GPS Running Tracker** module for Android.

The feature starts from a new **Running card** on the existing `Discovery` screen.

User flow:

`Discovery Screen` → tap **Running** card → navigate to `walk_run_screen` → request location permission when GPS tracking is needed → start running session → continuously record GPS → render the route on Stadia Maps via MapLibre Native → display real-time running telemetry → pause/resume/finish the session.

Do not create a separate duplicate screen if `walk_run_screen` already exists. Extend the existing architecture and UI patterns used by the project.

---

# 2. Core technology

Use:

- **MapLibre Native Android SDK** as the map renderer.
- **Stadia Maps** as vector map/style provider.
- Stadia Maps **`outdoors`** style.
- Android **ForegroundService** for continuous running-session GPS tracking.
- A location provider appropriate for the existing project:
  - Prefer `FusedLocationProviderClient` if Google Play Services is already used.
  - Otherwise use Android location APIs without adding Google Play Services only for this feature.
- Kotlin Coroutines / `StateFlow` for exposing tracking state if consistent with the current project architecture.
- Existing project persistence layer; if no suitable persistence layer exists, use Room for run sessions and GPS points.

Do not replace the project's existing architecture, navigation library, dependency injection framework, database, or UI framework unless required.

Use a pinned stable MapLibre Native Android version. Do not use a dynamic dependency version such as `+`.

The standard MapLibre artifact is:

```gradle
implementation("org.maplibre.gl:android-sdk:<stable-version>")
```

If the project requires maximum compatibility with devices where Vulkan support is a concern, evaluate the OpenGL artifact instead of changing `minSdk` just for MapLibre.

MapLibre supports a `GeoJsonSource` connected to a `LineLayer`, which should be used for the live running route. citeturn646682search0turn438169search8

---

# 3. Discovery integration

Add a new card to the existing `Discovery` screen:

**Title:** Running

Recommended subtitle:

`Track your run with live GPS, distance and pace.`

Interaction:

```text
Running card tapped
        ↓
navigate to walk_run_screen
```

Do not request GPS permission when merely opening Discovery.

Permission should be requested only when the user enters the running experience and attempts to use functionality that requires GPS, preferably when pressing **Start Run**.

---

# 4. walk_run_screen states

The screen should support at least these states:

```text
READY
RUNNING
PAUSED
FINISHED
LOCATION_PERMISSION_REQUIRED
LOCATION_DISABLED
GPS_SEARCHING
ERROR
```

### READY

Display:

- Stadia Outdoors map.
- Current location when available.
- Start Run button.
- Initial telemetry:
  - Distance `0.00 km`
  - Duration `00:00`
  - Pace `--`
  - Calories `--` or `0` depending on whether user weight is available.

### RUNNING

Display:

- Current GPS location.
- Route line updating in real time.
- Distance.
- Duration.
- Average pace.
- Calories if weight is available.
- Pause.
- Finish.

### PAUSED

Stop adding distance and route points.

Elapsed active running time must not increase while paused.

Keep the running session recoverable and provide:

- Resume
- Finish

### FINISHED

Stop location updates and foreground service.

Persist the completed session and display final telemetry.

---

# 5. Location permission flow

Location permission is mandatory before GPS tracking starts.

Request:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

The application does **not** need to request `ACCESS_BACKGROUND_LOCATION` for the normal user-initiated running flow.

The running foreground service must be started while `walk_run_screen` / another Activity belonging to the app is visible. This is especially important on Android 14+, because location is a while-in-use permission and starting a location foreground service from the background is heavily restricted. citeturn438169search10turn438169search11

Permission flow:

```text
User presses Start Run
        ↓
Check ACCESS_FINE_LOCATION
        ↓
Not granted
        ↓
Show explanation/rationale
        ↓
Request COARSE + FINE
        ↓
Granted?
 ┌──────┴──────┐
 No            Yes
 ↓              ↓
show blocked    check system Location/GPS
state           ↓
                enabled?
              ┌─┴─┐
             No   Yes
             ↓     ↓
       prompt user start ForegroundService
       to enable GPS
```

For a GPS running tracker, precise location should be requested.

If the user grants only approximate location, do not silently treat it as equivalent to precise GPS. Explain that **Precise Location is required for accurate distance and route tracking**, and provide an action to grant precise location.

Do not repeatedly trigger the Android permission dialog after permanent denial. Show an explanation and a button that opens application settings instead.

---

# 6. Android Foreground Service

Create a dedicated service, for example:

```text
RunningTrackingService
```

Manifest:

```xml
<service
    android:name=".running.RunningTrackingService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="location" />
```

Android 14+ requires the location foreground service type and `FOREGROUND_SERVICE_LOCATION` permission for this use case. citeturn438169search11

Start the service using:

```kotlin
ContextCompat.startForegroundService(...)
```

Then call `startForeground(...)` immediately from the service.

Android requires a service started through `startForegroundService()` to promote itself to foreground within five seconds. citeturn381543search0

Create a Notification Channel, for example:

```text
channelId = "running_tracking"
name = "Running tracking"
importance = LOW
```

Foreground notification should be ongoing while the run is active.

Recommended notification content:

```text
Running in progress
2.43 km • 00:14:21
```

Recommended actions:

```text
Pause / Resume
Finish
```

Notification actions must operate on the existing running service and must not create duplicate sessions.

Android 13+ has the `POST_NOTIFICATIONS` runtime permission. Request it at an appropriate point, but tracking logic must still be implemented correctly if the user denies it; Android does not require this permission merely to start a foreground service, although the foreground service itself still has to create its required notification. citeturn869829search12

---

# 7. Location update configuration

GPS updates must prioritize tracking quality while keeping battery usage reasonable.

Use configurable constants rather than magic numbers.

Suggested initial configuration:

```text
desired interval: 1–2 seconds
minimum movement: approximately 2 meters
priority: high accuracy
```

Do not update the UI directly from the location callback.

Recommended flow:

```text
Location Provider
      ↓
RunningTrackingService
      ↓
LocationFilter
      ↓
accepted GPS point
      ↓
Session Repository
      ↓
Telemetry Calculator
      ↓
StateFlow / observable session state
      ↓
walk_run_screen
```

The foreground service should be the authoritative owner of an active tracking session.

The UI should observe the service/repository state rather than owning GPS tracking itself.

This allows tracking to continue correctly when:

- screen turns off,
- user switches app,
- Activity is recreated,
- device rotates,
- navigation temporarily leaves the screen.

---

# 8. GPS filtering — Drift & Noise

GPS noise must not artificially increase the user's distance.

Examples:

- user stands still but GPS moves around several meters,
- tall buildings,
- tree cover,
- weak satellite signal,
- temporary GPS jumps.

Use at least these baseline thresholds:

```kotlin
MAX_ACCEPTABLE_ACCURACY_METERS = 15f
MIN_MOVING_SPEED_MPS = 0.5f
MAX_REASONABLE_RUNNING_SPEED_MPS = 12f
```

Basic acceptance rule:

```text
Reject location if accuracy > 15 m.

For normal tracking points:
accept movement only when effective speed > 0.5 m/s.

Reject obviously impossible running jumps.
```

Do not blindly trust `Location.speed` on every device.

When appropriate, derive segment speed using:

```text
segmentSpeed =
    distanceBetween(previousPoint, currentPoint)
    / elapsedTimeSeconds
```

Use monotonic elapsed time rather than wall-clock time for duration/speed calculations.

For each candidate point:

```text
1. Validate latitude/longitude.
2. Validate timestamp.
3. Reject poor accuracy.
4. Calculate distance from previous accepted point.
5. Calculate elapsed time.
6. Calculate effective speed.
7. Reject stationary jitter.
8. Reject unrealistic teleport/jump.
9. Add accepted point.
10. Add segment distance.
11. Persist point.
12. Update route GeoJSON.
13. Recalculate telemetry.
```

The first valid point starts the route but contributes `0 m` to total distance.

Never calculate distance from rejected GPS points.

---

# 9. Distance calculation

For consecutive accepted points use Android's geographical distance calculation:

```kotlin
Location.distanceBetween(
    previous.latitude,
    previous.longitude,
    current.latitude,
    current.longitude,
    results
)
```

Then:

```text
totalDistanceMeters += results[0]
```

Store distance internally in meters.

Convert only for presentation:

```text
distanceKm = totalDistanceMeters / 1000.0
```

Do not repeatedly derive the complete session distance from rounded UI values.

---

# 10. Duration

Track **active running duration**, excluding paused periods.

Do not base workout duration purely on `System.currentTimeMillis()` because wall-clock time can change.

Prefer monotonic elapsed time such as:

```text
SystemClock.elapsedRealtime()
```

Conceptually:

```text
activeDuration =
    total completed RUNNING periods
    + current running period
```

Pause stops active duration accumulation.

Resume begins another running period.

---

# 11. Pace

Average pace:

```text
paceMinutesPerKm =
    activeDurationMinutes / distanceKm
```

Example:

```text
duration = 30 minutes
distance = 5 km

pace = 6:00 / km
```

Avoid division when the run has not accumulated enough distance.

Before the minimum usable distance is reached, display:

```text
-- /km
```

Do not show extreme pace values caused by the first few GPS samples.

Internally keep full precision and only round for UI presentation.

---

# 12. Calories

Use the basic requested running estimate:

\[
Calories \approx Weight_{kg} \times Distance_{km} \times 1.036
\]

Example implementation:

```kotlin
calories =
    weightKg *
    distanceKm *
    1.036
```

Important:

- Obtain `weightKg` from the user's existing profile/settings if available.
- Do not hard-code an assumed body weight such as 70 kg.
- If weight does not exist, return `null` / unavailable and show `--` instead of inventing a calorie value.

This is only an estimated calorie value and should not be represented as medically precise.

---

# 13. Stadia Maps setup

## Account setup

For Android/native mobile apps, Stadia Maps recommends API-key authentication. citeturn511938view0

The developer must:

```text
1. Create/sign in to a Stadia Maps account.
2. Open Client Dashboard.
3. Open Manage Properties.
4. Create or select the property belonging to this Android app.
5. Open Authentication Configuration.
6. Generate an API key.
7. Store the API key outside the Git repository.
```

Use:

```text
styleId = "outdoors"
```

Stadia's Outdoors style is specifically suitable for outdoor/walking/cycling-style maps and is available as a MapLibre-compatible vector style. citeturn438169search4

For vector styles used from a mobile app, append the Stadia API key to the stylesheet request as documented by Stadia Maps. Stadia then propagates authentication to the data sources referenced by the stylesheet. citeturn511938view0

Conceptually:

```kotlin
val styleId = "outdoors"

val styleUrl =
    buildStadiaStyleUrl(
        styleId = styleId,
        apiKey = BuildConfig.STADIA_MAPS_API_KEY
    )

mapLibreMap.setStyle(styleUrl)
```

Do not hard-code the real API key directly in committed Kotlin/XML files.

Development:

```text
local.properties / developer machine secret
```

CI/CD:

```text
CI secret / environment variable
```

Expose it to the build through the project's existing secure build configuration.

Remember that secrets shipped inside a mobile client can ultimately be extracted. Prevent accidental repository exposure, rotate keys when needed and monitor usage in Stadia's dashboard. Stadia also recommends hardware-secured storage such as Android Keystore when long-term client-side key storage is required. citeturn511938view0

---

# 14. MapLibre initialization

Initialize MapLibre according to the project's Application lifecycle.

Create or reuse a `MapView` on `walk_run_screen`.

After MapLibre finishes loading the Stadia Outdoors style:

```text
create GeoJsonSource
        ↓
create LineLayer referencing source
        ↓
add source to Style
        ↓
add LineLayer to Style
```

IDs should be constants, for example:

```kotlin
const val RUN_ROUTE_SOURCE_ID = "run-route-source"
const val RUN_ROUTE_LAYER_ID = "run-route-layer"
```

The route geometry should be:

```text
GeoJSON Feature
    └── LineString
         ├── point 1
         ├── point 2
         ├── point 3
         └── ...
```

MapLibre supports updating GeoJSON sources with runtime data and rendering LineString geometries through a `LineLayer`. citeturn438169search3turn438169search8

---

# 15. Real-time route rendering

Maintain accepted GPS coordinates in the session domain/repository.

Do not store map-specific classes as the source of truth.

Domain model example:

```kotlin
data class RunPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double?,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val elapsedRealtimeNanos: Long
)
```

Map layer converts `RunPoint` objects into MapLibre GeoJSON points.

Whenever a point is accepted:

```text
accepted points
      ↓
Point list
      ↓
LineString
      ↓
Feature
      ↓
GeoJsonSource update
```

Do not destroy and recreate the map/layer for every location update.

Create the `GeoJsonSource` and `LineLayer` once after the map style loads and then update the source data.

---

# 16. Route styling

The running route must remain clearly visible over the Stadia Outdoors basemap.

Use the project's design tokens where possible.

Recommended properties:

```text
line cap: round
line join: round
line width: clearly visible at running zoom levels
```

Optionally render:

- Start marker.
- Current runner marker.
- Finish marker.

Do not add excessive markers for every GPS point.

---

# 17. Camera behavior

When the first valid GPS point arrives:

```text
center map on runner
```

During tracking:

- Default to following the runner.
- Do not constantly override user camera gestures.

Recommended behavior:

```text
user manually pans/zooms
        ↓
temporarily disable auto-follow
        ↓
show "recenter" button
        ↓
user taps recenter
        ↓
resume follow mode
```

Do not animate the camera aggressively on every one-second GPS update.

---

# 18. Stadia / OpenStreetMap attribution

Do not remove required map attribution.

Stadia states that automatically generated map attribution must remain available, or an equivalent prominent attribution must be provided.

For standard Stadia maps this includes attribution to:

```text
Stadia Maps
OpenMapTiles
OpenStreetMap
```

Preserve MapLibre attribution functionality unless the project deliberately provides a compliant custom attribution UI. citeturn261574search0

---

# 19. Session persistence

A production implementation must not keep the entire run only in Activity memory.

Persist at least:

```text
RunSession
- id
- startedAt
- finishedAt
- activeDuration
- distanceMeters
- calories
- status

RunPoint
- sessionId
- latitude
- longitude
- accuracy
- speed
- elapsed timestamp
- sequence
```

Persist accepted points progressively instead of waiting until Finish.

This prevents losing the entire run if the process crashes.

Do not automatically create a new run if an unfinished session already exists.

On reopening the application, detect interrupted/unfinished sessions and provide a safe recovery path consistent with the product UX.

---

# 20. Foreground service lifecycle

### Start

```text
User explicitly presses Start Run
↓
permissions OK
↓
GPS/location enabled
↓
create run session
↓
start foreground service while Activity is visible
↓
start location updates
```

### Pause

```text
status = PAUSED
stop adding route points
stop accumulating active duration
```

The session remains active/recoverable.

### Resume

```text
status = RUNNING
restart active timing
resume accepted GPS points
```

Be careful with the first point after Resume.

Do not calculate a distance segment connecting the last pre-pause point to a distant first post-pause point if the user physically moved during the pause.

Start a new route segment after Resume.

### Finish

```text
stop location updates
↓
finalize telemetry
↓
persist session
↓
update status = FINISHED
↓
stopForeground(...)
↓
stopSelf()
```

---

# 21. Route segmentation

Support route segments rather than assuming the complete run is always one continuous line.

A new segment should begin after events such as:

```text
Pause → Resume
```

This avoids drawing a fake straight line through movement that occurred while tracking was intentionally paused.

GeoJSON can therefore be represented as either:

```text
LineString
```

for a simple uninterrupted session, or preferably internally as multiple route segments and rendered appropriately.

---

# 22. Offline/network behavior

GPS tracking must not depend on map network availability.

If Stadia map tiles cannot load:

```text
tracking continues
distance continues
duration continues
GPS points continue to persist
```

The map may show an offline/error state, but a tile-network failure must **not stop or corrupt the run**.

Map rendering and GPS tracking are separate concerns.

---

# 23. Battery optimization

Do **not** immediately force every user to disable Android Battery Optimization.

A correctly implemented location foreground service is the primary solution.

Only provide a battery-optimization troubleshooting flow if testing shows that a specific device/OEM aggressively interrupts active workout tracking.

If such a flow is required:

```text
Explain why continuous tracking is affected
↓
user explicitly chooses Fix battery settings
↓
open the appropriate Android settings screen
```

Do not silently request power-management exemption.

Android documentation states that most apps should not request direct battery-optimization exemptions, and Google Play restricts this unless the app's core functionality is genuinely adversely affected. citeturn802576search0turn802576search4

---

# 24. Error handling

Handle at least:

### Location permission denied

Show explanation and Start remains unavailable.

### Permission permanently denied

Show **Open Settings**.

### Precise location disabled

Explain why precise GPS is needed.

### Device location/GPS disabled

Provide action to enable Location.

### GPS temporarily unavailable

Show:

```text
Searching for GPS…
```

Do not end the session automatically.

### Poor GPS accuracy

Ignore bad points and continue waiting for a better fix.

### Stadia/API/network error

Keep running tracking active.

### Map style failed to load

Show recoverable map error while keeping telemetry/tracking functional.

### Foreground service start failure

Do not mark session as successfully running.

Surface an actionable error and keep data state consistent.

---

# 25. Suggested module structure

Adapt names to the project's architecture.

```text
running/
├── ui/
│   ├── walk_run_screen
│   └── RunningViewModel
│
├── service/
│   └── RunningTrackingService
│
├── location/
│   ├── RunningLocationProvider
│   └── LocationFilter
│
├── domain/
│   ├── RunSession
│   ├── RunPoint
│   ├── RunStatus
│   └── RunningTelemetryCalculator
│
├── data/
│   └── RunningRepository
│
└── map/
    └── RunningRouteRenderer
```

Keep:

```text
location tracking
telemetry calculation
map rendering
UI
persistence
```

as separate concerns.

---

# 26. Testing requirements

Add unit tests for:

```text
LocationFilter
- rejects accuracy > 15 m
- rejects stationary GPS drift
- accepts normal running movement
- rejects impossible GPS jumps

TelemetryCalculator
- accumulates distance correctly
- excludes rejected locations
- calculates pace correctly
- handles zero distance
- excludes paused duration
- calculates calories correctly
- handles missing user weight

Session state
- READY → RUNNING
- RUNNING → PAUSED
- PAUSED → RUNNING
- RUNNING → FINISHED
```

Also manually/instrumentation test:

```text
permission denied
permission permanently denied
approximate-only location
GPS disabled
notification permission denied
screen turned off
app moved to background
Activity recreation
temporary network loss
temporary GPS loss
pause and walk elsewhere then resume
poor GPS near tall buildings
long running session
finish from notification
```

---

# 27. Acceptance criteria

The feature is complete only when all of the following are true:

1. Discovery screen contains a Running card.

2. Tapping Running navigates to `walk_run_screen`.

3. The app requests location permission before starting GPS tracking.

4. The app correctly handles denied and permanently denied permissions.

5. Precise GPS tracking does not begin without appropriate permission.

6. A user-initiated run starts a `location` foreground service.

7. The run continues tracking when:
   - screen turns off,
   - app goes to background.

8. An ongoing foreground notification is visible according to Android's notification rules.

9. GPS points with unacceptable accuracy are rejected.

10. Stationary GPS drift does not significantly increase distance.

11. Unrealistic GPS jumps are rejected.

12. Distance is calculated from accepted consecutive locations.

13. Active duration excludes paused time.

14. Pace is calculated from active duration and distance.

15. Calories use the user's real profile weight when available.

16. No fake/default body weight is used.

17. Stadia Maps `outdoors` vector style renders through MapLibre Native.

18. Running route updates in real time using `GeoJsonSource` + `LineLayer`.

19. Pausing/resuming does not create a fake route line through the paused movement.

20. Losing map/network access does not stop GPS tracking.

21. Required map attribution remains available.

22. Stadia API key is not committed to source control.

23. Finishing the run stops GPS updates and the foreground service.

24. Session and accepted GPS points are persisted safely.

25. Existing project architecture and unrelated screens are not unnecessarily refactored.

---

# 28. Implementation order for the agent

Implement in this order:

```text
1. Inspect existing project architecture/navigation/design system.

2. Add Running card to Discovery.

3. Connect navigation to walk_run_screen.

4. Add Android manifest permissions and location foreground service declaration.

5. Implement runtime location permission flow.

6. Implement RunningTrackingService + notification channel.

7. Implement location provider.

8. Implement LocationFilter.

9. Implement RunSession state model.

10. Implement distance/duration/pace/calorie calculations.

11. Implement persistence.

12. Integrate MapLibre Native.

13. Configure Stadia Maps Outdoors style.

14. Add GeoJsonSource + LineLayer route rendering.

15. Connect service state to walk_run_screen.

16. Implement Pause / Resume / Finish.

17. Implement notification actions.

18. Handle map/network/GPS errors.

19. Add tests.

20. Run the complete background/screen-off test flow on a physical Android device.
```

Do not consider the task complete merely because a route appears on the emulator. Continuous GPS behavior and foreground-service lifecycle must be tested on a real Android device.

---

# 29. Final agent constraints

The implementation must be production code, not a demo.

Do not:

```text
- put location tracking inside Activity lifecycle only
- keep the full session only in memory
- hard-code the Stadia API key
- request location before there is user intent
- request ACCESS_BACKGROUND_LOCATION unnecessarily
- count rejected GPS points toward distance
- count paused time toward running duration
- connect route across a paused interval
- stop a run because map tiles failed
- recreate GeoJsonSource/LineLayer for every GPS update
- force battery-optimization exemption on every user
- remove legally required map attribution
- introduce an unrelated mapping SDK such as Google Maps or Mapbox
```

The final implementation must use:

```text
MapLibre Native Android SDK
+
Stadia Maps Outdoors
+
Android Location ForegroundService
+
GeoJsonSource
+
LineLayer
```