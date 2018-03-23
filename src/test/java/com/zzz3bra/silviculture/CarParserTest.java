package com.zzz3bra.silviculture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class CarParserTest {

    private static final String ONLINER_URL = "https://ab.onliner.by/search";

    @Test
    void getOldRattletraps() {
        assumeThat(isUrlAvailable(ONLINER_URL)).overridingErrorMessage("[%s] is unavailable", ONLINER_URL).isTrue();
        assertThat(CarParser.getOldRattletraps()).isNotNull();
    }

    private static boolean isUrlAvailable(String url) {
        try {
            new URL(url).openConnection().connect();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
