package foodpantry

import scalafx.scene.layout.VBox
import scalafx.scene.control.Label
import scalafx.geometry.Insets

class AboutView extends VBox:
  spacing = 15
  padding = Insets(20)
  style = "-fx-background-color: #fbf9f4;"

  // Header Title
  private val titleLabel = new Label("Pantry System Overview"):
    style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 20px;"

  // System Purpose Card
  private val purposeCard = new VBox:
    spacing = 8
    padding = Insets(18)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("Food Pantry Inventory & Demand Manager"):
        style = "-fx-text-fill: #1e3a8a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 16px;"
      ,
      new Label("This application helps food pantry staff and volunteers manage donated food inventory, track household requests, and automatically generate fair daily distribution plans while minimizing food waste."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
    )

  // UN SDGs Card
  private val sdgCard = new VBox:
    spacing = 8
    padding = Insets(18)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("Sustainable Development Goals (SDGs)"):
        style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      new Label("• SDG 1 (No Poverty): Matches food packages to families based on household size and dietary restrictions (Halal, Vegetarian, Gluten-Free)."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
      ,
      new Label("• SDG 12 (Responsible Consumption and Production): Uses an expiry-first policy to distribute food items before they spoil."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
    )

  // Navigation Guide Card
  private val navCard = new VBox:
    spacing = 8
    padding = Insets(18)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("Application Features"):
        style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      new Label("• Dashboard: View total inventory counts, pending family requests, category breakdown chart, and urgent expiring items."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
      ,
      new Label("• Inventory Log: Add, search, filter, and delete perishable and non-perishable food items."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
      ,
      new Label("• Family Requests: Log recipient family details, household sizes, and dietary constraints."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
      ,
      new Label("• Distribution Plan: Automatically generate allocation plans, fulfill orders, and export PDF reports."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px; -fx-wrap-text: true;"
    )

  // Technical Info Card
  private val techCard = new VBox:
    spacing = 8
    padding = Insets(18)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("Technical Overview"):
        style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      new Label("• Developed using Scala 3.3.3 and ScalaFX 21."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px;"
      ,
      new Label("• Uses CSV file persistence for inventory and demand records."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px;"
      ,
      new Label("• Includes automated PDF report generation via Apache PDFBox."):
        style = "-fx-text-fill: #374151; -fx-font-size: 13px;"
    )

  children = Seq(titleLabel, purposeCard, sdgCard, navCard, techCard)
