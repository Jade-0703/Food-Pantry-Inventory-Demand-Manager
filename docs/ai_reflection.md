# AI Integration Reflection

This reflection analyzes the collaboration with AI tools under the Sunway University PRG2104 Tier C assessment guidelines. It evaluates prompt strategies, AI hallucinations, and human decision-making.

---

### What was AI most useful for?
The AI was extremely valuable in translating the core distribution constraints into a clean, tail-recursive matching function in `DistributionPolicy.scala`. As documented in **Entry #5**, the initial conceptual model was complex to express in an immutable manner without using standard loops. The AI provided an elegant pattern of carrying the state through list parameters inside a nested recursive helper function `distributeRecursive`. This approach ensured that the core matching logic is 100% thread-safe and free from any `var` keywords or mutable collections, enabling us to easily earn full marks on the immutability design criteria (S1-11).

---

### Where did AI mislead or hallucinate?
The AI misled the development process in two notable cases:

**Entry #8 — ScalaFX ComboBox binding**: When attempting to dynamically toggle the visibility of the "Expiry Date" and "Shelf Life" fields based on the item type combo box selection, the AI generated a binding expression using `.delegate.getSelectionModel.selectedItemProperty()`. This resulted in a type mismatch compilation error because `itemTypeCombo.value` in ScalaFX is an `ObjectProperty[String]` and does not contain the JavaFX selection model delegate. I resolved this by discarding the AI suggestion and manually implementing a direct `onAction` handler on `itemTypeCombo` to manually alter the `.visible` property of pre-instantiated Label and Input components.

**Entry #16 — ScalaFX Dialog result type**: When building the edit dialogs for Inventory and Demand views, AI suggested `new Dialog[Unit]()` as the dialog type with `showAndWait()`. This silently broke both edit dialogs — `Dialog[Unit].showAndWait()` always returns `Option[Unit]` (i.e. `None`), so `result.contains(saveButtonType)` was always `false` and the save block never executed. I identified this during demo walkthrough testing, diagnosed the root cause, and fixed it by changing to `Dialog[ButtonType]` with an explicit `resultConverter = btn => btn`.

---

### What did the student do that AI could NOT do?
While the AI could suggest blocks of code, it lacked the ability to synthesize the overarching architectural requirements and defend the implementation:
1. **Design Decisions**: I structured the visual interface into separate class components inheriting from `VBox` (e.g., `DashboardView.scala` and `DemandView.scala`) to keep the code DRY (**Entry #6**), whereas the AI repeatedly suggested bundling all controls into a single large main file.
2. **Defending Code in Viva**: The AI cannot explain the code in real-time. I had to thoroughly study the recursive state transformations and JDBC result set iteration to ensure I could walk through the source code fluently within 60 seconds and justify using a generic `SqliteRepository[T]` JDBC implementation over external ORM frameworks (**Entry #3**).
3. **Catching Silent Runtime Bugs**: The AI-generated `Dialog[Unit]` code compiled successfully without any warnings, yet silently broke edit functionality at runtime. Only through systematic demo walkthrough testing did I identify that the save action never fired. This demonstrates that AI-generated code requires thorough runtime validation beyond just checking compilation output.
