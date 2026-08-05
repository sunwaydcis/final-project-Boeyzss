package ui

import scala.annotation.nowarn

import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Node
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.{BorderPane, Priority, Region, VBox}

final class AppShell(controller: ApplicationController) extends BorderPane:
  private val dashboardView = DashboardView(controller)
  private val borrowerView = BorrowerView(controller)
  private val loanView = LoanView(controller)
  private val repaymentView = RepaymentView(controller)

  private def showScreen(screen: Node): Unit = center = screen

  def showDashboard(): Unit = showScreen(dashboardView)
  def showBorrowers(): Unit = showScreen(borrowerView)
  def showDisbursement(): Unit = showScreen(loanView)
  def showRepayments(): Unit = showScreen(repaymentView)

  private val dashboardButton = navigationButton("Dashboard", () => showDashboard())
  private val borrowerButton = navigationButton("Borrowers", () => showBorrowers())
  private val loanButton = navigationButton("Disburse loan", () => showDisbursement())
  private val repaymentButton = navigationButton("Repayments", () => showRepayments())
  private val spacer = new Region()

  VBox.setVgrow(spacer, Priority.Always)

  left = new VBox(12) {
    styleClass += "sidebar"
    padding = Insets(24, 16, 20, 16)
    prefWidth = 220
    children = Seq(
      new VBox(2) {
        children = Seq(
          new Label("COMMUNITY FUND") { styleClass += "brand-eyebrow" },
          new Label("Microfinance") { styleClass += "brand-title" },
          new Label("Loan Tracker") { styleClass += "brand-subtitle" }
        )
      },
      new Label("WORKSPACE") { styleClass += "nav-heading" },
      dashboardButton,
      borrowerButton,
      loanButton,
      repaymentButton,
      spacer,
      new Label("Local JSON storage\nMYR • Offline") {
        styleClass += "sidebar-footer"
        wrapText = true
      }
    )
  }

  private val notificationLabel = new Label {
    styleClass += "notification"
    text <== controller.notificationProperty
    maxWidth = Double.MaxValue
  }

  bottom = notificationLabel
  BorderPane.setAlignment(notificationLabel, Pos.CenterLeft)
  showDashboard()

  @nowarn("cat=deprecation")
  private def navigationButton(buttonText: String, action: () => Unit): Button =
    new Button(buttonText) {
      styleClass += "nav-button"
      maxWidth = Double.MaxValue
      alignment = Pos.CenterLeft
      onAction = handle { action() }
    }
