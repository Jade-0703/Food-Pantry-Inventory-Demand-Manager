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
  private val requestsTable: TableView[FamilyRequest] = new TableView[FamilyRequest]():
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("No pending family requests."):
      style = "-fx-text-fill: #64748b;"

    private val idCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Request ID"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "id", cellData.value.id)
      prefWidth = 100
      
    private val nameCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Family Name"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "name", cellData.value.familyName)
      prefWidth = 160
      
    private val sizeCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Household Size"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "size", cellData.value.householdSize.toString)
      prefWidth = 110
      
    private val restrictionCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Dietary Restriction"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "diet", cellData.value.dietaryRestriction.displayName)
      prefWidth = 140
      cellFactory = UIUtils.createBadgeCellFactory(UIUtils.getDietaryLabel)

    private val categoryCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Category Requested"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "category", cellData.value.requestedCategory.toString)
      prefWidth = 140

    private val statusCol: TableColumn[FamilyRequest, String] = new TableColumn[FamilyRequest, String]("Status"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "status", cellData.value.status.toString)
      prefWidth = 110
      cellFactory = UIUtils.createBadgeCellFactory(UIUtils.getStatusLabel)

    columns ++= Seq(idCol, nameCol, sizeCol, restrictionCol, categoryCol, statusCol)
    columns.foreach(_.setReorderable(false))
    prefHeight <== scalafx.beans.binding.Bindings.createDoubleBinding(
      () =>
        val rowCount = items.value.size()
        if rowCount == 0 then 100.0
        else math.min((rowCount * 40.0) + 45.0, 360.0),
      items
    )

  requestsTable.clip = UIUtils.createRoundedClip(requestsTable)

  private val tableWrapper = new StackPane:
    styleClass = Seq("table-wrapper")
    children = Seq(requestsTable)

  private val tableSectionTitle = new Label("Family Request Records"):
    styleClass = Seq("section-card-title")

  private val tableSection = new VBox:
    spacing = 10
    styleClass = Seq("section-card")
    VBox.setVgrow(tableWrapper, Priority.Always)
    children = Seq(
      tableSectionTitle,
      tableWrapper
    )

  // Bind requests buffer
  private val searchField = new TextField():
    promptText = "🔍 Search requests by family name..."
    styleClass = Seq("filter-field")
    maxWidth = Double.MaxValue

  private val categoryFilterCombo = new ComboBox[String](Seq("All Categories") ++ FoodCategory.values.map(_.toString).toSeq):
    value = "All Categories"
    prefWidth = 190
    minWidth = 170

  private val dietaryFilterCombo = new ComboBox[String](Seq("All Dietary Needs") ++ DietaryRestriction.values.map(_.displayName).toSeq):
    value = "All Dietary Needs"
    prefWidth = 180
    minWidth = 160

  private val addButton = new Button("Add Request"):
    styleClass = Seq("button", "button-primary")
    onAction = _ => performAddRequest()

  private val editButton = new Button("Edit Selected"):
    styleClass = Seq("button", "button-secondary")
    minWidth = 130
    onAction = _ => performEditSelected()

  private val deleteButton = new Button("Delete Selected"):
    styleClass = Seq("button", "button-danger")
    minWidth = 138
    onAction = _ => performDeleteSelected()

  private val archiveButton = new Button("Archive Fulfilled"):
    styleClass = Seq("button", "button-secondary")
    minWidth = 146
    onAction = _ => performArchiveFulfilled()

  private val exportPdfBtn = new Button("Export PDF Report"):
    styleClass = Seq("button", "button-secondary")
    minWidth = 135
    onAction = _ => performExportPdf()

  private val resetFilterBtn = new Button("Reset"):
    styleClass = Seq("filter-reset-btn")
    minWidth = 90
    onAction = _ =>
      searchField.text = ""
      categoryFilterCombo.value = "All Categories"
      dietaryFilterCombo.value = "All Dietary Needs"

  HBox.setHgrow(searchField, Priority.Always)

  private val filterBar = UIUtils.createFilterBar(searchField, categoryFilterCombo, dietaryFilterCombo, resetFilterBtn)
  private val tableActionsBar = UIUtils.createTableActionsBar("Manage selected request", editButton, deleteButton, archiveButton, exportPdfBtn)

  private val filteredRequests = new FilteredBuffer[FamilyRequest](requests)
  private val sortedRequests = new SortedBuffer[FamilyRequest](filteredRequests)

  UIUtils.bindTableSorter(requestsTable, sortedRequests)

  private def updateFilter(): Unit =
    val query = UIUtils.getSearchQuery(searchField)
    val cat = categoryFilterCombo.value.value
    val diet = dietaryFilterCombo.value.value
    filteredRequests.predicate = { item =>
      val matchesSearch = query.isEmpty || item.familyName.toLowerCase.contains(query)
      val matchesCategory = cat == "All Categories" || item.requestedCategory.toString == cat
      val matchesDiet = diet == "All Dietary Needs" || item.dietaryRestriction.displayName == diet
      matchesSearch && matchesCategory && matchesDiet
    }

  UIUtils.bindFilterTriggers(() => updateFilter(), searchField.text, categoryFilterCombo.value, dietaryFilterCombo.value)

  requestsTable.items = sortedRequests

  // Form Controls
  private val familyNameField = new TextField():
    promptText = "Family Name"
    maxWidth = Double.MaxValue

  private val sizeField = new TextField():
    promptText = "Size (e.g. 4)"
    maxWidth = Double.MaxValue

  private val restrictionCombo = new ComboBox[DietaryRestriction](DietaryRestriction.values.toIndexedSeq):
    promptText = "Dietary Restriction"
    maxWidth = Double.MaxValue
    converter = scalafx.util.StringConverter.toStringConverter((d: DietaryRestriction) => if d != null then d.displayName else "")

  private val categoryCombo = new ComboBox[FoodCategory](FoodCategory.values.toIndexedSeq):
    promptText = "Requested Category"
    maxWidth = Double.MaxValue

  private val statusLabel = new Label():
    styleClass = Seq("status-label", "status-error")

  // Keyboard navigation Setup
  private def setupFormActions(submitAction: () => Unit): Unit =
    familyNameField.onAction = _ => submitAction()
    sizeField.onAction = _ => submitAction()

  private def performExportPdf(): Unit =
    Try {
      val doc = new org.apache.pdfbox.pdmodel.PDDocument()
      try
        val page = new org.apache.pdfbox.pdmodel.PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(org.apache.pdfbox.pdmodel.common.PDRectangle.A4.getHeight, org.apache.pdfbox.pdmodel.common.PDRectangle.A4.getWidth))
        doc.addPage(page)
        
        val content = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)
        
        // Header
        content.beginText()
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16)
        content.newLineAtOffset(40, 545)
        content.showText("FOOD PANTRY — RECIPIENT DEMAND REPORT")
        content.endText()
        
        content.beginText()
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10)
        content.newLineAtOffset(40, 528)
        content.showText(s"Generated on: ${LocalDate.now().toString} | Author: Jade Wenxi (ID: 23093495) | Total Requests: ${requests.size}")
        content.endText()
        
        // Table Column Headers
        val headerY = 490f
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10)
        content.beginText()
        content.newLineAtOffset(40, headerY)
        content.showText("Request ID")
        content.newLineAtOffset(80, 0)
        content.showText("Family Name")
        content.newLineAtOffset(160, 0)
        content.showText("Household Size")
        content.newLineAtOffset(100, 0)
        content.showText("Dietary Need")
        content.newLineAtOffset(140, 0)
        content.showText("Category Requested")
        content.newLineAtOffset(140, 0)
        content.showText("Status")
        content.endText()
        
        // Divider line
        content.setLineWidth(1.0f)
        content.moveTo(40, headerY - 6f)
        content.lineTo(760, headerY - 6f)
        content.stroke()
        
        // Draw Rows
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9.5f)
        requests.zipWithIndex.foreach { (req, idx) =>
          val rowY = headerY - 22f - (idx * 20f)
          if rowY > 40f then
            content.beginText()
            content.newLineAtOffset(40, rowY)
            content.showText(req.id)
            content.newLineAtOffset(80, 0)
            content.showText(req.familyName)
            content.newLineAtOffset(160, 0)
            content.showText(s"${req.householdSize} members")
            content.newLineAtOffset(100, 0)
            content.showText(req.dietaryRestriction.displayName)
            content.newLineAtOffset(140, 0)
            content.showText(req.requestedCategory.toString)
            content.newLineAtOffset(140, 0)
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
      clearForm()
    else
      val sizeOpt = scala.util.Try(sizeStr.toInt).toOption
      sizeOpt match
        case None =>
          UIUtils.applyStatus(statusLabel, "error", "Error: Household size must be a valid integer.")
          clearForm()
        case Some(size) if size <= 0 =>
          UIUtils.applyStatus(statusLabel, "error", "Error: Household size must be positive.")
          clearForm()
        case Some(size) =>
          val maxId = requests.map(_.id).collect {
            case id if id.startsWith("REQ-") => scala.util.Try(id.stripPrefix("REQ-").toInt).getOrElse(0)
            case id if id.startsWith("req-") => scala.util.Try(id.stripPrefix("req-").toInt).getOrElse(0)
          }.maxOption.getOrElse(0)
          val newRequest = FamilyRequest(
            id = f"REQ-${maxId + 1}%03d",
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
          UIUtils.showToast(s"Logged demand request for '$familyName' (${size} members)")

  private def performEditSelected(): Unit =
    val selectedItem = requestsTable.selectionModel.value.getSelectedItem
    if selectedItem != null then
      val saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OKDone)
      val dialog = new Dialog[ButtonType]():
        title = "Edit Family Request"
        headerText = s"Edit Demand Request: ${selectedItem.familyName} (${selectedItem.id})"
      dialog.initOwner(MainApp.stage)

      val editNameField = new TextField():
        text = selectedItem.familyName
      val editSizeField = new TextField():
        text = selectedItem.householdSize.toString
      val editDietCombo = new ComboBox[DietaryRestriction](DietaryRestriction.values.toIndexedSeq):
        value = selectedItem.dietaryRestriction
        converter = scalafx.util.StringConverter.toStringConverter((d: DietaryRestriction) => if d != null then d.displayName else "")
      val editCatCombo = new ComboBox[FoodCategory](FoodCategory.values.toIndexedSeq):
        value = selectedItem.requestedCategory
      val editStatusCombo = new ComboBox[RequestStatus](RequestStatus.values.toIndexedSeq):
        value = selectedItem.status

      val grid = new GridPane():
        hgap = 10
        vgap = 10
        padding = Insets(20)
        add(new Label("Family Name:"), 0, 0)
        add(editNameField, 1, 0)
        add(new Label("Household Size:"), 0, 1)
        add(editSizeField, 1, 1)
        add(new Label("Dietary Restriction:"), 0, 2)
        add(editDietCombo, 1, 2)
        add(new Label("Category:"), 0, 3)
        add(editCatCombo, 1, 3)
        add(new Label("Status:"), 0, 4)
        add(editStatusCombo, 1, 4)

      dialog.dialogPane().content = grid
      dialog.dialogPane().buttonTypes = Seq(saveButtonType, ButtonType.Cancel)
      dialog.resultConverter = btn => btn

      val result = dialog.showAndWait()
      if result.contains(saveButtonType) then
        val name = editNameField.text.value.trim
        val sizeOpt = Try(editSizeField.text.value.trim.toInt).toOption
        val diet = editDietCombo.delegate.getValue
        val cat  = editCatCombo.delegate.getValue
        val stat = editStatusCombo.delegate.getValue
        if name.nonEmpty && sizeOpt.exists(_ > 0) && diet != null && cat != null && stat != null then
          val updated = selectedItem.copy(
            familyName         = name,
            householdSize      = sizeOpt.get,
            dietaryRestriction = diet,
            requestedCategory  = cat,
            status             = stat
          )
          val idx = requests.indexOf(selectedItem)
          if idx >= 0 then
            requests.update(idx, updated)
            onSave()
            UIUtils.applyStatus(statusLabel, "success", s"Success: Updated request for '$name'.")
            UIUtils.showToast(s"Updated request for '$name'")
        else
          UIUtils.applyStatus(statusLabel, "error", "Error: Invalid inputs for edit.")

    else
      UIUtils.applyStatus(statusLabel, "error", "Warning: Select a request in the table to edit.")

  private def performDeleteSelected(): Unit =
    val selectedItem = requestsTable.selectionModel.value.getSelectedItem
    if selectedItem != null then
      if UIUtils.showConfirmation("Confirm Deletion", "Delete Family Demand Request", s"Are you sure you want to delete the request for '${selectedItem.familyName}'?") then
        requests.remove(selectedItem)
        onSave()
        UIUtils.applyStatus(statusLabel, "success", s"Success: Deleted request for '${selectedItem.familyName}'.")
        UIUtils.showToast(s"Deleted demand request for '${selectedItem.familyName}'")
    else
      UIUtils.applyStatus(statusLabel, "error", "Warning: Select a request in the table to delete.")

  private def performArchiveFulfilled(): Unit =
    val fulfilled = requests.filter(_.status == RequestStatus.Fulfilled)
    if fulfilled.nonEmpty then
      if UIUtils.showConfirmation("Archive Requests", "Archive Fulfilled Demand Requests", s"Are you sure you want to remove ${fulfilled.size} fulfilled request(s)?") then
        requests.removeAll(fulfilled)
        onSave()
        UIUtils.applyStatus(statusLabel, "success", s"Success: Archived ${fulfilled.size} fulfilled requests.")
        UIUtils.showToast(s"Archived ${fulfilled.size} fulfilled demand requests")
    else
      UIUtils.applyStatus(statusLabel, "info", "Notice: No fulfilled requests to archive.")

  private def clearForm(): Unit =
    familyNameField.text = ""
    sizeField.text = ""
    restrictionCombo.value = null
    categoryCombo.value = null

  private val formContent = new GridPane:
    hgap = 14
    vgap = 10
    columnConstraints = Seq(
      UIUtils.createColumnConstraints(minW = 100),
      UIUtils.createColumnConstraints(percentW = 34.0, grow = Priority.Always),
      UIUtils.createColumnConstraints(minW = 104),
      UIUtils.createColumnConstraints(percentW = 34.0, grow = Priority.Always)
    )

    add(UIUtils.createFormFieldLabel("Family Name:"), 0, 0)
    add(familyNameField, 1, 0)
    add(UIUtils.createFormFieldLabel("Household Size:"), 2, 0)
    add(sizeField, 3, 0)
    add(UIUtils.createFormFieldLabel("Dietary Restriction:"), 0, 1)
    add(restrictionCombo, 1, 1)
    add(UIUtils.createFormFieldLabel("Category:"), 2, 1)
    add(categoryCombo, 3, 1)

  private val formTitle = new Label("📝 Log Household Demand Request"):
    styleClass = Seq("form-card-title")

  private val formFooterRow = new HBox:
    spacing = 12
    alignment = Pos.CenterLeft
    children = Seq(addButton, statusLabel)

  private val formContainer = new VBox:
    spacing = 12
    padding = Insets(18)
    styleClass = Seq("form-card", "card-color-demand")
    children = Seq(
      formTitle,
      formContent,
      formFooterRow
    )

  setupFormActions(() => performAddRequest())

  children = Seq(
    headerBlock,
    filterBar,
    tableSection,
    tableActionsBar,
    formContainer
  )
