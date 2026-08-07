# Development Log - Food Pantry Inventory & Demand

This log tracks the chronological planning and implementation progress of the final project.

---

## 2026-07-13: Project Planning & Architecture
- **Goal**: Propose application scope and establish standard folder structures.
- **Progress**:
  - Chose the "Food Pantry Inventory & Demand" sub-domain under SDG-1.
  - Defined the 4 core features: dashboard analytics, inventory logger, demand logger, and the waste-minimizing plan generator.
  - Sketched standard project folder structures to conform strictly with standard submission templates.

## 2026-07-14: Core Domain Design & Immutability Strategy
- **Goal**: Establish type-safe models for food items and family requests.
- **Progress**:
  - Outlined the `FoodItem` sealed trait model with concrete `PerishableItem` and `NonPerishableItem` case classes to support subtype polymorphism.
  - Refactored all domain collections to rely solely on immutable Scala lists to satisfy the zero-var and zero-mutable-collection rules.
  - Configured `build.sbt` with ScalaFX 21 and the `-Wunused:all` compile options.

## 2026-07-15: Generic Persistence & Serialization
- **Goal**: Develop the file-based persistence layer.
- **Progress**:
  - Created `Repository[T]` and `FileRepository[T]` demonstrating parametric polymorphism and encapsulation.
  - Wrapped all file systems and parsing pipelines in `scala.util.Try` block handlers.
  - Handled invalid SQL queries and database connections gracefully inside `scala.util.Try` to avoid any uncaught JDBC exceptions during loading.

## 2026-07-16: Distribution Policy Implementation
- **Goal**: Create the daily plan matching algorithm.
- **Progress**:
  - Implemented `WasteMinimizingPolicy` extending `DistributionPolicy`.
  - Authored a tail-recursive allocation loop that prioritizes perishable items closest to their expiry dates.
  - Wired restriction filters (Vegetarian, Halal, Gluten-Free) directly into the candidate selector to match household requests safely.
  - Wrote initial unit tests verifying the correctness of allocations.

## 2026-07-17: UI Layout & Navigation
- **Goal**: Construct side-bar layout and primary UI screens.
- **Progress**:
  - Built the main window in `MainApp.scala` using `JFXApp3` and structured navigation with side-bar buttons.
  - Created separate view component classes `DashboardView`, `InventoryView`, `DemandView`, and `DistributionView` inheriting from `VBox` to keep code DRY.
  - Handled form actions to trigger submission on `Enter` key presses, providing standard keyboard-friendly navigation.

## 2026-07-18: Polishing, Refactoring, & Final Verification
- **Goal**: Build cleanly with no warnings and finalize documentation.
- **Progress**:
  - Fixed small compiler warnings regarding unused variables and imports to achieve a 100% clean build.
  - Tested edge cases including empty inputs, invalid numbers, and out-of-range dates in forms, displaying clean red warnings.
  - Populated citations, interaction logs, and self-reported manifest metrics.

## 2026-08-07: Critical Bug Fixes & UI Polish
- **Goal**: Resolve runtime bugs found during final demo walkthrough and polish UI details.
- **Progress**:
  - **Edit Dialog Fix**: Discovered `Dialog[Unit].showAndWait()` always returns `None`, causing both the Inventory and Demand edit dialogs to silently do nothing when "Save Changes" was clicked. Fixed by changing both to `Dialog[ButtonType]` and adding `resultConverter = btn => btn` so the result can be matched against the save button type.
  - **DatePicker Fix**: `expiryDatePicker.value.value` (ScalaFX wrapper) did not reflect the user's calendar popup selection. Fixed by reading `expiryDatePicker.delegate.getValue` (JavaFX delegate) directly in both the add and edit flows.
  - **ComboBox Fix**: Applied the same `.delegate.getValue` pattern to all edit-dialog combo boxes in `InventoryView` and `DemandView` to ensure user dropdown selections are always captured.
  - **Chart Hover Tooltips**: Added interactive hover tooltips on PieChart slices and BarChart bars using double-nested `Platform.runLater` so tooltips are installed after the JavaFX layout pass creates the node objects.
  - **Pie Chart Colors**: Fixed `Other` category color from teal (`#14b8a6`) which clashed with `Vegetables` to a distinct orange (`#f97316`). All 6 category colors are now maximally distinct.
  - **Reset Button**: Centred the "Reset" wording inside the filter-reset-btn using `-fx-alignment: center` and `-fx-text-alignment: center`.
  - **Build.sbt Classloader**: Added `Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat` to permanently resolve an intermittent `NoClassDefFoundError` in `sbt test` caused by ScalaFX classloader isolation.
