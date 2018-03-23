package com.zzz3bra.silviculture;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class CarParserTest {

    @Test
    void getOldRattletraps() {
        assertThat(CarParser.getOldRattletraps(), is(notNullValue()));
    }

}
