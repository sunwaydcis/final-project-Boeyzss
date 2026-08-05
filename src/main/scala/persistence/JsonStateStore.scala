package persistence

import java.nio.charset.StandardCharsets
import java.nio.file.{AtomicMoveNotSupportedException, Files, Path, StandardCopyOption, StandardOpenOption}
import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode
import scala.util.Try

import model.*
import upickle.default.{ReadWriter, macroRW, read, readwriter, write}

object JsonCodecs:
  given ReadWriter[BigDecimal] = readwriter[String].bimap(
    decimal => decimal.setScale(2, RoundingMode.HALF_UP).toString,
    text => BigDecimal(text)
  )
  given ReadWriter[LocalDate] = readwriter[String].bimap(_.toString, LocalDate.parse)
  given ReadWriter[BorrowerId] = macroRW
  given ReadWriter[LoanId] = macroRW
  given ReadWriter[PaymentId] = macroRW
  given ReadWriter[Borrower] = macroRW
  given ReadWriter[Installment] = macroRW
  given ReadWriter[Payment] = macroRW
  given ReadWriter[Loan] = macroRW
  given ReadWriter[AppState] = macroRW

// ai-assisted: #6
// why: Codex helped isolate risky JSON and file operations behind an injectable persistence boundary.
final class JsonStateStore(val path: Path):
  import JsonCodecs.given

  def load(): Either[PersistenceError, AppState] =
    if Files.notExists(path) then Right(AppState.empty)
    else
      Try(Files.readString(path, StandardCharsets.UTF_8))
        .flatMap(jsonText => Try(read[AppState](jsonText)))
        .toEither
        .left
        .map(error => PersistenceError(s"Saved data could not be loaded: ${friendlyMessage(error)}"))

  def save(state: AppState): Either[PersistenceError, Unit] =
    val saveAttempt = for
      _ <- Try(Option(path.getParent).foreach(parentPath => Files.createDirectories(parentPath)))
      jsonText <- Try(write(state, indent = 2))
      temporaryPath = path.resolveSibling(s"${path.getFileName}.tmp")
      _ <- Try(
        Files.writeString(
          temporaryPath,
          jsonText,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE
        )
      )
      _ <- moveIntoPlace(temporaryPath)
    yield ()

    saveAttempt.toEither.left.map(error =>
      PersistenceError(s"Changes could not be saved: ${friendlyMessage(error)}")
    )

  private def moveIntoPlace(temporaryPath: Path): Try[Path] =
    Try(
      Files.move(
        temporaryPath,
        path,
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING
      )
    ).recoverWith {
      case _: AtomicMoveNotSupportedException =>
        Try(Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING))
    }

  private def friendlyMessage(error: Throwable): String =
    Option(error.getMessage).filter(_.trim.nonEmpty).getOrElse(error.getClass.getSimpleName)
