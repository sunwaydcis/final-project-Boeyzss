package service

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

import model.*
import repository.{EntityIdentity, Repository}
import scala.math.BigDecimal.RoundingMode

// ai-assisted: #3
// why: Codex helped implement pure, testable state transitions for lending and repayment rules.
object LoanService:
  private given EntityIdentity[Borrower, BorrowerId] with
    override def id(entity: Borrower): BorrowerId = entity.id

  def registerBorrower(
      state: AppState,
      input: BorrowerInput,
      createdOn: LocalDate
  ): Either[AppError, (AppState, Borrower)] =
    for
      validInput <- LoanRules.validateBorrower(input)
      _ <- ensureNationalIdAvailable(state, validInput.nationalId, None)
      borrower = Borrower(
        id = BorrowerId(UUID.randomUUID().toString),
        fullName = validInput.fullName,
        phone = validInput.phone,
        nationalId = validInput.nationalId,
        monthlyIncome = validInput.monthlyIncome,
        address = validInput.address,
        createdOn = createdOn
      )
      repository = Repository.from[Borrower, BorrowerId](state.borrowers)
      updatedRepository <- repository
        .add(borrower)
        .toRight(ValidationError("A borrower with this identifier already exists."))
    yield (state.copy(borrowers = updatedRepository.all), borrower)

  def editBorrower(
      state: AppState,
      borrowerId: BorrowerId,
      input: BorrowerInput
  ): Either[AppError, (AppState, Borrower)] =
    for
      validInput <- LoanRules.validateBorrower(input)
      repository = Repository.from[Borrower, BorrowerId](state.borrowers)
      existing <- repository.find(borrowerId).toRight(NotFoundError("Borrower was not found."))
      _ <- ensureNationalIdAvailable(state, validInput.nationalId, Some(borrowerId))
      updatedBorrower = existing.copy(
        fullName = validInput.fullName,
        phone = validInput.phone,
        nationalId = validInput.nationalId,
        monthlyIncome = validInput.monthlyIncome,
        address = validInput.address
      )
      updatedRepository <- repository
        .update(updatedBorrower)
        .toRight(NotFoundError("Borrower was not found."))
    yield (state.copy(borrowers = updatedRepository.all), updatedBorrower)

  def disburseLoan(
      state: AppState,
      input: LoanInput,
      today: LocalDate
  ): Either[AppError, (AppState, Loan)] =
    for
      validInput <- LoanRules.validateLoan(input)
      _ <- Either.cond(
        !validInput.disbursedOn.isAfter(today),
        (),
        ValidationError("Disbursement date cannot be in the future.")
      )
      _ <- state.borrowers
        .find(_.id == validInput.borrowerId)
        .toRight(NotFoundError("Select a valid borrower before disbursing a loan."))
      _ <- Either.cond(
        state.loans.forall(loan => loan.borrowerId != validInput.borrowerId || loan.outstanding == zeroMoney),
        (),
        ValidationError("This borrower already has an active loan.")
      )
      installments = buildSchedule(validInput)
      loan = Loan(
        id = LoanId(UUID.randomUUID().toString),
        borrowerId = validInput.borrowerId,
        principal = validInput.principal,
        annualRatePercent = validInput.annualRatePercent,
        termMonths = validInput.termMonths,
        disbursedOn = validInput.disbursedOn,
        installments = installments,
        payments = Vector.empty
      )
    yield (state.copy(loans = state.loans :+ loan), loan)

  def recordPayment(
      state: AppState,
      input: PaymentInput,
      today: LocalDate
  ): Either[AppError, (AppState, Loan)] =
    for
      validInput <- LoanRules.validatePayment(input)
      loan <- state.loans.find(_.id == validInput.loanId).toRight(NotFoundError("Loan was not found."))
      _ <- Either.cond(
        !validInput.paidOn.isBefore(loan.disbursedOn),
        (),
        ValidationError("Payment date cannot be before the loan disbursement date.")
      )
      _ <- Either.cond(
        !validInput.paidOn.isAfter(today),
        (),
        ValidationError("Payment date cannot be in the future.")
      )
      _ <- Either.cond(
        validInput.amount <= loan.outstanding,
        (),
        ValidationError(f"Payment exceeds the outstanding balance of RM${loan.outstanding}%.2f.")
      )
      updatedInstallments = allocatePayment(loan.installments, validInput.amount)
      payment = Payment(PaymentId(UUID.randomUUID().toString), validInput.amount, validInput.paidOn)
      updatedLoan = loan.copy(
        installments = updatedInstallments,
        payments = loan.payments :+ payment
      )
      updatedLoans = state.loans.map(existingLoan =>
        if existingLoan.id == updatedLoan.id then updatedLoan else existingLoan
      )
    yield (state.copy(loans = updatedLoans), updatedLoan)

  def quote(input: LoanInput): Either[AppError, LoanQuote] =
    LoanRules.validateLoan(input).map(validInput =>
      val installments = buildSchedule(validInput)
      val totalDue = installments.map(_.amountDue).sum.setScale(2)
      LoanQuote(
        totalInterest = (totalDue - validInput.principal).setScale(2),
        totalDue = totalDue,
        firstInstallment = installments.headOption.map(_.amountDue).getOrElse(zeroMoney),
        finalInstallment = installments.lastOption.map(_.amountDue).getOrElse(zeroMoney)
      )
    )

  def loanStatus(loan: Loan, today: LocalDate): LoanStatus =
    if loan.outstanding == zeroMoney then LoanStatus.Repaid
    else if loan.installments.exists(installment =>
        installment.outstanding > zeroMoney && installment.dueDate.isBefore(today)
      )
    then LoanStatus.Overdue
    else LoanStatus.Active

  def riskLevel(loan: Loan, today: LocalDate): RiskLevel =
    if loan.outstanding == zeroMoney then RiskLevel.Low
    else
      maximumOverdueDays(loan, today) match
        case overdueDays if overdueDays > 30 => RiskLevel.High
        case overdueDays if overdueDays > 0  => RiskLevel.Medium
        case _                              => RiskLevel.Low

  def dashboardSnapshot(state: AppState, today: LocalDate): DashboardSnapshot =
    val overviews = state.loans.map(loan => loanOverview(state, loan, today))
    DashboardSnapshot(
      borrowerCount = state.borrowers.size,
      activeLoanCount = overviews.count(overview => overview.status == LoanStatus.Active),
      overdueLoanCount = overviews.count(overview => overview.status == LoanStatus.Overdue),
      totalOutstanding = overviews.map(_.outstanding).sum.setScale(2),
      lowRiskCount = overviews.count(_.risk == RiskLevel.Low),
      mediumRiskCount = overviews.count(_.risk == RiskLevel.Medium),
      highRiskCount = overviews.count(_.risk == RiskLevel.High),
      loans = overviews.sortBy(overview => (-overview.overdueDays, overview.borrowerName))
    )

  private def ensureNationalIdAvailable(
      state: AppState,
      nationalId: String,
      excludedBorrowerId: Option[BorrowerId]
  ): Either[ValidationError, Unit] =
    Either.cond(
      state.borrowers.forall(borrower =>
        !borrower.nationalId.equalsIgnoreCase(nationalId) || excludedBorrowerId.contains(borrower.id)
      ),
      (),
      ValidationError("National ID is already registered.")
    )

  private def buildSchedule(input: LoanInput): Vector[Installment] =
    val interest = (
      input.principal * (input.annualRatePercent / BigDecimal(100)) * (BigDecimal(input.termMonths) / BigDecimal(12))
    ).setScale(2, RoundingMode.HALF_UP)
    val totalDue = (input.principal + interest).setScale(2, RoundingMode.HALF_UP)
    val standardInstallment = (totalDue / BigDecimal(input.termMonths)).setScale(2, RoundingMode.HALF_UP)
    val finalInstallment = (
      totalDue - (standardInstallment * BigDecimal(input.termMonths - 1))
    ).setScale(2, RoundingMode.HALF_UP)

    Vector.tabulate(input.termMonths)(installmentIndex =>
      val installmentNumber = installmentIndex + 1
      val amountDue = if installmentNumber == input.termMonths then finalInstallment else standardInstallment
      Installment(
        number = installmentNumber,
        dueDate = input.disbursedOn.plusMonths(installmentNumber.toLong),
        amountDue = amountDue,
        amountPaid = zeroMoney
      )
    )

  private def allocatePayment(
      installments: Vector[Installment],
      paymentAmount: BigDecimal
  ): Vector[Installment] =
    installments
      .foldLeft((paymentAmount, Vector.empty[Installment])) {
        case ((remainingAmount, updatedInstallments), installment) =>
          val allocatedAmount = remainingAmount.min(installment.outstanding).setScale(2)
          val updatedInstallment = installment.copy(
            amountPaid = (installment.amountPaid + allocatedAmount).setScale(2)
          )
          (
            (remainingAmount - allocatedAmount).max(zeroMoney).setScale(2),
            updatedInstallments :+ updatedInstallment
          )
      }
      ._2

  private def loanOverview(state: AppState, loan: Loan, today: LocalDate): LoanOverview =
    val borrowerName = state.borrowers.find(_.id == loan.borrowerId).map(_.fullName).getOrElse("Unknown borrower")
    val pendingInstallments = loan.installments.filterNot(_.isPaid)
    LoanOverview(
      loanId = loan.id,
      borrowerName = borrowerName,
      principal = loan.principal,
      outstanding = loan.outstanding,
      nextDueDate = pendingInstallments.map(_.dueDate).minOption,
      overdueDays = maximumOverdueDays(loan, today),
      status = loanStatus(loan, today),
      risk = riskLevel(loan, today)
    )

  private def maximumOverdueDays(loan: Loan, today: LocalDate): Int =
    loan.installments
      .filter(installment => installment.outstanding > zeroMoney && installment.dueDate.isBefore(today))
      .map(installment => ChronoUnit.DAYS.between(installment.dueDate, today).toInt)
      .maxOption
      .getOrElse(0)

  private val zeroMoney: BigDecimal = BigDecimal(0).setScale(2)
