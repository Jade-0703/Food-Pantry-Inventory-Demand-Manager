package foodpantry

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.ObjectProperty
import scalafx.Includes._
import scala.util.Try

// ai-assisted: #6
// why: Assisted with setting up dynamic scene-swapping layout in MainApp.
object MainApp extends JFXApp3:
  
  private val dbFile = "./pantry.db"

  private val inventoryRepo = SqliteRepository[FoodItem](
    dbFile,
    "inventory",
    FoodItem.createTableSql,
    FoodItem.sqlRowToFoodItem,
    FoodItem.foodItemToSqlParams
  )

  private val demandRepo = SqliteRepository[FamilyRequest](
    dbFile,
    "requests",
    FamilyRequest.createTableSql,
    FamilyRequest.sqlRowToFamilyRequest,
    FamilyRequest.familyRequestToSqlParams
  )


  // Reactive state buffers
  private val inventoryItems: ObservableBuffer[FoodItem] = ObservableBuffer[FoodItem]()
  private val familyRequests: ObservableBuffer[FamilyRequest] = ObservableBuffer[FamilyRequest]()

  override def start(): Unit =
    // Seed initial files if missing
    seedInitialData()

    // Load data from files
    loadAllData()

    // Load custom fonts
    scalafx.scene.text.Font.loadFont(getClass.getResourceAsStream("/fonts/Inter-Regular.ttf"), 12)
    scalafx.scene.text.Font.loadFont(getClass.getResourceAsStream("/fonts/Inter-Bold.ttf"), 12)
    scalafx.scene.text.Font.loadFont(getClass.getResourceAsStream("/fonts/Inter-SemiBold.ttf"), 12)

    // Initialize view instances
    val dashboardView = new DashboardView(inventoryItems, familyRequests)
    
    val inventoryView = new InventoryView(inventoryItems, () => saveInventory())
    val demandView = new DemandView(familyRequests, () => saveRequests())
    
    val distributionView = new DistributionView(inventoryItems, familyRequests, (updatedInv, updatedReqs) => {
      inventoryItems.clear()
      inventoryItems.addAll(updatedInv)
      saveInventory()

      familyRequests.clear()
      familyRequests.addAll(updatedReqs)
      saveRequests()
    })

    val aboutView = new AboutView(inventoryItems, familyRequests, () => {
      inventoryRepo.saveAll(List.empty)
      demandRepo.saveAll(List.empty)
      seedInitialData()
      loadAllData()
    })

    // Navigation state container (dashboard active by default)
    val activeView = ObjectProperty[scalafx.scene.Node](dashboardView)

    stage = new PrimaryStage:
      title = "Food Pantry Inventory & Demand Manager"
      width = 1240
      height = 780
      
      scene = new Scene:
        stylesheets = Seq(getClass.getResource("/style.css").toExternalForm)
        root = new StackPane:
          val toastBox = new VBox:
            spacing = 8
            alignment = scalafx.geometry.Pos.TopRight
            padding = scalafx.geometry.Insets(20, 25, 0, 0)
            pickOnBounds = false

          UIUtils.setToastContainer(toastBox)

          val mainBorderPane = new BorderPane:
            val borderPaneRef: BorderPane = this

            // Top Application MenuBar (as requested by Dr. Chin Teck Min)
            val appMenuBar = new MenuBar:
              useSystemMenuBar = false
              menus = Seq(
                new Menu("File"):
                  items = Seq(
                    new MenuItem("🌱 Reset to Sample Data"):
                      onAction = _ => {
                        if UIUtils.showConfirmation("Confirm Reset", "Reset Sample Data", "Reset all inventory and request data to sample defaults?") then
                          inventoryRepo.saveAll(List.empty)
                          demandRepo.saveAll(List.empty)
                          seedInitialData()
                          loadAllData()
                          UIUtils.showToast("Re-seeded initial sample data", "info")
                      },
                    new SeparatorMenuItem(),
                    new MenuItem("❌ Exit Application"):
                      onAction = _ => {
                        sys.exit(0)
                      }
                  ),
                new Menu("Navigation"):
                  items = Seq(
                    new MenuItem("📊 Dashboard"):
                      onAction = _ => { activeView.value = dashboardView },
                    new MenuItem("📦 Inventory Stock"):
                      onAction = _ => { activeView.value = inventoryView },
                    new MenuItem("👪 Family Requests"):
                      onAction = _ => { activeView.value = demandView },
                    new MenuItem("🌾 Distribution Planner"):
                      onAction = _ => { activeView.value = distributionView },
                    new MenuItem("ℹ️ About System"):
                      onAction = _ => { activeView.value = aboutView }
                  ),
                new Menu("Help"):
                  items = Seq(
                    new MenuItem("ℹ️ About Food Pantry App..."):
                      onAction = _ => {
                        activeView.value = aboutView
                        UIUtils.showNotification(
                          "About System",
                          "Food Pantry Inventory & Demand Manager v1.0.0",
                          "Author: Jade Wenxi (ID: 23093495) | Sunway University PRG2104 Tier C (AI-Integrated)"
                        )
                      },
                    new MenuItem("📖 Quick Operations Guide"):
                      onAction = _ => {
                        UIUtils.showNotification(
                          "Operations Guide",
                          "Daily Pantry Operations Flow",
                          "1. Log items in Inventory -> 2. Log requests in Demand -> 3. Run Distribution Planner to generate waste-minimizing allocation."
                        )
                      }
                  )
              )

            top = appMenuBar

            // Left Sidebar Navigation
            left = new VBox:
              spacing = 8
              styleClass = Seq("sidebar")
              
              val appTitle: Label = new Label("Food Pantry"):
                styleClass = Seq("sidebar-app-title")
              val appSubtitle: Label = new Label("SDG 1 · SDG 12 · Food Bank Ops"):
                styleClass = Seq("sidebar-app-subtitle")
              val navSectionLabel: Label = new Label("NAVIGATION"):
                styleClass = Seq("sidebar-section-label")
              
              def createNavButton(text: String, view: scalafx.scene.Node): Button = 
                new Button(text):
                  styleClass = Seq("sidebar-btn")
                  onAction = _ => {
                    activeView.value = view
                  }

              val btnAbout = createNavButton("ℹ️ About", aboutView)
              val btnDash = createNavButton("📊 Dashboard", dashboardView)
              val btnInv = createNavButton("📦 Inventory Log", inventoryView)
              val btnReq = createNavButton("👪 Family Requests", demandView)
              val btnDist = createNavButton("🚛 Distribution Plan", distributionView)

              // Dynamic background highlight for selected nav button and center view swap
              activeView.onChange { (_, _, newView) =>
                borderPaneRef.center = new ScrollPane {
                  content = newView
                  fitToWidth = true
                  hbarPolicy = ScrollPane.ScrollBarPolicy.Never
                  styleClass = Seq("scroll-pane")
                  style = "-fx-background-color: #fbf9f4;"
                }
                Seq(
                  (btnAbout, aboutView),
                  (btnDash, dashboardView),
                  (btnInv, inventoryView),
                  (btnReq, demandView),
                  (btnDist, distributionView)
                ).foreach { case (btn, viewInstance) =>
                  if newView == viewInstance then
                    btn.styleClass = Seq("sidebar-btn-active")
                  else
                    btn.styleClass = Seq("sidebar-btn")
                }
              }

              // Set Dashboard active by default
              btnDash.styleClass = Seq("sidebar-btn-active")

              children = Seq(appTitle, appSubtitle, navSectionLabel, btnAbout, btnDash, btnInv, btnReq, btnDist)
            
            // Initial Content Pane
            center = new ScrollPane {
              content = dashboardView
              fitToWidth = true
              hbarPolicy = ScrollPane.ScrollBarPolicy.Never
              styleClass = Seq("scroll-pane")
              style = "-fx-background-color: #fbf9f4;"
            }

          children = Seq(mainBorderPane, toastBox)

  private def saveInventory(): Unit =
    inventoryRepo.saveAll(inventoryItems.toList)

  private def saveRequests(): Unit =
    demandRepo.saveAll(familyRequests.toList)

  private def loadAllData(): Unit =
    inventoryRepo.loadAll().foreach { items =>
      inventoryItems.clear()
      inventoryItems.addAll(items)
    }
    demandRepo.loadAll().foreach { reqs =>
      familyRequests.clear()
      familyRequests.addAll(reqs)
    }

  private def seedInitialData(): Unit =
    val currentInv = inventoryRepo.loadAll().getOrElse(List.empty)
    if currentInv.isEmpty then
      Try {
        val stream = getClass.getResourceAsStream("/inventory.sql")
        if stream != null then
          val source = scala.io.Source.fromInputStream(stream)
          val conn = java.sql.DriverManager.getConnection(s"jdbc:sqlite:$dbFile")
          try
            val stmt = conn.createStatement()
            try
              source.getLines().filter(_.trim.nonEmpty).foreach(stmt.execute)
            finally
              stmt.close()
          finally
            source.close()
            conn.close()
      }

    val currentReqs = demandRepo.loadAll().getOrElse(List.empty)
    if currentReqs.isEmpty then
      Try {
        val stream = getClass.getResourceAsStream("/demand.sql")
        if stream != null then
          val source = scala.io.Source.fromInputStream(stream)
          val conn = java.sql.DriverManager.getConnection(s"jdbc:sqlite:$dbFile")
          try
            val stmt = conn.createStatement()
            try
              source.getLines().filter(_.trim.nonEmpty).foreach(stmt.execute)
            finally
              stmt.close()
          finally
            source.close()
            conn.close()
      }



