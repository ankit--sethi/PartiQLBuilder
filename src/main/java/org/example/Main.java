package org.example;

import org.example.partiql.Conditions;
import org.example.partiql.Direction;
import org.example.partiql.PartiQLDelete;
import org.example.partiql.PartiQLInsert;
import org.example.partiql.PartiQLPath;
import org.example.partiql.PartiQLSelect;
import org.example.partiql.PartiQLUpdate;
import org.example.partiql.PartiQLValues;
import org.example.partiql.ReturnValues;

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
