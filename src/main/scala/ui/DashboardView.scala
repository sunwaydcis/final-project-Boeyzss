package ui

import model.*
import service.LoanService
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, TableColumn, TableView}
import scalafx.scene.layout.{BorderPane, HBox, Priority, VBox}

final class DashboardView(controller: ApplicationController) extends BorderPane with ViewSupport:
  private val borrowerCountLabel = metricValue()
  private val activeLoanCountLabel = metricValue()
  private val overdueLoanCountLabel = metricValue()
  private val outstandingLabel = metricValue()
  private val riskSummaryLabel = new Label()
  private val loanItems = ObservableBuffer.empty[LoanOverview]

  private val loanTable = new TableView[LoanOverview] {
    items = loanItems
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("No loans yet. Disburse a loan to populate the dashboard.")
    columns ++= Seq(
      stringColumn("Borrower", overview => overview.borrowerName),
      stringColumn("Status", overview => overview.status.label),
      stringColumn("Risk", overview => overview.risk.label),
      stringColumn("Outstanding", overview => money(overview.outstanding)),
      stringColumn("Next due", overview => dateText(overview.nextDueDate)),
      stringColumn("Days overdue", overview => overview.overdueDays.toString)
    )
  }

  private val cards = new HBox(16) {
    alignment = Pos.CenterLeft
    children = Seq(
      metricCard("Borrowers", borrowerCountLabel),
      metricCard("Active loans", activeLoanCountLabel),
      metricCard("Overdue loans", overdueLoanCountLabel),
      metricCard("Outstanding", outstandingLabel)
    )
  }

  private val body = new VBox(18) {
    padding = Insets(24)
    children = Seq(
      new VBox(4) {
        children = Seq(
          new Label("Dashboard") { styleClass += "page-title" },
          new Label("Portfolio health and default-risk overview") { styleClass += "page-subtitle" }
        )
      },
      cards,
      new VBox(8) {
        styleClass += "panel"
        children = Seq(
          new Label("Risk distribution") { styleClass += "section-title" },
          riskSummaryLabel
        )
      },
      new Label("Loan portfolio") { styleClass += "section-title" },
      loanTable
    )
  }

  VBox.setVgrow(loanTable, Priority.Always)
  center = body

  refresh(controller.stateProperty.value)
  controller.stateProperty.onChange { (_, _, updatedState) => refresh(updatedState) }

  private def refresh(state: AppState): Unit =
    val snapshot = LoanService.dashboardSnapshot(state, controller.today)
    borrowerCountLabel.text = snapshot.borrowerCount.toString
    activeLoanCountLabel.text = snapshot.activeLoanCount.toString
    overdueLoanCountLabel.text = snapshot.overdueLoanCount.toString
    outstandingLabel.text = money(snapshot.totalOutstanding)
    riskSummaryLabel.text =
      s"Low ${snapshot.lowRiskCount}    •    Medium ${snapshot.mediumRiskCount}    •    High ${snapshot.highRiskCount}"
    loanItems.setAll(snapshot.loans*)

  private def metricValue(): Label = new Label("0") { styleClass += "metric-value" }

  private def metricCard(labelText: String, valueLabel: Label): VBox =
    new VBox(6) {
      styleClass += "metric-card"
      HBox.setHgrow(this, Priority.Always)
      maxWidth = Double.MaxValue
      children = Seq(
        new Label(labelText) { styleClass += "metric-label" },
        valueLabel
      )
    }

  private def stringColumn(titleText: String, value: LoanOverview => String): TableColumn[LoanOverview, String] =
    new TableColumn[LoanOverview, String] {
      text = titleText
      cellValueFactory = cellData => StringProperty(value(cellData.value))
    }
