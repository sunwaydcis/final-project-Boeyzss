package ui

import java.time.LocalDate
import scala.util.Try

import model.ValidationError
import scalafx.scene.control.{Label, TextField}

trait ViewSupport:
  protected def parseMoney(field: TextField, fieldName: String): Either[ValidationError, BigDecimal] =
    Try(BigDecimal(field.text.value.trim))
      .toEither
      .left
      .map(_ => ValidationError(s"$fieldName must be a valid number."))

  protected def parseWholeNumber(field: TextField, fieldName: String): Either[ValidationError, Int] =
    Try(field.text.value.trim.toInt)
      .toEither
      .left
      .map(_ => ValidationError(s"$fieldName must be a whole number."))

  protected def money(amount: BigDecimal): String = f"RM${amount}%.2f"

  protected def dateText(date: Option[LocalDate]): String = date.map(_.toString).getOrElse("—")

  protected def setStatus(label: Label, message: String, isError: Boolean): Unit =
    label.text = message
    label.styleClass.removeAll("status-success", "status-error")
    label.styleClass += (if isError then "status-error" else "status-success")
