package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.geometry.{Insets, Pos}
import scalafx.Includes._
import java.time.LocalDate
import scala.util.Try

class InventoryView(
  inventory: ObservableBuffer[FoodItem],
  onSave: () => Unit
) extends VBox:

  spacing = 15
  padding = Insets(20)
  styleClass = Seq("content-pane")

  private val headerBlock = UIUtils.createPageHeader(
    "Pantry Inventory Log",
    "Add, view, and delete food items. Form inputs switch dynamically between perishable and non-perishable."
  )

  // Inventory Table
  private val inventoryTable: TableView[FoodItem] = new TableView[FoodItem]():
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("No items in inventory."):
      style = "-fx-text-fill: #64748b;"

    private val idCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("ID"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "id", cellData.value.id)
      prefWidth = 60
      
    private val nameCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Name"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "name", cellData.value.name)
      prefWidth = 180
      cellFactory = (col: TableColumn[FoodItem, String]) =>
        new TableCell[FoodItem, String]:
          item.onChange { (_, _, newText) =>
            text = newText
            tooltip = if newText != null && newText.nonEmpty then new Tooltip(newText) else null
          }
      
    private val categoryCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Category"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "category", cellData.value.category.toString)
      prefWidth = 100
      
    private val qtyCol: TableColumn[FoodItem, FoodItem] = new TableColumn[FoodItem, FoodItem]("Quantity"):
      cellValueFactory = cellData => new scalafx.beans.property.ObjectProperty(this, "quantityItem", cellData.value)
      prefWidth = 100
      cellFactory = (col: TableColumn[FoodItem, FoodItem]) =>
        new TableCell[FoodItem, FoodItem]:
          item.onChange { (_, _, foodItem) =>
            if foodItem != null then
              val displayStr = s"${foodItem.quantity} ${foodItem.unit}"
              if foodItem.quantity < 5.0 then
                graphic = UIUtils.getLowStockLabel(displayStr, foodItem.quantity)
                text = null
                alignment = scalafx.geometry.Pos.Center
              else
                graphic = null
                text = displayStr
            else
              graphic = null
              text = null
          }

    private val perishableCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Perishable?"):
      cellValueFactory = cellData => new scalafx.beans.property.StringProperty(this, "isPerishable", if cellData.value.isPerishable then "Yes" else "No")
      prefWidth = 100
      cellFactory = UIUtils.createBadgeCellFactory(UIUtils.getPerishableLabel)

    private val detailCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Expiry / Shelf Life"):
      cellValueFactory = cellData => 
        val text = cellData.value.getExpiryStatus(LocalDate.now())
        new scalafx.beans.property.StringProperty(this, "detail", text)
      prefWidth = 180
      cellFactory = UIUtils.createBadgeCellFactory(UIUtils.getExpiryLabel)

    columns ++= Seq(idCol, nameCol, categoryCol, qtyCol, perishableCol, detailCol)
    columns.foreach(_.setReorderable(false))
    prefHeight <== scalafx.beans.binding.Bindings.createDoubleBinding(
      () =>
        val rowCount = items.value.size()
        if rowCount == 0 then 100.0
        else math.min((rowCount * 40.0) + 45.0, 360.0),
      items
    )

  inventoryTable.clip = UIUtils.createRoundedClip(inventoryTable)

  private val tableWrapper = new StackPane:
    styleClass = Seq("table-wrapper")
    children = Seq(inventoryTable)

  private val tableSectionTitle = new Label("Inventory Items"):
    styleClass = Seq("section-card-title")

  private val tableSection = new VBox:
    spacing = 10
    styleClass = Seq("section-card")
    VBox.setVgrow(tableWrapper, Priority.Always)
    children = Seq(
      tableSectionTitle,
      tableWrapper
    )

  // Bind repository items to the table
  private val searchField = new TextField():
    promptText = "🔍 Search inventory by name..."
    styleClass = Seq("filter-field")
    maxWidth = Double.MaxValue

  private val categoryFilterCombo = new ComboBox[String](Seq("All Categories") ++ FoodCategory.values.map(_.toString).toSeq):
    value = "All Categories"
    prefWidth = 190
    minWidth = 170

  private val addButton = new Button("Add Item"):
    styleClass = Seq("button", "button-primary")
    onAction = _ => performAddItem()

  private val editButton = new Button("Edit Selected"):
    styleClass = Seq("button", "button-secondary")
    minWidth = 130
    onAction = _ => performEditSelected()

  private val deleteButton = new Button("Delete Selected"):
    styleClass = Seq("button", "button-danger")
    minWidth = 132
    onAction = _ => performDeleteSelected()

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

  HBox.setHgrow(searchField, Priority.Always)

  private val filterBar = UIUtils.createFilterBar(searchField, categoryFilterCombo, resetFilterBtn)
  private val tableActionsBar = UIUtils.createTableActionsBar("Manage selected inventory item", editButton, deleteButton, exportPdfBtn)

  private val filteredInventory = new FilteredBuffer[FoodItem](inventory)
  private val sortedInventory = new SortedBuffer[FoodItem](filteredInventory)

  UIUtils.bindTableSorter(inventoryTable, sortedInventory)

  private def updateFilter(): Unit =
    val query = UIUtils.getSearchQuery(searchField)
    val cat = categoryFilterCombo.value.value
    filteredInventory.predicate = { item =>
      val matchesSearch = query.isEmpty || item.name.toLowerCase.contains(query)
      val matchesCategory = cat == "All Categories" || item.category.toString == cat
      matchesSearch && matchesCategory
    }

  UIUtils.bindFilterTriggers(() => updateFilter(), searchField.text, categoryFilterCombo.value)

  inventoryTable.items = sortedInventory

  // Form Controls
  private val nameField = new TextField():
    promptText = "Item Name"
    maxWidth = Double.MaxValue

  private val categoryCombo = new ComboBox[FoodCategory](FoodCategory.values.toIndexedSeq):
    promptText = "Select Category"
    maxWidth = Double.MaxValue

  private val qtyField = new TextField():
    promptText = "Qty (e.g. 5.0)"
    maxWidth = Double.MaxValue

  private val unitField = new TextField():
    promptText = "Unit (e.g. kg)"
    maxWidth = Double.MaxValue
  
  private val itemTypeCombo = new ComboBox[String](Seq("Perishable", "Non-Perishable")):
    value = "Perishable"
    maxWidth = Double.MaxValue

  private val expiryDatePicker = new DatePicker():
    promptText = "Expiry Date"
    maxWidth = Double.MaxValue
    value = LocalDate.now().plusDays(7)

  private val shelfLifeField = new TextField():
    promptText = "Shelf Life (months)"
    maxWidth = Double.MaxValue
    visible = false

  private val expiryLabel = UIUtils.createFormFieldLabel("Expiry Date:")
  private val shelfLifeLabel = UIUtils.createFormFieldLabel("Shelf Life (Mo.):")
  shelfLifeLabel.visible = false

  expiryDatePicker.managed <== expiryDatePicker.visible
  expiryLabel.managed <== expiryLabel.visible
  shelfLifeField.managed <== shelfLifeField.visible
  shelfLifeLabel.managed <== shelfLifeLabel.visible

  private val statusLabel = new Label():
    styleClass = Seq("status-label", "status-error")

  private def updateTypeVisibility(typeVal: String): Unit =
    val isPerishable = typeVal == "Perishable"
    expiryDatePicker.visible = isPerishable
    expiryLabel.visible = isPerishable
    shelfLifeField.visible = !isPerishable
    shelfLifeLabel.visible = !isPerishable

  // Toggle input visibility based on perishable selection
  itemTypeCombo.value.onChange { (_, _, newType) =>
    if newType != null then updateTypeVisibility(newType)
  }
  itemTypeCombo.onAction = _ =>
    val current = itemTypeCombo.value.value
    if current != null then updateTypeVisibility(current)

  // Keyboard navigation & submission setup
  private def setupFormActions(submitAction: () => Unit): Unit =
    nameField.onAction = _ => submitAction()
    qtyField.onAction = _ => submitAction()
    unitField.onAction = _ => submitAction()
    shelfLifeField.onAction = _ => submitAction()

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
        content.showText("FOOD PANTRY — INVENTORY STOCK REPORT")
        content.endText()
        
        content.beginText()
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10)
        content.newLineAtOffset(40, 528)
        content.showText(s"Generated on: ${LocalDate.now().toString} | Author: Jade Wenxi (ID: 23093495) | Total Items: ${inventory.size}")
        content.endText()
        
        // Table Column Headers
        val headerY = 490f
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10)
        content.beginText()
        content.newLineAtOffset(40, headerY)
        content.showText("ID")
        content.newLineAtOffset(90, 0)
        content.showText("Item Name")
        content.newLineAtOffset(240, 0)
        content.showText("Category")
        content.newLineAtOffset(140, 0)
        content.showText("Quantity")
        content.newLineAtOffset(140, 0)
        content.showText("Status")
        content.endText()
        
        // Divider line
        content.setLineWidth(1f)
        content.moveTo(40, headerY - 6)
        content.lineTo(800, headerY - 6)
        content.stroke()
        
        // Draw Rows
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9.5f)
        inventory.zipWithIndex.foreach { (item, idx) =>
          val rowY = headerY - 22f - (idx * 20f)
          if rowY > 40f then
            content.beginText()
            content.newLineAtOffset(40, rowY)
            content.showText(item.id)
            content.newLineAtOffset(90, 0)
            content.showText(item.name)
            content.newLineAtOffset(240, 0)
            content.showText(item.category.toString)
            content.newLineAtOffset(140, 0)
            content.showText(s"${item.quantity} ${item.unit}")
            content.newLineAtOffset(140, 0)
            content.showText(item.getExpiryStatus(LocalDate.now()))
            content.endText()
        }
        
        content.close()
        val file = new java.io.File("inventory_report.pdf")
        doc.save(file)
        UIUtils.applyStatus(statusLabel, "success", s"✓ PDF Report successfully exported to ${file.getAbsolutePath}!")
      finally
        doc.close()
    }.recover { case ex =>
      UIUtils.applyStatus(statusLabel, "error", s"✗ Error exporting PDF: ${ex.getMessage}")
    }

  private def performAddItem(): Unit =
    statusLabel.text = ""
    
    val name = nameField.text.value.trim
    val category = categoryCombo.value.value
    val qtyStr = qtyField.text.value.trim
    val unit = unitField.text.value.trim
    val itemType = itemTypeCombo.value.value

    // S1-18 Invalid-input handling 1: Check empty fields
    if name.isEmpty then
      UIUtils.applyStatus(statusLabel, "error", "Error: Item Name cannot be empty!")
      clearForm()
    else if category == null then
      UIUtils.applyStatus(statusLabel, "error", "Error: Please select a Food Category!")
      clearForm()
    else if qtyStr.isEmpty then
      UIUtils.applyStatus(statusLabel, "error", "Error: Quantity field cannot be empty!")
      clearForm()
    else if unit.isEmpty then
      UIUtils.applyStatus(statusLabel, "error", "Error: Unit field cannot be empty!")
      clearForm()
    else
      // S1-12 & S1-18 Invalid-input handling 2: Parse numeric quantity safely
      Try(qtyStr.toDouble).toOption match
        case None =>
          UIUtils.applyStatus(statusLabel, "error", "Error: Quantity must be a valid decimal number!")
          clearForm()
        case Some(quantity) if quantity <= 0 =>
          UIUtils.applyStatus(statusLabel, "error", "Error: Quantity must be a positive number (> 0)!")
          clearForm()
        case Some(quantity) =>
          if itemType == "Perishable" then
            val expiryVal = expiryDatePicker.delegate.getValue
            if expiryVal == null then
              UIUtils.applyStatus(statusLabel, "error", "Error: Please select an Expiry Date!")
              clearForm()
            else
              // Create perishable item
              val maxId = inventory.map(_.id).collect {
                case id if id.startsWith("inv-") => scala.util.Try(id.stripPrefix("inv-").toInt).getOrElse(0)
              }.maxOption.getOrElse(0)
              val nextId = s"inv-${maxId + 1}"
              val newItem = PerishableItem(nextId, name, category, quantity, unit, expiryVal)
              inventory.add(newItem)
              onSave()
              clearForm()
              UIUtils.applyStatus(statusLabel, "success", s"Success: Added perishable item '$name'.")
              UIUtils.showToast(s"Added perishable item '$name' ($quantity $unit)")
          else
            val shelfLifeStr = shelfLifeField.text.value.trim
            // S1-12 & S1-18 Invalid-input handling 3: Parse integer shelf life safely
            Try(shelfLifeStr.toInt).toOption match
              case None =>
                UIUtils.applyStatus(statusLabel, "error", "Error: Shelf Life must be a valid integer number of months!")
              case Some(months) if months <= 0 =>
                UIUtils.applyStatus(statusLabel, "error", "Error: Shelf Life must be a positive number of months (> 0)!")
              case Some(months) =>
                val maxId = inventory.map(_.id).collect {
                  case id if id.startsWith("inv-") => scala.util.Try(id.stripPrefix("inv-").toInt).getOrElse(0)
                }.maxOption.getOrElse(0)
                val nextId = s"inv-${maxId + 1}"
                val newItem = NonPerishableItem(nextId, name, category, quantity, unit, months)
                inventory.add(newItem)
                onSave()
                clearForm()
                UIUtils.applyStatus(statusLabel, "success", s"Success: Added non-perishable item '$name'.")
                UIUtils.showToast(s"Added non-perishable item '$name' ($quantity $unit)")

  private def performEditSelected(): Unit =
    val selectedItem = inventoryTable.selectionModel.value.getSelectedItem
    if selectedItem != null then
      val saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OKDone)
      val dialog = new Dialog[ButtonType]():
        title = "Edit Inventory Item"
        headerText = s"Edit Item: ${selectedItem.name} (${selectedItem.id})"
      dialog.initOwner(MainApp.stage)

      val editNameField = new TextField():
        text = selectedItem.name
      val editCategoryCombo = new ComboBox[FoodCategory](FoodCategory.values.toIndexedSeq):
        value = selectedItem.category
      val editQtyField = new TextField():
        text = selectedItem.quantity.toString
      val editUnitField = new TextField():
        text = selectedItem.unit
      
      val editTypeCombo = new ComboBox[String](Seq("Perishable", "Non-Perishable")):
        value = selectedItem match
          case _: PerishableItem => "Perishable"
          case _: NonPerishableItem => "Non-Perishable"

      val editDatePicker = new DatePicker():
        value = selectedItem match
          case p: PerishableItem => p.expiryDate
          case _ => LocalDate.now().plusDays(7)

      val editShelfLifeField = new TextField():
        text = selectedItem match
          case np: NonPerishableItem => np.shelfLifeMonths.toString
          case _ => "12"

      val grid = new GridPane():
        hgap = 10
        vgap = 10
        padding = Insets(20)
        add(new Label("Item Name:"), 0, 0)
        add(editNameField, 1, 0)
        add(new Label("Category:"), 0, 1)
        add(editCategoryCombo, 1, 1)
        add(new Label("Quantity:"), 0, 2)
        add(editQtyField, 1, 2)
        add(new Label("Unit:"), 0, 3)
        add(editUnitField, 1, 3)
        add(new Label("Item Type:"), 0, 4)
        add(editTypeCombo, 1, 4)
        add(new Label("Expiry Date:"), 0, 5)
        add(editDatePicker, 1, 5)
        add(new Label("Shelf Life (Mo.):"), 0, 6)
        add(editShelfLifeField, 1, 6)

      dialog.dialogPane().content = grid
      dialog.dialogPane().buttonTypes = Seq(saveButtonType, ButtonType.Cancel)
      dialog.resultConverter = btn => btn

      val result = dialog.showAndWait()
      if result.contains(saveButtonType) then
        val name     = editNameField.text.value.trim
        val qtyOpt   = Try(editQtyField.text.value.trim.toDouble).toOption
        val unit     = editUnitField.text.value.trim
        val cat      = editCategoryCombo.delegate.getValue
        val itemType = editTypeCombo.delegate.getValue

        if name.nonEmpty && cat != null && itemType != null && qtyOpt.exists(_ > 0) && unit.nonEmpty then
          val updatedItem: FoodItem = if itemType == "Perishable" then
            val exp = if editDatePicker.delegate.getValue != null then editDatePicker.delegate.getValue else LocalDate.now().plusDays(7)
            PerishableItem(selectedItem.id, name, cat, qtyOpt.get, unit, exp)
          else
            val sl = Try(editShelfLifeField.text.value.trim.toInt).getOrElse(12)
            NonPerishableItem(selectedItem.id, name, cat, qtyOpt.get, unit, sl)

          val idx = inventory.indexOf(selectedItem)
          if idx >= 0 then
            inventory.update(idx, updatedItem)
            onSave()
            UIUtils.applyStatus(statusLabel, "success", s"Success: Updated inventory item '$name'.")
            UIUtils.showToast(s"Updated item '$name' in inventory")
        else
          UIUtils.applyStatus(statusLabel, "error", "Error: Invalid inputs for item edit.")

    else
      UIUtils.applyStatus(statusLabel, "error", "Warning: Select an item in the table to edit.")

  private def performDeleteSelected(): Unit =
    val selectedItem = inventoryTable.selectionModel.value.getSelectedItem
    if selectedItem != null then
      if UIUtils.showConfirmation("Confirm Deletion", "Delete Inventory Item", s"Are you sure you want to delete '${selectedItem.name}' from inventory?") then
        inventory.remove(selectedItem)
        onSave()
        UIUtils.applyStatus(statusLabel, "success", s"Success: Deleted item '${selectedItem.name}'.")
        UIUtils.showToast(s"Deleted item '${selectedItem.name}' from stock")
    else
      UIUtils.applyStatus(statusLabel, "error", "Warning: Select an item in the table to delete.")

  private def clearForm(): Unit =
    nameField.text = ""
    qtyField.text = ""
    unitField.text = ""
    shelfLifeField.text = ""
    categoryCombo.value = null
    expiryDatePicker.value = LocalDate.now().plusDays(7)

  // Form Layout
  private val formContent = new GridPane:
    hgap = 14
    vgap = 10
    columnConstraints = Seq(
      UIUtils.createColumnConstraints(minW = 72),
      UIUtils.createColumnConstraints(percentW = 36.0, grow = Priority.Always),
      UIUtils.createColumnConstraints(minW = 92),
      UIUtils.createColumnConstraints(percentW = 36.0, grow = Priority.Always)
    )

    add(UIUtils.createFormFieldLabel("Item Name:"), 0, 0)
    add(nameField, 1, 0)
    add(UIUtils.createFormFieldLabel("Category:"), 2, 0)
    add(categoryCombo, 3, 0)
    add(UIUtils.createFormFieldLabel("Quantity:"), 0, 1)
    add(qtyField, 1, 1)
    add(UIUtils.createFormFieldLabel("Unit:"), 2, 1)
    add(unitField, 3, 1)
    add(UIUtils.createFormFieldLabel("Type:"), 0, 2)
    add(itemTypeCombo, 1, 2)
    add(expiryLabel, 2, 2)
    add(expiryDatePicker, 3, 2)
    add(shelfLifeLabel, 2, 2)
    add(shelfLifeField, 3, 2)

  private val formTitle = new Label("➕ Add New Inventory Item"):
    styleClass = Seq("form-card-title")

  private val formFooterRow = new HBox:
    spacing = 12
    alignment = Pos.CenterLeft
    children = Seq(addButton, statusLabel)

  private val formContainer = new VBox:
    spacing = 12
    padding = Insets(18)
    styleClass = Seq("form-card", "card-color-inventory")
    children = Seq(
      formTitle,
      formContent,
      formFooterRow
    )

  children = Seq(
    headerBlock,
    filterBar,
    tableSection,
    tableActionsBar,
    formContainer
  )

  // Setup key listener actions for Form
  setupFormActions(() => performAddItem())
