package foodpantry

import scalafx.scene.layout.VBox
import scalafx.scene.control.Label
import scalafx.geometry.Insets

class AboutView extends VBox:
  spacing = 16
  padding = Insets(24)
  style = "-fx-background-color: #fbf9f4;"

  // Header Title Area
  private val headerArea = new VBox:
    spacing = 4
    children = Seq(
      new Label("Food Pantry Management System"):
        style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 22px;"
      ,
      new Label("Community Resource Allocation & Food Waste Reduction Platform"):
        style = "-fx-text-fill: #64748b; -fx-font-family: 'Inter'; -fx-font-size: 13px;"
    )

  // Card 1: Project Overview & Mission
  private val purposeCard = new VBox:
    spacing = 8
    padding = Insets(20)
    style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #1e3a8a #e2e8f0 #e2e8f0 #e2e8f0; -fx-border-radius: 12px; -fx-border-width: 4px 1px 1px 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
    children = Seq(
      new Label("Project Overview & Mission"):
        style = "-fx-text-fill: #1e3a8a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      new Label("Designed for food bank coordinators and volunteers, this platform streamlines inventory logging, tracks household demands, and automates daily food allocation to ensure fair distribution while eliminating food waste."):
        style = "-fx-text-fill: #334155; -fx-font-size: 13px; -fx-wrap-text: true;"
    )

  // Card 2: UN Sustainable Development Goals (SDGs)
  private val sdgCard = new VBox:
    spacing = 10
    padding = Insets(20)
    style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
    children = Seq(
      new Label("UN Sustainable Development Goals"):
        style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      createSdgTile(
        "SDG 1: No Poverty",
        "#fff1f2", "#be123c", "#fecdd3",
        "Matches food packages to vulnerable households based on family size and dietary restrictions (Halal, Vegetarian, Gluten-Free)."
      ),
      createSdgTile(
        "SDG 12: Responsible Consumption & Production",
        "#ecfdf5", "#047857", "#a7f3d0",
        "Prioritizes perishable food items close to expiration date to prevent food waste and minimize landfill impact."
      )
    )

  // Card 3: Core Application Features
  private val featuresCard = new VBox:
    spacing = 10
    padding = Insets(20)
    style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
    children = Seq(
      new Label("Core Application Features"):
        style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      createFeatureRow("Dashboard", "Provides KPI summary counters, category stock charts, and urgent perishable expiration alerts."),
      createFeatureRow("Inventory Log", "Supports adding, searching, filtering, and deleting perishable and non-perishable food items."),
      createFeatureRow("Family Requests", "Records recipient family sizes, preference notes, and strict dietary constraints."),
      createFeatureRow("Distribution Plan", "Executes waste-minimizing allocation policy and generates downloadable PDF daily reports.")
    )

  // Card 4: Technical Implementation
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

  children = Seq(headerArea, purposeCard, sdgCard, featuresCard, techCard)

  // Helper Methods
  private def createSdgTile(title: String, bg: String, fg: String, bc: String, desc: String): VBox =
    new VBox:
      spacing = 4
      padding = Insets(12)
      style = s"-fx-background-color: $bg; -fx-background-radius: 8px; -fx-border-color: $bc; -fx-border-radius: 8px; -fx-border-width: 1px;"
      children = Seq(
        new Label(title):
          style = s"-fx-text-fill: $fg; -fx-font-weight: bold; -fx-font-size: 13px;"
        ,
        new Label(desc):
          style = "-fx-text-fill: #334155; -fx-font-size: 12px; -fx-wrap-text: true;"
      )

  private def createFeatureRow(title: String, desc: String): VBox =
    new VBox:
      spacing = 2
      padding = Insets(8, 12, 8, 12)
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
