package foodpantry

import scalafx.scene.layout.{VBox, HBox, Priority}
import scalafx.scene.control.Label
import scalafx.geometry.Insets

class AboutView extends VBox:
  spacing = 16
  padding = Insets(24)
  style = "-fx-background-color: #fbf9f4;"

  // 1. Header Banner (Warm Cream Accent)
  private val headerBanner = new VBox:
    spacing = 6
    padding = Insets(24)
    style = "-fx-background-color: linear-gradient(to right, #fffbeb, #fef3c7); -fx-background-radius: 12px; -fx-border-color: #fde047; -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
    children = Seq(
      new Label("Food Pantry Inventory & Demand Manager"):
        style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 22px;"
      ,
      new Label("Community Resource Allocation & Food Waste Reduction Platform"):
        style = "-fx-text-fill: #475569; -fx-font-size: 13px;"
    )

  // 2. Card 1: Mission & UN Sustainable Development Goals
  private val sdgTile1 = createSdgTile(
    "SDG 1: No Poverty",
    "#fff1f2", "#be123c", "#fecdd3",
    "Matches food packages to vulnerable households based on family size and dietary restrictions (Halal, Vegetarian, Gluten-Free)."
  )

  private val sdgTile2 = createSdgTile(
    "SDG 12: Responsible Consumption & Production",
    "#ecfdf5", "#047857", "#a7f3d0",
    "Prioritizes perishable food items close to expiration date to prevent food waste and minimize landfill impact."
  )

  private val sdgRow = new HBox:
    spacing = 14
    children = Seq(sdgTile1, sdgTile2)

  private val sdgCard = new VBox:
    spacing = 12
    padding = Insets(20)
    style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
    children = Seq(
      new Label("Mission & UN Sustainable Development Goals"):
        style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      sdgRow
    )

  // 3. Card 2: Core Application Features
  private val featuresCard = new VBox:
    spacing = 10
    padding = Insets(20)
    style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
    children = Seq(
      new Label("Core Application Features"):
        style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      createFeatureRow("📊 Real-Time Analytics Dashboard", "Provides KPI summary counters, category stock charts, and urgent perishable expiration alerts (<3 days)."),
      createFeatureRow("📦 Smart Inventory Logging", "Supports adding, searching, filtering, and deleting perishable and non-perishable food items."),
      createFeatureRow("👪 Family Request Management", "Records recipient family sizes, preference notes, and strict dietary constraints."),
      createFeatureRow("🚛 Automated Distribution & PDF Export", "Executes waste-minimizing allocation policy and generates downloadable daily PDF reports.")
    )

  // 4. Card 3: Technical Implementation
  private val techCard = new VBox:
    spacing = 10
    padding = Insets(20)
    style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #059669 #e2e8f0 #e2e8f0 #e2e8f0; -fx-border-radius: 12px; -fx-border-width: 4px 1px 1px 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
    children = Seq(
      new Label("Technical Implementation"):
        style = "-fx-text-fill: #059669; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      createTechRow("Language & UI Toolkit", "Scala 3.3.3 & ScalaFX 21 (JVM 21)"),
      createTechRow("Architecture", "Pure functional domain model with immutable data structures"),
      createTechRow("Data Storage", "Type-safe CSV file repositories wrapped in Try"),
      createTechRow("Report Engine", "Apache PDFBox automated report exporter")
    )

  children = Seq(headerBanner, sdgCard, featuresCard, techCard)

  // Helper Methods
  private def createSdgTile(title: String, bg: String, fg: String, bc: String, desc: String): VBox =
    val box = new VBox:
      spacing = 6
      padding = Insets(14)
      style = s"-fx-background-color: $bg; -fx-background-radius: 8px; -fx-border-color: $bc; -fx-border-radius: 8px; -fx-border-width: 1px;"
      children = Seq(
        new Label(title):
          style = s"-fx-text-fill: $fg; -fx-font-weight: bold; -fx-font-size: 13px;"
        ,
        new Label(desc):
          style = "-fx-text-fill: #334155; -fx-font-size: 12px; -fx-wrap-text: true;"
      )
    HBox.setHgrow(box, Priority.Always)
    box

  private def createFeatureRow(title: String, desc: String): VBox =
    new VBox:
      spacing = 2
      padding = Insets(10, 14, 10, 14)
      style = "-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-border-width: 1px;"
      children = Seq(
        new Label(title):
          style = "-fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 13px;"
        ,
        new Label(desc):
          style = "-fx-text-fill: #475569; -fx-font-size: 12px; -fx-wrap-text: true;"
      )

  private def createTechRow(label: String, value: String): VBox =
    new VBox:
      spacing = 2
      children = Seq(
        new Label(s"• $label"):
          style = "-fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 12px;"
        ,
        new Label(s"  $value"):
          style = "-fx-text-fill: #475569; -fx-font-size: 12px; -fx-wrap-text: true;"
      )
