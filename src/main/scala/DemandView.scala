package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.Includes._
import scala.util.Try

@annotation.nowarn("cat=deprecation")
class DemandView(
  requests: ObservableBuffer[FamilyRequest],
  onSave: () => Unit
) extends VBox:

  spacing = 15
  padding = Insets(20)
  style = "-fx-background-color: #f1f5f9;"

  // Header Title
  private val titleLabel = new Label("Family Demand & Requests Log"):
    style = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e293b;"

  // Requests Table
  private val requestsTable = new TableView[FamilyRequest]:
    style = "-fx-background-radius: 8px; -fx-background-color: #ffffff;"
    placeholder = new Label("No pending family requests.") { style = "-fx-text-fill: #64748b;" }

    val idCol = new TableColumn[FamilyRequest, String]("Request ID"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "id", cellData.value.id) }
      prefWidth = 100
      
    val nameCol = new TableColumn[FamilyRequest, String]("Family Name"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "name", cellData.value.familyName) }
      prefWidth = 160
      
    val sizeCol = new TableColumn[FamilyRequest, String]("Household Size"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "size", cellData.value.householdSize.toString) }
      prefWidth = 110
      
    val restrictionCol = new TableColumn[FamilyRequest, String]("Dietary Restriction"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "diet", cellData.value.dietaryRestriction.toString) }
      prefWidth = 140

    val categoryCol = new TableColumn[FamilyRequest, String]("Category Requested"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "category", cellData.value.requestedCategory.toString) }
      prefWidth = 140

    val statusCol = new TableColumn[FamilyRequest, String]("Status"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "status", cellData.value.status.toString) }
      prefWidth = 110

    columns ++= Seq(idCol, nameCol, sizeCol, restrictionCol, categoryCol, statusCol)
    prefHeight = 300

  // Bind requests buffer
  requestsTable.items = requests

  // Form Controls
  private val familyNameField = new TextField { promptText = "Family Name"; prefWidth = 160 }
  private val sizeField = new TextField { promptText = "Size (e.g. 4)"; prefWidth = 100 }
  private val restrictionCombo = new ComboBox[DietaryRestriction](DietaryRestriction.values.toIndexedSeq) { promptText = "Dietary Restriction"; prefWidth = 150 }
  private val categoryCombo = new ComboBox[FoodCategory](FoodCategory.values.toIndexedSeq) { promptText = "Requested Category"; prefWidth = 150 }
  
  private val statusLabel = new Label { style = "-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 13px;" }

  // Keyboard navigation Setup
  private def setupFormActions(submitAction: () => Unit): Unit =
    familyNameField.onAction = handle { submitAction() }
    sizeField.onAction = handle { submitAction() }

  private val addButton = new Button("Add Request"):
    styleClass = Seq("button", "button-primary")
    onAction = handle { performAddRequest() }

  private val deleteButton = new Button("Delete Selected"):
    styleClass = Seq("button", "button-danger")
    onAction = handle { performDeleteSelected() }

  private def performAddRequest(): Unit =
    statusLabel.text = ""
    
    val familyName = familyNameField.text.value.trim
    val sizeStr = sizeField.text.value.trim
    val diet = restrictionCombo.value.value
    val category = categoryCombo.value.value

    // S1-18 Invalid-input handling 1: Check empty fields
    if familyName.isEmpty then
      statusLabel.text = "Error: Family Name cannot be empty!"
    else if sizeStr.isEmpty then
      statusLabel.text = "Error: Household size cannot be empty!"
    else if diet == null then
      statusLabel.text = "Error: Please select a Dietary Restriction!"
    else if category == null then
      statusLabel.text = "Error: Please select a Requested Food Category!"
    else
      // S1-12 & S1-18 Invalid-input handling 2: Parse integer safely
      Try(sizeStr.toInt).toOption match
        case None =>
          statusLabel.text = "Error: Household size must be a valid integer number!"
        case Some(sizeVal) if sizeVal <= 0 =>
          statusLabel.text = "Error: Household size must be a positive integer (> 0)!"
        case Some(sizeVal) =>
          val nextId = s"req-${requests.size + 1}"
          val newRequest = FamilyRequest(nextId, familyName, sizeVal, diet, category, RequestStatus.Pending)
          requests.add(newRequest)
          onSave()
          clearForm()
          statusLabel.style = "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
          statusLabel.text = s"Success: Request for '$familyName' logged."

  private def performDeleteSelected(): Unit =
    val selectedIndex = requestsTable.selectionModel.value.getSelectedIndex
    if selectedIndex >= 0 then
      requests.remove(selectedIndex)
      onSave()
      statusLabel.style = "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
      statusLabel.text = "Success: Selected request deleted."
    else
      statusLabel.style = "-fx-text-fill: #dc2626; -fx-font-weight: bold;"
      statusLabel.text = "Warning: Select a request in the table to delete."

  private def clearForm(): Unit =
    familyNameField.text = ""
    sizeField.text = ""
    restrictionCombo.value = null
    categoryCombo.value = null
    statusLabel.style = "-fx-text-fill: #dc2626; -fx-font-weight: bold;"

  // Form Layout
  private val formGrid = new GridPane:
    hgap = 10
    vgap = 10
    padding = Insets(15)
    styleClass = Seq("form-card")

    add(new Label("Log Family Request") { style = "-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1e293b;"; minWidth = 500 }, 0, 0, 4, 1)

    add(new Label("Family Name:"), 0, 1)
    add(familyNameField, 1, 1)

    add(new Label("Household Size:"), 2, 1)
    add(sizeField, 3, 1)

    add(new Label("Dietary Restr.:"), 0, 2)
    add(restrictionCombo, 1, 2)

    add(new Label("Category:"), 2, 2)
    add(categoryCombo, 3, 2)

  private val buttonRow = new HBox:
    spacing = 15
    children = Seq(addButton, deleteButton, statusLabel)
    alignment = Pos.CenterLeft

  children = Seq(
    titleLabel,
    requestsTable,
    formGrid,
    buttonRow
  )

  // Setup keyboard actions
  setupFormActions(() => performAddRequest())