object UIUtils:
  import scalafx.beans.property.ObjectProperty
  import scalafx.scene.control.{Label, TableView, Alert, ButtonType}
  import scalafx.scene.control.Alert.AlertType
  import scalafx.scene.layout.VBox
  import scalafx.scene.shape.Rectangle
  import scalafx.animation.{FadeTransition, PauseTransition}
  import scalafx.util.Duration
  import scalafx.Includes._

  private val toastBoxOpt = ObjectProperty[Option[VBox]](None)

  def setToastContainer(box: VBox): Unit =
    toastBoxOpt.value = Some(box)

  /** Displays a top-right floating toast notification */
  def showToast(message: String): Unit = showToast(message, "success")

  def showToast(message: String, kind: String): Unit =
    toastBoxOpt.value.foreach { box =>
      val (bg, icon) = kind match
        case "error" => ("#b91c1c", "✗")
        case "info"  => ("#0284c7", "ℹ")
        case _       => ("#15803d", "✓")

      val toastLabel = new Label(s"$icon $message") {
        style = s"-fx-background-color: $bg; -fx-text-fill: #ffffff; -fx-padding: 10px 18px; -fx-background-radius: 8px; -fx-font-weight: bold; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 10, 0, 0, 4);"
      }

      box.children.add(toastLabel)

      val fade = new FadeTransition(Duration(500), toastLabel) {
        fromValue = 1.0
        toValue = 0.0
        onFinished = _ => { box.children.remove(toastLabel) }
      }
      val pause = new PauseTransition(Duration(2200)) {
        onFinished = _ => { fade.play() }
      }
      pause.play()
    }

  /** Displays a popup notification alert */
  def showNotification(titleText: String, headerTextMsg: String, contentTextMsg: String): Unit =
    showToast(s"$headerTextMsg: $contentTextMsg", "info")

  /** Displays a popup confirmation alert returning true if confirmed */
  def showConfirmation(titleText: String, headerTextMsg: String, contentTextMsg: String): Boolean =
    val alert = new Alert(AlertType.Confirmation) {
      title = titleText
      headerText = headerTextMsg
      contentText = contentTextMsg
    }
    alert.initOwner(MainApp.stage)
    val res = alert.showAndWait()
    res.contains(ButtonType.OK)

  /** Shared page header used across all four operational views (S1-13 DRY). */
  def createPageHeader(title: String, subtitle: String): VBox =
    new VBox:
      spacing = 4
      styleClass = Seq("page-header-box")
      children = Seq(
        new Label(title) { styleClass = Seq("page-title") },
        new Label(subtitle) { styleClass = Seq("page-subtitle") }
      )

  def applyStatus(label: Label, kind: String, message: String): Unit =
    label.text = message
    label.styleClass = Seq("status-label", s"status-$kind")

  def createRoundedClip(table: TableView[_]): Rectangle = new Rectangle {
    width <== table.width
    height <== table.height
    arcWidth = 24
    arcHeight = 24
  }

  /** Creates a reusable horizontal spacer region that consumes remaining HBox space (S1-13 DRY optimization) */
  def createHSpacer(): Region =
    val spacer = new Region()
    HBox.setHgrow(spacer, Priority.Always)
    spacer

  /** Creates a standardized filter bar container (S1-13 DRY optimization) */
  def createFilterBar(controls: scalafx.scene.Node*): VBox =
    val filterControlsRow = new HBox {
      spacing = 10
      alignment = scalafx.geometry.Pos.CenterLeft
      children = controls
    }
    new VBox {
      spacing = 0
      styleClass = Seq("filter-bar", "filter-bar-stacked")
      children = Seq(filterControlsRow)
    }

  /** Creates a standardized table action bar container (S1-13 DRY optimization) */
  def createTableActionsBar(label: String, buttons: scalafx.scene.Node*): HBox =
    new HBox {
      spacing = 10
      alignment = scalafx.geometry.Pos.CenterRight
      styleClass = Seq("table-actions-bar")
      children = Seq(
        new Label(label) { styleClass = Seq("table-actions-label") },
        createHSpacer()
      ) ++ buttons
    }

  /** Safely extracts clean lowercased search query string (S1-13 DRY optimization) */
  def getSearchQuery(field: scalafx.scene.control.TextField): String =
    if field.text.value == null then "" else field.text.value.toLowerCase.trim

  /** Creates a ColumnConstraints helper without redundant block wrappers (S1-13 DRY optimization) */
  def createColumnConstraints(minW: Double = -1, percentW: Double = -1, grow: scalafx.scene.layout.Priority = scalafx.scene.layout.Priority.Never): ColumnConstraints =
    val c = new ColumnConstraints()
    if minW > 0 then c.minWidth = minW
    if percentW > 0 then c.percentWidth = percentW
    if grow != scalafx.scene.layout.Priority.Never then c.hgrow = grow
    c

  /** Creates a standardized form field label (S1-13 DRY optimization) */
  def createFormFieldLabel(text: String): Label =
    val lbl = new Label(text)
    lbl.styleClass = Seq("form-field-label")
    lbl

  import scalafx.collections.transformation.SortedBuffer

  /** Binds table comparator to sorted buffer delegate (S1-13 DRY optimization) */
  def bindTableSorter[T](table: TableView[T], sortedBuffer: SortedBuffer[T]): Unit =
    sortedBuffer.delegate.comparatorProperty().bind(table.comparatorProperty)

  /** Binds multiple observable controls to trigger a filter update callback (S1-13 DRY optimization) */
  def bindFilterTriggers(updateFilter: () => Unit, controls: scalafx.beans.value.ObservableValue[_, _]*): Unit =
    controls.foreach(_.onChange { (_, _, _) => updateFilter() })

  /** Creates a standard centered badge cell factory for TableColumn (S1-13 DRY optimization) */
  def createBadgeCellFactory[T](labelProvider: String => Label): TableColumn[T, String] => TableCell[T, String] =
    _ => new TableCell[T, String] {
      item.onChange { (_, _, newText) =>
        if newText != null then
          graphic = labelProvider(newText)
          text = null
          alignment = scalafx.geometry.Pos.Center
        else
          graphic = null
          text = null
      }
    }

  /** Generic pill-shaped label with background, text colour, and border */
  def createPillLabel(text: String, bg: String, fg: String, borderColor: String): Label =
    new Label(text) {
      alignment = scalafx.geometry.Pos.Center
      style = s"-fx-background-color: $bg; -fx-text-fill: $fg; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: $borderColor; -fx-border-radius: 12px; -fx-border-width: 1px"
    }

  def getExpiryLabel(statusStr: String): Label =
    val (bg, fg, bc) = if statusStr.contains("EXPIRED") then
      ("#fee2e2", "#b91c1c", "#fca5a5")
    else if statusStr.contains("Expires TODAY!") then
      ("#fef2f2", "#dc2626", "#fca5a5")
    else if statusStr.contains("Expires in") then
      ("#ffedd5", "#c2410c", "#fed7aa")
    else
      ("#f0fdf4", "#16a34a", "#22c55e")
    createPillLabel(statusStr, bg, fg, bc)

  def getDietaryLabel(restriction: String): Label =
    val (bg, fg, bc, displayStr) = restriction match
      case "Vegetarian"  => ("#dcfce7", "#15803d", "#86efac", "Vegetarian")
      case "Halal"       => ("#fce7f3", "#be185d", "#f472b6", "Halal")
      case "GlutenFree" | "Gluten-Free" => ("#fef3c7", "#b45309", "#fcd34d", "Gluten-Free")
      case _             => ("#e0f2fe", "#0369a1", "#7dd3fc", "Standard")
    createPillLabel(displayStr, bg, fg, bc)

  def getStatusLabel(status: String): Label =
    val (bg, fg, bc) = status match
      case "Fulfilled" => ("#dcfce7", "#15803d", "#16a34a")
      case _           => ("#ffedd5", "#c2410c", "#fed7aa")
    createPillLabel(status, bg, fg, bc)

  def getPerishableLabel(text: String): Label =
    val (bg, fg, bc) = if text == "Yes" then
      ("#e0f2fe", "#0369a1", "#bae6fd")
    else
      ("#f1f5f9", "#475569", "#cbd5e1")
    createPillLabel(text, bg, fg, bc)

  def getLowStockLabel(displayStr: String, quantity: Double): Label =
    val (bg, fg, bc) = if quantity <= 1.0 then
      ("#fee2e2", "#b91c1c", "#fca5a5")
    else
      ("#fef3c7", "#d97706", "#fcd34d")
    createPillLabel(displayStr, bg, fg, bc)
