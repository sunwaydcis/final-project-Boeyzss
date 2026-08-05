package service

import model.*

object LoanRules:
  val MinimumPrincipal: BigDecimal = BigDecimal(100)
  val MaximumPrincipal: BigDecimal = BigDecimal(50000)
  val MinimumRatePercent: BigDecimal = BigDecimal(0)
  val MaximumRatePercent: BigDecimal = BigDecimal(30)
  val MinimumTermMonths: Int = 1
  val MaximumTermMonths: Int = 36

  private val phonePattern = "^\\+?[0-9][0-9 -]{6,18}[0-9]$".r

  def validateBorrower(input: BorrowerInput): Either[ValidationError, BorrowerInput] =
    for
      _ <- require(input.fullName.trim.nonEmpty, "Borrower name is required.")
      _ <- require(input.fullName.trim.length >= 2, "Borrower name must contain at least 2 characters.")
      _ <- require(
        phonePattern.matches(input.phone.trim),
        "Phone must contain 7 to 20 digits and may start with +."
      )
      _ <- require(input.nationalId.trim.nonEmpty, "National ID is required.")
      _ <- require(input.monthlyIncome > 0, "Monthly income must be greater than RM0.")
      _ <- require(input.address.trim.nonEmpty, "Address is required.")
    yield input.copy(
      fullName = input.fullName.trim,
      phone = input.phone.trim,
      nationalId = input.nationalId.trim,
      monthlyIncome = input.monthlyIncome.setScale(2),
      address = input.address.trim
    )

  def validateLoan(input: LoanInput): Either[ValidationError, LoanInput] =
    for
      _ <- require(
        input.principal >= MinimumPrincipal && input.principal <= MaximumPrincipal,
        s"Principal must be between RM$MinimumPrincipal and RM$MaximumPrincipal."
      )
      _ <- require(
        input.annualRatePercent >= MinimumRatePercent && input.annualRatePercent <= MaximumRatePercent,
        s"Annual flat interest rate must be between $MinimumRatePercent% and $MaximumRatePercent%."
      )
      _ <- require(
        input.termMonths >= MinimumTermMonths && input.termMonths <= MaximumTermMonths,
        s"Loan term must be between $MinimumTermMonths and $MaximumTermMonths months."
      )
    yield input.copy(
      principal = input.principal.setScale(2),
      annualRatePercent = input.annualRatePercent.setScale(2)
    )

  def validatePayment(input: PaymentInput): Either[ValidationError, PaymentInput] =
    require(input.amount > 0, "Payment amount must be greater than RM0.")
      .map(_ => input.copy(amount = input.amount.setScale(2)))

  private def require(condition: Boolean, message: String): Either[ValidationError, Unit] =
    Either.cond(condition, (), ValidationError(message))
