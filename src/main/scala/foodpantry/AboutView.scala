package foodpantry

import scalafx.scene.layout.{VBox, HBox, Priority}
import scalafx.scene.control.Label
import scalafx.geometry.{Insets, Pos}

class AboutView extends VBox:
  spacing = 24
  padding = Insets(0)
  style = "-fx-background-color: #fffdf7;"

  // 1. Warm Hero Top Section
  private val heroHeader = new VBox:
    spacing = 10
    padding = Insets(40, 30, 40, 30)
    alignment = Pos.Center
    style = "-fx-background-color: linear-gradient(to bottom, #fef08a, #fffdf7); -fx-background-radius: 0 0 24px 24px;"
    children = Seq(
      new Label("✨ ABOUT US"):
        style = "-fx-text-fill: #b45309; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 2px;"
      ,
      new Label("About Food Pantry Manager"):
        style = "-fx-text-fill: #1e293b; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 32px;"
      ,
      new Label("Empowering community food banks through automated inventory tracking, household demand matching, and waste-minimizing distribution."):
        style = "-fx-text-fill: #475569; -fx-font-size: 14px; -fx-wrap-text: true; -fx-max-width: 650px; -fx-text-alignment: center;"
    )

  // Container with side padding for body content
  private val bodyContent = new VBox:
    spacing = 32
    padding = Insets(0, 32, 40, 32)

    // 2. Section 1: "We make sure food allocation & distribution is delivered properly"
    private val section1Title = new Label("We make sure your inventory & distribution is delivered properly"):
      style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 22px; -fx-wrap-text: true;"

    private val section1TextRow = new HBox:
      spacing = 24
      children = Seq(
        new VBox:
          HBox.setHgrow(this, Priority.Always)
          children = Seq(
            new Label("Pantry coordinators can log donated food items (perishable and shelf-stable), record household demands, and generate daily distribution plans to serve vulnerable families."):
              style = "-fx-text-fill: #475569; -fx-font-size: 13px; -fx-wrap-text: true;"
          )
        ,
        new VBox:
          HBox.setHgrow(this, Priority.Always)
          children = Seq(
            new Label("The system utilizes an expiry-first matching algorithm that prioritizes distributing food close to expiration to eliminate food waste while strictly respecting dietary constraints."):
              style = "-fx-text-fill: #475569; -fx-font-size: 13px; -fx-wrap-text: true;"
          )
      )

    private val section1 = new VBox:
      spacing = 14
      children = Seq(section1Title, section1TextRow)

    // 3. Section 2: Side-by-Side Highlight Banner Card
    private val quoteCard = new VBox:
      spacing = 8
      padding = Insets(24)
      prefWidth = 300
      style = "-fx-background-color: #fef9c3; -fx-background-radius: 16px; -fx-border-color: #fde047; -fx-border-radius: 16px; -fx-border-width: 1px;"
      children = Seq(
        new Label("“Making an impact, together”"):
          style = "-fx-text-fill: #854d0e; -fx-font-weight: bold; -fx-font-size: 16px; -fx-wrap-text: true;"
        ,
        new Label("Reducing food waste and supporting families in need across our local communities."):
          style = "-fx-text-fill: #a16207; -fx-font-size: 12px; -fx-wrap-text: true;"
      )

    private val empTextContainer = new VBox:
      spacing = 10
      alignment = Pos.CenterLeft
      HBox.setHgrow(this, Priority.Always)
      children = Seq(
        new Label("We empower community pantries & volunteers"):
          style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 20px; -fx-wrap-text: true;"
        ,
        new Label("By providing intuitive digital logging and automated matching engines, volunteers save hours of manual calculation every day and can focus directly on serving recipients."):
          style = "-fx-text-fill: #475569; -fx-font-size: 13px; -fx-wrap-text: true;"
        ,
        new Label("“Designed under UN Sustainable Development Goals (SDG 1: No Poverty & SDG 12: Responsible Consumption) to create lasting social impact.”"):
          style = "-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 13px; -fx-wrap-text: true; -fx-padding: 8 0 0 0;"
      )

    private val section2Card = new HBox:
      spacing = 24
      alignment = Pos.CenterLeft
      padding = Insets(20)
      style = "-fx-background-color: #ffffff; -fx-background-radius: 20px; -fx-border-color: #e2e8f0; -fx-border-radius: 20px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 12, 0, 0, 4);"
      children = Seq(quoteCard, empTextContainer)

    // 4. Section 3: Bottom 3 Value Pillars (Centered Tiles Row)
    private val pillarsTitle = new VBox:
      spacing = 6
      alignment = Pos.Center
      children = Seq(
        new Label("We help food banks run smarter & faster"):
          style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 22px; -fx-text-alignment: center;"
        ,
        new Label("Built with modern Scala 3 and ScalaFX architecture to deliver speed, accuracy, and reliability."):
          style = "-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-text-alignment: center;"
      )

    private val pillar1 = createPillarTile(
      "📊",
      "Smart Inventory",
      "Categorizes perishables & shelf-stable items with real-time stock alert thresholds."
    )

    private val pillar2 = createPillarTile(
      "🎯",
      "Targeted Distribution",
      "Matches dietary restrictions (Halal, Vegetarian, Gluten-Free) and family sizes."
    )

    private val pillar3 = createPillarTile(
      "🚛",
      "Zero Waste Impact",
      "Prioritizes items nearing expiry dates to eliminate landfill food waste."
    )

    private val pillarsRow = new HBox:
      spacing = 20
      alignment = Pos.Center
      children = Seq(pillar1, pillar2, pillar3)

    private val section3 = new VBox:
      spacing = 20
      children = Seq(pillarsTitle, pillarsRow)

    children = Seq(section1, section2Card, section3)

  children = Seq(heroHeader, bodyContent)

  // Helper method for the 3 Pillar Tiles at the bottom
  private def createPillarTile(icon: String, title: String, desc: String): VBox =
    val circleIcon = new Label(icon):
      style = "-fx-background-color: #fef08a; -fx-background-radius: 30px; -fx-min-width: 54px; -fx-min-height: 54px; -fx-alignment: center; -fx-font-size: 22px;"

    val tLabel = new Label(title):
      style = "-fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 15px; -fx-text-alignment: center;"

    val dLabel = new Label(desc):
      style = "-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-wrap-text: true; -fx-text-alignment: center;"

    new VBox:
      spacing = 10
      alignment = Pos.Center
      padding = Insets(20)
      prefWidth = 240
      HBox.setHgrow(this, Priority.Always)
      style = "-fx-background-color: #ffffff; -fx-background-radius: 16px; -fx-border-color: #e2e8f0; -fx-border-radius: 16px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
      children = Seq(circleIcon, tLabel, dLabel)
