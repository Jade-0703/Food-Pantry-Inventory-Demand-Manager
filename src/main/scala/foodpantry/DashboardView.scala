package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.scene.chart._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.Includes._
import java.time.LocalDate

class DashboardView(
  inventory: ObservableBuffer[FoodItem],
  requests: ObservableBuffer[FamilyRequest]
) extends VBox:
  
  spacing = 20
  padding = Insets(20)
  styleClass = Seq("content-pane")

  private val headerBlock = UIUtils.createPageHeader(
    "Dashboard & Pantry Analytics",
    "Real-time KPIs, category PieChart, dietary needs overview, and expiring-stock alerts (SDG 1 & 12)."
  )

  private val summaryTitle = new Label("Pantry operations at a glance") {
    styleClass = Seq("dashboard-summary-title")
  }
  private val summaryText = new Label("Loading today’s inventory and request overview...") {
    styleClass = Seq("dashboard-summary-text")
    wrapText = true
  }
  private val summaryBadge = new Label("Live overview") {
    styleClass = Seq("dashboard-summary-badge")
  }
  private val summaryTextBox = new VBox:
    spacing = 4
    children = Seq(summaryTitle, summaryText)
  HBox.setHgrow(summaryTextBox, Priority.Always)

  private val summaryBanner = new HBox:
    spacing = 14
    alignment = Pos.CenterLeft
    styleClass = Seq("dashboard-summary-card")
    children = Seq(summaryTextBox, summaryBadge)

  // KPI Panels
  private val totalStockVal = new Label("0") { styleClass = Seq("kpi-card-value", "kpi-blue") }
  private val pendingFamiliesVal = new Label("0") { styleClass = Seq("kpi-card-value", "kpi-orange") }
  private val expiringSoonVal = new Label("0") { styleClass = Seq("kpi-card-value", "kpi-red") }
  private val familiesHelpedVal = new Label("0") { styleClass = Seq("kpi-card-value", "kpi-green") }

  private def createKpiCard(title: String, caption: String, valueLabel: Label, colorClass: String): VBox =
    new VBox:
      spacing = 6
      padding = Insets(16)
      styleClass = Seq("kpi-card", colorClass)
      hgrow = Priority.Always
      children = Seq(
        new Label(title) { styleClass = Seq("kpi-card-title") },
        valueLabel,
        new Label(caption) { styleClass = Seq("kpi-card-caption") }
      )

  private val kpiGrid = new HBox:
    spacing = 15
    alignment = Pos.CenterLeft
    hgrow = Priority.Always
    children = Seq(
      createKpiCard("Total Stock Units", "Current available pantry quantity", totalStockVal, "card-blue"),
      createKpiCard("Pending Requests", "Families still waiting for support", pendingFamiliesVal, "card-orange"),
      createKpiCard("Expiring Soon", "Perishables due within 3 days", expiringSoonVal, "card-red"),
      createKpiCard("Families Helped", "Requests already fulfilled", familiesHelpedVal, "card-green")
    )

  private val priorityLowStock = new Label("Low stock: 0") {
    styleClass = Seq("priority-pill", "priority-pill-orange")
    wrapText = true
  }
  private val priorityPending = new Label("Pending requests: 0") {
    styleClass = Seq("priority-pill", "priority-pill-blue")
    wrapText = true
  }
  private val priorityExpiry = new Label("Expiring soon: 0") {
    styleClass = Seq("priority-pill", "priority-pill-red")
    wrapText = true
  }
  private val priorityHint = new Label("Loading priority suggestions...") {
    styleClass = Seq("priority-hint")
    wrapText = true
  }

  private val priorityPillsRow = new HBox:
    spacing = 10
    alignment = Pos.CenterLeft
    children = Seq(priorityLowStock, priorityPending, priorityExpiry)

  private val priorityPanel = new VBox:
    spacing = 10
    styleClass = Seq("priority-panel")
    children = Seq(
      new Label("Today’s Priorities") { styleClass = Seq("section-card-title") },
      priorityPillsRow,
      priorityHint
    )

  // Chart and Critical Inventory Table
  private val pieChart = new PieChart:
    title = "Inventory Categories"
    styleClass = Seq("chart-card")
    legendVisible = true
    labelsVisible = false
    prefWidth = 560
    minWidth = 460
    prefHeight = 420

  private val xAxis = new CategoryAxis { label = "Restriction Category" }
  private val yAxis = new NumberAxis { label = "Families" }
  private val barSeries = new XYChart.Series[String, Number]()
  barSeries.name = "Families"

  private val barChart = new BarChart[String, Number](xAxis, yAxis):
    title = "Family Dietary Needs"
    styleClass = Seq("chart-card")
    legendVisible = false
    prefWidth = 560
    minWidth = 460
    prefHeight = 420
    data = barSeries

  // Critical items Table (perishables expiring within 3 days)
  private val criticalTable: TableView[FoodItem] = new TableView[FoodItem]():
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("No critical expiring items.") { style = "-fx-text-fill: #64748b;" }
    
    private val nameCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Name"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "name", cellData.value.name) }
      prefWidth = 220
      cellFactory = { (col: TableColumn[FoodItem, String]) =>
        new TableCell[FoodItem, String] {
          item.onChange { (_, _, newText) =>
            text = newText
            tooltip = if newText != null && newText.nonEmpty then new Tooltip(newText) else null
          }
        }
      }
      
    private val categoryCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Category"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "category", cellData.value.category.toString) }
      prefWidth = 90
      
    private val qtyCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Quantity"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "quantity", s"${cellData.value.quantity} ${cellData.value.unit}") }
      prefWidth = 90
      
    private val statusCol: TableColumn[FoodItem, String] = new TableColumn[FoodItem, String]("Expiry Status"):
      cellValueFactory = { cellData => 
        val statusStr = cellData.value.getExpiryStatus(LocalDate.now())
        new scalafx.beans.property.StringProperty(this, "status", statusStr)
      }
      prefWidth = 180
      cellFactory = UIUtils.createBadgeCellFactory(UIUtils.getExpiryLabel)

    columns ++= Seq(nameCol, categoryCol, qtyCol, statusCol)
    columns.foreach(_.setReorderable(false))
    prefHeight <== scalafx.beans.binding.Bindings.createDoubleBinding(
      () => {
        val rowCount = items.value.size()
        if rowCount == 0 then 80.0
        else math.min((rowCount * 40.0) + 45.0, 250.0)
      },
      items
    )

  criticalTable.clip = UIUtils.createRoundedClip(criticalTable)

  private val criticalTableWrapper = new StackPane:
    styleClass = Seq("table-wrapper")
    children = Seq(criticalTable)

  private val criticalSection = new VBox:
    spacing = 10
    styleClass = Seq("section-card")
    hgrow = Priority.Always
    children = Seq(
      new Label("Urgently Expiring Stock") { styleClass = Seq("section-card-title") },
      criticalTableWrapper
    )

  HBox.setHgrow(pieChart, Priority.Always)
  HBox.setHgrow(barChart, Priority.Always)

  private val chartSectionTitle = new Label("Analytics Overview") { styleClass = Seq("section-card-title") }

  private val chartsHBox = new HBox:
    spacing = 20
    alignment = Pos.TopCenter
    hgrow = Priority.Always
    children = Seq(
      pieChart,
      barChart
    )

  private val chartsSection = new VBox:
    spacing = 10
    styleClass = Seq("section-card")
    children = Seq(chartSectionTitle, chartsHBox)

  private val alertsBox = new VBox {
    spacing = 8
    style = "-fx-background-color: transparent;"
  }

  children = Seq(
    headerBlock,
    summaryBanner,
    alertsBox,
    kpiGrid,
    priorityPanel,
    chartsSection,
    criticalSection
  )

  // Reactive Stats Update
  private def refreshAll(): Unit =
    val today = LocalDate.now()
    
    // 1. Update KPI Values
    val totalQty = inventory.map(_.quantity).sum
    val totalQtyText = f"$totalQty%.1f"
    totalStockVal.text = totalQtyText

    val pendingCount = requests.count(_.status == RequestStatus.Pending)
    pendingFamiliesVal.text = pendingCount.toString

    val helpedCount = requests.count(_.status == RequestStatus.Fulfilled)
    familiesHelpedVal.text = helpedCount.toString

    val soonCount = inventory.count {
      case item: PerishableItem =>
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, item.expiryDate)
        days >= 0 && days <= 3
      case _ => false
    }
    expiringSoonVal.text = soonCount.toString

    val lowStockCount = inventory.count(_.quantity < 5.0)
    priorityLowStock.text = s"Low stock: $lowStockCount"
    priorityPending.text = s"Pending requests: $pendingCount"
    priorityExpiry.text = s"Expiring soon: $soonCount"
    priorityHint.text =
      if soonCount > 0 then "Start with expiring perishables, then generate a distribution plan for pending families."
      else if pendingCount > 0 then "Generate a distribution plan to match current stock with pending family requests."
      else if lowStockCount > 0 then "Review low-stock items and plan restocking before the next distribution cycle."
      else "No urgent action right now — your pantry records look calm."

    summaryText.text =
      s"Today you have $totalQtyText stock units, $pendingCount pending family requests, and $soonCount expiring items to watch."
    summaryBadge.text =
      if soonCount > 0 || pendingCount > 0 then "Needs attention" else "All clear"

    // 2. Update Table (Perishables expiring within 3 days or already expired)
    val criticalItems = inventory.filter {
      case item: PerishableItem =>
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, item.expiryDate)
        days <= 3
      case _ => false
    }.toList
    criticalTable.items = ObservableBuffer.from(criticalItems)

    // 3. Update Pie Chart
    val categoryTotals = inventory.groupBy(_.category).map { case (category, items) =>
      (category.toString, items.map(_.quantity).sum)
    }
    val chartData = categoryTotals.map { case (name, total) =>
      PieChart.Data(s"$name", total)
    }.toSeq
    pieChart.data = ObservableBuffer.from(chartData)
    // Double-nested runLater: outer queues after data bind, inner runs after layout creates nodes
    scalafx.application.Platform.runLater {
      scalafx.application.Platform.runLater {
        pieChart.data.value.forEach { sliceData =>
          if sliceData.getNode != null then
            val tip = new javafx.scene.control.Tooltip(
              f"${sliceData.getName}: ${sliceData.getPieValue}%.1f units"
            )
            tip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;")
            javafx.scene.control.Tooltip.install(sliceData.getNode, tip)
            sliceData.getNode.setOnMouseEntered(_ =>
              sliceData.getNode.setStyle("-fx-opacity: 0.7; -fx-cursor: hand;")
            )
            sliceData.getNode.setOnMouseExited(_ =>
              sliceData.getNode.setStyle("-fx-opacity: 1.0;")
            )
        }
      }
    }

    // 4. Update Bar Chart (Dietary Restrictions)
    val restrictionCounts = DietaryRestriction.values.map { restriction =>
      val count = requests.count(_.dietaryRestriction == restriction)
      (restriction.displayName, count)
    }
    val newData = restrictionCounts.map { case (restrictionName, count) =>
      XYChart.Data[String, Number](restrictionName, count: java.lang.Number)
    }
    barSeries.data = ObservableBuffer.from(newData)
    // Double-nested runLater: outer queues after data bind, inner runs after layout creates bar nodes
    scalafx.application.Platform.runLater {
      scalafx.application.Platform.runLater {
        barSeries.data.value.forEach { barData =>
          if barData.getNode != null then
            val tip = new javafx.scene.control.Tooltip(
              s"${barData.getXValue}: ${barData.getYValue.intValue()} families"
            )
            tip.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;")
            javafx.scene.control.Tooltip.install(barData.getNode, tip)
            barData.getNode.setOnMouseEntered(_ =>
              barData.getNode.setStyle("-fx-opacity: 0.7; -fx-cursor: hand;")
            )
            barData.getNode.setOnMouseExited(_ =>
              barData.getNode.setStyle("-fx-opacity: 1.0;")
            )
        }
      }
    }

    // 5. Update Shortage Alerts
    alertsBox.children.clear()
    val requestedByCategory = requests
      .filter(_.status == RequestStatus.Pending)
      .groupBy(_.requestedCategory)
      .map { case (cat, reqList) =>
        val totalRequestedQty = reqList.map(_.householdSize).sum.toDouble
        (cat, totalRequestedQty)
      }
    val inventoryByCategory = inventory
      .groupBy(_.category)
      .map { case (cat, itemList) => (cat, itemList.map(_.quantity).sum) }

    val shortages = requestedByCategory.flatMap { case (cat, reqQty) =>
      val invQty = inventoryByCategory.getOrElse(cat, 0.0)
      if invQty < reqQty then
        Some(s"⚠️ Shortage Warning: Pending requests for $cat require ${reqQty.toInt} items, but current stock is only ${invQty.toInt} items!")
      else
        None
    }.toList

    if shortages.nonEmpty then
      shortages.foreach { alertText =>
        val alertLabel = new Label(alertText) {
          styleClass = Seq("alert-banner")
          maxWidth = Double.MaxValue
        }
        alertsBox.children.add(alertLabel)
      }
    else
      alertsBox.children.add(
        new Label("✓ No category shortage warnings right now.") {
          styleClass = Seq("dashboard-clear-banner")
          maxWidth = Double.MaxValue
        }
      )

  // Register listeners for reactive auto-refresh
  inventory.onChange { (_, _) => refreshAll() }
  requests.onChange { (_, _) => refreshAll() }

  // Initial calculation
  refreshAll()
