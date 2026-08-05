package ui

import java.time.Clock

import model.*
import persistence.JsonStateStore
import scalafx.beans.property.{ObjectProperty, StringProperty}

final class ApplicationController(store: JsonStateStore, val clock: Clock):
  private val initialLoad = store.load()

  val stateProperty: ObjectProperty[AppState] = ObjectProperty(initialLoad.getOrElse(AppState.empty))
  val notificationProperty: StringProperty = StringProperty(
    initialLoad.left.toOption.map(_.message).getOrElse("Ready")
  )

  def transact[Entity](
      operation: AppState => Either[AppError, (AppState, Entity)],
      successMessage: String
  ): Either[AppError, Entity] =
    operation(stateProperty.value).flatMap { case (updatedState, entity) =>
      store.save(updatedState).map { _ =>
        stateProperty.value = updatedState
        notificationProperty.value = successMessage
        entity
      }
    }

  def report(error: AppError): Unit =
    notificationProperty.value = error.message

  def today = java.time.LocalDate.now(clock)
