import munit.FunSuite

import model.{Borrower, BorrowerId}
import repository.{EntityIdentity, Repository}

final class RepositorySuite extends FunSuite:
  private given EntityIdentity[Borrower, BorrowerId] with
    override def id(entity: Borrower): BorrowerId = entity.id

  test("generic repository adds, finds, and updates an immutable entity") {
    val borrower = TestFixtures.borrower(TestFixtures.stateWithBorrower)
    val repository = Repository.empty[Borrower, BorrowerId]
    val added = repository.add(borrower).getOrElse(fail("Borrower should be added."))
    val changed = borrower.copy(phone = "+60111111111")
    val updated = added.update(changed).getOrElse(fail("Borrower should be updated."))

    assertEquals(repository.all, Vector.empty)
    assertEquals(updated.find(borrower.id).map(_.phone), Some("+60111111111"))
  }

  test("generic repository prevents duplicate identifiers") {
    val borrower = TestFixtures.borrower(TestFixtures.stateWithBorrower)
    val repository = Repository.empty[Borrower, BorrowerId]
      .add(borrower)
      .getOrElse(fail("Borrower should be added."))

    assertEquals(repository.add(borrower), None)
  }
