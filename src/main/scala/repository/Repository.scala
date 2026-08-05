package repository

trait EntityIdentity[Entity, Identifier]:
  def id(entity: Entity): Identifier

// ai-assisted: #1
// why: A generic immutable repository demonstrates reusable parametric polymorphism without forcing storage details into the domain model.
final class Repository[Entity, Identifier] private (
    // Kept private so callers cannot bypass the repository's immutable update operations.
    private val entries: Vector[Entity]
)(using identity: EntityIdentity[Entity, Identifier]):

  def all: Vector[Entity] = entries

  def find(identifier: Identifier): Option[Entity] =
    entries.find(entity => identity.id(entity) == identifier)

  def add(entity: Entity): Option[Repository[Entity, Identifier]] =
    Option.when(find(identity.id(entity)).isEmpty)(new Repository(entries :+ entity))

  def update(entity: Entity): Option[Repository[Entity, Identifier]] =
    Option.when(find(identity.id(entity)).nonEmpty)(
      new Repository(entries.map(existing =>
        if identity.id(existing) == identity.id(entity) then entity else existing
      ))
    )

object Repository:
  def empty[Entity, Identifier](using EntityIdentity[Entity, Identifier]): Repository[Entity, Identifier] =
    new Repository(Vector.empty)

  def from[Entity, Identifier](entries: Vector[Entity])(using
      EntityIdentity[Entity, Identifier]
  ): Repository[Entity, Identifier] =
    new Repository(entries)
