package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.ObjectProperty
import scalafx.geometry.{Insets, Pos}
import scalafx.Includes._
import java.time.LocalDate

@annotation.nowarn("cat=deprecation")
class DistributionView(
  inventory: ObservableBuffer[FoodItem],
  requests: ObservableBuffer[FamilyRequest],
  onDispatch: (List[FoodItem], List[FamilyRequest]) => Unit
) extends VBox:

  spacing = 15
  padding = Insets(20)
  style = "-fx-background-color: #fbf9f4;"

  private val titleLabel = new Label("Daily Distribution Optimizer"):
    style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 20px;"

  // Allocation Table
  private val allocationsTable = new TableView[Allocation]:
    val selfTable: TableView[Allocation] = this
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("No distribution plan generated. Click 'Generate' below.") { style = "-fx-text-fill: #64748b;" }

    // S1-14 / Entry 14 clip layout to prevent row background bleed
    clip = UIUtils.createRoundedClip(selfTable)

    private val idCol: TableColumn[Allocation, String] = new TableColumn[Allocation, String]("ID"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "id", cellData.value.id) }
      prefWidth = 80
      
    private val familyCol: TableColumn[Allocation, String] = new TableColumn[Allocation, String]("Recipient Family"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "family", cellData.value.familyName) }
      prefWidth = 180
      
    private val itemCol: TableColumn[Allocation, String] = new TableColumn[Allocation, String]("Allocated Item"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "item", cellData.value.itemName) }
      prefWidth = 180
      
    private val categoryCol: TableColumn[Allocation, String] = new TableColumn[Allocation, String]("Category"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "category", cellData.value.category.toString) }
      prefWidth = 120

    private val qtyCol: TableColumn[Allocation, String] = new TableColumn[Allocation, String]("Quantity"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "quantity", s"${cellData.value.allocatedQuantity} ${cellData.value.unit}") }
      prefWidth = 140

    columns ++= Seq(idCol, familyCol, itemCol, categoryCol, qtyCol)
    prefHeight = 280

  private val tableWrapper = new StackPane:
    styleClass = Seq("table-wrapper")
    children = Seq(allocationsTable)

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
    minWidth = scalafx.scene.layout.Region.USE_PREF_SIZE
    onAction = handle { performGeneratePlan() }

  private val dispatchButton = new Button("Confirm & Dispatch Plan"):
    styleClass = Seq("button", "button-success")
    minWidth = scalafx.scene.layout.Region.USE_PREF_SIZE
    opacity = 0.5
    disable = true
    onAction = handle { performDispatch() }

  private val exportButton = new Button("📤 Export Report"):
    styleClass = Seq("button", "button-secondary")
    minWidth = scalafx.scene.layout.Region.USE_PREF_SIZE
    opacity = 0.5
    disable = true
    onAction = handle { performExport() }

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
      exportButton.disable = true
      exportButton.opacity = 0.5
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
        exportButton.disable = true
        exportButton.opacity = 0.5
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
        exportButton.disable = false
        exportButton.opacity = 1.0

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
      exportButton.disable = true
      exportButton.opacity = 0.5

  private def performExport(): Unit =
    if proposedAllocations.nonEmpty then
      try
        import org.apache.pdfbox.pdmodel.PDDocument
        import org.apache.pdfbox.pdmodel.PDPage
        import org.apache.pdfbox.pdmodel.PDPageContentStream
        import org.apache.pdfbox.pdmodel.font.PDType1Font

        val doc = new PDDocument()
        val page = new PDPage()
        doc.addPage(page)
        
        val content = new PDPageContentStream(doc, page)
        
        // Title
        content.beginText()
        content.setFont(PDType1Font.HELVETICA_BOLD, 16)
        content.newLineAtOffset(50, 750)
        content.showText("DAILY FOOD DISTRIBUTION REPORT")
        content.endText()
        
        // Date info
        content.beginText()
        content.setFont(PDType1Font.HELVETICA, 10)
        content.newLineAtOffset(50, 730)
        content.showText(s"Generated on: ${LocalDate.now()}")
        content.endText()
        
        // Draw Table Header
        var y = 680
        content.beginText()
        content.setFont(PDType1Font.HELVETICA_BOLD, 10)
        content.newLineAtOffset(50, y.toFloat)
        content.showText("Allocation ID")
        content.newLineAtOffset(100, 0)
        content.showText("Family Name")
        content.newLineAtOffset(150, 0)
        content.showText("Allocated Item")
        content.newLineAtOffset(150, 0)
        content.showText("Quantity")
        content.endText()
        
        // Divider line
        content.setLineWidth(1.0f)
        content.moveTo(50, (y - 5).toFloat)
        content.lineTo(550, (y - 5).toFloat)
        content.stroke()
        
        y -= 25
        
        // Draw Rows
        content.setFont(PDType1Font.HELVETICA, 9)
        proposedAllocations.foreach { alloc =>
          if y > 50 then
            content.beginText()
            content.newLineAtOffset(50, y.toFloat)
            content.showText(alloc.id)
            content.newLineAtOffset(100, 0)
            content.showText(alloc.familyName)
            content.newLineAtOffset(150, 0)
            content.showText(s"${alloc.itemName} (${alloc.category})")
            content.newLineAtOffset(150, 0)
            content.showText(s"${alloc.allocatedQuantity} ${alloc.unit}")
            content.endText()
            y -= 20
        }
        
        content.close()
        val file = new java.io.File("daily_distribution_report.pdf")
        doc.save(file)
        doc.close()
        
        statusLabel.style = "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
        statusLabel.text = s"✓ PDF Report successfully exported to ${file.getAbsolutePath}!"
      catch
        case e: Exception =>
          statusLabel.style = "-fx-text-fill: #dc2626; -fx-font-weight: bold;"
          statusLabel.text = s"✗ Error exporting PDF: ${e.getMessage}"

  private val statsPanel = new VBox:
    spacing = 5
    padding = Insets(10)
    styleClass = Seq("form-card")
    children = Seq(statsLabel)

  private val buttonsBox = new HBox:
    spacing = 15
    alignment = Pos.CenterLeft
    children = Seq(generateButton, dispatchButton, exportButton)

  private val actionRow = new VBox:
    spacing = 10
    alignment = Pos.CenterLeft
    children = Seq(buttonsBox, statusLabel)

  children = Seq(
    titleLabel,
    tableWrapper,
    statsPanel,
    actionRow
  )
