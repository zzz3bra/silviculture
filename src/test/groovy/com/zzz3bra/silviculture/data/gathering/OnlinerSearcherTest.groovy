package com.zzz3bra.silviculture.data.gathering

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import groovy.sql.Sql
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection

class OnlinerSearcherTest extends Specification {

    private static final String ONLINER_URL = "https://ab.onliner.by/search"
    private static final String MANUFACTURERS = "onliner/Manufacturers.json"
    private static final String MANUFACTURERS_MODELS = "onliner/ManufacturersModel.json"
    private static final int DEFAULT_PAGE_SIZE = 50

    private static OnlinerSearcher onlinerSearcher = new OnlinerSearcher(MANUFACTURERS, MANUFACTURERS_MODELS)
    @Shared
    private static DataSource postgresDatabase = EmbeddedPostgres.builder().start().postgresDatabase
    private Connection connection = postgresDatabase.connection

    static boolean isUrlAvailable(String url) {
        new URL(url).openConnection().connect()
        return true
    }

    def setupSpec() {
        [MANUFACTURERS, MANUFACTURERS_MODELS].each {
            assert ClassLoader.systemClassLoader.getResource(it)
        }
    }

    def setup() {
        connection.autoCommit = false
    }

    def cleanup() {
        connection?.rollback()
        connection?.close()
    }

    def "SQL could be executed"() {
        given: "an empty DB"
        Sql sql = new Sql(connection)
        when:
        sql.execute("CREATE TABLE users (ID int, NAME TEXT NOT NULL,PRIMARY KEY(ID)); INSERT INTO users VALUES(1,'username');")
        then:
        sql.rows("SELECT * FROM users WHERE ID = 1;").each { it.name == "username" }
    }

    def "SQL could be executed twice"() {
        given: "an empty DB"
        Sql sql = new Sql(connection)
        when:
        sql.execute("CREATE TABLE users (ID int, NAME TEXT NOT NULL,PRIMARY KEY(ID)); INSERT INTO users VALUES(1,'username');")
        then:
        sql.rows("SELECT * FROM users WHERE ID = 1;").each { it.name == "username" }
    }

    def "Should be abel to parse ONLINER IDs"() {
        expect:
        onlinerSearcher.supportedManufacturersAndModels().size() == 147
    }

    @IgnoreIf({ !OnlinerSearcherTest.isUrlAvailable(ONLINER_URL) })
    def "Should search for rocking silvias of course why else we bother writing all this code, Stas"() {
        expect:
        onlinerSearcher.find(Search.builder().manufacturer("Nissan").modelName("Silvia").build()) != null
    }

    @IgnoreIf({ !OnlinerSearcherTest.isUrlAvailable(ONLINER_URL) })
    def "Should search by manufacturer name and model"() {
        given:
        def listOfAds = onlinerSearcher.find(Search.builder().manufacturer("Audi").modelName("100").build())
        expect:
        listOfAds.every {
            it.car.model == "100"
        }
    }

    @IgnoreIf({ !OnlinerSearcherTest.isUrlAvailable(ONLINER_URL) })
    def "Should search for old rattletraps"() {
        expect:
        onlinerSearcher.find(Search.builder().maxYear(1985).build()).size() == DEFAULT_PAGE_SIZE
    }

    @IgnoreIf({ !OnlinerSearcherTest.isUrlAvailable(ONLINER_URL) })
    def "Should not fail on nonexistent models"() {
        expect:
        onlinerSearcher.find(Search.builder().manufacturer("стасян").modelName("civic").build()).size() == DEFAULT_PAGE_SIZE
    }

}
