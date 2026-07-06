package org.example.partiql.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.example.partiql.Conditions;
import org.example.partiql.PartiQLBatch;
import org.example.partiql.PartiQLInsert;
import org.example.partiql.PartiQLSelect;
import org.example.partiql.PartiQLTransaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.BatchExecuteStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchStatementRequest;
import software.amazon.awssdk.services.dynamodb.model.ExecuteTransactionRequest;
import software.amazon.awssdk.services.dynamodb.model.ParameterizedStatement;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Exercises {@link PartiQLTransaction} and {@link PartiQLBatch} — including an {@code EXISTS}
 * condition check — by feeding their statement strings to {@code ExecuteTransaction} and
 * {@code BatchExecuteStatement} on DynamoDB Local.
 */
class TransactionBatchIT {

    private static final String TABLE = "TxnMusic";

    @BeforeAll
    static void setup() {
        Tables.hashRange(TABLE, "Artist", "SongTitle");
    }

    private static void runTransaction(List<String> statements) {
        DynamoDbLocal.client().executeTransaction(ExecuteTransactionRequest.builder()
                .transactStatements(statements.stream()
                        .map(s -> ParameterizedStatement.builder().statement(s).build())
                        .toList())
                .build());
    }

    private static int selectCount(String artist) {
        return DynamoDbLocal.execute(PartiQLSelect.from(TABLE)
                .where(Conditions.attr("Artist").eq(artist)).build()).size();
    }

    @Test
    void atomicMultiInsert() {
        List<String> statements = PartiQLTransaction.builder()
                .add(PartiQLInsert.into(TABLE).set("Artist", "Txn").set("SongTitle", "A"))
                .add(PartiQLInsert.into(TABLE).set("Artist", "Txn").set("SongTitle", "B"))
                .statements();

        runTransaction(statements);
        assertEquals(2, selectCount("Txn"));
    }

    @Test
    void existsConditionCheckSucceedsWhenGuardPresent() {
        // Seed a guard row, then a write transaction guarded by EXISTS on that row.
        DynamoDbLocal.execute(PartiQLInsert.into(TABLE)
                .set("Artist", "Guarded").set("SongTitle", "Guard").set("Active", true).build());

        List<String> statements = PartiQLTransaction.builder()
                .check(PartiQLSelect.from(TABLE)
                        .where(Conditions.attr("Artist").eq("Guarded")
                                .and(Conditions.attr("SongTitle").eq("Guard"))
                                .and(Conditions.attr("Active").eq(true))))
                .add(PartiQLInsert.into(TABLE).set("Artist", "Guarded").set("SongTitle", "New"))
                .statements();

        runTransaction(statements);
        assertEquals(2, selectCount("Guarded"));
    }

    @Test
    void existsConditionCheckCancelsWhenGuardMissing() {
        List<String> statements = PartiQLTransaction.builder()
                .check(PartiQLSelect.from(TABLE)
                        .where(Conditions.attr("Artist").eq("Nope")
                                .and(Conditions.attr("SongTitle").eq("Nope"))
                                .and(Conditions.attr("Active").eq(true))))
                .add(PartiQLInsert.into(TABLE).set("Artist", "Nope").set("SongTitle", "ShouldNotExist"))
                .statements();

        assertThrows(TransactionCanceledException.class, () -> runTransaction(statements));
        assertEquals(0, selectCount("Nope"));
    }

    @Test
    void batchInsert() {
        List<String> statements = PartiQLBatch.builder()
                .add(PartiQLInsert.into(TABLE).set("Artist", "Batch").set("SongTitle", "X"))
                .add(PartiQLInsert.into(TABLE).set("Artist", "Batch").set("SongTitle", "Y"))
                .add(PartiQLInsert.into(TABLE).set("Artist", "Batch").set("SongTitle", "Z"))
                .statements();

        DynamoDbLocal.client().batchExecuteStatement(BatchExecuteStatementRequest.builder()
                .statements(statements.stream()
                        .map(s -> BatchStatementRequest.builder().statement(s).build())
                        .toList())
                .build());

        assertEquals(3, selectCount("Batch"));
    }
}
