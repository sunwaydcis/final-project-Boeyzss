package ui

import scala.annotation.nowarn

import model.*
import service.LoanService
import scalafx.Includes.*
import scalafx.beans.property.{ObjectProperty, StringProperty}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Button, Label, TableColumn, TableView, TextArea, TextField}
import scalafx.scene.layout.{BorderPane, ColumnConstraints, GridPane, HBox, Priority, VBox}

@nowarn("cat=deprecation")
final class BorrowerView(controller: ApplicationController) extends BorderPane with ViewSupport:
  private val borrowerItems = ObservableBuffer.empty[Borrower]
  private val selectedBorrowerId = ObjectProperty[Option[BorrowerId]](None)
  private val formModeLabel = new Label("Register borrower") { styleClass += "section-title" }
  private val statusLabel = new Label("Enter the borrower’s details.") { styleClass += "status-text" }

  private val fullNameField = new TextField { promptText = "Full name" }
  private val phoneField = new TextField { promptText = "+60123456789" }
  private val nationalIdField = new TextField { promptText = "Demo identifier" }
  private val incomeField = new TextField { promptText = "2400.00" }
  private val addressField = new TextArea {
    promptText = "Residential address"
    prefRowCount = 3
    wrapText = true
  }

  private val saveButton = new Button("Register borrower") {
    styleClass += "primary-button"
    defaultButton = true
    onAction = handle { submit() }
  }

  private val newButton = new Button("New borrower") {
    onAction = handle { clearForm() }
  }

  private val borrowerTable = new TableView[Borrower] {
    items = borrowerItems
    columnResizePolicy = TableView.ConstrainedResizePolicy
    placeholder = new Label("No borrowers registered yet.")
    columns ++= Seq(
      stringColumn("Name", borrower => borrower.fullName),
      stringColumn("Phone", borrower => borrower.phone),
      stringColumn("National ID", borrower => borrower.nationalId),
      stringColumn("Monthly income", borrower => money(borrower.monthlyIncome))
    )
  }

  private val formGrid = new GridPane {
    hgap = 12
    vgap = 12
    columnConstraints = Seq(
      new ColumnConstraints { minWidth = 130; prefWidth = 130 },
      new ColumnConstraints { hgrow = Priority.Always; fillWidth = true }
    )
    add(new Label("Full name"), 0, 0)
    add(fullNameField, 1, 0)
    add(new Label("Phone"), 0, 1)
    add(phoneField, 1, 1)
    add(new Label("National ID"), 0, 2)
    add(nationalIdField, 1, 2)
    add(new Label("Monthly income (RM)"), 0, 3)
    add(incomeField, 1, 3)
    add(new Label("Address"), 0, 4)
    add(addressField, 1, 4)
  }

  private val formPanel = new VBox(14) {
    styleClass += "panel"
    prefWidth = 450
    minWidth = 450
    children = Seq(
      formModeLabel,
      formGrid,
      new HBox(10) {
        alignment = Pos.CenterRight
        children = Seq(newButton, saveButton)
      },
      statusLabel
    )
  }

  private val content = new VBox(18) {
    padding = Insets(24)
    children = Seq(
      new VBox(4) {
        children = Seq(
          new Label("Borrowers") { styleClass += "page-title" },
          new Label("Register community borrowers or update an existing record") { styleClass += "page-subtitle" }
        )
      },
      new HBox(18) {
        children = Seq(formPanel, new VBox(10) {
          children = Seq(new Label("Borrower directory") { styleClass += "section-title" }, borrowerTable)
          HBox.setHgrow(this, Priority.Always)
          VBox.setVgrow(borrowerTable, Priority.Always)
        })
      }
    )
  }

  VBox.setVgrow(content.children(1), Priority.Always)
  center = content

  borrowerTable.selectionModel().selectedItem.onChange { (_, _, borrower) =>
    Option(borrower).foreach(populateForm)
  }
  refresh(controller.stateProperty.value)
  controller.stateProperty.onChange { (_, _, updatedState) => refresh(updatedState) }

  private def submit(): Unit =
    val inputResult = parseMoney(incomeField, "Monthly income").map(income =>
      BorrowerInput(
        fullName = fullNameField.text.value,
        phone = phoneField.text.value,
        nationalId = nationalIdField.text.value,
        monthlyIncome = income,
        address = addressField.text.value
      )
    )

    val result = inputResult.flatMap(input =>
      selectedBorrowerId.value match
        case Some(borrowerId) =>
          controller.transact(
            state => LoanService.editBorrower(state, borrowerId, input),
            "Borrower updated and saved."
          )
        case None =>
          controller.transact(
            state => LoanService.registerBorrower(state, input, controller.today),
            "Borrower registered and saved."
          )
    )

    result match
      case Right(_) =>
        setStatus(statusLabel, controller.notificationProperty.value, isError = false)
        clearForm(preserveStatus = true)
      case Left(error) =>
        controller.report(error)
        setStatus(statusLabel, error.message, isError = true)

  private def populateForm(borrower: Borrower): Unit =
    selectedBorrowerId.value = Some(borrower.id)
    formModeLabel.text = "Edit borrower"
    saveButton.text = "Save changes"
    fullNameField.text = borrower.fullName
    phoneField.text = borrower.phone
    nationalIdField.text = borrower.nationalId
    incomeField.text = borrower.monthlyIncome.toString
    addressField.text = borrower.address
    setStatus(statusLabel, "Editing the selected borrower.", isError = false)

  private def clearForm(preserveStatus: Boolean = false): Unit =
    selectedBorrowerId.value = None
    borrowerTable.selectionModel().clearSelection()
    formModeLabel.text = "Register borrower"
    saveButton.text = "Register borrower"
    fullNameField.clear()
    phoneField.clear()
    nationalIdField.clear()
    incomeField.clear()
    addressField.clear()
    if !preserveStatus then setStatus(statusLabel, "Enter the borrower’s details.", isError = false)
    fullNameField.requestFocus()

  private def refresh(state: AppState): Unit = borrowerItems.setAll(state.borrowers.sortBy(_.fullName)*)

  private def stringColumn(titleText: String, value: Borrower => String): TableColumn[Borrower, String] =
    new TableColumn[Borrower, String] {
      text = titleText
      cellValueFactory = cellData => StringProperty(value(cellData.value))
    }
