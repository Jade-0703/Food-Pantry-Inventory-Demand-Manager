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
              
              @annotation.nowarn("cat=deprecation")
              def createNavButton(text: String, view: scalafx.scene.Node): Button = 
                new Button(text):
                  styleClass = Seq("sidebar-btn")
                  onAction = handle {
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
  def showToast(message: String, kind: String = "success"): Unit =
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
    val (bg, fg, bc) = restriction match
      case "Vegetarian" => ("#ecfdf5", "#047857", "#059669")
      case "Halal"      => ("#fdf2f8", "#be185d", "#fbcfe8")
      case "GlutenFree" => ("#fffbeb", "#b45309", "#fde68a")
      case _            => ("#f1f5f9", "#475569", "#cbd5e1")
    createPillLabel(restriction, bg, fg, bc)

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
