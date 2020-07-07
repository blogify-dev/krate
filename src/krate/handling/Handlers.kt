package krate.handling

import krate.util.getOr
import krate.annotations.DatabaseDsl

import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

import com.github.kittinunf.result.coroutines.SuspendableResult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.lang.Exception
import java.lang.IllegalStateException

/**
 * This function executes a query asynchronously and returns its result.
 *
 * @param block the query block to execute
 *
 * @author Benjozork
 */
@DatabaseDsl
suspend fun <T : Any> query(block: suspend () -> T): SuspendableResult<T, Exception> {
    return SuspendableResult.of<T, Exception> { // The transaction can throw any Exception; specify that
        withContext(Dispatchers.IO) { newSuspendedTransaction { block() } } // Run the transaction
    }
}

/**
 * This function executes a query asynchronously and returns its result with the resulting [SuspendableResult] forcibly unwrapped.
 * Should be used for queries that should not fail in a normal situation, even if their proper use can include failures such as not found errors.
 *
 * @param block the query block to execute
 *
 * @author Benjozork
 */
@DatabaseDsl
suspend fun <T : Any> unwrappedQuery(block: suspend () -> T): T =
    query(block).getOr { throw IllegalStateException("unwrapped query resulted in a failure", it) }
