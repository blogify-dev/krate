# Module krate

Unopiniated and simple ORM for Kotlin using [Exposed](https://github.com/JetBrains/Exposed)

Krate works using two base models: `Entity` and `Repository`. As the names imply, repositories do the job of managing CRUD operations regarding entities.

## Features

- `Entity` to `Table` binding
- Extensions for `exposed`'s tables
- Query OptimKotlin library for adding support for computed properties, mapping of objects and generation of DTOs and metadata and helpers for dealing with entitiesizer
- Default `Repository` implementation

## Installation

Simply include the library from jitpack

#### Sample (Gradle Kotlin DSL) :

```kotlin
repositories {
    maven("https://jitpack.io")
}

/* ... */

dependencies {
    implementation("com.github.blogify-dev", "krate", "master-SNAPSHOT")
}

``` 

# Package krate.binding

A binding makes a link (called a relationship) between a property of an entity class and a way of representing that relationship with SQL.

There currently exists five kinds of bindings supported by Krate, all declared in the `SqlBinding` sealed class :

- `SqlBinding.Value` - binds a property of an Entity class to a plain value column (in other words, a column containing a simple value without linking to other entities)
- `SqlBinding.OneToOne` - binds a property of type `Entity` of an the Entity class to a UUID column storing a reference to an entity of that type 
- `SqlBinding.OneToOneOrNone` - same, but accepts `null`
- `SqlBinding.OneToMany` -  binds a property of type `Collection<Entity>` of an the Entity classs to another table storing entities.
    - **note:** this binding kind represents a true one-to-many relationship; entities on the right-hand side of the relationship can only be linked to a single entity on the left-hand side, and this is enforced by Krate by requiring the entity table on the right-hand side to possess a FK to the entity table on the left-hand side.
- `SqlBinding.OneToManyValues` -  binds a property of an Entity class to a table storing plain values


## Example

```kotlin
@SqlTable(People::class)
class Person(val name: String, uuid: UUID = UUID.randomUUID()) : Entity(uuid)

object People : EntityTable<Person>(Person::class, name = "people") {

    val name  = text ("name")
    val uuid  = uuid ("uuid")

    init {
        bind(uuid, Person::uuid)
        bind(name, Person::name)
    }

}
```

# Package krate.computed

Includes special computed property containers for entities 

# Package krate.computed.extensions

Extensions for `krate.computed` package

# Package krate.handling

Houses query handler functions

# Package krate.models

Model classes for dealing with repositories and tables

# Package krate.optimizer

Optimizes entity queries on common operations to reduce the amount of effective SQL queries

