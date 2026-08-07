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
