package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.scene.chart.PieChart
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import java.time.LocalDate
import scalafx.scene.text.{Font, FontWeight}

class DashboardView(
  inventory: ObservableBuffer[FoodItem],
  requests: ObservableBuffer[FamilyRequest]
) extends VBox:
  
  spacing = 20
  padding = Insets(20)
  style = "-fx-background-color: #fbf9f4;"

  private val titleLabel = new Label("Dashboard & Pantry Analytics"):
    font = Font.font("System", FontWeight.Bold, 24)
    style = "-fx-text-fill: #111827;"
    minWidth = 500

  // KPI Panels
  private val totalStockVal = new Label("0") { style = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0369a1;" }
  private val pendingFamiliesVal = new Label("0") { style = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #c2410c;" }
  private val expiringSoonVal = new Label("0") { style = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;" }
  private val familiesHelpedVal = new Label("0") { style = "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #6b21a8;" }

  private def createKpiCard(title: String, valueLabel: Label, bgStyle: String, colorClass: String): VBox =
    new VBox:
      spacing = 5
      padding = Insets(15)
      styleClass = Seq("kpi-card", colorClass)
      style = s"-fx-background-color: $bgStyle;"
      children = Seq(
        new Label(title) { style = "-fx-font-size: 11px; -fx-text-fill: #4b5563; -fx-font-weight: bold;" },
        valueLabel
      )

  private val kpiGrid = new HBox:
    spacing = 15
    alignment = Pos.CenterLeft
    children = Seq(
      createKpiCard("TOTAL STOCK UNITS", totalStockVal, "#e0f2fe", "card-blue"),
      createKpiCard("PENDING REQUESTS", pendingFamiliesVal, "#ffedd5", "card-orange"),
      createKpiCard("EXPIRING SOON (<3 DAYS)", expiringSoonVal, "#fee2e2", "card-red"),
      createKpiCard("FAMILIES HELPED", familiesHelpedVal, "#f3e8ff", "card-green")
    )

  // Chart and Critical Inventory Table
  private val pieChart = new PieChart:
    title = "Inventory Categories"
    style = "-fx-background-color: #ffffff; -fx-background-radius: 8px; -fx-padding: 10px;"
    legendVisible = true

  // Critical items Table (perishables expiring within 3 days)
  private val criticalTable = new TableView[FoodItem]:
    columnResizePolicy = TableView.ConstrainedResizePolicy
    style = "-fx-background-radius: 8px; -fx-background-color: #ffffff;"
    placeholder = new Label("No critical expiring items.") { style = "-fx-text-fill: #64748b;" }
    
    val nameCol = new TableColumn[FoodItem, String]("Name"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "name", cellData.value.name) }
      prefWidth = 120
      
    val categoryCol = new TableColumn[FoodItem, String]("Category"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "category", cellData.value.category.toString) }
      prefWidth = 100
      
    val qtyCol = new TableColumn[FoodItem, String]("Quantity"):
      cellValueFactory = { cellData => new scalafx.beans.property.StringProperty(this, "quantity", s"${cellData.value.quantity} ${cellData.value.unit}") }
      prefWidth = 100
      
    val statusCol = new TableColumn[FoodItem, String]("Expiry Status"):
      cellValueFactory = { cellData => 
        val statusStr = cellData.value.getExpiryStatus(LocalDate.now())
        new scalafx.beans.property.StringProperty(this, "status", statusStr)
      }
      prefWidth = 150

    columns ++= Seq(nameCol, categoryCol, qtyCol, statusCol)
    prefHeight = 250

  private val criticalSection = new VBox:
    spacing = 10
    style = "-fx-background-color: #ffffff; -fx-background-radius: 8px; -fx-padding: 15px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);"
    hgrow = Priority.Always
    children = Seq(
      new Label("Urgently Expiring Stock") { style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;" },
      criticalTable
    )

  private val chartsAndTableHBox = new HBox:
    spacing = 20
    alignment = Pos.TopCenter
    hgrow = Priority.Always
    children = Seq(
      pieChart,
      criticalSection
    )

  children = Seq(
    titleLabel,
    kpiGrid,
    chartsAndTableHBox
  )

  // Reactive Stats Update
  private def refreshAll(): Unit =
    val today = LocalDate.now()
    
    // 1. Update KPI Values
    val totalQty = inventory.map(_.quantity).sum
    totalStockVal.text = f"$totalQty%.1f"

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
      PieChart.Data(s"$name ($total)", total)
    }.toSeq
    pieChart.data = ObservableBuffer.from(chartData)

  // Register listeners for reactive auto-refresh
  inventory.onChange { (_, _) => refreshAll() }
  requests.onChange { (_, _) => refreshAll() }

  // Initial calculation
  refreshAll()
