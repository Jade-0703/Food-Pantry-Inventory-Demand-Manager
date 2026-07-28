package foodpantry

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.scene.chart._
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import java.time.LocalDate

class DashboardView(
  inventory: ObservableBuffer[FoodItem],
  requests: ObservableBuffer[FamilyRequest]
) extends VBox:
  
  spacing = 20
  padding = Insets(20)
  style = "-fx-background-color: #fbf9f4;"

  private val titleLabel = new Label("Dashboard & Pantry Analytics"):
    style = "-fx-text-fill: #111827; -fx-font-family: 'Inter'; -fx-font-weight: bold; -fx-font-size: 20px;"

  // KPI Panels
  private val totalStockVal = new Label("0") { style = "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #0369a1; -fx-font-family: 'Inter';" }
  private val pendingFamiliesVal = new Label("0") { style = "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #c2410c; -fx-font-family: 'Inter';" }
  private val expiringSoonVal = new Label("0") { style = "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #b91c1c; -fx-font-family: 'Inter';" }
  private val familiesHelpedVal = new Label("0") { style = "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #6b21a8; -fx-font-family: 'Inter';" }

  private def createKpiCard(title: String, valueLabel: Label, bgStyle: String, colorClass: String): VBox =
    new VBox:
      spacing = 6
      padding = Insets(16)
      styleClass = Seq("kpi-card", colorClass)
      style = s"-fx-background-color: $bgStyle;"
      hgrow = Priority.Always
      children = Seq(
        new Label(title) { style = "-fx-font-size: 12px; -fx-text-fill: #4b5563; -fx-font-weight: 600; -fx-font-family: 'Inter';" },
        valueLabel
      )

  private val kpiGrid = new HBox:
    spacing = 15
    alignment = Pos.CenterLeft
    hgrow = Priority.Always
    children = Seq(
      createKpiCard("Total Stock Units", totalStockVal, "#e0f2fe", "card-blue"),
      createKpiCard("Pending Requests", pendingFamiliesVal, "#ffedd5", "card-orange"),
      createKpiCard("Expiring Soon (<3 Days)", expiringSoonVal, "#fee2e2", "card-red"),
      createKpiCard("Families Helped", familiesHelpedVal, "#f3e8ff", "card-green")
    )

  // Chart and Critical Inventory Table
  private val pieChart = new PieChart:
    title = "Inventory Categories"
    style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-padding: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);"
    legendVisible = true
    labelsVisible = true
    labelLineLength = 8
    prefWidth = 480
    minWidth = 420
    prefHeight = 350

  private val xAxis = new CategoryAxis { label = "Restriction Category" }
  private val yAxis = new NumberAxis { label = "Families" }
  private val barChart = new BarChart[String, Number](xAxis, yAxis):
    title = "Family Dietary Needs"
    style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-padding: 10px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);"
    legendVisible = false
    prefWidth = 480
    minWidth = 420
    prefHeight = 350

  // Critical items Table (perishables expiring within 3 days)
  private val criticalTable = new TableView[FoodItem]:
    val selfTable: TableView[FoodItem] = this
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("No critical expiring items.") { style = "-fx-text-fill: #64748b;" }

    // S1-14 / Entry 14 clip layout to prevent row background bleed
    clip = UIUtils.createRoundedClip(selfTable)
    
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
      cellFactory = { (col: TableColumn[FoodItem, String]) =>
        new TableCell[FoodItem, String] {
          item.onChange { (_, _, newText) =>
            if newText != null then
              graphic = UIUtils.getExpiryLabel(newText)
              text = null
              alignment = scalafx.geometry.Pos.Center
            else
              graphic = null
              text = null
          }
        }
      }

    columns ++= Seq(nameCol, categoryCol, qtyCol, statusCol)
    prefHeight <== scalafx.beans.binding.Bindings.createDoubleBinding(
      () => {
        val rowCount = items.value.size()
        if rowCount == 0 then 80.0
        else math.min((rowCount * 40.0) + 45.0, 250.0)
      },
      items
    )

  private val criticalTableWrapper = new StackPane:
    styleClass = Seq("table-wrapper")
    children = Seq(criticalTable)

  private val criticalSection = new VBox:
    spacing = 10
    style = "-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-radius: 12px; -fx-border-color: #cbd5e1; -fx-border-width: 1px; -fx-padding: 15px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);"
    hgrow = Priority.Always
    children = Seq(
      new Label("Urgently Expiring Stock") { style = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;" },
      criticalTableWrapper
    )

  private val chartsHBox = new HBox:
    spacing = 20
    alignment = Pos.TopCenter
    hgrow = Priority.Always
    children = Seq(
      pieChart,
      barChart
    )

  private val alertsBox = new VBox {
    spacing = 8
    style = "-fx-background-color: transparent;"
  }

  children = Seq(
    titleLabel,
    alertsBox,
    kpiGrid,
    chartsHBox,
    criticalSection
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

    // 4. Update Bar Chart (Dietary Restrictions)
    val restrictionCounts = requests.groupBy(_.dietaryRestriction).map { case (restr, reqs) =>
      (restr.toString, reqs.length)
    }
    val series = new XYChart.Series[String, Number] {
      name = "Families"
      data = ObservableBuffer.from(
        restrictionCounts.map { case (restr, count) =>
          XYChart.Data[String, Number](restr, count)
        }.toSeq
      )
    }
    barChart.data = series

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
          style = "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-border-color: #fca5a5; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8px 12px; -fx-font-weight: bold; -fx-font-size: 13px;"
          maxWidth = Double.MaxValue
        }
        alertsBox.children.add(alertLabel)
      }

  // Register listeners for reactive auto-refresh
  inventory.onChange { (_, _) => refreshAll() }
  requests.onChange { (_, _) => refreshAll() }

  // Initial calculation
  refreshAll()
