package foodpantry

import scalafx.scene.layout.{VBox, HBox, Priority}
import scalafx.scene.control.Label
import scalafx.geometry.{Insets, Pos}

class AboutView extends VBox:
  spacing = 18
  padding = Insets(20)
  style = "-fx-background-color: #fbf9f4;"

  // 1. Header Title Area
  private val titleArea = new VBox:
    spacing = 4
    children = Seq(
      new Label("Food Pantry Inventory & Demand Manager"):
        style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 22px;"
      ,
      new Label("Community Resource Allocation & Food Waste Reduction Platform"):
        style = "-fx-text-fill: #64748b; -fx-font-size: 13px;"
    )

  // 2. Ticker Ribbon Bar (Matching Sidebar Theme)
  private val tickerRibbon = new HBox:
    spacing = 16
    alignment = Pos.Center
    padding = Insets(8, 18, 8, 18)
    style = "-fx-background-color: #f8efde; -fx-background-radius: 8px; -fx-border-color: #e5e7eb; -fx-border-radius: 8px; -fx-border-width: 1px;"
    children = Seq(
      new Label("COMMUNITY PANTRY  •  UN SDG 1: NO POVERTY  •  UN SDG 12: RESPONSIBLE CONSUMPTION  •  ZERO WASTE ALGORITHM"):
        style = "-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;"
    )

  // 3. Card 1: Mission & UN Sustainable Development Goals
  private val sdgTile1 = createSdgTile(
    "SDG 1: No Poverty",
    "#fef2f2", "#dc2626", "#fca5a5",
    "Matches food packages to vulnerable households based on family size and dietary restrictions (Halal, Vegetarian, Gluten-Free)."
  )

  private val sdgTile2 = createSdgTile(
    "SDG 12: Responsible Consumption & Production",
    "#f0fdf4", "#16a34a", "#bbf7d0",
    "Prioritizes perishable food items close to expiration date to prevent food waste and minimize landfill impact."
  )

  private val sdgRow = new HBox:
    spacing = 12
    children = Seq(sdgTile1, sdgTile2)

  private val missionCard = new VBox:
    spacing = 12
    padding = Insets(18)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("Mission & UN Sustainable Development Goals"):
        style = "-fx-text-fill: #1e3a8a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 16px;"
      ,
      new Label("Designed for food bank coordinators and volunteers, this platform streamlines inventory logging, tracks household demands, and automates daily food allocation to ensure fair distribution while eliminating food waste."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
      ,
      sdgRow
    )

  // 4. Card 2: Core Application Features
  private val featuresCard = new VBox:
    spacing = 10
    padding = Insets(18)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("Core Application Features"):
        style = "-fx-text-fill: #1e3a8a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 16px;"
      ,
      createFeatureTile("📊 Real-Time Analytics Dashboard", "#f0f9ff", "#0369a1", "#bae6fd", "Provides KPI summary counters, category stock charts, and urgent perishable expiration alerts (<3 days)."),
      createFeatureTile("📦 Smart Inventory Logging", "#f8fafc", "#334155", "#cbd5e1", "Supports adding, searching, filtering, and deleting perishable and non-perishable food items with automatic expiry status calculation."),
      createFeatureTile("👪 Family Request Management", "#fffbeb", "#b45309", "#fde68a", "Records recipient family details, household sizes, preference notes, and strict dietary constraints."),
      createFeatureTile("🚛 Automated Distribution & PDF Export", "#f0fdf4", "#15803d", "#bbf7d0", "Executes waste-minimizing allocation policy and generates downloadable daily PDF reports.")
    )

  // 5. Card 3: Technical Architecture
  private val techCard = new VBox:
    spacing = 10
    padding = Insets(18)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("Technical Architecture & Implementation"):
        style = "-fx-text-fill: #1e3a8a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 16px;"
      ,
      createTechRow("Language & UI Toolkit", "Scala 3.3.3 & ScalaFX 21 (JVM 21)"),
      createTechRow("Architecture", "Pure functional domain model with 100% immutable data structures"),
      createTechRow("Data Storage", "Type-safe CSV file repositories wrapped in Try"),
      createTechRow("Report Engine", "Apache PDFBox automated daily report exporter")
    )

  children = Seq(titleArea, tickerRibbon, missionCard, featuresCard, techCard)

  // Helper Methods
  private def createSdgTile(title: String, bg: String, fg: String, bc: String, desc: String): VBox =
    val box = new VBox:
      spacing = 4
      padding = Insets(12)
      style = s"-fx-background-color: $bg; -fx-background-radius: 8px; -fx-border-color: $bc; -fx-border-radius: 8px; -fx-border-width: 1px;"
      children = Seq(
        new Label(title):
          style = s"-fx-text-fill: $fg; -fx-font-weight: bold; -fx-font-size: 13px;"
        ,
        new Label(desc):
          style = "-fx-text-fill: #374151; -fx-font-size: 12px; -fx-wrap-text: true;"
      )
    HBox.setHgrow(box, Priority.Always)
    box

  private def createFeatureTile(title: String, bg: String, fg: String, bc: String, desc: String): VBox =
    new VBox:
      spacing = 3
      padding = Insets(10, 14, 10, 14)
      style = s"-fx-background-color: $bg; -fx-background-radius: 8px; -fx-border-color: $bc; -fx-border-radius: 8px; -fx-border-width: 1px;"
      children = Seq(
        new Label(title):
          style = s"-fx-text-fill: $fg; -fx-font-weight: bold; -fx-font-size: 13px;"
        ,
        new Label(desc):
          style = "-fx-text-fill: #374151; -fx-font-size: 12px; -fx-wrap-text: true;"
      )

  private def createTechRow(label: String, value: String): VBox =
    new VBox:
      spacing = 2
      children = Seq(
        new Label(s"• $label:"):
          style = "-fx-text-fill: #111827; -fx-font-weight: bold; -fx-font-size: 12px;"
        ,
        new Label(s"  $value"):
          style = "-fx-text-fill: #475569; -fx-font-size: 12px; -fx-wrap-text: true;"
      )
