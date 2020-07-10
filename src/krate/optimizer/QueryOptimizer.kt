package krate.optimizer

import krate.util.*
import reflectr.entity.Entity
import reflectr.extensions.klass
import reflectr.extensions.safeKlass
import krate.binding.table
import krate.binding.SqlBinding
import krate.handling.query
import krate.models.QueryContext

import org.jetbrains.exposed.sql.*

import com.github.kittinunf.result.coroutines.SuspendableResult
import com.github.kittinunf.result.coroutines.mapError
import reflectr.extensions.okHandle
import reflectr.util.MappedData

import kotlin.reflect.KClass

import java.util.*

/**
 * Takes care of generating optimized, broad queries for fetching all data needed to instantiate
 * one of more entities.
 *
 * @author Benjozork
 */
object QueryOptimizer {

    private val classJoinCache = MapCache<KClass<out Entity>, ColumnSet>()

    fun <TEntity : Entity> optimize(klass: KClass<TEntity>, condition: SqlExpressionBuilder.() -> Op<Boolean>): Query {
        val mainJoin = classJoinCache.findOr(klass) {
            makeJoinForClass(
                klass
            )
        }
            .assertGet()

        return mainJoin.select(condition)
    }

    private object NullValue : Exception("no error - null value for SingleRef - this should not be thrown")

    /**
     * Takes a list of [result row][ResultRow] from a query of a [Join] generated by [optimize] and transforms it into a list of instances
     * of [TEntity] using the data in the row.
     *
     * This takes care of feeding the columns to the convert() functions of the [tables][krate.models.EntityTable] for
     * the classes of the different properties of [TEntity], and of running additional queries for [x-to-many references][SqlBinding.ReferenceToMany] if needed.
     *
     * @param queryContext the [QueryContext] for caching
     * @param klass          the class associated with [TEntity], used for reflection purposes
     * @param rows           a set of [result rows][ResultRow], each to be converted to instances of [TEntity]
     */
    suspend fun <TEntity : Entity> convertOptimizedRows (
            queryContext: QueryContext,
            klass: KClass<TEntity>,
            rows: List<ResultRow>
    ): List<TEntity> {
        return rows.map { row ->
            val bindingsData: MappedData = klass.table.bindings.map { binding ->
                binding.property.okHandle!! to Wrap {
                    when (binding) {
                        is SqlBinding.Reference<*, *>,
                        is SqlBinding.NullableReference<*, *> ->
                            resolveSingleRefInstance(
                                queryContext,
                                binding,
                                row
                            )
                                .get()
                        is SqlBinding.ReferenceToMany<*, *> -> {
                            resolveManyRefItems(
                                queryContext,
                                binding,
                                row[binding.table.uuid]
                            )
                                .get()
                        }
                        is SqlBinding.HasColumn<*> -> // Get the data directly from the row
                            row[binding.column] ?: if (binding.property.returnType.isMarkedNullable) {
                                throw NullValue
                            } else error("property '${binding.property.name}' is not marked nullable but column contained null value'")
                        else -> never
                    }
                }.mapError { exception ->
                    if (exception is NullValue)
                        return@mapError exception
                    else IllegalStateException("error occurred during optimized row processing for property '${binding.property.name}' " +
                            "- ${exception::class.simpleName}", exception)
                }
            }.toMap().mapValues { (_, result) ->
                when (result) {
                    is SuspendableResult.Success<*, *> -> result.value
                    is SuspendableResult.Failure<*, *> ->
                        if (result.getException() is NullValue)
                            null
                        else throw result.getException()
                }
            }

            with(queryContext) {
                klass.construct(bindingsData).get() // Finally, construct the class
            }
        }
    }

    private suspend fun resolveSingleRefInstance (
            queryContext: QueryContext,
            binding: SqlBinding<*, *, *>,
            row: ResultRow
    ): Sr<Entity> {
        val bindingRightTable = binding.property.returnType.safeKlass<Entity>()?.table ?: never
        val rightTableAlias = bindingRightTable.alias("${binding.property.klass.simpleName}->${binding.property.name}")

        // If row[bindingRightTable.uuid] is null, the item is null, so we should return a failure accordingly
        if (binding is SqlBinding.NullableReference)
            letCatchingOrNull {
                row[rightTableAlias[bindingRightTable.uuid]]
            } ?: return Sr.error(NullValue)

        // Call convert() on the property's table with the row; it contains its data too
        return bindingRightTable.convert (
            queryContext = queryContext,
            source = row,
            aliasToUse = bindingRightTable.alias("${binding.property.klass.simpleName}->${binding.property.name}")
        )
    }

    private suspend fun resolveManyRefItems (
            request: QueryContext,
            binding: SqlBinding.ReferenceToMany<*, *>,
            withId: UUID
    ): SrList<Any> =
        query { // Run a query to select the ManyRef items
            binding.otherTable.select { binding.otherTableFkToPkCol eq withId }
                .toSet().map { row -> binding.conversionFunction(row) }
        }

}
