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
  style = "-fx-background-color: #fbf9f4;"

  // Header Title
  private val titleLabel = new Label("Pantry Inventory Log"):
    style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 20px;"

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
    prefHeight = 300

  private val tableWrapper = new StackPane:
    styleClass = Seq("table-wrapper")
    children = Seq(inventoryTable)

  // Bind repository items to the table
  private val searchField = new TextField {
    promptText = "🔍 Search inventory by name..."
    style = "-fx-pref-width: 250px; -fx-background-radius: 8px; -fx-padding: 6px 12px; -fx-font-size: 13px;"
  }

  private val categoryFilterCombo = new ComboBox[String](Seq("All Categories") ++ FoodCategory.values.map(_.toString).toSeq) {
    value = "All Categories"
    style = "-fx-background-radius: 8px; -fx-padding: 6px 12px; -fx-font-size: 13px;"
  }

  private val filterBar = new HBox {
    spacing = 10
    children = Seq(searchField, categoryFilterCombo)
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
  private val nameField = new TextField { promptText = "Item Name"; prefWidth = 150 }
  private val categoryCombo = new ComboBox[FoodCategory](FoodCategory.values.toIndexedSeq) { promptText = "Select Category"; prefWidth = 140 }
  private val qtyField = new TextField { promptText = "Qty (e.g. 5.0)"; prefWidth = 100 }
  private val unitField = new TextField { promptText = "Unit (e.g. kg)"; prefWidth = 80 }
  
  private val itemTypeCombo = new ComboBox[String](Seq("Perishable", "Non-Perishable")) { value = "Perishable"; prefWidth = 140 }
  private val expiryDatePicker = new DatePicker { promptText = "Expiry Date"; prefWidth = 140; value = LocalDate.now().plusDays(7) }
  private val shelfLifeField = new TextField { promptText = "Shelf Life (months)"; prefWidth = 140; visible = false }

  private val expiryLabel = new Label("Expiry Date:")
  private val shelfLifeLabel = new Label("Shelf Life (Mo.):") { visible = false }

  private val statusLabel = new Label { style = "-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 13px;" }

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

  private def performAddItem(): Unit =
    statusLabel.text = ""
    
    val name = nameField.text.value.trim
    val category = categoryCombo.value.value
    val qtyStr = qtyField.text.value.trim
    val unit = unitField.text.value.trim
    val itemType = itemTypeCombo.value.value

    // S1-18 Invalid-input handling 1: Check empty fields
    if name.isEmpty then
      statusLabel.text = "Error: Item Name cannot be empty!"
    else if category == null then
      statusLabel.text = "Error: Please select a Food Category!"
    else if qtyStr.isEmpty then
      statusLabel.text = "Error: Quantity field cannot be empty!"
    else if unit.isEmpty then
      statusLabel.text = "Error: Unit field cannot be empty!"
    else
      // S1-12 & S1-18 Invalid-input handling 2: Parse numeric quantity safely
      Try(qtyStr.toDouble).toOption match
        case None =>
          statusLabel.text = "Error: Quantity must be a valid decimal number!"
        case Some(quantity) if quantity <= 0 =>
          statusLabel.text = "Error: Quantity must be a positive number (> 0)!"
        case Some(quantity) =>
          if itemType == "Perishable" then
            val expiryVal = expiryDatePicker.value.value
            if expiryVal == null then
              statusLabel.text = "Error: Please select an Expiry Date!"
            else
              // Create perishable item
              val nextId = s"inv-${inventory.size + 1}"
              val newItem = PerishableItem(nextId, name, category, quantity, unit, expiryVal)
              inventory.add(newItem)
              onSave()
              clearForm()
              statusLabel.style = "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
              statusLabel.text = s"Success: Added perishable item '$name'."
          else
            val shelfLifeStr = shelfLifeField.text.value.trim
            // S1-12 & S1-18 Invalid-input handling 3: Parse integer shelf life safely
            Try(shelfLifeStr.toInt).toOption match
              case None =>
                statusLabel.text = "Error: Shelf Life must be a valid integer number of months!"
              case Some(months) if months <= 0 =>
                statusLabel.text = "Error: Shelf Life must be a positive number of months (> 0)!"
              case Some(months) =>
                val nextId = s"inv-${inventory.size + 1}"
                val newItem = NonPerishableItem(nextId, name, category, quantity, unit, months)
                inventory.add(newItem)
                onSave()
                clearForm()
                statusLabel.style = "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                statusLabel.text = s"Success: Added non-perishable item '$name'."

  private def performDeleteSelected(): Unit =
    val selectedIndex = inventoryTable.selectionModel.value.getSelectedIndex
    if selectedIndex >= 0 then
      inventory.remove(selectedIndex)
      onSave()
      statusLabel.style = "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
      statusLabel.text = "Success: Selected item deleted."
    else
      statusLabel.style = "-fx-text-fill: #dc2626; -fx-font-weight: bold;"
      statusLabel.text = "Warning: Select an item in the table to delete."

  private def clearForm(): Unit =
    nameField.text = ""
    qtyField.text = ""
    unitField.text = ""
    shelfLifeField.text = ""
    categoryCombo.value = null
    expiryDatePicker.value = LocalDate.now().plusDays(7)
    statusLabel.style = "-fx-text-fill: #dc2626; -fx-font-weight: bold;"

  // Form Layout
  private val formGrid = new GridPane:
    hgap = 10
    vgap = 10

    add(new Label("Name:"), 0, 0)
    add(nameField, 1, 0)

    add(new Label("Category:"), 2, 0)
    add(categoryCombo, 3, 0)

    add(new Label("Quantity:"), 0, 1)
    add(qtyField, 1, 1)

    add(new Label("Unit:"), 2, 1)
    add(unitField, 3, 1)

    add(new Label("Type:"), 0, 2)
    add(itemTypeCombo, 1, 2)

    add(expiryLabel, 2, 2)
    add(expiryDatePicker, 3, 2)

    add(shelfLifeLabel, 2, 2)
    add(shelfLifeField, 3, 2)

  private val formContainer = new VBox:
    spacing = 10
    padding = Insets(15)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("Add Inventory Item") { style = "-fx-text-fill: #1e293b; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"; minWidth = 500 },
      formGrid
    )

  // Layout assembly
  private val buttonRow = new HBox:
    spacing = 15
    children = Seq(addButton, deleteButton, statusLabel)
    alignment = Pos.CenterLeft

  children = Seq(
    titleLabel,
    filterBar,
    tableWrapper,
    formContainer,
    buttonRow
  )

  // Setup key listener actions for Form
  setupFormActions(() => performAddItem())
