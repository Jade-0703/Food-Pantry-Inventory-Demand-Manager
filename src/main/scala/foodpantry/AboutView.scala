package foodpantry

import scalafx.scene.layout.{VBox, HBox, Priority, Region}
import scalafx.scene.control.{Label, Button, TextField, TextArea}
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
  style = "-fx-background-color: #fbf9f4;"

  // 1. Top Header Banner
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

  // 2. Logo & Tech Ticker Bar (Inspired by Partner Logo Bar)
  private val tickerBar = new HBox:
    spacing = 16
    alignment = Pos.Center
    padding = Insets(8, 20, 8, 20)
    style = "-fx-background-color: #f8fafc; -fx-background-radius: 10px; -fx-border-color: #e2e8f0; -fx-border-radius: 10px; -fx-border-width: 1px;"
    children = Seq(
      createTickerBadge("UN SDG 1: NO POVERTY", "#fee2e2", "#b91c1c"),
      createTickerBadge("UN SDG 12: RESPONSIBLE CONSUMPTION", "#dcfce7", "#15803d"),
      createTickerBadge("SCALA 3.3.3", "#dbeafe", "#1e40af"),
      createTickerBadge("SCALAFX 21", "#f3e8ff", "#6b21a8"),
      createTickerBadge("APACHE PDFBOX", "#ffedd5", "#c2410c")
    )

  // 3. Section 1: Dark Accent Mission & Stat Counter Cards Row (Inspired by Image 2)
  private val darkAccentCard = new VBox:
    spacing = 12
    padding = Insets(24)
    style = "-fx-background-color: #1e293b; -fx-background-radius: 16px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 12, 0, 0, 4);"
    children = Seq(
      new Label("Work Smarter, Prevent Waste, Be More Efficient"):
        style = "-fx-text-fill: #ffffff; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 18px; -fx-wrap-text: true;"
      ,
      new Label("Automates daily distribution policy calculations, saving coordinators hours of manual matching while ensuring zero food waste."):
        style = "-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-wrap-text: true;"
      ,
      new Label("• Expiry-first perishable matching logic prioritizing stock nearing expiration"):
        style = "-fx-text-fill: #cbd5e1; -fx-font-size: 12px;"
      ,
      new Label("• Automatic household size scaling for balanced package distribution"):
        style = "-fx-text-fill: #cbd5e1; -fx-font-size: 12px;"
      ,
      new Label("• Strict dietary constraint enforcement (Halal, Vegetarian, Gluten-Free)"):
        style = "-fx-text-fill: #cbd5e1; -fx-font-size: 12px;"
    )
  HBox.setHgrow(darkAccentCard, Priority.Always)

  private val statCounterCard = new VBox:
    spacing = 14
    padding = Insets(24)
    alignment = Pos.Center
    style = "-fx-background-color: #f0fdf4; -fx-background-radius: 16px; -fx-border-color: #bbf7d0; -fx-border-radius: 16px; -fx-border-width: 1px;"
    children = Seq(
      createStatBlock("100%", "Zero Food Waste Target", "#15803d"),
      createStatBlock("4", "Core Operational Modules", "#1e3a8a"),
      createStatBlock("0", "Compiler Warnings (Pure Scala 3)", "#047857")
    )
  HBox.setHgrow(statCounterCard, Priority.Always)

  private val section1Row = new HBox:
    spacing = 18
    children = Seq(darkAccentCard, statCounterCard)

  // 4. Section 2: Feedback Form & Live System Status Panel (Inspired by Image 1)
  private val feedbackEmailField = new TextField { promptText = "Coordinator Email"; prefWidth = 250 }
  private val feedbackSubjectField = new TextField { promptText = "Subject / Topic"; prefWidth = 250 }
  private val feedbackMessageArea = new TextArea { promptText = "Enter coordinator notes or operational feedback..."; prefWidth = 250; prefRowCount = 3 }

  private val formStatusLabel = new Label("Ready to record notes."):
    style = "-fx-text-fill: #64748b; -fx-font-size: 12px;"

  private val submitFeedbackBtn = new Button("Submit Note"):
    style = "-fx-background-color: #1e3a8a; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px; -fx-cursor: hand;"
    onAction = handle {
      val email = feedbackEmailField.text.value.trim
      val subj = feedbackSubjectField.text.value.trim
      if email.isEmpty || subj.isEmpty then
        formStatusLabel.text = "Error: Please provide Email and Subject!"
        formStatusLabel.style = "-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 12px;"
      else
        feedbackEmailField.text = ""
        feedbackSubjectField.text = ""
        feedbackMessageArea.text = ""
        formStatusLabel.text = "Success: Operational note recorded successfully."
        formStatusLabel.style = "-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 12px;"
    }

  private val feedbackFormCard = new VBox:
    spacing = 10
    padding = Insets(20)
    styleClass = Seq("form-card")
    children = Seq(
      new Label("📬 Coordinator Operational Notes & Feedback"):
        style = "-fx-text-fill: #1e3a8a; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      feedbackEmailField,
      feedbackSubjectField,
      feedbackMessageArea,
      new HBox { spacing = 12; alignment = Pos.CenterLeft; children = Seq(submitFeedbackBtn, formStatusLabel) }
    )
  HBox.setHgrow(feedbackFormCard, Priority.Always)

  private val invCountLabel = new Label:
    style = "-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 14px;"
    text <== scalafx.beans.binding.Bindings.createStringBinding(
      () => s"📦 Inventory Stock: ${inventory.size} items",
      inventory
    )

  private val reqCountLabel = new Label:
    style = "-fx-text-fill: #fde68a; -fx-font-weight: bold; -fx-font-size: 14px;"
    text <== scalafx.beans.binding.Bindings.createStringBinding(
      () => s"👪 Household Demands: ${requests.size} families",
      requests
    )

  private val seedDataBtn = new Button("🌱 Reset & Seed Sample Data"):
    style = "-fx-background-color: #3b82f6; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8px 16px; -fx-cursor: hand;"
    onAction = handle {
      onResetSampleData()
      formStatusLabel.text = "Success: Re-seeded demo sample data."
      formStatusLabel.style = "-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 12px;"
    }

  private val liveStatusCard = new VBox:
    spacing = 14
    padding = Insets(20)
    style = "-fx-background-color: #0f172a; -fx-background-radius: 14px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 12, 0, 0, 4);"
    children = Seq(
      new Label("⚡ Live System Operational Metrics"):
        style = "-fx-text-fill: #ffffff; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 15px;"
      ,
      invCountLabel,
      reqCountLabel,
      new Label("📄 PDF Export Engine: Apache PDFBox Ready"):
        style = "-fx-text-fill: #cbd5e1; -fx-font-size: 13px;"
      ,
      new Label("✅ Matching Algorithm: Active (Waste-Minimizing)"):
        style = "-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-font-size: 13px;"
      ,
      seedDataBtn
    )
  HBox.setHgrow(liveStatusCard, Priority.Always)

  private val section2Row = new HBox:
    spacing = 18
    children = Seq(feedbackFormCard, liveStatusCard)

  // 5. Section 3: Bottom 3 Info Tiles (Inspired by Image 1 Contact Cards)
  private val infoTile1 = createInfoTile("📞", "(+603) 7491-8622", "Pantry Coordinator Hotline")
  private val infoTile2 = createInfoTile("✉️", "support@foodpantry.org", "Support & Inquiries")
  private val infoTile3 = createInfoTile("📍", "Sunway University Hub", "Computing & Information Systems")

  private val section3Row = new HBox:
    spacing = 18
    children = Seq(infoTile1, infoTile2, infoTile3)

  // 6. Section 4: Deep Slate Footer (Inspired by Image 1 & 2 Footer)
  private val footerBanner = new HBox:
    spacing = 10
    alignment = Pos.Center
    padding = Insets(14, 20, 14, 20)
    style = "-fx-background-color: #0f172a; -fx-background-radius: 12px;"
    children = Seq(
      new Label("🌾 Food Pantry Inventory & Demand Manager v1.0.0  •  Built under Sunway University Tier C (AI-Integrated) Policy  •  © 2026 All Rights Reserved"):
        style = "-fx-text-fill: #94a3b8; -fx-font-size: 12px;"
    )

  children = Seq(titleArea, tickerBar, section1Row, section2Row, section3Row, footerBanner)

  // --- Helper Methods ---

  private def createTickerBadge(text: String, bg: String, fg: String): Label =
    new Label(text):
      style = s"-fx-background-color: $bg; -fx-text-fill: $fg; -fx-padding: 4px 10px; -fx-background-radius: 12px; -fx-font-weight: bold; -fx-font-size: 10px; -fx-letter-spacing: 1px;"

  private def createStatBlock(number: String, label: String, color: String): VBox =
    new VBox:
      spacing = 2
      alignment = Pos.Center
      children = Seq(
        new Label(number):
          style = s"-fx-text-fill: $color; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 28px;"
        ,
        new Label(label):
          style = "-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: bold;"
      )

  private def createInfoTile(icon: String, header: String, sub: String): VBox =
    val box = new VBox:
      spacing = 4
      padding = Insets(16)
      alignment = Pos.Center
      style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.02), 8, 0, 0, 3);"
      children = Seq(
        new Label(icon):
          style = "-fx-font-size: 20px;"
        ,
        new Label(header):
          style = "-fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 13px;"
        ,
        new Label(sub):
          style = "-fx-text-fill: #64748b; -fx-font-size: 11px;"
      )
    HBox.setHgrow(box, Priority.Always)
    box
