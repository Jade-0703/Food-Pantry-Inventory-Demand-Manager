# AI Integration Reflection

This reflection analyzes the collaboration with AI tools under the Sunway University PRG2104 Tier C assessment guidelines. It evaluates prompt strategies, AI hallucinations, and human decision-making.

---

### What was AI most useful for?
The AI was extremely valuable in translating the core distribution constraints into a clean, tail-recursive matching function in `DistributionPolicy.scala`. As documented in **Entry #5**, the initial conceptual model was complex to express in an immutable manner without using standard loops. The AI provided an elegant pattern of carrying the state through list parameters inside a nested recursive helper function `distributeRecursive`. This approach ensured that the core matching logic is 100% thread-safe and free from any `var` keywords or mutable collections, enabling us to easily earn full marks on the immutability design criteria (S1-11).

---

### Where did AI mislead or hallucinate?
The AI misled the development process during the ScalaFX layout construction in `InventoryView.scala`. As logged in **Entry #8**, when attempting to dynamically toggle the visibility of the "Expiry Date" and "Shelf Life" fields based on the item type combo box selection, the AI generated a binding expression using `.delegate.getSelectionModel.selectedItemProperty()`. This resulted in a type mismatch compilation error because `itemTypeCombo.value` in ScalaFX is an `ObjectProperty[String]` and does not contain the JavaFX selection model delegate. I resolved this compile failure by bypassing the complicated binding entirely and creating a direct `onAction` handler on `itemTypeCombo` to manually alter the `.visible` property of pre-instantiated Label and Input components.

---

### What did the student do that AI could NOT do?
While the AI could suggest blocks of code, it lacked the ability to synthesize the overarching architectural requirements and defend the implementation:
1. **Design Decisions**: I structured the visual interface into separate class components inheriting from `VBox` (e.g., `DashboardView.scala` and `DemandView.scala`) to keep the code DRY (**Entry #6**), whereas the AI repeatedly suggested bundling all controls into a single large main file.
2. **Defending Code in Viva**: The AI cannot explain the code in real-time. I had to thoroughly study the recursive state transformations and exception handlers to ensure I could walk through the source code fluently within 60 seconds and justify using a custom CSV serialization format over external JSON dependencies (**Entry #3**).
