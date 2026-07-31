package foodpantry

import scalafx.scene.layout.VBox
import scalafx.scene.control.Label
import scalafx.geometry.Insets

class AboutView extends VBox:
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
