package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.Includes._
import java.time.LocalDate
import scala.util.Try
import javafx.collections.transformation.{FilteredList, SortedList}

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
    columns.foreach { col =>
      col.setReorderable(false)
      col.setResizable(false)
    }
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

  // Bind repository items to the table
  private val searchField = new TextField {
    promptText = "🔍 Search inventory by name..."
    styleClass = Seq("filter-field")
  }

  private val categoryFilterCombo = new ComboBox[String](Seq("All Categories") ++ FoodCategory.values.map(_.toString).toSeq) {
    value = "All Categories"
  }

  private val resetFilterBtn = new Button("🔄 Reset"):
    styleClass = Seq("filter-reset-btn")
    onAction = handle {
      searchField.text = ""
      categoryFilterCombo.value = "All Categories"
    }

  private val filterBar = new HBox {
    spacing = 10
    styleClass = Seq("filter-bar")
    children = Seq(searchField, categoryFilterCombo, resetFilterBtn)
    alignment = scalafx.geometry.Pos.CenterLeft
  }

  private val filteredInventory = new FilteredList[FoodItem](inventory.delegate)
  private val sortedInventory = new SortedList[FoodItem](filteredInventory)

  sortedInventory.comparatorProperty().bind(inventoryTable.comparatorProperty)

  private def updateFilter(): Unit =
    val query = if searchField.text.value == null then "" else searchField.text.value.toLowerCase.trim
    val cat = categoryFilterCombo.value.value
    filteredInventory.setPredicate { item =>
      val matchesSearch = query.isEmpty || item.name.toLowerCase.contains(query)
      val matchesCategory = cat == "All Categories" || item.category.toString == cat
      matchesSearch && matchesCategory
    }

  searchField.text.onChange { (_, _, _) => updateFilter() }
  categoryFilterCombo.value.onChange { (_, _, _) => updateFilter() }

  inventoryTable.items = scalafx.collections.transformation.SortedBuffer(sortedInventory)

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

  private val addButton = new Button("Add Item"):
    styleClass = Seq("button", "button-primary")
    onAction = handle { performAddItem() }

  private val deleteButton = new Button("Delete Selected"):
    styleClass = Seq("button", "button-danger")
    onAction = handle { performDeleteSelected() }

  private val exportCsvBtn = new Button("📄 Export CSV"):
    styleClass = Seq("button", "button-secondary")
    onAction = handle { performExportCsv() }

  private def performExportCsv(): Unit =
    val file = new java.io.File("inventory_backup.csv")
    Try {
      val writer = new java.io.PrintWriter(file)
      try
        writer.println("id,name,category,quantity,unit,isPerishable,expiryDate,shelfLifeMonths")
        inventory.foreach { item =>
          writer.println(FoodItemSerializer.serialize(item))
        }
      finally
        writer.close()
      UIUtils.applyStatus(statusLabel, "success", s"✓ Inventory backup exported to ${file.getAbsolutePath}!")
    }.recover { case e =>
      UIUtils.applyStatus(statusLabel, "error", s"✗ Error exporting CSV: ${e.getMessage}")
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
  private val formGrid = new GridPane:
    hgap = 14
    vgap = 12
    columnConstraints = Seq(
      new ColumnConstraints { minWidth = 70 },
      new ColumnConstraints { hgrow = Priority.Always },
      new ColumnConstraints { minWidth = 90 },
      new ColumnConstraints { hgrow = Priority.Always }
    )

    add(new Label("Name:") { styleClass = Seq("form-field-label") }, 0, 0)
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
      formGrid
    )

  // Layout assembly
  private val buttonRow = new HBox:
    spacing = 15
    children = Seq(addButton, deleteButton, exportCsvBtn, statusLabel)
    alignment = Pos.CenterLeft

  children = Seq(
    headerBlock,
    filterBar,
    tableWrapper,
    formContainer,
    buttonRow
  )

  // Setup key listener actions for Form
  setupFormActions(() => performAddItem())
