package io.github.ankitsethi;

import io.github.ankitsethi.partiql.Conditions;
import io.github.ankitsethi.partiql.Direction;
import io.github.ankitsethi.partiql.PartiQLDelete;
import io.github.ankitsethi.partiql.PartiQLInsert;
import io.github.ankitsethi.partiql.PartiQLPath;
import io.github.ankitsethi.partiql.PartiQLSelect;
import io.github.ankitsethi.partiql.PartiQLUpdate;
import io.github.ankitsethi.partiql.PartiQLValues;
import io.github.ankitsethi.partiql.ReturnValues;

/** A small demo that prints one statement per builder. */
public class Main {

    public static void main(String[] args) {
        System.out.println(PartiQLSelect.from("Orders")
                .select("OrderID", "Total")
                .where(Conditions.attr("OrderID").in(1, 2, 3))
                .orderBy("OrderID", Direction.DESC)
                .build());

        System.out.println(PartiQLInsert.into("Music")
                .set("Artist", "Acme Band")
                .set("SongTitle", "PartiQL Rocks")
                .set("Awards", PartiQLValues.numberSet(2020, 2018))
                .build());

        System.out.println(PartiQLUpdate.table("Music")
                .set("AwardsWon", 1)
                .set(PartiQLPath.of("AwardDetail", "Grammys"),
                        PartiQLValues.listOf(2020, 2018))
                .where(Conditions.attr("Artist").eq("Acme Band")
                        .and(Conditions.attr("SongTitle").eq("PartiQL Rocks")))
                .returning(ReturnValues.ALL_NEW)
                .build());

        System.out.println(PartiQLDelete.from("Music")
                .where(Conditions.attr("Artist").eq("Acme Band")
                        .and(Conditions.attr("SongTitle").eq("PartiQL Rocks")))
                .returning(ReturnValues.ALL_OLD)
                .build());
    }
}
