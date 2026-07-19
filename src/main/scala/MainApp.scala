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
  val inventoryItems: ObservableBuffer[FoodItem] = ObservableBuffer[FoodItem]()
  val familyRequests: ObservableBuffer[FamilyRequest] = ObservableBuffer[FamilyRequest]()

  override def start(): Unit =
    // Seed initial files if missing
    seedInitialData()

    // Load data from files
    loadAllData()

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

    // Navigation state container
    val activeView = ObjectProperty[scalafx.scene.Node](dashboardView)

    stage = new PrimaryStage:
      title = "Food Pantry Inventory & Demand Manager"
      width = 1100
      height = 750
      
      scene = new Scene:
        root = new BorderPane:
          // Left Sidebar Navigation
          left = new VBox:
            spacing = 10
            padding = Insets(20)
            style = "-fx-background-color: #1e293b; -fx-min-width: 220px;"
            
            val appTitle = new Label("Food Pantry"):
              style = "-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 0 0 15 0;"
            
            @annotation.nowarn("cat=deprecation")
            def createNavButton(text: String, view: scalafx.scene.Node): Button = 
              new Button(text):
                style = "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-pref-width: 180px; -fx-padding: 10px;"
                onAction = handle {
                  activeView.value = view
                }

            val btnDash = createNavButton("📊 Dashboard", dashboardView)
            val btnInv = createNavButton("📦 Inventory Log", inventoryView)
            val btnReq = createNavButton("👪 Family Requests", demandView)
            val btnDist = createNavButton("🚛 Distribution Plan", distributionView)

            // Dynamic background highlight for selected nav button
            activeView.onChange { (_, _, newView) =>
              Seq(
                (btnDash, dashboardView),
                (btnInv, inventoryView),
                (btnReq, demandView),
                (btnDist, distributionView)
              ).foreach { case (btn, viewInstance) =>
                if newView == viewInstance then
                  btn.style = "-fx-background-color: #3b82f6; -fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-pref-width: 180px; -fx-padding: 10px; -fx-background-radius: 6px;"
                else
                  btn.style = "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-pref-width: 180px; -fx-padding: 10px;"
              }
            }

            // Set dashboard active by default
            btnDash.style = "-fx-background-color: #3b82f6; -fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center-left; -fx-pref-width: 180px; -fx-padding: 10px; -fx-background-radius: 6px;"

            children = Seq(appTitle, btnDash, btnInv, btnReq, btnDist)
          
          // Main Content Pane
          center <== activeView

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
