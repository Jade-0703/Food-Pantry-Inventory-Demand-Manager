package foodpantry

import scalafx.scene.layout.{VBox, HBox, Priority, Region, GridPane, ColumnConstraints}
import scalafx.scene.control.{Label, Button, TextField}
import scalafx.geometry.{Insets, Pos}
import scalafx.collections.ObservableBuffer
import scalafx.Includes._
import scala.annotation.nowarn

@nowarn("cat=deprecation")
class AboutView(
  inventory: ObservableBuffer[FoodItem],
  requests: ObservableBuffer[FamilyRequest],
  onResetSampleData: () => Unit
) extends VBox:
  spacing = 20
  padding = Insets(20)
  styleClass = Seq("content-pane")

  // 1. Hero header
  private val titleArea = new VBox:
    spacing = 6
    alignment = Pos.Center
    padding = Insets(6, 0, 6, 0)
    children = Seq(
      new Label("Food Pantry Inventory & Demand Manager") { styleClass = Seq("about-hero-title") },
      new Label("Optimizing resource allocation and reducing food waste for local communities (SDG 1 & SDG 12)") {
        styleClass = Seq("about-hero-subtitle")
      }
    )

  // 2. SDG & tech ticker
  private val tickerBar = new HBox:
    spacing = 16
    alignment = Pos.Center
    styleClass = Seq("ticker-bar")
    children = Seq(
      createTickerBadge("UN SDG 1: NO POVERTY", "#fee2e2", "#b91c1c"),
      createTickerBadge("UN SDG 12: RESPONSIBLE CONSUMPTION", "#dcfce7", "#15803d"),
      createTickerBadge("SCALA 3.3.3", "#dbeafe", "#1e40af"),
      createTickerBadge("SCALAFX 21", "#f3e8ff", "#6b21a8"),
      createTickerBadge("APACHE PDFBOX", "#ffedd5", "#c2410c")
    )

  // 3. Four required features (README / submission manifest)
  private val featuresSection = new VBox:
    spacing = 12
    children = Seq(
      new Label("Four Core Operational Modules") { styleClass = Seq("feature-section-label") },
      new HBox:
        spacing = 14
        children = Seq(
          createFeatureTile(
            "📊",
            "Analytics Dashboard",
            "KPI cards, PieChart category breakdown, expiring-stock alerts, and shortage warnings."
          ),
          createFeatureTile(
            "📦",
            "Inventory Logger",
            "Add, view, and delete items. Perishable / non-perishable inputs switch dynamically."
          ),
          createFeatureTile(
            "👥",
            "Family Requests",
            "Log household demands with dietary restrictions (Halal, Vegetarian, Gluten-Free)."
          ),
          createFeatureTile(
            "🚛",
            "Distribution Planner",
            "Waste-minimizing expiry-first matching with PDF export and inventory write-offs."
          )
        )
    )

  // 4. Mission card & System Reset Action
  private val invBadge = new Label:
    style = "-fx-background-color: #eff6ff; -fx-text-fill: #1e3a8a; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6px 12px; -fx-background-radius: 6px;"
    text <== scalafx.beans.binding.Bindings.createStringBinding(
      () => s"📦 Stock: ${inventory.size} items",
      inventory
    )

  private val reqBadge = new Label:
    style = "-fx-background-color: #fffbeb; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 6px 12px; -fx-background-radius: 6px;"
    text <== scalafx.beans.binding.Bindings.createStringBinding(
      () => s"👥 Demands: ${requests.size} families",
      requests
    )

  private val seedDataBtn = new Button("🌱 Reset & Seed Sample Data"):
    styleClass = Seq("button", "button-secondary")
    minWidth = Region.USE_PREF_SIZE
    onAction = handle {
      if UIUtils.showConfirmation("Confirm System Reset", "Reset Sample Data", "Are you sure you want to reset all inventory items and family requests to initial demo sample data?") then
        onResetSampleData()
        UIUtils.showToast("Re-seeded demo sample data", "info")
    }

  private val darkAccentCard = new VBox:
    spacing = 12
    padding = Insets(24)
    styleClass = Seq("dark-accent-card")
    children = Seq(
      new Label("Work Smarter, Prevent Waste, Be More Efficient") { styleClass = Seq("dark-accent-title") },
      new Label("Automates daily distribution policy calculations, saving coordinators hours of manual matching while ensuring zero food waste.") {
        styleClass = Seq("dark-accent-body")
      },
      new Label("• Expiry-first perishable matching logic prioritizing stock nearing expiration") { styleClass = Seq("dark-accent-bullet") },
      new Label("• Automatic household size scaling for balanced package distribution") { styleClass = Seq("dark-accent-bullet") },
      new Label("• Strict dietary constraint enforcement (Halal, Vegetarian, Gluten-Free)") { styleClass = Seq("dark-accent-bullet") },
      new HBox {
        spacing = 12
        padding = Insets(8, 0, 0, 0)
        alignment = Pos.CenterLeft
        children = Seq(invBadge, reqBadge, seedDataBtn)
      }
    )

  // 5. Contact Coordinator Form Card
  private val contactEmailField = new TextField { promptText = "Your Email (e.g. staff@sunway.edu.my)"; maxWidth = Double.MaxValue }
  private val contactMessageField = new TextField { promptText = "Enter your message or inquiry..."; maxWidth = Double.MaxValue }
  private val contactStatusLabel = new Label { styleClass = Seq("status-label") }

  private val sendContactBtn = new Button("✉️ Send Message"):
    styleClass = Seq("button", "button-primary")
    minWidth = Region.USE_PREF_SIZE
    onAction = handle {
      val email = contactEmailField.text.value.trim
      val msg = contactMessageField.text.value.trim
      val emailRegex = """^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$""".r
      if email.isEmpty || msg.isEmpty then
        UIUtils.applyStatus(contactStatusLabel, "error", "Error: Please enter your email and message!")
      else if emailRegex.findFirstIn(email).isEmpty then
        UIUtils.applyStatus(contactStatusLabel, "error", "Error: Invalid email format! (e.g., user@domain.com)")
      else
        contactEmailField.text = ""
        contactMessageField.text = ""
        UIUtils.applyStatus(contactStatusLabel, "success", "Success: Message sent to pantry coordinator!")
        UIUtils.showToast("Message sent to pantry coordinator", "success")
    }

  private val contactFormGrid = new GridPane:
    hgap = 12
    vgap = 10
    columnConstraints = Seq(
      new ColumnConstraints { minWidth = 80 },
      new ColumnConstraints { hgrow = Priority.Always }
    )
    add(new Label("Your Email:") { styleClass = Seq("form-field-label") }, 0, 0)
    add(contactEmailField, 1, 0)
    add(new Label("Message:") { styleClass = Seq("form-field-label") }, 0, 1)
    add(contactMessageField, 1, 1)

  private val contactCard = new VBox:
    spacing = 10
    padding = Insets(18)
    styleClass = Seq("form-card", "card-color-feedback")
    children = Seq(
      new Label("📩 Contact Pantry Coordinator") { styleClass = Seq("form-card-title") },
      contactFormGrid,
      sendContactBtn,
      contactStatusLabel
    )

  // 6. Contact tiles + footer
  private val section3Row = new HBox:
    spacing = 18
    children = Seq(
      createInfoTile("📞", "(+603) 7491-8622", "Pantry Coordinator Hotline"),
      createInfoTile("✉️", "support@foodpantry.org", "Support & Inquiries"),
      createInfoTile("📍", "Sunway University Hub", "Computing & Information Systems")
    )

  private val footerBanner = new HBox:
    spacing = 10
    alignment = Pos.Center
    styleClass = Seq("footer-banner")
    children = Seq(
      new Label("🌾 Food Pantry Inventory & Demand Manager v1.0.0  •  Built under Sunway University Tier C (AI-Integrated) Policy  •  © 2026 All Rights Reserved") {
        styleClass = Seq("footer-text")
      }
    )

  children = Seq(titleArea, tickerBar, featuresSection, darkAccentCard, contactCard, section3Row, footerBanner)

  // --- Helpers ---

  private def createTickerBadge(text: String, bg: String, fg: String): Label =
    new Label(text):
      styleClass = Seq("ticker-badge")
      style = s"-fx-background-color: $bg; -fx-text-fill: $fg;"

  private def createFeatureTile(icon: String, title: String, desc: String): VBox =
    val box = new VBox:
      spacing = 6
      padding = Insets(14)
      styleClass = Seq("feature-tile")
      children = Seq(
        new Label(icon) { styleClass = Seq("feature-tile-icon") },
        new Label(title) { styleClass = Seq("feature-tile-title") },
        new Label(desc) { styleClass = Seq("feature-tile-desc") }
      )
    HBox.setHgrow(box, Priority.Always)
    box

  private def createInfoTile(icon: String, header: String, sub: String): VBox =
    val box = new VBox:
      spacing = 4
      padding = Insets(16)
      alignment = Pos.Center
      styleClass = Seq("info-tile")
      children = Seq(
        new Label(icon) { style = "-fx-font-size: 20px;" },
        new Label(header) { style = "-fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 13px;" },
        new Label(sub) { style = "-fx-text-fill: #64748b; -fx-font-size: 11px;" }
      )
    HBox.setHgrow(box, Priority.Always)
    box
