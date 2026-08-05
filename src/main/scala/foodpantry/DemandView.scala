package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.geometry.{Insets, Pos}
import scalafx.Includes._
import scala.util.Try
import java.time.LocalDate

class DemandView(
  requests: ObservableBuffer[FamilyRequest],
  onSave: () => Unit
) extends VBox:

  spacing = 15
  padding = Insets(20)
  styleClass = Seq("content-pane")

  private val headerBlock = UIUtils.createPageHeader(
    "Family Demand & Requests Log",
    "Log and manage recipient family requests with household size, dietary restrictions, and food preferences."
  )

  // Requests Table
  private val requestsTable = new TableView[FamilyRequest]:
    val selfTable: TableView[FamilyRequest] = this
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("No pending family requests.") { style = "-fx-text-fill: #64748b;" }

    // S1-14 / Entry 14 clip layout to prevent row background bleed
    clip = UIUtils.createRoundedClip(selfTable)

    private val idCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Request ID"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "id", cellData.value.id) }
      prefWidth = 100
      
    private val nameCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Family Name"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "name", cellData.value.familyName) }
      prefWidth = 160
      
    private val sizeCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Household Size"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "size", cellData.value.householdSize.toString) }
      prefWidth = 110
      
    private val restrictionCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Dietary Restriction"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "diet", cellData.value.dietaryRestriction.toString) }
      prefWidth = 140
      cellFactory = { (col: TableColumn[FamilyRequest, String]) =>
        new TableCell[FamilyRequest, String] {
          item.onChange { (_, _, newText) =>
            if newText != null then
              graphic = UIUtils.getDietaryLabel(newText)
              text = null
              alignment = scalafx.geometry.Pos.Center
            else
              graphic = null
              text = null
          }
        }
      }

    private val categoryCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Category Requested"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "category", cellData.value.requestedCategory.toString) }
      prefWidth = 140

    private val statusCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Status"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "status", cellData.value.status.toString) }
      prefWidth = 110
      cellFactory = { (col: TableColumn[FamilyRequest, String]) =>
        new TableCell[FamilyRequest, String] {
          item.onChange { (_, _, newText) =>
            if newText != null then
              graphic = UIUtils.getStatusLabel(newText)
              text = null
              alignment = scalafx.geometry.Pos.Center
            else
              graphic = null
              text = null
          }
        }
      }

    columns ++= Seq(idCol, nameCol, sizeCol, restrictionCol, categoryCol, statusCol)
    columns.foreach(_.setReorderable(false))
    prefHeight <== scalafx.beans.binding.Bindings.createDoubleBinding(
      () => {
        val rowCount = items.value.size()
        if rowCount == 0 then 100.0
        else math.min((rowCount * 40.0) + 45.0, 360.0)
      },
      items
    )

  private val tableWrapper = new StackPane:
    styleClass = Seq("table-wrapper")
    children = Seq(requestsTable)

  private val tableSection = new VBox:
    spacing = 10
    styleClass = Seq("section-card")
    VBox.setVgrow(tableWrapper, Priority.Always)
    children = Seq(
      new Label("Family Request Records") { styleClass = Seq("section-card-title") },
      tableWrapper
    )

  // Bind requests buffer
  private val searchField = new TextField {
    promptText = "🔍 Search requests by family name..."
    styleClass = Seq("filter-field")
    maxWidth = Double.MaxValue
  }

  private val categoryFilterCombo = new ComboBox[String](Seq("All Categories") ++ FoodCategory.values.map(_.toString).toSeq) {
    value = "All Categories"
    prefWidth = 190
    minWidth = 170
  }

  private val dietaryFilterCombo = new ComboBox[String](Seq("All Dietary Needs") ++ DietaryRestriction.values.map(_.toString).toSeq) {
    value = "All Dietary Needs"
    prefWidth = 180
    minWidth = 160
  }

  private val addButton = new Button("Add Request"):
    styleClass = Seq("button", "button-primary")
    onAction = _ => performAddRequest()

  private val deleteButton = new Button("Delete Selected"):
    styleClass = Seq("button", "button-danger")
    minWidth = 138
    onAction = _ => performDeleteSelected()

  private val archiveButton = new Button("Archive Fulfilled"):
    styleClass = Seq("button", "button-secondary")
    minWidth = 146
    onAction = _ => performArchiveFulfilled()

  private val exportPdfBtn = new Button("📄 Export PDF Report"):
    styleClass = Seq("button", "button-secondary")
    minWidth = 135
    onAction = _ => performExportPdf()

  private val resetFilterBtn = new Button("🔄 Reset"):
    styleClass = Seq("filter-reset-btn")
    minWidth = 90
    onAction = _ => {
      searchField.text = ""
      categoryFilterCombo.value = "All Categories"
      dietaryFilterCombo.value = "All Dietary Needs"
    }

  HBox.setHgrow(searchField, Priority.Always)

  private val filterControlsRow = new HBox {
    spacing = 10
    alignment = scalafx.geometry.Pos.CenterLeft
    children = Seq(searchField, categoryFilterCombo, dietaryFilterCombo, resetFilterBtn)
  }

  private val filterBar = new VBox {
    spacing = 0
    styleClass = Seq("filter-bar", "filter-bar-stacked")
    children = Seq(filterControlsRow)
  }

  private val tableActionSpacer = new Region()
  HBox.setHgrow(tableActionSpacer, Priority.Always)

  private val tableActionsBar = new HBox {
    spacing = 10
    alignment = scalafx.geometry.Pos.CenterRight
    styleClass = Seq("table-actions-bar")
    children = Seq(
      new Label("Manage selected request") { styleClass = Seq("table-actions-label") },
      tableActionSpacer,
      deleteButton,
      archiveButton,
      exportPdfBtn
    )
  }

  private val filteredRequests = new FilteredBuffer[FamilyRequest](requests)
  private val sortedRequests = new SortedBuffer[FamilyRequest](filteredRequests)

  sortedRequests.delegate.comparatorProperty().bind(requestsTable.comparatorProperty)

  private def updateFilter(): Unit =
    val query = if searchField.text.value == null then "" else searchField.text.value.toLowerCase.trim
    val cat = categoryFilterCombo.value.value
    val diet = dietaryFilterCombo.value.value
    filteredRequests.predicate = { item =>
      val matchesSearch = query.isEmpty || item.familyName.toLowerCase.contains(query)
      val matchesCategory = cat == "All Categories" || item.requestedCategory.toString == cat
      val matchesDiet = diet == "All Dietary Needs" || item.dietaryRestriction.toString == diet
      matchesSearch && matchesCategory && matchesDiet
    }

  searchField.text.onChange { (_, _, _) => updateFilter() }
  categoryFilterCombo.value.onChange { (_, _, _) => updateFilter() }
  dietaryFilterCombo.value.onChange { (_, _, _) => updateFilter() }

  requestsTable.items = sortedRequests

  // Form Controls
  private val familyNameField = new TextField { promptText = "Family Name"; maxWidth = Double.MaxValue }
  private val sizeField = new TextField { promptText = "Size (e.g. 4)"; maxWidth = Double.MaxValue }
  private val restrictionCombo = new ComboBox[DietaryRestriction](DietaryRestriction.values.toIndexedSeq) { promptText = "Dietary Restriction"; maxWidth = Double.MaxValue }
  private val categoryCombo = new ComboBox[FoodCategory](FoodCategory.values.toIndexedSeq) { promptText = "Requested Category"; maxWidth = Double.MaxValue }
  
  private val statusLabel = new Label { styleClass = Seq("status-label", "status-error") }

  // Keyboard navigation Setup
  private def setupFormActions(submitAction: () => Unit): Unit =
    familyNameField.onAction = _ => submitAction()
    sizeField.onAction = _ => submitAction()

  private def performExportPdf(): Unit =
    Try {
      val doc = new org.apache.pdfbox.pdmodel.PDDocument()
      try
        val page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4)
        doc.addPage(page)
        
        val content = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)
        
        // Header
        content.beginText()
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16)
        content.newLineAtOffset(50, 780)
        content.showText("Food Pantry — Recipient Demand Report")
        content.endText()
        
        content.beginText()
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10)
        content.newLineAtOffset(50, 762)
        content.showText(s"Generated on: ${LocalDate.now().toString} | Total Requests: ${requests.size}")
        content.endText()
        
        // Table Column Headers
        val headerY = 730f
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10)
        content.beginText()
        content.newLineAtOffset(50, headerY)
        content.showText("ID")
        content.newLineAtOffset(70, 0)
        content.showText("Family Name")
        content.newLineAtOffset(150, 0)
        content.showText("Size")
        content.newLineAtOffset(50, 0)
        content.showText("Dietary Need")
        content.newLineAtOffset(110, 0)
        content.showText("Category")
        content.newLineAtOffset(110, 0)
        content.showText("Status")
        content.endText()
        
        // Divider line
        content.setLineWidth(1f)
        content.moveTo(50, headerY - 5)
        content.lineTo(545, headerY - 5)
        content.stroke()
        
        // Draw Rows
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9)
        requests.zipWithIndex.foreach { (req, idx) =>
          val rowY = headerY - 22f - (idx * 18f)
          if rowY > 50f then
            content.beginText()
            content.newLineAtOffset(50, rowY)
            content.showText(req.id)
            content.newLineAtOffset(70, 0)
            val nameText = if req.familyName.length > 20 then req.familyName.substring(0, 20) + "..." else req.familyName
            content.showText(nameText)
            content.newLineAtOffset(150, 0)
            content.showText(s"${req.householdSize} pax")
            content.newLineAtOffset(50, 0)
            content.showText(req.dietaryRestriction.toString)
            content.newLineAtOffset(110, 0)
            content.showText(req.requestedCategory.toString)
            content.newLineAtOffset(110, 0)
            content.showText(req.status.toString)
            content.endText()
        }
        
        content.close()
        val file = new java.io.File("demand_report.pdf")
        doc.save(file)
        UIUtils.applyStatus(statusLabel, "success", s"✓ PDF Report successfully exported to ${file.getAbsolutePath}!")
      finally
        doc.close()
    }.recover { case ex =>
      UIUtils.applyStatus(statusLabel, "error", s"✗ Error exporting PDF: ${ex.getMessage}")
    }

  private def performAddRequest(): Unit =
    statusLabel.text = ""
    
    val familyName = familyNameField.text.value.trim
    val sizeStr = sizeField.text.value.trim
    val diet = restrictionCombo.value.value
    val category = categoryCombo.value.value
    
    if familyName.isEmpty || sizeStr.isEmpty || diet == null || category == null then
      UIUtils.applyStatus(statusLabel, "error", "Error: All fields are required.")
    else
      val sizeOpt = scala.util.Try(sizeStr.toInt).toOption
      sizeOpt match
        case None =>
          UIUtils.applyStatus(statusLabel, "error", "Error: Household size must be a valid integer.")
        case Some(size) if size <= 0 =>
          UIUtils.applyStatus(statusLabel, "error", "Error: Household size must be positive.")
        case Some(size) =>
          val newRequest = FamilyRequest(
            id = s"req-${System.currentTimeMillis()}",
            familyName = familyName,
            householdSize = size,
            dietaryRestriction = diet,
            requestedCategory = category,
            status = RequestStatus.Pending
          )
          requests.add(newRequest)
          onSave()
          clearForm()
          UIUtils.applyStatus(statusLabel, "success", s"Success: Request for '$familyName' logged.")
          UIUtils.showToast(s"Logged demand request for '$familyName' (${size} members)", "success")

  private def performDeleteSelected(): Unit =
    val selectedItem = requestsTable.selectionModel.value.getSelectedItem
    if selectedItem != null then
      if UIUtils.showConfirmation("Confirm Deletion", "Delete Household Request", s"Are you sure you want to delete request for '${selectedItem.familyName}'?") then
        requests.remove(selectedItem)
        onSave()
        UIUtils.applyStatus(statusLabel, "success", s"Success: Deleted request for '${selectedItem.familyName}'.")
        UIUtils.showToast(s"Deleted demand request for '${selectedItem.familyName}'", "success")
    else
      UIUtils.applyStatus(statusLabel, "error", "Warning: Select a request in the table to delete.")

  private def performArchiveFulfilled(): Unit =
    val fulfilled = requests.filter(_.status == RequestStatus.Fulfilled).toList
    if fulfilled.nonEmpty then
      if UIUtils.showConfirmation("Confirm Archive", "Archive Fulfilled Requests", s"Are you sure you want to archive ${fulfilled.size} fulfilled household requests?") then
        requests --= fulfilled
        onSave()
        UIUtils.applyStatus(statusLabel, "success", s"Success: Archived ${fulfilled.size} fulfilled requests.")
        UIUtils.showToast(s"Archived ${fulfilled.size} fulfilled demand requests", "success")
    else
      UIUtils.applyStatus(statusLabel, "info", "Notice: No fulfilled requests to archive.")

  private def clearForm(): Unit =
    familyNameField.text = ""
    sizeField.text = ""
    restrictionCombo.value = null
    categoryCombo.value = null

  // Form Layout
  private val formContent = new GridPane:
    hgap = 14
    vgap = 10
    columnConstraints = Seq(
      new ColumnConstraints { minWidth = 100 },
      new ColumnConstraints { percentWidth = 34.0; hgrow = Priority.Always },
      new ColumnConstraints { minWidth = 104 },
      new ColumnConstraints { percentWidth = 34.0; hgrow = Priority.Always }
    )

    add(new Label("Family Name:") { styleClass = Seq("form-field-label") }, 0, 0)
    add(familyNameField, 1, 0)
    add(new Label("Household Size:") { styleClass = Seq("form-field-label") }, 2, 0)
    add(sizeField, 3, 0)
    add(new Label("Dietary Restr.:") { styleClass = Seq("form-field-label") }, 0, 1)
    add(restrictionCombo, 1, 1)
    add(new Label("Category:") { styleClass = Seq("form-field-label") }, 2, 1)
    add(categoryCombo, 3, 1)

  private val formContainer = new VBox:
    spacing = 12
    padding = Insets(18)
    styleClass = Seq("form-card", "card-color-demand")
    children = Seq(
      new Label("📝 Log Household Demand Request") { styleClass = Seq("form-card-title") },
      formContent,
      new HBox {
        spacing = 12
        alignment = Pos.CenterLeft
        children = Seq(addButton, statusLabel)
      }
    )

  children = Seq(
    headerBlock,
    filterBar,
    tableSection,
    tableActionsBar,
    formContainer
  )

  // Setup keyboard actions
  setupFormActions(() => performAddRequest())
