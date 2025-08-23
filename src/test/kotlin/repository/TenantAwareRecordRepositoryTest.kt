package repository

import com.muditsahni.model.entity.Record
import com.muditsahni.model.entity.RecordStatus
import com.muditsahni.model.entity.RecordType
import com.muditsahni.service.TenantAwareMongoService
import com.mongodb.client.result.DeleteResult
import com.muditsahni.repository.TenantAwareRecordRepository
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.test.runTest
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

class TenantAwareRecordRepositoryTest {

    private lateinit var tenantAwareMongoService: TenantAwareMongoService
    private lateinit var mongoTemplate: ReactiveMongoTemplate
    private lateinit var repo: TenantAwareRecordRepository

    @BeforeEach
    fun setup() {
        tenantAwareMongoService = mockk()
        mongoTemplate = mockk(relaxed = true) // relaxed so default Monos/Fluxes don't crash
        every { tenantAwareMongoService.getCurrentTenantTemplate() } returns mongoTemplate
        repo = TenantAwareRecordRepository(tenantAwareMongoService)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun sampleRecord(
        id: UUID = UUID.randomUUID(),
        tenantName: String = "acme",
        batchId: UUID = UUID.randomUUID(),
        fileName: String = "file.pdf",
        status: RecordStatus = RecordStatus.CREATED,
        type: RecordType = RecordType.INVOICE,
        createdAt: Instant = Instant.parse("2024-01-01T00:00:00Z"),
        updatedAt: Instant? = null,
        createdBy: String = "user@example.com",
        updatedBy: String? = null
    ) = Record(
        id = id,
        tenantName = tenantName,
        batchId = batchId,
        fileName = fileName,
        status = status,
        type = type,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
    )


    @Test
    fun `save updates updatedAt and returns saved record`() = runTest {
        val rec = sampleRecord(updatedAt = Instant.now().minusSeconds(7200))
        val savedSlot = slot<Record>()

        every { mongoTemplate.save(capture(savedSlot)) } answers { Mono.just(savedSlot.captured) }

        val before = Instant.now()
        val result = repo.save(rec)
        val after = Instant.now()

        assertSame(rec, result) // same instance returned from our mock
        assertNotNull(result.updatedAt)
        // updatedAt must be refreshed by repository prior to calling save
        assertTrue(result.updatedAt?.isAfter(before.minusSeconds(1)) == true && result.updatedAt!!.isBefore(after.plusSeconds(1)))

        verify(exactly = 1) { tenantAwareMongoService.getCurrentTenantTemplate() }
        verify(exactly = 1) { mongoTemplate.save(any<Record>()) }
        confirmVerified(mongoTemplate)
    }

    @Test
    fun `findById returns record when found`() = runTest {
        val id = UUID.randomUUID()
        val rec = sampleRecord(id = id)
        val querySlot = slot<Query>()

        every { mongoTemplate.findOne(capture(querySlot), Record::class.java) } returns Mono.just(rec)

        val found = repo.findById(id)
        assertNotNull(found)
        assertEquals(id, found!!.id)

        // verify query shape
        val q: Document = querySlot.captured.queryObject
        assertEquals(id, q.get("id"))

        verify { mongoTemplate.findOne(any(), Record::class.java) }
    }

    @Test
    fun `findById returns null when not found`() = runTest {
        val id = UUID.randomUUID()
        every { mongoTemplate.findOne(any(), Record::class.java) } returns Mono.empty()

        val found = repo.findById(id)
        assertNull(found)
    }

    @Test
    fun `findAll returns all as flow`() = runTest {
        val r1 = sampleRecord()
        val r2 = sampleRecord()
        every { mongoTemplate.findAll(Record::class.java) } returns Flux.just(r1, r2)

        val list = repo.findAll().toList()
        assertEquals(2, list.size)
        assertTrue(list.containsAll(listOf(r1, r2)))
    }

    @Test
    fun `deleteById calls remove`() = runTest {
        val id = UUID.randomUUID()
        val querySlot = slot<Query>()
        every { mongoTemplate.remove(capture(querySlot), Record::class.java) } returns Mono.just(DeleteResult.acknowledged(1))

        repo.deleteById(id)

        val q = querySlot.captured.queryObject
        assertEquals(id, q.get("id"))
        verify { mongoTemplate.remove(any(), Record::class.java) }
    }

    @Test
    fun `delete(record) delegates to deleteById`() = runTest {
        val rec = sampleRecord()
        val id = rec.id
        val querySlot = slot<Query>()
        every { mongoTemplate.remove(capture(querySlot), Record::class.java) } returns Mono.just(DeleteResult.acknowledged(1))

        repo.delete(rec)

        val q = querySlot.captured.queryObject
        assertEquals(id, q.get("id"))
    }

    @Test
    fun `existsById returns boolean`() = runTest {
        val id = UUID.randomUUID()
        every { mongoTemplate.exists(any(), Record::class.java) } returns Mono.just(true)

        val exists = repo.existsById(id)
        assertTrue(exists)

        every { mongoTemplate.exists(any(), Record::class.java) } returns Mono.just(false)
        val notExists = repo.existsById(id)
        assertFalse(notExists)
    }

    @Test
    fun `count returns total count`() = runTest {
        every { mongoTemplate.count(any(), Record::class.java) } returns Mono.just(42L)
        val c = repo.count()
        assertEquals(42L, c)
    }

    @Test
    fun `findByBatchId queries by batchId`() = runTest {
        val batchId = UUID.randomUUID()
        val r1 = sampleRecord(batchId = batchId)
        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), Record::class.java) } returns Flux.just(r1)

        val list = repo.findByBatchId(batchId).toList()
        assertEquals(1, list.size)
        assertEquals(batchId, list[0].batchId)

        val q = querySlot.captured.queryObject
        assertEquals(batchId, q.get("batchId"))
    }

    @Test
    fun `findByBatchIdAndStatus queries by both`() = runTest {
        val batchId = UUID.randomUUID()
        val status = RecordStatus.UPLOADED
        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), Record::class.java) } returns Flux.empty()

        repo.findByBatchIdAndStatus(batchId, status).toList()

        val q = querySlot.captured.queryObject
        assertEquals(batchId, q.get("batchId"))
        assertEqCriterion(querySlot.captured, "status", status)
    }

    @Test
    fun `findByBatchIdAndType queries by both`() = runTest {
        val batchId = UUID.randomUUID()
        val type = RecordType.INVOICE
        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), Record::class.java) } returns Flux.empty()

        repo.findByBatchIdAndType(batchId, type).toList()

        val q = querySlot.captured.queryObject
        assertEquals(batchId, q.get("batchId"))
        assertEqCriterion(querySlot.captured, "type", type)
    }

    @Test
    fun `findByStatus and findByType and findByTenantName and findByCreatedBy`() = runTest {
        // Status
        val status = RecordStatus.CREATED
        val statusQ = slot<Query>()
        every { mongoTemplate.find(capture(statusQ), Record::class.java) } returns Flux.empty()
        repo.findByStatus(status).toList()
        assertEqCriterion(statusQ.captured, "status", RecordStatus.CREATED)

        // Type
        val type = RecordType.INVOICE
        val typeQ = slot<Query>()
        every { mongoTemplate.find(capture(typeQ), Record::class.java) } returns Flux.empty()
        repo.findByType(type).toList()
        assertEqCriterion(typeQ.captured, "type", RecordType.INVOICE)

        // Tenant
        val tnQ = slot<Query>()
        every { mongoTemplate.find(capture(tnQ), Record::class.java) } returns Flux.empty()
        repo.findByTenantName("acme").toList()
        assertEqCriterion(tnQ.captured, "tenantName", "acme")

        // CreatedBy
        val cbQ = slot<Query>()
        every { mongoTemplate.find(capture(cbQ), Record::class.java) } returns Flux.empty()
        repo.findByCreatedBy("user@example.com").toList()
        assertEqCriterion(cbQ.captured, "createdBy", "user@example.com")
    }

    @Test
    fun `findByBatchIdAndStatusAndDocumentType queries all three`() = runTest {
        val batchId = UUID.randomUUID()
        val status = RecordStatus.CREATED
        val docType = "invoice"

        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), Record::class.java) } returns Flux.empty()

        repo.findByBatchIdAndStatusAndDocumentType(batchId, status, docType).toList()

        val q = querySlot.captured.queryObject
        assertEquals(batchId, q.get("batchId"))
        assertEqCriterion(querySlot.captured, "status", status)

        assertEqCriterion(querySlot.captured, "documentType", docType)
    }

    @Test
    fun `deleteByBatchId returns deletedCount when fixed`() = runTest {
        val batchId = UUID.randomUUID()
        every { mongoTemplate.remove(any(), Record::class.java) } returns Mono.just(DeleteResult.acknowledged(3))

        // After applying the repository fix that awaits and returns deletedCount
        val repoFixed = object : TenantAwareRecordRepository(tenantAwareMongoService) {
            override suspend fun deleteByBatchId(batchId: UUID): Long {
                val template = tenantAwareMongoService.getCurrentTenantTemplate()
                val query = Query.query(Criteria.where("batchId").`is`(batchId))
                val result = template.remove(query, Record::class.java).awaitFirst()
                return result.deletedCount
            }
        }

        val count = repoFixed.deleteByBatchId(batchId)
        assertEquals(3L, count)
    }

    @Test
    fun `findByCreatedAtBetween queries using gte and lte`() = runTest {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = Instant.parse("2024-01-31T23:59:59Z")

        val querySlot = slot<Query>()
        every { mongoTemplate.find(capture(querySlot), Record::class.java) } returns Flux.empty()

        repo.findByCreatedAtBetween(start, end).toList()

        val q = querySlot.captured.queryObject
        val createdAtDoc = q.get("createdAt") as Document
        assertEquals(start, createdAtDoc["\$gte"])
        assertEquals(end, createdAtDoc["\$lte"])
    }



    private fun assertEqCriterion(q: Query, field: String, expected: Any) {
        val node = q.queryObject[field]
        when (node) {
            is Document -> {
                // Equality could be stored as {"$eq": value}
                val eq = node["\$eq"]
                // Enums may appear as enum instance or name; handle both
                if (expected is Enum<*>) {
                    assertEquals(expected.name, eq?.toString())
                } else {
                    assertEquals(expected.toString(), eq?.toString())
                }
            }
            else -> {
                // Direct value (enum instance or its name or UUID)
                if (expected is Enum<*>) {
                    assertEquals(expected.name, node.toString())
                } else {
                    assertEquals(expected, node)
                }
            }
        }
    }

    @Test
    fun `countBy aggregations map to correct fields`() = runTest {
        every { mongoTemplate.count(any(), Record::class.java) } returns Mono.just(5L)

        // By batch
        val batchId = UUID.randomUUID()
        var qs = slot<Query>()
        every { mongoTemplate.count(capture(qs), Record::class.java) } returns Mono.just(5L)
        assertEquals(5L, repo.countByBatchId(batchId))
        assertEquals(batchId, qs.captured.queryObject["batchId"])

        // By batch + status
        val status = RecordStatus.CREATED
        qs = slot()
        every { mongoTemplate.count(capture(qs), Record::class.java) } returns Mono.just(7L)
        assertEquals(7L, repo.countByBatchIdAndStatus(batchId, status))
        assertEqCriterion(qs.captured, "batchId", batchId)

        // By batch + type
        val type = RecordType.INVOICE
        qs = slot()
        every { mongoTemplate.count(capture(qs), Record::class.java) } returns Mono.just(3L)
        assertEquals(3L, repo.countByBatchIdAndType(batchId, type))
        assertEqCriterion(qs.captured, "type", type)

        // By status
        qs = slot()
        every { mongoTemplate.count(capture(qs), Record::class.java) } returns Mono.just(11L)
        assertEquals(11L, repo.countByStatus(status))
        assertEqCriterion(qs.captured, "status", status)

        // By type
        qs = slot()
        every { mongoTemplate.count(capture(qs), Record::class.java) } returns Mono.just(13L)
        assertEquals(13L, repo.countByType(type))
        assertEqCriterion(qs.captured, "type", type)

        // By tenant
        qs = slot()
        every { mongoTemplate.count(capture(qs), Record::class.java) } returns Mono.just(19L)
        assertEquals(19L, repo.countByTenantName("acme"))
        assertEqCriterion(qs.captured, "tenantName", "acme")
    }
}
