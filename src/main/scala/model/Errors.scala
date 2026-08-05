package model

sealed trait AppError:
  def message: String

case class ValidationError(override val message: String) extends AppError
case class NotFoundError(override val message: String) extends AppError
case class PersistenceError(override val message: String) extends AppError
