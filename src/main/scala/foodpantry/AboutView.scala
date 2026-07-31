package foodpantry

import scalafx.scene.layout.{VBox, HBox, Priority}
import scalafx.scene.control.Label
import scalafx.geometry.{Insets, Pos}

class AboutView extends VBox:
  spacing = 22
  padding = Insets(24)
  style = "-fx-background-color: #fafaf9;"

  // 1. Top Title Header
  private val titleArea = new VBox:
    spacing = 6
    alignment = Pos.Center
    padding = Insets(6, 0, 6, 0)
    children = Seq(
      new Label("Food Pantry Inventory & Demand Manager"):
        style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 26px; -fx-text-alignment: center;"
      ,
      new Label("Optimizing Resource Allocation & Reducing Food Waste for Local Communities"):
        style = "-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-text-alignment: center;"
    )

  // 2. Horizontal Ticker Ribbon (Soft Light Pastel Yellow)
  private val tickerRibbon = new HBox:
    spacing = 16
    alignment = Pos.Center
    padding = Insets(8, 20, 8, 20)
    style = "-fx-background-color: #fef9c3; -fx-background-radius: 20px; -fx-border-color: #fef08a; -fx-border-radius: 20px; -fx-border-width: 1px;"
    children = Seq(
      new Label("COMMUNITY PANTRY  •  UN SDG 1: NO POVERTY  •  UN SDG 12: RESPONSIBLE CONSUMPTION  •  ZERO WASTE ALGORITHM"):
        style = "-fx-text-fill: #854d0e; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;"
    )

  // 3. Center Highlight Callout Card (Lighter Pastel Cream with Dashed Border)
  private val dashedHighlightCard = new VBox:
    spacing = 12
    alignment = Pos.Center
    padding = Insets(26)
    style = "-fx-background-color: #fefce8; -fx-background-radius: 20px; -fx-border-color: #fde047; -fx-border-style: dashed; -fx-border-radius: 20px; -fx-border-width: 2px;"
    children = Seq(
      new Label("SUSTAINABILITY MISSION"):
        style = "-fx-background-color: #eab308; -fx-text-fill: #ffffff; -fx-padding: 3px 12px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 10px; -fx-letter-spacing: 1px;"
      ,
      new Label("Streamlining Food Bank Operations with Expiry-First Matching"):
        style = "-fx-text-fill: #1e293b; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 19px; -fx-text-alignment: center; -fx-wrap-text: true;"
      ,
      new Label("By combining perishable inventory tracking with recipient family dietary constraints (Halal, Vegetarian, Gluten-Free), our algorithm prioritizes items nearing expiration to ensure zero food waste while fulfilling household needs."):
        style = "-fx-text-fill: #475569; -fx-font-size: 13px; -fx-text-alignment: center; -fx-wrap-text: true; -fx-max-width: 750px;"
    )

  // 4. Bottom Lighter Pastel Yellow Container with 3 Feature Cards
  private val bottomSectionTitle = new Label("LET'S OPTIMIZE PANTRY DISTRIBUTION TOGETHER"):
    style = "-fx-text-fill: #a16207; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 12px; -fx-letter-spacing: 1.5px; -fx-text-alignment: center;"

  private val card1 = createFeatureCard(
    "📦", "#ffedd5",
    "STEP 01",
    "Smart Inventory Log",
    "Track perishable and non-perishable food items, record quantity units, and calculate expiration dates automatically."
  )

  private val card2 = createFeatureCard(
    "👪", "#dbeafe",
    "STEP 02",
    "Household Matching",
    "Log recipient family requests, household sizes, and strict dietary constraints (Halal, Vegetarian, Gluten-Free)."
  )

  private val card3 = createFeatureCard(
    "🚛", "#dcfce7",
    "STEP 03",
    "Waste-Minimizing Engine",
    "Executes automated allocation matching, prioritizes urgent items nearing expiry, and exports PDF daily reports."
  )

  private val cardsRow = new HBox:
    spacing = 18
    alignment = Pos.Center
    children = Seq(card1, card2, card3)

  private val bottomContainer = new VBox:
    spacing = 18
    alignment = Pos.Center
    padding = Insets(26)
    style = "-fx-background-color: #fef9c3; -fx-background-radius: 24px; -fx-border-color: #fef08a; -fx-border-radius: 24px; -fx-border-width: 1px;"
    children = Seq(bottomSectionTitle, cardsRow)

  children = Seq(titleArea, tickerRibbon, dashedHighlightCard, bottomContainer)

  // Helper method for the 3 Feature Cards with circular icon badges
  private def createFeatureCard(icon: String, iconBg: String, stepText: String, titleText: String, descText: String): VBox =
    val iconBadge = new Label(icon):
      style = s"-fx-background-color: $iconBg; -fx-background-radius: 24px; -fx-min-width: 48px; -fx-min-height: 48px; -fx-alignment: center; -fx-font-size: 20px;"

    val stepLabel = new Label(stepText):
      style = "-fx-text-fill: #a16207; -fx-font-weight: bold; -fx-font-size: 10px; -fx-letter-spacing: 1px;"

    val title = new Label(titleText):
      style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-wrap-text: true; -fx-text-alignment: center;"

    val desc = new Label(descText):
      style = "-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-wrap-text: true; -fx-text-alignment: center;"

    val card = new VBox:
      spacing = 10
      alignment = Pos.Center
      padding = Insets(20)
      style = "-fx-background-color: #ffffff; -fx-background-radius: 18px; -fx-border-color: #f1f5f9; -fx-border-radius: 18px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 4);"
      children = Seq(iconBadge, stepLabel, title, desc)

    HBox.setHgrow(card, Priority.Always)
    card
