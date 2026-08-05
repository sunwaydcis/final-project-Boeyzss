import munit.FunSuite

import model.*
import service.LoanRules

final class LoanRulesSuite extends FunSuite:
  test("borrower validation trims valid fields") {
    val result = LoanRules.validateBorrower(
      TestFixtures.borrowerInput.copy(fullName = "  Amina Rahman  ", address = "  Selangor  ")
    )

    assertEquals(result.map(_.fullName), Right("Amina Rahman"))
    assertEquals(result.map(_.address), Right("Selangor"))
  }

  test("borrower validation rejects empty name, wrong phone, and non-positive income") {
    val emptyName = LoanRules.validateBorrower(TestFixtures.borrowerInput.copy(fullName = ""))
    val wrongPhone = LoanRules.validateBorrower(TestFixtures.borrowerInput.copy(phone = "abc"))
    val invalidIncome = LoanRules.validateBorrower(TestFixtures.borrowerInput.copy(monthlyIncome = BigDecimal(0)))

    assert(emptyName.isLeft)
    assert(wrongPhone.isLeft)
    assert(invalidIncome.isLeft)
  }

  test("loan validation enforces principal, rate, and term boundaries") {
    val state = TestFixtures.stateWithBorrower
    val baseInput = TestFixtures.loanInput(state)

    assert(LoanRules.validateLoan(baseInput.copy(principal = BigDecimal("99.99"))).isLeft)
    assert(LoanRules.validateLoan(baseInput.copy(annualRatePercent = BigDecimal("30.01"))).isLeft)
    assert(LoanRules.validateLoan(baseInput.copy(termMonths = 37)).isLeft)
    assert(LoanRules.validateLoan(baseInput).isRight)
  }
