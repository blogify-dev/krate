@file:Suppress("MemberVisibilityCanBePrivate")

package krate.optimizer

import reflectr.entity.Entity
import krate.models.EntityTable
import krate.annotations.SqlTable

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

import com.andreapivetta.kolor.lightRed

object TestEntityTable : EntityTable<TestEntity>() {
    val one = text("one")

    init {
        bind(one, TestEntity::one)
    }
}

@SqlTable(TestEntityTable::class)
data class TestEntity (
    val one: String
) : Entity()

class SingleRefOptimizerTest {

    @Test fun `should create proper join for TestEntity`() {
        val join = makeJoinForClass(TestEntity::class)

        println(join.columns.map { "${it.table.tableName}[${it.name.lightRed()}]" })

        listOf (
            "TestEntity[one]"
        ).forEach {
            assertTrue (
                join.columns.map { c -> "${c.table.tableName}[${c.name}]" }
                    .contains(it), "join did not contain $it"
            )
        }
    }

}
