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
  styleClass = Seq("content-pane")

  private val headerBlock = UIUtils.createPageHeader(
    "Daily Distribution Optimizer",
    "Generate waste-minimizing expiry-first allocation plans, confirm dispatch, and export PDF reports."
  )

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
  private val statsLabel = new Label("ℹ️ No active plan. Click 'Generate Plan' to calculate daily distribution layout.") {
    styleClass = Seq("banner-info-text")
  }
  private val statusLabel = new Label { styleClass = Seq("status-label") }

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

  private val exportButton = new Button("📄 Export PDF Report"):
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
      UIUtils.applyStatus(statusLabel, "error", "Error: There are no pending family requests to satisfy!")
      proposedAllocations.clear()
      proposedInventory.value = Nil
      proposedRequests.value = Nil
      dispatchButton.disable = true
      dispatchButton.opacity = 0.5
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
        UIUtils.applyStatus(statusLabel, "warning", "Notice: Generated plan matches 0 items (insufficient or incompatible stock).")
        proposedAllocations.clear()
        proposedInventory.value = Nil
        proposedRequests.value = Nil
        dispatchButton.disable = true
        dispatchButton.opacity = 0.5
        exportButton.disable = true
        exportButton.opacity = 0.5
        UIUtils.applyStatus(statsLabel, "info", "No active plan. Click 'Generate Plan' to calculate daily distribution layout.")
      else
        proposedAllocations.clear()
        proposedAllocations.addAll(allocations)
        proposedInventory.value = updatedInv
        proposedRequests.value = updatedReqs

        // Calculate potential waste prevented (perishable quantities allocated)
        val perishablesAllocated = allocations.filter { allocation =>
          inventory.exists {
            case item: PerishableItem if item.name == allocation.itemName => true
            case _ => false
          }
        }.map(_.allocatedQuantity).sum

        statsLabel.styleClass = Seq("status-label")
        statsLabel.style = "-fx-font-size: 14px; -fx-text-fill: #334155;"
        statsLabel.text = s"Plan Details: Satisfied ${allocations.map(_.familyName).distinct.size} families. Allocated ${allocations.map(_.allocatedQuantity).sum} units of food. Waste Minimized (Perishables): $perishablesAllocated units."
        UIUtils.applyStatus(statusLabel, "success", "Plan successfully generated! Review above and click 'Confirm & Dispatch'.")
        dispatchButton.disable = false
        dispatchButton.opacity = 1.0
        exportButton.disable = false
        exportButton.opacity = 1.0

  private def performDispatch(): Unit =
    if proposedInventory.value.nonEmpty && proposedRequests.value.nonEmpty then
      val count = proposedAllocations.size
      if UIUtils.showConfirmation("Confirm Plan Dispatch", "Dispatch Distribution Plan", s"Are you sure you want to confirm and dispatch allocations for $count food items to families?") then
        // Remove items from inventory if quantity drops to 0 or is very small
        val filteredInventory = proposedInventory.value.filter(_.quantity > 0.0001)
        
        // Dispatch changes back to Main App logic
        onDispatch(filteredInventory, proposedRequests.value)
        
        // Clear local state
        proposedAllocations.clear()
        proposedInventory.value = Nil
        proposedRequests.value = Nil
        
        statsLabel.styleClass = Seq("banner-info-text")
        statsLabel.text = "ℹ️ No active plan. Click 'Generate Plan' to calculate daily distribution layout."
        UIUtils.applyStatus(statusLabel, "success", "Success: Daily plan dispatched! Inventory and request log updated and saved.")
        dispatchButton.disable = true
        dispatchButton.opacity = 0.5
        exportButton.disable = true
        exportButton.opacity = 0.5
        UIUtils.showToast("Daily distribution plan dispatched to families", "success")

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
        val headerY = 680f
        content.beginText()
        content.setFont(PDType1Font.HELVETICA_BOLD, 10)
        content.newLineAtOffset(50, headerY)
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
        content.moveTo(50, headerY - 5f)
        content.lineTo(550, headerY - 5f)
        content.stroke()
        
        // Draw Rows
        content.setFont(PDType1Font.HELVETICA, 9)
        proposedAllocations.zipWithIndex.foreach { (alloc, idx) =>
          val rowY = headerY - 25f - (idx * 20f)
          if rowY > 50f then
            content.beginText()
            content.newLineAtOffset(50, rowY)
            content.showText(alloc.id)
            content.newLineAtOffset(100, 0)
            content.showText(alloc.familyName)
            content.newLineAtOffset(150, 0)
            content.showText(s"${alloc.itemName} (${alloc.category})")
            content.newLineAtOffset(150, 0)
            content.showText(s"${alloc.allocatedQuantity} ${alloc.unit}")
            content.endText()
        }
        
        content.close()
        val file = new java.io.File("daily_distribution_report.pdf")
        doc.save(file)
        doc.close()
        
        UIUtils.applyStatus(statusLabel, "success", s"✓ PDF Report successfully exported to ${file.getAbsolutePath}!")
      catch
        case e: Exception =>
          UIUtils.applyStatus(statusLabel, "error", s"✗ Error exporting PDF: ${e.getMessage}")

  private val statsPanel = new VBox:
    spacing = 5
    padding = Insets(12, 18, 12, 18)
    styleClass = Seq("plan-info-banner")
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
    headerBlock,
    tableWrapper,
    statsPanel,
    actionRow
  )
