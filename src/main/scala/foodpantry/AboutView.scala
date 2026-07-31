package foodpantry

import scalafx.scene.layout.{VBox, HBox, Priority}
import scalafx.scene.control.Label
import scalafx.geometry.{Insets, Pos}

class AboutView extends VBox:
  spacing = 24
  padding = Insets(20)
  style = "-fx-background-color: #fffdf7;"

  // 1. Top Title Header
  private val titleArea = new VBox:
    spacing = 6
    alignment = Pos.Center
    padding = Insets(10, 0, 10, 0)
    children = Seq(
      new Label("Food Pantry Inventory & Demand Manager"):
        style = "-fx-text-fill: #1e293b; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 26px; -fx-text-alignment: center;"
      ,
      new Label("Optimizing Resource Allocation & Reducing Food Waste for Local Communities"):
        style = "-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-text-alignment: center;"
    )

  // 2. Horizontal Ticker Ribbon
  private val tickerRibbon = new HBox:
    spacing = 16
    alignment = Pos.Center
    padding = Insets(8, 16, 8, 16)
    style = "-fx-background-color: #f8efde; -fx-background-radius: 8px; -fx-border-color: #e5e7eb; -fx-border-radius: 8px; -fx-border-width: 1px;"
    children = Seq(
      new Label("COMMUNITY PANTRY  •  UN SDG 1: NO POVERTY  •  UN SDG 12: RESPONSIBLE CONSUMPTION  •  ZERO WASTE ALGORITHM"):
        style = "-fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;"
    )

  // 3. Center Highlight Callout Card (Dashed Accent Border)
  private val dashedHighlightCard = new VBox:
    spacing = 10
    alignment = Pos.Center
    padding = Insets(24)
    style = "-fx-background-color: #fffbeb; -fx-background-radius: 16px; -fx-border-color: #f59e0b; -fx-border-style: dashed; -fx-border-radius: 16px; -fx-border-width: 2px;"
    children = Seq(
      new Label("SUSTAINABILITY MISSION"):
        style = "-fx-background-color: #d97706; -fx-text-fill: #ffffff; -fx-padding: 3px 10px; -fx-background-radius: 10px; -fx-font-weight: bold; -fx-font-size: 10px;"
      ,
      new Label("Streamlining Food Bank Operations with Expiry-First Matching"):
        style = "-fx-text-fill: #78350f; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 18px; -fx-text-alignment: center; -fx-wrap-text: true;"
      ,
      new Label("By combining perishable inventory tracking with recipient family dietary constraints (Halal, Vegetarian, Gluten-Free), our algorithm prioritizes items nearing expiration to ensure zero food waste while fulfilling household needs."):
        style = "-fx-text-fill: #92400e; -fx-font-size: 13px; -fx-text-alignment: center; -fx-wrap-text: true; -fx-max-width: 750px;"
    )

  // 4. Bottom Warm Yellow Container with 3 White Cards
  private val bottomSectionTitle = new Label("LET'S OPTIMIZE PANTRY DISTRIBUTION TOGETHER"):
    style = "-fx-text-fill: #854d0e; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-letter-spacing: 1px; -fx-text-alignment: center;"

  private val card1 = createWhiteCard(
    "INVENTORY LOGGING",
    "Smart Inventory Log",
    "Track perishable and non-perishable food items, record quantity units, and calculate expiration dates automatically."
  )

  private val card2 = createWhiteCard(
    "FAMILY DEMANDS",
    "Household Matching",
    "Log recipient family requests, household sizes, and strict dietary constraints (Halal, Vegetarian, Gluten-Free)."
  )

  private val card3 = createWhiteCard(
    "ALLOCATION PLAN",
    "Waste-Minimizing Engine",
    "Executes automated allocation matching, prioritizes urgent items nearing expiry, and exports PDF daily reports."
  )

  private val cardsRow = new HBox:
    spacing = 16
    alignment = Pos.Center
    children = Seq(card1, card2, card3)

  private val bottomContainer = new VBox:
    spacing = 16
    alignment = Pos.Center
    padding = Insets(24)
    style = "-fx-background-color: #fef08a; -fx-background-radius: 20px; -fx-border-color: #fde047; -fx-border-radius: 20px; -fx-border-width: 1px;"
    children = Seq(bottomSectionTitle, cardsRow)

  children = Seq(titleArea, tickerRibbon, dashedHighlightCard, bottomContainer)

  // Helper method for the 3 White Cards
  private def createWhiteCard(badgeText: String, titleText: String, descText: String): VBox =
    val badge = new Label(badgeText):
      style = "-fx-text-fill: #b45309; -fx-font-weight: bold; -fx-font-size: 10px; -fx-letter-spacing: 1px;"

    val title = new Label(titleText):
      style = "-fx-text-fill: #0f172a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 16px; -fx-wrap-text: true;"

    val desc = new Label(descText):
      style = "-fx-text-fill: #475569; -fx-font-size: 12px; -fx-wrap-text: true;"

    val card = new VBox:
      spacing = 10
      padding = Insets(20)
      style = "-fx-background-color: #ffffff; -fx-background-radius: 14px; -fx-border-color: #e2e8f0; -fx-border-radius: 14px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
      children = Seq(badge, title, desc)

    HBox.setHgrow(card, Priority.Always)
    card
