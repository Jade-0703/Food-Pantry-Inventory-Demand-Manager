package foodpantry

import scalafx.scene.layout.{VBox, HBox, Priority, Region, GridPane}
import scalafx.scene.control.Label
import scalafx.geometry.{Insets, Pos}

class AboutView extends VBox:
  spacing = 20
  padding = Insets(24)
  style = "-fx-background-color: #fbf9f4;"

  // 1. Hero Banner Card
  private val heroBanner = new VBox:
    spacing = 12
    padding = Insets(24)
    style = "-fx-background-color: linear-gradient(to right, #1e3a8a, #0f172a); -fx-background-radius: 16px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 15, 0, 0, 6);"

    private val badge = new Label("🌾 COMMUNITY FOOD BANK SYSTEM"):
      style = "-fx-background-color: rgba(251, 191, 36, 0.15); -fx-text-fill: #fbbf24; -fx-padding: 4px 12px; -fx-background-radius: 20px; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: rgba(251, 191, 36, 0.3); -fx-border-radius: 20px;"

    private val title = new Label("Food Pantry Inventory & Demand Manager"):
      style = "-fx-text-fill: #ffffff; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 24px;"

    private val subtitle = new Label("An intelligent, waste-minimizing resource allocation desktop application built with Scala 3 & ScalaFX to eliminate hunger and reduce food waste."):
      style = "-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-wrap-text: true;"

    private val tagPills = new HBox:
      spacing = 8
      alignment = Pos.CenterLeft
      children = Seq(
        createTagPill("Scala 3.3.3", "#3b82f6", "#ffffff"),
        createTagPill("ScalaFX 21", "#10b981", "#ffffff"),
        createTagPill("UN SDG 1 & 12", "#f59e0b", "#ffffff"),
        createTagPill("PDFBox Exporter", "#8b5cf6", "#ffffff"),
        createTagPill("v1.0.0 Production", "#64748b", "#ffffff")
      )

    children = Seq(badge, title, subtitle, tagPills)

  // 2. Left Column: UN SDGs & Key Features
  private val leftColumn = new VBox:
    spacing = 18
    HBox.setHgrow(this, Priority.Always)

    // Card A: UN SDG Alignment
    private val sdgCard = createCard("🎯 UN Sustainable Development Goals (SDGs)", "#ef4444", Seq(
      createFeatureRow(
        "SDG 1: No Poverty",
        "#fee2e2", "#b91c1c", "#fca5a5",
        "Optimizes resource distribution to vulnerable families, scaling package quantities dynamically based on household size and dietary constraints."
      ),
      createFeatureRow(
        "SDG 12: Responsible Consumption",
        "#dcfce7", "#15803d", "#bbf7d0",
        "Implements a Waste-Minimizing Expiry-First policy engine that prioritizes perishable stock closest to expiration, preventing landfill food waste."
      )
    ))

    // Card B: Key System Capabilities
    private val featuresCard = createCard("💡 Key System Capabilities", "#3b82f6", Seq(
      createBulletPoint("📊 Analytics Dashboard", "Real-time KPI metrics, stock category PieCharts, and instant perishable alerts (<3 days)."),
      createBulletPoint("📦 Smart Inventory Logging", "Log perishable & non-perishable items with automatic expiry status calculations."),
      createBulletPoint("👪 Household Request Management", "Track family sizes, specific requests, and dietary preferences (Halal, Vegetarian, Gluten-Free)."),
      createBulletPoint("🚛 Automated Distribution & PDF Export", "Execute waste-minimizing allocation policy and generate instant PDF report documents.")
    ))

    children = Seq(sdgCard, featuresCard)

  // 3. Right Column: Technical Architecture & Academic Info
  private val rightColumn = new VBox:
    spacing = 18
    HBox.setHgrow(this, Priority.Always)

    // Card C: Technical Architecture
    private val techCard = createCard("🛠️ Technical Architecture & Quality Standards", "#10b981", Seq(
      createBulletPoint("⚡ Pure Functional Domain Core", "100% immutable domain models with zero mutable collections in logic."),
      createBulletPoint("🛡️ Safe Exception Handling", "All file operations and CSV parsing routines are safely wrapped inside scala.util.Try."),
      createBulletPoint("✨ Zero Compiler Warnings", "Fully clean build under Scala 3 -Wunused compilation flags and strict type safety."),
      createBulletPoint("💾 Type-Safe Persistence", "Generic FileRepository[T] implementation for robust data persistence.")
    ))

    // Card D: Academic Credentials
    private val academicCard = createCard("🎓 Project & Academic Credentials", "#8b5cf6", Seq(
      createBulletPoint("🏛️ Institution", "Sunway University • Department of Computing & Information Systems"),
      createBulletPoint("📋 Project Type", "Final Programming Project • Food Pantry Manager"),
      createBulletPoint("🤖 Academic Integrity", "Built under Sunway University Tier C (AI-Integrated) Academic Policy."),
      createBulletPoint("🔗 GitHub Repository", "github.com/sunwaydcis/final-project-Jade-0703")
    ))

    children = Seq(techCard, academicCard)

  // Main 2-Column Grid Container
  private val gridContainer = new HBox:
    spacing = 18
    children = Seq(leftColumn, rightColumn)

  // 4. Bottom Quick Tip Banner
  private val bottomBanner = new HBox:
    spacing = 12
    padding = Insets(14, 20, 14, 20)
    alignment = Pos.CenterLeft
    style = "-fx-background-color: #f0fdf4; -fx-background-radius: 12px; -fx-border-color: #bbf7d0; -fx-border-radius: 12px; -fx-border-width: 1px;"
    children = Seq(
      new Label("💡"):
        style = "-fx-font-size: 16px;"
      ,
      new Label("Quick Tip: Use the left navigation sidebar to switch between 📊 Dashboard, 📦 Inventory Log, 👪 Family Requests, and 🚛 Distribution Plan!"):
        style = "-fx-text-fill: #166534; -fx-font-weight: bold; -fx-font-size: 13px;"
    )

  children = Seq(heroBanner, gridContainer, bottomBanner)

  // --- Helper Methods ---

  private def createTagPill(text: String, bg: String, fg: String): Label =
    new Label(text):
      style = s"-fx-background-color: $bg; -fx-text-fill: $fg; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px;"

  private def createCard(titleText: String, accentColor: String, contentNodes: Seq[scalafx.scene.Node]): VBox =
    new VBox:
      spacing = 12
      padding = Insets(20)
      style = s"-fx-background-color: #ffffff; -fx-background-radius: 14px; -fx-border-color: #e2e8f0; -fx-border-radius: 14px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 4); -fx-border-color: $accentColor #e2e8f0 #e2e8f0 #e2e8f0; -fx-border-width: 4px 1px 1px 1px;"
      children = Seq(
        new Label(titleText):
          style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ) ++ contentNodes

  private def createBulletPoint(header: String, body: String): VBox =
    new VBox:
      spacing = 2
      children = Seq(
        new Label(header):
          style = "-fx-text-fill: #1e293b; -fx-font-weight: bold; -fx-font-size: 13px;"
        ,
        new Label(body):
          style = "-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-wrap-text: true;"
      )

  private def createFeatureRow(badgeText: String, bg: String, fg: String, bc: String, description: String): VBox =
    new VBox:
      spacing = 6
      children = Seq(
        new Label(badgeText):
          style = s"-fx-background-color: $bg; -fx-text-fill: $fg; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 11px; -fx-border-color: $bc; -fx-border-radius: 12px; -fx-border-width: 1px;"
        ,
        new Label(description):
          style = "-fx-text-fill: #475569; -fx-font-size: 12px; -fx-wrap-text: true;"
      )
