package ui

import scala.annotation.nowarn

import model.*
import service.LoanService
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, ComboBox, DatePicker, Label, TextField}
import scalafx.scene.layout.{BorderPane, GridPane, HBox, VBox}
import scalafx.util.StringConverter

@nowarn("cat=deprecation")
final class LoanView(controller: ApplicationController) extends BorderPane with ViewSupport:
  private val borrowerItems = ObservableBuffer.empty[Borrower]
  private val borrowerCombo = new ComboBox[Borrower](borrowerItems) {
    promptText = "Select borrower"
    maxWidth = Double.MaxValue
    converter = StringConverter.toStringConverter[Borrower](borrower =>
      Option(borrower).map(value => s"${value.fullName} • ${value.nationalId}").getOrElse("")
    )
  }
  private val principalField = new TextField { promptText = "1000.00" }
  private val rateField = new TextField { promptText = "10.00" }
  private val termField = new TextField { promptText = "12" }
  private val disbursementDate = new DatePicker(controller.today)
  private val quoteLabel = new Label("Enter loan details to preview the repayment total.") {
    wrapText = true
    styleClass += "quote-text"
  }
  private val statusLabel = new Label("One active loan is allowed per borrower.") { styleClass += "status-text" }

  private val previewButton = new Button("Preview") {
    onAction = handle { updateQuote(showErrors = true) }
  }
  private val disburseButton = new Button("Disburse loan") {
    styleClass += "primary-button"
    defaultButton = true
    onAction = handle { submit() }
  }

  private val formGrid = new GridPane {
    hgap = 12
    vgap = 12
    add(new Label("Borrower"), 0, 0)
    add(borrowerCombo, 1, 0)
    add(new Label("Principal (RM)"), 0, 1)
    add(principalField, 1, 1)
    add(new Label("Annual flat rate (%)"), 0, 2)
    add(rateField, 1, 2)
    add(new Label("Term (months)"), 0, 3)
    add(termField, 1, 3)
    add(new Label("Disbursement date"), 0, 4)
    add(disbursementDate, 1, 4)
  }

  private val content = new VBox(18) {
    padding = Insets(24)
    children = Seq(
      new VBox(4) {
        children = Seq(
          new Label("Disburse loan") { styleClass += "page-title" },
          new Label("Create a flat-interest repayment schedule for a registered borrower") {
            styleClass += "page-subtitle"
          }
        )
      },
      new HBox(18) {
        alignment = Pos.TopLeft
        children = Seq(
          new VBox(14) {
            styleClass += "panel"
            prefWidth = 470
            children = Seq(
              new Label("Loan details") { styleClass += "section-title" },
              formGrid,
              new HBox(10) {
                alignment = Pos.CenterRight
                children = Seq(previewButton, disburseButton)
              },
              statusLabel
            )
          },
          new VBox(10) {
            styleClass += "quote-card"
            prefWidth = 330
            children = Seq(
              new Label("Repayment preview") { styleClass += "section-title" },
              quoteLabel,
              new Label("Flat interest is calculated once at disbursement. The final instalment absorbs currency rounding.") {
                wrapText = true
                styleClass += "helper-text"
              }
            )
          }
        )
      }
    )
  }

  center = content
  refresh(controller.stateProperty.value)
  controller.stateProperty.onChange { (_, _, updatedState) => refresh(updatedState) }
  Seq(principalField, rateField, termField).foreach(field =>
    field.text.onChange { (_, _, _) => updateQuote(showErrors = false) }
  )

  private def formInput(): Either[AppError, LoanInput] =
    for
      borrower <- Option(borrowerCombo.value.value)
        .toRight(ValidationError("Select a borrower."))
      principal <- parseMoney(principalField, "Principal")
      rate <- parseMoney(rateField, "Interest rate")
      term <- parseWholeNumber(termField, "Term")
      selectedDate <- Option(disbursementDate.value.value)
        .toRight(ValidationError("Select a disbursement date."))
    yield LoanInput(borrower.id, principal, rate, term, selectedDate)

  private def updateQuote(showErrors: Boolean): Unit =
    val result = formInput().flatMap(LoanService.quote)
    result match
      case Right(quote) =>
        quoteLabel.text =
          s"Interest: ${money(quote.totalInterest)}\nTotal due: ${money(quote.totalDue)}\n" +
            s"First instalment: ${money(quote.firstInstallment)}\nFinal instalment: ${money(quote.finalInstallment)}"
      case Left(error) if showErrors =>
        setStatus(statusLabel, error.message, isError = true)
      case Left(_) =>
        quoteLabel.text = "Enter valid loan details to preview the repayment total."

  private def submit(): Unit =
    val result = formInput().flatMap(input =>
      controller.transact(
        state => LoanService.disburseLoan(state, input, controller.today),
        "Loan disbursed and repayment schedule saved."
      )
    )

    result match
      case Right(_) =>
        setStatus(statusLabel, controller.notificationProperty.value, isError = false)
        principalField.clear()
        rateField.clear()
        termField.clear()
        borrowerCombo.value = null
        quoteLabel.text = "Enter loan details to preview the repayment total."
      case Left(error) =>
        controller.report(error)
        setStatus(statusLabel, error.message, isError = true)

  private def refresh(state: AppState): Unit = borrowerItems.setAll(state.borrowers.sortBy(_.fullName)*)
