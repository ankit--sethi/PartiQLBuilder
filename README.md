# PartiQLBuilder

[![CI](https://github.com/ankit--sethi/PartiQLBuilder/actions/workflows/ci.yml/badge.svg)](https://github.com/ankit--sethi/PartiQLBuilder/actions/workflows/ci.yml)

Fluent Java builders for constructing valid, well-formatted [PartiQL](https://partiql.org/) statements for
Amazon DynamoDB. The builders handle the fiddly parts — string escaping, identifier quoting, reserved
words, typed value literals (maps, lists, string/number sets) and document paths — so you get a
ready-to-execute statement string every time.

Four entry points cover the full statement surface:

- `PartiQLSelect`
- `PartiQLInsert`
- `PartiQLUpdate`
- `PartiQLDelete`

Plus `PartiQLTransaction` and `PartiQLBatch` for assembling multi-statement `ExecuteTransaction` /
`BatchExecuteStatement` requests.

## Requirements

- Java 21+
- Maven 3.9+

## Installation

Once published to Maven Central (see [PUBLISHING.md](PUBLISHING.md) for how releases are cut):

```xml
<dependency>
    <groupId>io.github.ankit--sethi</groupId>
    <artifactId>PartiQLBuilder</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'io.github.ankit--sethi:PartiQLBuilder:1.0.0'
```

## Quick start

```java
import io.github.ankitsethi.partiql.*;

// SELECT
String select = PartiQLSelect.from("Orders")
        .select("OrderID", "Total")
        .where(Conditions.attr("OrderID").in(1, 2, 3))
        .orderBy("OrderID", Direction.DESC)
        .build();
// SELECT "OrderID", "Total" FROM "Orders" WHERE "OrderID" IN [1, 2, 3] ORDER BY "OrderID" DESC

// Query a global secondary index: FROM "table"."index"
String byIndex = PartiQLSelect.from("Orders")
        .index("CustomerIndex")
        .where(Conditions.attr("CustomerID").eq("C1"))
        .build();

// INSERT
String insert = PartiQLInsert.into("Music")
        .set("Artist", "Acme Band")
        .set("SongTitle", "PartiQL Rocks")
        .set("Awards", PartiQLValues.numberSet(2020, 2018))
        .build();
// INSERT INTO "Music" VALUE {'Artist': 'Acme Band', 'SongTitle': 'PartiQL Rocks', 'Awards': <<2020, 2018>>}

// UPDATE
String update = PartiQLUpdate.table("Music")
        .set("AwardsWon", 1)
        .add(PartiQLPath.attribute("Plays"), 100)                 // SET "Plays" = "Plays" + 100
        .listAppend(PartiQLPath.of("AwardDetail", "Grammys"),     // list_append(...)
                PartiQLValues.listOf(2016))
        .where(Conditions.attr("Artist").eq("Acme Band")
                .and(Conditions.attr("SongTitle").eq("PartiQL Rocks")))
        .returning(ReturnValues.ALL_NEW)
        .build();

// DELETE
String delete = PartiQLDelete.from("Music")
        .where(Conditions.attr("Artist").eq("Acme Band")
                .and(Conditions.attr("SongTitle").eq("PartiQL Rocks")))
        .returning(ReturnValues.ALL_OLD)
        .build();
```

Feed the resulting strings to the AWS SDK:

```java
dynamoDbClient.executeStatement(ExecuteStatementRequest.builder().statement(select).build());
```

## Conditions

`Conditions.attr(name)` starts a fluent condition. Supported: comparisons (`eq/ne/gt/lt/ge/le`),
`between`, `in`, `beginsWith`, `contains`, `attributeType`, `isNull`/`isMissing` (and negations),
`size()`, and `and`/`or`/`not` with automatic, correct parenthesization.

```java
Conditions.attr("OrderID").eq(100)
        .and(Conditions.attr("Address").beginsWith("7834"))
        .and(Conditions.attr("Image").size().gt(300));
// "OrderID" = 100 AND begins_with("Address", '7834') AND size("Image") > 300
```

## Values, quoting and escaping

- **Strings** are single-quoted with embedded quotes doubled: `it's` → `'it''s'`.
- **Identifiers** (tables, indexes, attributes) are always double-quoted with `"` doubled, so reserved
  words and special characters are always safe.
- **Types**: numbers, `TRUE`/`FALSE`, `NULL`, lists `[...]`, maps `{'k': v}`, string sets `<<'a','b'>>`,
  number sets `<<1,2>>`. Binary has no PartiQL literal form and is rejected with a clear error.
- `ReservedWords.isReserved(name)` is available for validation/introspection.

Values are inlined (not parameter markers), with rigorous escaping to prevent injection.

## Building and testing

```bash
mvn test      # unit tests (statement/escaping correctness)
mvn verify    # unit + integration tests
```

Integration tests execute the generated statements against an in-process
[DynamoDB Local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)
(no Docker required).

> **Note:** DynamoDB Local `3.2.x`–`3.3.0` shipped without its PartiQL runtime, so PartiQL calls
> failed with a 500 unless the parser was added by hand
> ([awslabs/amazon-dynamodb-local-samples#35](https://github.com/awslabs/amazon-dynamodb-local-samples/issues/35)).
> This project uses `3.3.2`, which declares those dependencies again.

## License

[MIT](LICENSE)
