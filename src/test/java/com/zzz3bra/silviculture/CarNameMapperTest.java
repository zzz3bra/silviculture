package com.zzz3bra.silviculture;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

class CarNameMapperTest {

    @Test
    void parseIds() throws Exception {
        String manufacturers = readFile("Manufacturers.json");
        String manufacturesModel = readFile("ManufacturesModel.json");
        Map<Pair<String, String>, List<Pair<String, String>>> map = CarNameMapper.parseIds(manufacturers, manufacturesModel);
        assertThat(map.get(ImmutablePair.of("nissan", "45")), hasItem(ImmutablePair.of("silvia", "922")));
    }

    private String readFile(String filename) throws IOException {
        File file = new File(ClassLoader.getSystemClassLoader().getResource(filename).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }

}
