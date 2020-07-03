package krate.extensions

import reflectr.entity.Entity
import krate.DatabaseContext
import krate.QueryContext
import krate.annotations.table
import krate.persistence.models.Repository
import krate.persistence.postgres.PostgresRepository

import kotlin.reflect.KClass

/**
 * Provides a repository for [TEntity]
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified TEntity : Entity> DatabaseContext.repository(): Repository<TEntity> =
    this.repoCache.findOr(TEntity::class) {
        PostgresRepository(TEntity::class.table) as Repository<Entity>
    }.get() as Repository<TEntity>

/**
 * Provides a repository for [TEntity]
 */
@Suppress("UNCHECKED_CAST")
fun <TEntity : Entity> DatabaseContext.repository(klass: KClass<out TEntity>): Repository<TEntity> =
    this.repoCache.findOr(klass) {
        PostgresRepository(klass.table) as Repository<Entity>
    }.get() as Repository<TEntity>

/**
 * Provides a repository for [TEntity]
 */
inline fun <reified TEntity : Entity> QueryContext.repository(): Repository<TEntity> =
    databaseContext.repository()

/**
 * Provides a repository for [TEntity]
 */
inline fun <reified TEntity : Entity> QueryContext.repository(klass: KClass<out TEntity>): Repository<TEntity> =
    databaseContext.repository(klass)
