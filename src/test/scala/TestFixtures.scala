import java.time.LocalDate

import model.*
import service.LoanService

object TestFixtures:
  val today: LocalDate = LocalDate.of(2026, 8, 5)

  val borrowerInput: BorrowerInput = BorrowerInput(
    fullName = "Amina Rahman",
    phone = "+60123456789",
    nationalId = "DEMO-900101-01",
    monthlyIncome = BigDecimal("2400.00"),
    address = "12 Community Road, Selangor"
  )

  def stateWithBorrower: AppState =
    LoanService.registerBorrower(AppState.empty, borrowerInput, today) match
      case Right((state, _)) => state
      case Left(error)       => failFixture(error.message)

  def borrower(state: AppState): Borrower =
    state.borrowers.headOption.getOrElse(failFixture("Expected one borrower in fixture."))

  def loanInput(state: AppState): LoanInput = LoanInput(
    borrowerId = borrower(state).id,
    principal = BigDecimal("1000.00"),
    annualRatePercent = BigDecimal("10.00"),
    termMonths = 3,
    disbursedOn = LocalDate.of(2026, 1, 1)
  )

  def stateWithLoan: AppState =
    val initialState = stateWithBorrower
    LoanService.disburseLoan(initialState, loanInput(initialState), today) match
      case Right((state, _)) => state
      case Left(error)       => failFixture(error.message)

  private def failFixture(message: String): Nothing =
    throw new IllegalStateException(message)
