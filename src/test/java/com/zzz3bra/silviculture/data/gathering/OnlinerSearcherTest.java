package com.zzz3bra.silviculture.data.gathering;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class OnlinerSearcherTest {

    private static final String ONLINER_URL = "https://ab.onliner.by/search";
    private static final String MANUFACTURERS = "onliner/Manufacturers.json";
    private static final String MANUFACTURERS_MODELS = "onliner/ManufacturersModel.json";
    private static final int DEFAULT_PAGE_SIZE = 50;

    @BeforeAll
    public static void setUpTests() {
        Stream.of(MANUFACTURERS, MANUFACTURERS_MODELS).forEach(filename -> {
            URL resource = ClassLoader.getSystemClassLoader().getResource(filename);
            assertThat(resource).overridingErrorMessage("File [%s] is not present in classpath but required for test execution", filename).isNotNull();
        });
    }

    @Test
    void shouldBeAbleToParseOnlinerIds() {
        OnlinerSearcher onlinerSearcher = new OnlinerSearcher(MANUFACTURERS, MANUFACTURERS_MODELS);

        assertThat(onlinerSearcher.supportedManufacturersAndModels()).size().isEqualTo(147);
    }

    @Test
    void shouldSearchForRockingSilviasOfCourseWhyElseWeBotherWritingAllThisCode() {
        assumeOnlinerIsAvailable();

        OnlinerSearcher onlinerSearcher = new OnlinerSearcher(MANUFACTURERS, MANUFACTURERS_MODELS);

        assertThat(onlinerSearcher.find(Search.builder().manufacturer("Nissan").modelName("Silvia").build())).isNotNull();
    }

    @Test
    void shouldSearchForOldRattletraps() {
        assumeOnlinerIsAvailable();

        OnlinerSearcher onlinerSearcher = new OnlinerSearcher(MANUFACTURERS, MANUFACTURERS_MODELS);

        assertThat(onlinerSearcher.find(Search.builder().maxYear(1985).build())).hasSize(DEFAULT_PAGE_SIZE);
    }

    @Test
    void shouldNotFailOnNonExistentModels() {
        assumeOnlinerIsAvailable();

        OnlinerSearcher onlinerSearcher = new OnlinerSearcher(MANUFACTURERS, MANUFACTURERS_MODELS);

        assertThat(onlinerSearcher.find(Search.builder().manufacturer("стасян").modelName("civic").build())).hasSize(DEFAULT_PAGE_SIZE);
    }

    private static Object assumeOnlinerIsAvailable() {
        return assumeThat(isUrlAvailable(ONLINER_URL)).overridingErrorMessage("[%s] is unavailable", ONLINER_URL).isTrue();
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
