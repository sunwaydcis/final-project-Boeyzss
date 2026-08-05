package ui

import scala.annotation.nowarn

import model.*
import service.LoanService
import scalafx.Includes.*
import scalafx.beans.property.StringProperty
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, ComboBox, DatePicker, Label, TableColumn, TableView, TextField}
import scalafx.scene.layout.{BorderPane, GridPane, HBox, Priority, VBox}
import scalafx.util.StringConverter

@nowarn("cat=deprecation")
final class RepaymentView(controller: ApplicationController) extends BorderPane with ViewSupport:
  private val loanItems = ObservableBuffer.empty[Loan]
  private val installmentItems = ObservableBuffer.empty[Installment]
  private val loanCombo = new ComboBox[Loan](loanItems) {
    promptText = "Select active loan"
    maxWidth = Double.MaxValue
    converter = StringConverter.toStringConverter[Loan](loan =>
      Option(loan).map(displayLoan).getOrElse("")
    )
  }
  private val amountField = new TextField { promptText = "100.00" }
  private val paymentDate = new DatePicker(controller.today)
  private val loanSummaryLabel = new Label("Select a loan to view its repayment schedule.") {
    wrapText = true
    styleClass += "quote-text"
  }
  private val statusLabel = new Label("Payments are allocated to the oldest unpaid instalment first.") {
    styleClass += "status-text"
  }

  private val scheduleTable = new TableView[Installment] {
    items = installmentItems
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("Select a loan to view its schedule.")
    columns ++= Seq(
      stringColumn("#", installment => installment.number.toString),
      stringColumn("Due date", installment => installment.dueDate.toString),
      stringColumn("Amount due", installment => money(installment.amountDue)),
      stringColumn("Paid", installment => money(installment.amountPaid)),
      stringColumn("Outstanding", installment => money(installment.outstanding))
    )
  }

  private val recordButton = new Button("Record payment") {
    styleClass += "primary-button"
    defaultButton = true
    onAction = handle { submit() }
  }

  private val paymentGrid = new GridPane {
    hgap = 12
    vgap = 12
    add(new Label("Active loan"), 0, 0)
    add(loanCombo, 1, 0)
    add(new Label("Payment amount (RM)"), 0, 1)
    add(amountField, 1, 1)
    add(new Label("Payment date"), 0, 2)
    add(paymentDate, 1, 2)
  }

  private val content = new VBox(18) {
    padding = Insets(24)
    children = Seq(
      new VBox(4) {
        children = Seq(
          new Label("Repayments") { styleClass += "page-title" },
          new Label("Record payments and inspect each instalment schedule") { styleClass += "page-subtitle" }
        )
      },
      new HBox(18) {
        children = Seq(
          new VBox(14) {
            styleClass += "panel"
            prefWidth = 430
            children = Seq(
              new Label("Payment details") { styleClass += "section-title" },
              paymentGrid,
              new HBox {
                alignment = Pos.CenterRight
                children = Seq(recordButton)
              },
              statusLabel,
              loanSummaryLabel
            )
          },
          new VBox(10) {
            children = Seq(new Label("Instalment schedule") { styleClass += "section-title" }, scheduleTable)
            HBox.setHgrow(this, Priority.Always)
            VBox.setVgrow(scheduleTable, Priority.Always)
          }
        )
      }
    )
  }

  VBox.setVgrow(content.children(1), Priority.Always)
  center = content

  loanCombo.value.onChange { (_, _, selectedLoan) => showLoan(Option(selectedLoan)) }
  refresh(controller.stateProperty.value)
  controller.stateProperty.onChange { (_, _, updatedState) => refresh(updatedState) }

  private def submit(): Unit =
    val inputResult = for
      loan <- Option(loanCombo.value.value).toRight(ValidationError("Select an active loan."))
      amount <- parseMoney(amountField, "Payment amount")
      selectedDate <- Option(paymentDate.value.value).toRight(ValidationError("Select a payment date."))
    yield PaymentInput(loan.id, amount, selectedDate)

    val result = inputResult.flatMap(input =>
      controller.transact(
        state => LoanService.recordPayment(state, input, controller.today),
        "Payment recorded and saved."
      )
    )

    result match
      case Right(updatedLoan) =>
        setStatus(statusLabel, controller.notificationProperty.value, isError = false)
        amountField.clear()
        showLoan(Some(updatedLoan))
      case Left(error) =>
        controller.report(error)
        setStatus(statusLabel, error.message, isError = true)

  private def refresh(state: AppState): Unit =
    val selectedLoanId = Option(loanCombo.value.value).map(_.id)
    val activeLoans = state.loans.filter(_.outstanding > BigDecimal(0)).sortBy(_.disbursedOn)
    loanItems.setAll(activeLoans*)
    val refreshedSelection = selectedLoanId.flatMap(identifier => activeLoans.find(_.id == identifier))
    refreshedSelection match
      case Some(loan) =>
        loanCombo.value = loan
        showLoan(Some(loan))
      case None =>
        loanCombo.value = null
        showLoan(None)

  private def showLoan(loan: Option[Loan]): Unit =
    loan match
      case Some(selectedLoan) =>
        installmentItems.setAll(selectedLoan.installments*)
        val status = LoanService.loanStatus(selectedLoan, controller.today)
        val risk = LoanService.riskLevel(selectedLoan, controller.today)
        loanSummaryLabel.text =
          s"Outstanding: ${money(selectedLoan.outstanding)}\nStatus: ${status.label}  •  Risk: ${risk.label}"
      case None =>
        installmentItems.clear()
        loanSummaryLabel.text = "Select a loan to view its repayment schedule."

  private def displayLoan(loan: Loan): String =
    val borrowerName = controller.stateProperty.value.borrowers
      .find(_.id == loan.borrowerId)
      .map(_.fullName)
      .getOrElse("Unknown borrower")
    s"$borrowerName • ${money(loan.outstanding)} outstanding"

  private def stringColumn(titleText: String, value: Installment => String): TableColumn[Installment, String] =
    new TableColumn[Installment, String] {
      text = titleText
      cellValueFactory = cellData => StringProperty(value(cellData.value))
    }
