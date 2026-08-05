import java.time.LocalDate

import munit.FunSuite

import model.*
import service.LoanService

final class LoanServiceSuite extends FunSuite:
  test("flat-interest schedule balances exactly after currency rounding") {
    val state = TestFixtures.stateWithBorrower
    val result = LoanService.disburseLoan(state, TestFixtures.loanInput(state), TestFixtures.today)
    val loan = result.toOption.map(_._2).getOrElse(fail("Loan should be created."))

    assertEquals(loan.totalDue, BigDecimal("1025.00"))
    assertEquals(loan.installments.map(_.amountDue), Vector(
      BigDecimal("341.67"), BigDecimal("341.67"), BigDecimal("341.66")
    ))
  }

  test("borrower cannot receive a second active loan") {
    val state = TestFixtures.stateWithLoan
    val secondLoan = LoanService.disburseLoan(state, TestFixtures.loanInput(state), TestFixtures.today)

    assertEquals(secondLoan.left.map(_.message), Left("This borrower already has an active loan."))
  }

  test("partial payment is allocated to oldest installments first") {
    val state = TestFixtures.stateWithLoan
    val loan = state.loans.headOption.getOrElse(fail("Expected one loan."))
    val result = LoanService.recordPayment(
      state,
      PaymentInput(loan.id, BigDecimal("400.00"), LocalDate.of(2026, 2, 15)),
      TestFixtures.today
    )
    val updatedLoan = result.toOption.map(_._2).getOrElse(fail("Payment should be recorded."))

    assertEquals(updatedLoan.installments.head.amountPaid, BigDecimal("341.67"))
    assertEquals(updatedLoan.installments(1).amountPaid, BigDecimal("58.33"))
    assertEquals(updatedLoan.outstanding, BigDecimal("625.00"))
  }

  test("overpayment is rejected with a friendly validation error") {
    val state = TestFixtures.stateWithLoan
    val loan = state.loans.headOption.getOrElse(fail("Expected one loan."))
    val result = LoanService.recordPayment(
      state,
      PaymentInput(loan.id, BigDecimal("2000.00"), LocalDate.of(2026, 2, 15)),
      TestFixtures.today
    )

    assert(result.left.exists(_.message.contains("exceeds the outstanding balance")))
  }

  test("risk changes from medium to high after thirty overdue days") {
    val state = TestFixtures.stateWithLoan
    val loan = state.loans.headOption.getOrElse(fail("Expected one loan."))

    assertEquals(LoanService.riskLevel(loan, LocalDate.of(2026, 2, 20)), RiskLevel.Medium)
    assertEquals(LoanService.riskLevel(loan, LocalDate.of(2026, 3, 10)), RiskLevel.High)
  }

  test("full repayment marks the loan as repaid") {
    val state = TestFixtures.stateWithLoan
    val loan = state.loans.headOption.getOrElse(fail("Expected one loan."))
    val result = LoanService.recordPayment(
      state,
      PaymentInput(loan.id, loan.outstanding, LocalDate.of(2026, 4, 2)),
      TestFixtures.today
    )
    val repaidLoan = result.toOption.map(_._2).getOrElse(fail("Payment should be recorded."))

    assertEquals(LoanService.loanStatus(repaidLoan, TestFixtures.today), LoanStatus.Repaid)
    assertEquals(repaidLoan.outstanding, BigDecimal("0.00"))
  }
