package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.Includes._
import scala.util.Try
import javafx.collections.transformation.{FilteredList, SortedList}

@annotation.nowarn("cat=deprecation")
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

  // Bind requests buffer
  private val searchField = new TextField {
    promptText = "🔍 Search requests by family name..."
    styleClass = Seq("filter-field")
  }

  private val categoryFilterCombo = new ComboBox[String](Seq("All Categories Requested") ++ FoodCategory.values.map(_.toString).toSeq) {
    value = "All Categories Requested"
  }

  private val resetFilterBtn = new Button("🔄 Reset"):
    styleClass = Seq("filter-reset-btn")
    onAction = handle {
      searchField.text = ""
      categoryFilterCombo.value = "All Categories Requested"
    }

  private val filterBar = new HBox {
    spacing = 10
    styleClass = Seq("filter-bar")
    children = Seq(searchField, categoryFilterCombo, resetFilterBtn)
    alignment = scalafx.geometry.Pos.CenterLeft
  }

  private val filteredRequests = new FilteredList[FamilyRequest](requests.delegate)
  private val sortedRequests = new SortedList[FamilyRequest](filteredRequests)

  sortedRequests.comparatorProperty().bind(requestsTable.comparatorProperty)

  private def updateFilter(): Unit =
    val query = if searchField.text.value == null then "" else searchField.text.value.toLowerCase.trim
    val cat = categoryFilterCombo.value.value
    filteredRequests.setPredicate { item =>
      val matchesSearch = query.isEmpty || item.familyName.toLowerCase.contains(query)
      val matchesCategory = cat == "All Categories Requested" || item.requestedCategory.toString == cat
      matchesSearch && matchesCategory
    }

  searchField.text.onChange { (_, _, _) => updateFilter() }
  categoryFilterCombo.value.onChange { (_, _, _) => updateFilter() }

  requestsTable.items = scalafx.collections.transformation.SortedBuffer(sortedRequests)

  // Form Controls
  private val familyNameField = new TextField { promptText = "Family Name"; prefWidth = 160 }
  private val sizeField = new TextField { promptText = "Size (e.g. 4)"; prefWidth = 100 }
  private val restrictionCombo = new ComboBox[DietaryRestriction](DietaryRestriction.values.toIndexedSeq) { promptText = "Dietary Restriction"; prefWidth = 150 }
  private val categoryCombo = new ComboBox[FoodCategory](FoodCategory.values.toIndexedSeq) { promptText = "Requested Category"; prefWidth = 150 }
  
  private val statusLabel = new Label { styleClass = Seq("status-label", "status-error") }

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

  private val archiveButton = new Button("Archive Fulfilled"):
    styleClass = Seq("button", "button-secondary")
    onAction = handle { performArchiveFulfilled() }

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

  private def performDeleteSelected(): Unit =
    val selectedItem = requestsTable.selectionModel.value.getSelectedItem
    if selectedItem != null then
      requests.remove(selectedItem)
      onSave()
      UIUtils.applyStatus(statusLabel, "success", "Success: Selected request deleted.")
    else
      UIUtils.applyStatus(statusLabel, "error", "Warning: Select a request in the table to delete.")

  private def performArchiveFulfilled(): Unit =
    val fulfilled = requests.filter(_.status == RequestStatus.Fulfilled).toList
    if fulfilled.nonEmpty then
      requests --= fulfilled
      onSave()
      UIUtils.applyStatus(statusLabel, "success", s"Success: Archived ${fulfilled.size} fulfilled requests.")
    else
      UIUtils.applyStatus(statusLabel, "info", "Notice: No fulfilled requests to archive.")

  private def clearForm(): Unit =
    familyNameField.text = ""
    sizeField.text = ""
    restrictionCombo.value = null
    categoryCombo.value = null

  // Form Layout
  private val formGrid = new GridPane:
    hgap = 10
    vgap = 10

    add(new Label("Family Name:"), 0, 0)
    add(familyNameField, 1, 0)

    add(new Label("Household Size:"), 2, 0)
    add(sizeField, 3, 0)

    add(new Label("Dietary Restr.:"), 0, 1)
    add(restrictionCombo, 1, 1)

    add(new Label("Category:"), 2, 1)
    add(categoryCombo, 3, 1)

  private val formContainer = new VBox:
    spacing = 10
    padding = Insets(15)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("Log Family Request") { styleClass = Seq("form-card-title"); minWidth = 500 },
      formGrid
    )

  private val buttonRow = new HBox:
    spacing = 15
    children = Seq(addButton, deleteButton, archiveButton, statusLabel)
    alignment = Pos.CenterLeft

  children = Seq(
    headerBlock,
    filterBar,
    tableWrapper,
    formContainer,
    buttonRow
  )

  // Setup keyboard actions
  setupFormActions(() => performAddRequest())
