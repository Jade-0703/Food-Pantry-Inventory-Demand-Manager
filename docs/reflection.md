# OOP Design Reflection

This reflection evaluates the object-oriented design patterns, challenges, and architectural decisions encountered while building the Food Pantry SDG-1 Application.

---

### 1. OOP Applied
The architecture employs core Object-Oriented Programming principles to create a maintainable and modular code base:
* **Inheritance & Subtype Polymorphism**: A `sealed trait FoodItem` serves as the abstract parent for `PerishableItem` and `NonPerishableItem`. It extracts common fields (`id`, `name`, `category`, `quantity`, `unit`) to keep the code DRY. Subclasses provide distinct implementations for the abstract `getExpiryStatus` method, allowing the GUI to print customized details depending on whether the item is perishable or shelf-stable.
* **Parametric Polymorphism**: A generic `Repository[T]` trait and `SqliteRepository[T]` class manage database persistence. By using generic ResultSet mapping functions, the application decouples database storage logic from specific domain models, allowing the same SQL access methods to work for both `FoodItem` and `FamilyRequest`.
* **Encapsulation**: The database connection string is encapsulated as a `private val dbUrl` inside the repository. This protects internal database credentials and connection parameters from external components, ensuring all database access goes strictly through public repository methods.

---

### 2. Problems Faced
* **Immutability in GUI State**: Managing updates in a stateful user interface without utilizing any `var` variables or mutable collection helpers was challenging. While standard JavaFX applications rely heavily on mutable lists, this design coordinates state exclusively using ScalaFX's `ObservableBuffer` and reactive properties, which are allowed exceptions in the rubric. This approach eliminated manual state syncing, allowing the dashboard and tables to update automatically whenever elements are added or deleted.
* **Platform Dependencies**: Setting up JavaFX libraries across different operating systems can lead to compile-time exceptions. This was solved in `build.sbt` by dynamically resolving the runtime OS name and mapping it to the appropriate platform classifier (`mac`, `win`, or `linux`).
* **Input Validation & Safety**: Converting input text fields into numbers (`toDouble`) or dates (`LocalDate.parse`) is highly risky. We wrapped every conversion pipeline in a `scala.util.Try` block to intercept parsing errors and report clean error labels in the user interface instead of throwing raw stack traces.
* **ScalaFX/JavaFX API Layer Mismatch**: ScalaFX wraps JavaFX controls but does not always immediately propagate user interactions to its `ObservableValue` wrapper. This surfaced in two ways: `DatePicker.value.value` did not reflect the user's calendar popup selection (fixed by reading `delegate.getValue` from the underlying JavaFX control), and `Dialog[Unit].showAndWait()` always returns `None` regardless of which button is clicked (fixed by using `Dialog[ButtonType]` with an explicit `resultConverter`). Both bugs compiled cleanly without warnings but failed silently at runtime, reinforcing the importance of end-to-end demo testing.

---

### 3. Strengths and Weaknesses
* **Strengths**: 
  * *High Type-Safety*: Sealed traits restrict inheritance, allowing the compiler to verify exhaustiveness in pattern matches.
  * *Thread-Safety*: The business logic is 100% thread-safe due to the use of immutable case classes and pure tail-recursive allocation algorithms.
  * *Extensibility*: Adding new food categories or alternative distribution policies requires no modifications to existing UI structures.
* **Weaknesses**: 
  * *File I/O Overhead*: The file repository overwrites the entire file upon every edit. While suitable for a small-scale pantry, this causes performance scaling issues for larger databases.
  * *Tight Coupling*: The UI views directly manipulate the repository state. Implementing a strict Model-View-ViewModel (MVVM) architecture with an intermediate controller layer would further isolate presentation from core business logic.
