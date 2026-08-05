package model

import java.time.LocalDate

// ai-assisted: #1
// why: Codex helped shape an immutable model that maps directly to the four assessed features.
case class BorrowerId(value: String)
case class LoanId(value: String)
case class PaymentId(value: String)

case class Borrower(
    id: BorrowerId,
    fullName: String,
    phone: String,
    nationalId: String,
    monthlyIncome: BigDecimal,
    address: String,
    createdOn: LocalDate
)

case class Installment(
    number: Int,
    dueDate: LocalDate,
    amountDue: BigDecimal,
    amountPaid: BigDecimal
):
  def outstanding: BigDecimal = (amountDue - amountPaid).max(BigDecimal(0)).setScale(2)
  def isPaid: Boolean = outstanding == BigDecimal(0).setScale(2)

case class Payment(
    id: PaymentId,
    amount: BigDecimal,
    paidOn: LocalDate
)

case class Loan(
    id: LoanId,
    borrowerId: BorrowerId,
    principal: BigDecimal,
    annualRatePercent: BigDecimal,
    termMonths: Int,
    disbursedOn: LocalDate,
    installments: Vector[Installment],
    payments: Vector[Payment]
):
  def totalDue: BigDecimal = installments.map(_.amountDue).sum.setScale(2)
  def totalPaid: BigDecimal = payments.map(_.amount).sum.setScale(2)
  def outstanding: BigDecimal = installments.map(_.outstanding).sum.setScale(2)

case class AppState(
    borrowers: Vector[Borrower],
    loans: Vector[Loan]
)

object AppState:
  val empty: AppState = AppState(Vector.empty, Vector.empty)

sealed trait LoanStatus:
  def label: String

object LoanStatus:
  case object Active extends LoanStatus:
    override val label: String = "Active"

  case object Overdue extends LoanStatus:
    override val label: String = "Overdue"

  case object Repaid extends LoanStatus:
    override val label: String = "Repaid"

sealed trait RiskLevel:
  def label: String

object RiskLevel:
  case object Low extends RiskLevel:
    override val label: String = "Low"

  case object Medium extends RiskLevel:
    override val label: String = "Medium"

  case object High extends RiskLevel:
    override val label: String = "High"

case class BorrowerInput(
    fullName: String,
    phone: String,
    nationalId: String,
    monthlyIncome: BigDecimal,
    address: String
)

case class LoanInput(
    borrowerId: BorrowerId,
    principal: BigDecimal,
    annualRatePercent: BigDecimal,
    termMonths: Int,
    disbursedOn: LocalDate
)

case class PaymentInput(
    loanId: LoanId,
    amount: BigDecimal,
    paidOn: LocalDate
)

case class LoanQuote(
    totalInterest: BigDecimal,
    totalDue: BigDecimal,
    firstInstallment: BigDecimal,
    finalInstallment: BigDecimal
)

case class LoanOverview(
    loanId: LoanId,
    borrowerName: String,
    principal: BigDecimal,
    outstanding: BigDecimal,
    nextDueDate: Option[LocalDate],
    overdueDays: Int,
    status: LoanStatus,
    risk: RiskLevel
)

case class DashboardSnapshot(
    borrowerCount: Int,
    activeLoanCount: Int,
    overdueLoanCount: Int,
    totalOutstanding: BigDecimal,
    lowRiskCount: Int,
    mediumRiskCount: Int,
    highRiskCount: Int,
    loans: Vector[LoanOverview]
)
