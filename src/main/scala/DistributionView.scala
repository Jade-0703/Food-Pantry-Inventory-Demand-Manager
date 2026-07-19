package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.ObjectProperty
import scalafx.geometry.{Insets, Pos}
import scalafx.Includes._
import java.time.LocalDate
import scalafx.scene.text.{Font, FontWeight}

@annotation.nowarn("cat=deprecation")
class DistributionView(
  inventory: ObservableBuffer[FoodItem],
  requests: ObservableBuffer[FamilyRequest],
  onDispatch: (List[FoodItem], List[FamilyRequest]) => Unit
) extends VBox:

  spacing = 15
  padding = Insets(20)
  style = "-fx-background-color: #f1f5f9;"

  // Header Title
  private val titleLabel = new Label("Daily Distribution Optimizer"):
    font = Font.font("System", FontWeight.Bold, 24)
    style = "-fx-text-fill: #1e293b;"

  // Allocation Table
  private val allocationsTable = new TableView[Allocation]:
    style = "-fx-background-radius: 8px; -fx-background-color: #ffffff;"
    placeholder = new Label("No distribution plan generated. Click 'Generate' below.") { style = "-fx-text-fill: #64748b;" }

    val idCol = new TableColumn[Allocation, String]("ID"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "id", cellData.value.id) }
      prefWidth = 80
      
    val familyCol = new TableColumn[Allocation, String]("Recipient Family"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "family", cellData.value.familyName) }
      prefWidth = 180
      
    val itemCol = new TableColumn[Allocation, String]("Allocated Item"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "item", cellData.value.itemName) }
      prefWidth = 180
      
    val categoryCol = new TableColumn[Allocation, String]("Category"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "category", cellData.value.category.toString) }
      prefWidth = 120

    val qtyCol = new TableColumn[Allocation, String]("Quantity"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "quantity", s"${cellData.value.allocatedQuantity} ${cellData.value.unit}") }
      prefWidth = 140

    columns ++= Seq(idCol, familyCol, itemCol, categoryCol, qtyCol)
    prefHeight = 280

  // Local state for the generated proposed changes
  private val proposedAllocations = ObservableBuffer[Allocation]()
  private val proposedInventory = ObjectProperty[List[FoodItem]](this, "proposedInventory", Nil)
  private val proposedRequests = ObjectProperty[List[FamilyRequest]](this, "proposedRequests", Nil)

  allocationsTable.items = proposedAllocations

  // Info and Stats Labels
  private val statsLabel = new Label("No active plan. Click 'Generate Plan' to calculate daily distribution layout.") { style = "-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: bold;" }
  private val statusLabel = new Label { style = "-fx-font-weight: bold; -fx-font-size: 13px;" }

  private val generateButton = new Button("Generate Plan"):
    styleClass = Seq("button", "button-primary")
    onAction = handle { performGeneratePlan() }

  private val dispatchButton = new Button("Confirm & Dispatch Plan"):
    styleClass = Seq("button", "button-success")
    opacity = 0.5
    disable = true
    onAction = handle { performDispatch() }

  private def performGeneratePlan(): Unit =
    statusLabel.text = ""
    val today = LocalDate.now()
    
    // Check if there are any pending requests
    val pendingCount = requests.count(_.status == RequestStatus.Pending)
    if pendingCount == 0 then
      statusLabel.style = "-fx-text-fill: #dc2626;"
      statusLabel.text = "Error: There are no pending family requests to satisfy!"
      proposedAllocations.clear()
      proposedInventory.value = Nil
      proposedRequests.value = Nil
      dispatchButton.disable = true
      dispatchButton.style = "-fx-background-color: #16a34a; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 6px; -fx-opacity: 0.5;"
      statsLabel.text = ""
    else
      // Execute the pure waste minimizing policy
      val (updatedInv, updatedReqs, allocations) = WasteMinimizingPolicy.generatePlan(
        inventory = inventory.toList,
        requests = requests.toList,
        today = today
      )

      if allocations.isEmpty then
        statusLabel.style = "-fx-text-fill: #ea580c;"
        statusLabel.text = "Notice: Generated plan matches 0 items (insufficient or incompatible stock)."
        proposedAllocations.clear()
        proposedInventory.value = Nil
        proposedRequests.value = Nil
        dispatchButton.disable = true
        dispatchButton.opacity = 0.5
        statsLabel.style = "-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: bold;"
        statsLabel.text = "No active plan. Click 'Generate Plan' to calculate daily distribution layout."
      else
        proposedAllocations.clear()
        proposedAllocations.addAll(allocations)
        proposedInventory.value = updatedInv
        proposedRequests.value = updatedReqs

        // Calculate potential waste prevented (perishable quantities allocated)
        val perishablesAllocated = allocations.filter { allocation =>
          // Look up if this item was perishable in original inventory
          inventory.exists {
            case item: PerishableItem if item.name == allocation.itemName => true
            case _ => false
          }
        }.map(_.allocatedQuantity).sum

        statsLabel.style = "-fx-font-size: 14px; -fx-text-fill: #334155; -fx-font-weight: bold;"
        statsLabel.text = s"Plan Details: Satisfied ${allocations.map(_.familyName).distinct.size} families. Allocated ${allocations.map(_.allocatedQuantity).sum} units of food. Waste Minimized (Perishables): $perishablesAllocated units."
        statusLabel.style = "-fx-text-fill: #16a34a;"
        statusLabel.text = "Plan successfully generated! Review above and click 'Confirm & Dispatch'."
        dispatchButton.disable = false
        dispatchButton.opacity = 1.0

  private def performDispatch(): Unit =
    if proposedInventory.value.nonEmpty && proposedRequests.value.nonEmpty then
      // Remove items from inventory if quantity drops to 0 or is very small
      val filteredInventory = proposedInventory.value.filter(_.quantity > 0.0001)
      
      // Dispatch changes back to Main App logic
      onDispatch(filteredInventory, proposedRequests.value)
      
      // Clear local state
      proposedAllocations.clear()
      proposedInventory.value = Nil
      proposedRequests.value = Nil
      
      statsLabel.style = "-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: bold;"
      statsLabel.text = "No active plan. Click 'Generate Plan' to calculate daily distribution layout."
      statusLabel.style = "-fx-text-fill: #16a34a;"
      statusLabel.text = "Success: Daily plan dispatched! Inventory and request log updated and saved."
      dispatchButton.disable = true
      dispatchButton.opacity = 0.5

  private val statsPanel = new VBox:
    spacing = 5
    padding = Insets(10)
    styleClass = Seq("form-card")
    children = Seq(statsLabel)

  private val actionRow = new HBox:
    spacing = 15
    alignment = Pos.CenterLeft
    children = Seq(generateButton, dispatchButton, statusLabel)

  children = Seq(
    titleLabel,
    allocationsTable,
    statsPanel,
    actionRow
  )
