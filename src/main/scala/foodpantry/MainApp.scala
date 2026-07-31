package foodpantry

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.collections.ObservableBuffer
import scalafx.beans.property.ObjectProperty
import scalafx.geometry.Insets
import scalafx.Includes._
import java.io.File
import scala.util.Try

// ai-assisted: #6
// why: Assisted with setting up dynamic scene-swapping layout in MainApp.
object MainApp extends JFXApp3:
  
  private val inventoryFile = "./inventory.csv"
  private val demandFile = "./demand.csv"

  private val inventoryRepo = FileRepository[FoodItem](inventoryFile, FoodItemSerializer)
  private val demandRepo = FileRepository[FamilyRequest](demandFile, FamilyRequestSerializer)

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

    val aboutView = createAboutView()

    // Navigation state container
    val activeView = ObjectProperty[scalafx.scene.Node](dashboardView)

    stage = new PrimaryStage:
      title = "Food Pantry Inventory & Demand Manager"
      width = 1240
      height = 780
      
      scene = new Scene:
        stylesheets = Seq(getClass.getResource("/style.css").toExternalForm)
        root = new BorderPane:
          val mainBorderPane: BorderPane = this
          // Left Sidebar Navigation
          left = new VBox:
            spacing = 10
            padding = Insets(20)
            style = "-fx-background-color: #f8efdeff; -fx-min-width: 220px;"
            
            val appTitle: Label = new Label("Food Pantry"):
              style = "-fx-text-fill: #1e3a8a; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 0 0 15 0;"
            
            @annotation.nowarn("cat=deprecation")
            def createNavButton(text: String, view: scalafx.scene.Node): Button = 
              new Button(text):
                styleClass = Seq("sidebar-btn")
                onAction = handle {
                  activeView.value = view
                }

            val btnDash = createNavButton("📊 Dashboard", dashboardView)
            val btnInv = createNavButton("📦 Inventory Log", inventoryView)
            val btnReq = createNavButton("👪 Family Requests", demandView)
            val btnDist = createNavButton("🚛 Distribution Plan", distributionView)
            val btnAbout = createNavButton("ℹ️ About", aboutView)

            // Dynamic background highlight for selected nav button and center view swap
            activeView.onChange { (_, _, newView) =>
              mainBorderPane.center = new ScrollPane {
                content = newView
                fitToWidth = true
                hbarPolicy = ScrollPane.ScrollBarPolicy.Never
                style = "-fx-background-color: #fbf9f4; -fx-background-insets: 0; -fx-padding: 0; -fx-border-width: 0px;"
              }
              Seq(
                (btnDash, dashboardView),
                (btnInv, inventoryView),
                (btnReq, demandView),
                (btnDist, distributionView),
                (btnAbout, aboutView)
              ).foreach { case (btn, viewInstance) =>
                if newView == viewInstance then
                  btn.styleClass = Seq("sidebar-btn-active")
                else
                  btn.styleClass = Seq("sidebar-btn")
              }
            }

            // Set dashboard active by default
            btnDash.styleClass = Seq("sidebar-btn-active")

            children = Seq(appTitle, btnDash, btnInv, btnReq, btnDist, btnAbout)
          
          // Initial Content Pane
          center = new ScrollPane {
            content = dashboardView
            fitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.Never
            style = "-fx-background-color: #fbf9f4; -fx-background-insets: 0; -fx-padding: 0; -fx-border-width: 0px;"
          }

  private def createAboutView(): VBox = new VBox:
    spacing = 15
    padding = Insets(20)
    style = "-fx-background-color: #fbf9f4;"

    private val titleLabel = new Label("About Food Pantry Manager"):
      style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 20px;"

    private val overviewCard = new VBox:
      spacing = 10
      padding = Insets(18)
      styleClass = Seq("form-card")
      children = Seq(
        new Label("🌾 Food Pantry Inventory & Demand Manager"):
          style = "-fx-text-fill: #1e3a8a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 16px;"
        ,
        new Label("Version 1.0.0 • Academic Final Project • Sunway University"):
          style = "-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-font-weight: bold;"
        ,
        new Label("An interactive ScalaFX desktop application designed to empower community food banks by streamlining inventory logging, tracking household demands, and automating food package distribution."):
          style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
      )

    private val sdgCard = new VBox:
      spacing = 10
      padding = Insets(18)
      styleClass = Seq("form-card")
      children = Seq(
        new Label("📌 UN Sustainable Development Goals Alignment"):
          style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
        ,
        new Label("• SDG 1 (No Poverty): Provides targeted food package allocation to vulnerable households tailored by family size and specific dietary restrictions."):
          style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
        ,
        new Label("• SDG 12 (Responsible Consumption & Production): Implements a Waste-Minimizing Expiry-First matching policy to distribute perishable stock before expiration."):
          style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
      )

    private val techCard = new VBox:
      spacing = 10
      padding = Insets(18)
      styleClass = Seq("form-card")
      children = Seq(
        new Label("🛠️ Technical Architecture & Features"):
          style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
        ,
        new Label("• Core Stack: Scala 3.3.3 (JVM 21) & ScalaFX 21 (JavaFX UI toolkit)"):
          style = "-fx-text-fill: #374151; -fx-font-size: 13px;"
        ,
        new Label("• Functional Design: Pure immutable domain modeling with zero mutable logic collections"):
          style = "-fx-text-fill: #374151; -fx-font-size: 13px;"
        ,
        new Label("• Persistence & Export: Type-safe CSV file repositories wrapped in Try, with PDFBox daily distribution report exporting"):
          style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
      )

    children = Seq(titleLabel, overviewCard, sdgCard, techCard)

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
    // If local inventory.csv doesn't exist, try to seed from resources
    val localInv = new File(inventoryFile)
    if !localInv.exists() then
      Try {
        val stream = getClass.getResourceAsStream("/inventory.csv")
        if stream != null then
          val source = scala.io.Source.fromInputStream(stream)
          val writer = new java.io.PrintWriter(localInv)
          try
            source.getLines().foreach(writer.println)
          finally
            source.close()
            writer.close()
      }

    val localDem = new File(demandFile)
    if !localDem.exists() then
      Try {
        val stream = getClass.getResourceAsStream("/demand.csv")
        if stream != null then
          val source = scala.io.Source.fromInputStream(stream)
          val writer = new java.io.PrintWriter(localDem)
          try
            source.getLines().foreach(writer.println)
          finally
            source.close()
            writer.close()
      }

object UIUtils:
  import scalafx.scene.control.{Label, TableView}
  import scalafx.scene.shape.Rectangle

  def createRoundedClip(table: TableView[_]): Rectangle = new Rectangle {
    width <== table.width
    height <== table.height
    arcWidth = 24
    arcHeight = 24
  }

  /** Generic pill-shaped label with background, text colour, and border */
  def createPillLabel(text: String, bg: String, fg: String, borderColor: String): Label =
    new Label(text) {
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
      ("#f0fdf4", "#16a34a", "#bbf7d0")
    createPillLabel(statusStr, bg, fg, bc)

  def getDietaryLabel(restriction: String): Label =
    val (bg, fg, bc) = restriction match
      case "Vegetarian" => ("#ecfdf5", "#047857", "#a7f3d0")
      case "Halal"      => ("#fdf2f8", "#be185d", "#fbcfe8")
      case "GlutenFree" => ("#fffbeb", "#b45309", "#fde68a")
      case _            => ("#f1f5f9", "#475569", "#cbd5e1")
    createPillLabel(restriction, bg, fg, bc)

  def getStatusLabel(status: String): Label =
    val (bg, fg, bc) = status match
      case "Fulfilled" => ("#dcfce7", "#15803d", "#bbf7d0")
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

