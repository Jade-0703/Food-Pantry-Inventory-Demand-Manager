package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.collections.transformation.{FilteredBuffer, SortedBuffer}
import scalafx.geometry.{Insets, Pos}
import scalafx.Includes._
import java.time.LocalDate
import scala.util.Try

@annotation.nowarn("cat=deprecation")
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
  private val inventoryTable = new TableView[FoodItem]:
    val selfTable: TableView[FoodItem] = this
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("No items in inventory.") { style = "-fx-text-fill: #64748b;" }

    // S1-14 / Entry 14 clip layout to prevent row background bleed
    clip = UIUtils.createRoundedClip(selfTable)

    private val idCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("ID"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "id", cellData.value.id) }
      prefWidth = 60
      
    private val nameCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Name"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "name", cellData.value.name) }
      prefWidth = 180
      cellFactory = { (col: TableColumn[FoodItem, String]) =>
        new TableCell[FoodItem, String] {
          item.onChange { (_, _, newText) =>
            text = newText
            tooltip = if newText != null && newText.nonEmpty then new Tooltip(newText) else null
          }
        }
      }
      
    private val categoryCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Category"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "category", cellData.value.category.toString) }
      prefWidth = 100
      
    private val qtyCol: TableColumn[FoodItem, FoodItem] = new TableColumn[FoodItem, FoodItem]("Quantity"):
      cellValueFactory = { cellData => new scalafx.beans.property.ObjectProperty(this, "quantityItem", cellData.value) }
      prefWidth = 100
      cellFactory = { (col: TableColumn[FoodItem, FoodItem]) =>
        new TableCell[FoodItem, FoodItem] {
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
        }
      }

    private val perishableCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Perishable?"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "isPerishable", if cellData.value.isPerishable then "Yes" else "No") }
      prefWidth = 100
      cellFactory = { (col: TableColumn[FoodItem, String]) =>
        new TableCell[FoodItem, String] {
          item.onChange { (_, _, newText) =>
            if newText != null then
              graphic = UIUtils.getPerishableLabel(newText)
              text = null
              alignment = scalafx.geometry.Pos.Center
            else
              graphic = null
              text = null
          }
        }
      }

    private val detailCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Expiry / Shelf Life"):
      cellValueFactory = { cellData => 
        val text = cellData.value.getExpiryStatus(LocalDate.now())
        new scalafx.beans.property.StringProperty(this, "detail", text)
      }
      prefWidth = 180
      cellFactory = { (col: TableColumn[FoodItem, String]) =>
        new TableCell[FoodItem, String] {
          item.onChange { (_, _, newText) =>
            if newText != null then
              graphic = UIUtils.getExpiryLabel(newText)
              text = null
              alignment = scalafx.geometry.Pos.Center
            else
              graphic = null
              text = null
          }
        }
      }

    columns ++= Seq(idCol, nameCol, categoryCol, qtyCol, perishableCol, detailCol)
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
    children = Seq(inventoryTable)

  private val tableSection = new VBox:
    spacing = 10
    styleClass = Seq("section-card")
    VBox.setVgrow(tableWrapper, Priority.Always)
    children = Seq(
      new Label("Inventory Items") { styleClass = Seq("section-card-title") },
      tableWrapper
    )

  // Bind repository items to the table
  private val searchField = new TextField {
    promptText = "🔍 Search inventory by name..."
    styleClass = Seq("filter-field")
    maxWidth = Double.MaxValue
  }

  private val categoryFilterCombo = new ComboBox[String](Seq("All Categories") ++ FoodCategory.values.map(_.toString).toSeq) {
    value = "All Categories"
    prefWidth = 190
    minWidth = 170
  }

  private val addButton = new Button("Add Item"):
    styleClass = Seq("button", "button-primary")
    onAction = handle { performAddItem() }

  private val deleteButton = new Button("Delete Selected"):
    styleClass = Seq("button", "button-danger")
    minWidth = 132
    onAction = handle { performDeleteSelected() }

  private val exportPdfBtn = new Button("📄 Export PDF Report"):
    styleClass = Seq("button", "button-secondary")
    minWidth = 135
    onAction = handle { performExportPdf() }

  private val resetFilterBtn = new Button("🔄 Reset"):
    styleClass = Seq("filter-reset-btn")
    minWidth = 90
    onAction = handle {
      searchField.text = ""
      categoryFilterCombo.value = "All Categories"
    }

  HBox.setHgrow(searchField, Priority.Always)

  private val filterControlsRow = new HBox {
    spacing = 10
    alignment = scalafx.geometry.Pos.CenterLeft
    children = Seq(searchField, categoryFilterCombo, resetFilterBtn)
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
      new Label("Manage selected inventory item") { styleClass = Seq("table-actions-label") },
      tableActionSpacer,
      deleteButton,
      exportPdfBtn
    )
  }

  private val filteredInventory = new FilteredBuffer[FoodItem](inventory)
  private val sortedInventory = new SortedBuffer[FoodItem](filteredInventory)

  sortedInventory.delegate.comparatorProperty().bind(inventoryTable.comparatorProperty)

  private def updateFilter(): Unit =
    val query = if searchField.text.value == null then "" else searchField.text.value.toLowerCase.trim
    val cat = categoryFilterCombo.value.value
    filteredInventory.predicate = { item =>
      val matchesSearch = query.isEmpty || item.name.toLowerCase.contains(query)
      val matchesCategory = cat == "All Categories" || item.category.toString == cat
      matchesSearch && matchesCategory
    }

  searchField.text.onChange { (_, _, _) => updateFilter() }
  categoryFilterCombo.value.onChange { (_, _, _) => updateFilter() }

  inventoryTable.items = sortedInventory

  // Form Controls
  private val nameField = new TextField { promptText = "Item Name"; maxWidth = Double.MaxValue }
  private val categoryCombo = new ComboBox[FoodCategory](FoodCategory.values.toIndexedSeq) { promptText = "Select Category"; maxWidth = Double.MaxValue }
  private val qtyField = new TextField { promptText = "Qty (e.g. 5.0)"; maxWidth = Double.MaxValue }
  private val unitField = new TextField { promptText = "Unit (e.g. kg)"; maxWidth = Double.MaxValue }
  
  private val itemTypeCombo = new ComboBox[String](Seq("Perishable", "Non-Perishable")) { value = "Perishable"; maxWidth = Double.MaxValue }
  private val expiryDatePicker = new DatePicker { promptText = "Expiry Date"; maxWidth = Double.MaxValue; value = LocalDate.now().plusDays(7) }
  private val shelfLifeField = new TextField { promptText = "Shelf Life (months)"; maxWidth = Double.MaxValue; visible = false }

  private val expiryLabel = new Label("Expiry Date:") { styleClass = Seq("form-field-label") }
  private val shelfLifeLabel = new Label("Shelf Life (Mo.):") { styleClass = Seq("form-field-label"); visible = false }

  private val statusLabel = new Label { styleClass = Seq("status-label", "status-error") }

  // Toggle input visibility based on perishable selection
  itemTypeCombo.onAction = handle {
    val isPerishable = itemTypeCombo.value.value == "Perishable"
    expiryDatePicker.visible = isPerishable
    expiryLabel.visible = isPerishable
    shelfLifeField.visible = !isPerishable
    shelfLifeLabel.visible = !isPerishable
  }

  // Keyboard navigation & submission setup
  private def setupFormActions(submitAction: () => Unit): Unit =
    nameField.onAction = handle { submitAction() }
    qtyField.onAction = handle { submitAction() }
    unitField.onAction = handle { submitAction() }
    shelfLifeField.onAction = handle { submitAction() }

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
        content.showText("Food Pantry — Inventory Stock Report")
        content.endText()
        
        content.beginText()
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10)
        content.newLineAtOffset(50, 762)
        content.showText(s"Generated on: ${LocalDate.now().toString} | Total Items: ${inventory.size}")
        content.endText()
        
        // Table Column Headers
        val headerY = 730f
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10)
        content.beginText()
        content.newLineAtOffset(50, headerY)
        content.showText("ID")
        content.newLineAtOffset(80, 0)
        content.showText("Item Name")
        content.newLineAtOffset(160, 0)
        content.showText("Category")
        content.newLineAtOffset(100, 0)
        content.showText("Quantity")
        content.newLineAtOffset(100, 0)
        content.showText("Status")
        content.endText()
        
        // Divider line
        content.setLineWidth(1f)
        content.moveTo(50, headerY - 5)
        content.lineTo(545, headerY - 5)
        content.stroke()
        
        // Draw Rows
        content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9)
        inventory.zipWithIndex.foreach { (item, idx) =>
          val rowY = headerY - 22f - (idx * 18f)
          if rowY > 50f then
            content.beginText()
            content.newLineAtOffset(50, rowY)
            content.showText(item.id)
            content.newLineAtOffset(80, 0)
            val nameText = if item.name.length > 22 then item.name.substring(0, 22) + "..." else item.name
            content.showText(nameText)
            content.newLineAtOffset(160, 0)
            content.showText(item.category.toString)
            content.newLineAtOffset(100, 0)
            content.showText(s"${item.quantity} ${item.unit}")
            content.newLineAtOffset(100, 0)
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
    else if category == null then
      UIUtils.applyStatus(statusLabel, "error", "Error: Please select a Food Category!")
    else if qtyStr.isEmpty then
      UIUtils.applyStatus(statusLabel, "error", "Error: Quantity field cannot be empty!")
    else if unit.isEmpty then
      UIUtils.applyStatus(statusLabel, "error", "Error: Unit field cannot be empty!")
    else
      // S1-12 & S1-18 Invalid-input handling 2: Parse numeric quantity safely
      Try(qtyStr.toDouble).toOption match
        case None =>
          UIUtils.applyStatus(statusLabel, "error", "Error: Quantity must be a valid decimal number!")
        case Some(quantity) if quantity <= 0 =>
          UIUtils.applyStatus(statusLabel, "error", "Error: Quantity must be a positive number (> 0)!")
        case Some(quantity) =>
          if itemType == "Perishable" then
            val expiryVal = expiryDatePicker.value.value
            if expiryVal == null then
              UIUtils.applyStatus(statusLabel, "error", "Error: Please select an Expiry Date!")
            else
              // Create perishable item
              val nextId = s"inv-${inventory.size + 1}"
              val newItem = PerishableItem(nextId, name, category, quantity, unit, expiryVal)
              inventory.add(newItem)
              onSave()
              clearForm()
              UIUtils.applyStatus(statusLabel, "success", s"Success: Added perishable item '$name'.")
              UIUtils.showToast(s"Added perishable item '$name' ($quantity $unit)", "success")
          else
            val shelfLifeStr = shelfLifeField.text.value.trim
            // S1-12 & S1-18 Invalid-input handling 3: Parse integer shelf life safely
            Try(shelfLifeStr.toInt).toOption match
              case None =>
                UIUtils.applyStatus(statusLabel, "error", "Error: Shelf Life must be a valid integer number of months!")
              case Some(months) if months <= 0 =>
                UIUtils.applyStatus(statusLabel, "error", "Error: Shelf Life must be a positive number of months (> 0)!")
              case Some(months) =>
                val nextId = s"inv-${inventory.size + 1}"
                val newItem = NonPerishableItem(nextId, name, category, quantity, unit, months)
                inventory.add(newItem)
                onSave()
                clearForm()
                UIUtils.applyStatus(statusLabel, "success", s"Success: Added non-perishable item '$name'.")
                UIUtils.showToast(s"Added non-perishable item '$name' ($quantity $unit)", "success")

  private def performDeleteSelected(): Unit =
    val selectedItem = inventoryTable.selectionModel.value.getSelectedItem
    if selectedItem != null then
      if UIUtils.showConfirmation("Confirm Deletion", "Delete Inventory Item", s"Are you sure you want to delete '${selectedItem.name}' from inventory?") then
        inventory.remove(selectedItem)
        onSave()
        UIUtils.applyStatus(statusLabel, "success", s"Success: Deleted item '${selectedItem.name}'.")
        UIUtils.showToast(s"Deleted item '${selectedItem.name}' from stock", "success")
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
      new ColumnConstraints { minWidth = 72 },
      new ColumnConstraints { percentWidth = 36.0; hgrow = Priority.Always },
      new ColumnConstraints { minWidth = 92 },
      new ColumnConstraints { percentWidth = 36.0; hgrow = Priority.Always }
    )

    add(new Label("Item Name:") { styleClass = Seq("form-field-label") }, 0, 0)
    add(nameField, 1, 0)
    add(new Label("Category:") { styleClass = Seq("form-field-label") }, 2, 0)
    add(categoryCombo, 3, 0)
    add(new Label("Quantity:") { styleClass = Seq("form-field-label") }, 0, 1)
    add(qtyField, 1, 1)
    add(new Label("Unit:") { styleClass = Seq("form-field-label") }, 2, 1)
    add(unitField, 3, 1)
    add(new Label("Type:") { styleClass = Seq("form-field-label") }, 0, 2)
    add(itemTypeCombo, 1, 2)
    add(expiryLabel, 2, 2)
    add(expiryDatePicker, 3, 2)
    add(shelfLifeLabel, 2, 2)
    add(shelfLifeField, 3, 2)

  private val formContainer = new VBox:
    spacing = 12
    padding = Insets(18)
    styleClass = Seq("form-card", "card-color-inventory")
    children = Seq(
      new Label("➕ Add New Inventory Item") { styleClass = Seq("form-card-title") },
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

  // Setup key listener actions for Form
  setupFormActions(() => performAddItem())
