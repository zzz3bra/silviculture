package com.zzz3bra.silviculture.data.gathering

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zzz3bra.silviculture.data.Customer
import groovy.sql.Sql
import io.ebean.Ebean
import io.ebean.EbeanServer
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.sql.DataSource
import java.sql.Connection

class OnlinerSearcherTest extends Specification {

    private static final String ONLINER_URL = "https://ab.onliner.by/search"
    private static final String MANUFACTURERS = "onliner/Manufacturers.json"
    private static final String MANUFACTURERS_MODELS = "onliner/ManufacturersModel.json"
    private static final int DEFAULT_PAGE_SIZE = 50

    private static OnlinerSearcher onlinerSearcher = new OnlinerSearcher(MANUFACTURERS, MANUFACTURERS_MODELS)
    @Shared
    private static DataSource postgresDatabase = EmbeddedPostgres.builder().setPort(5432).start().postgresDatabase
    private Connection connection = postgresDatabase.connection
    @Shared
    private static EbeanServer server = Ebean.getDefaultServer()

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
        sql.rows("SELECT * FROM users WHERE ID = 1;").every { it.name == "username" }
    }

    def "SQL could be executed twice"() {
        given: "an empty DB"
        Sql sql = new Sql(connection)
        when:
        sql.execute("CREATE TABLE users (ID int, NAME TEXT NOT NULL,PRIMARY KEY(ID)); INSERT INTO users VALUES(1,'username');")
        then:
        sql.rows("SELECT * FROM users WHERE ID = 1;").every { it.name == "username" }
    }

    @Unroll
    def "Should be able to parse ONLINER IDs and get #manufacturer #model"() {
        given:
        def models = onlinerSearcher.supportedManufacturersAndModels()
        assert models.size() == 147

        expect:
        models.get(manufacturer).contains(model)

        where:
        manufacturer | model
        "honda"      | "civic"
        "bmw"        | "740"
        "toyota"     | "supra"
    }

    @IgnoreIf({ !OnlinerSearcherTest.isUrlAvailable(ONLINER_URL) })
    def "Should search for rocking silvias of course why else we bother writing all this code, Stas"() {
        expect:
        onlinerSearcher.find(Search.builder().manufacturer(manufacturer).modelName(model).build()) != null
        where:
        manufacturer | model
        "Nissan"     | "Silvia"
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

    def "first ebean test"() {
        given:
        Customer customer = new Customer();
        customer.setName("Hello world");

        // insert the customer in the DB
        server.save(customer);

        expect:
        // Find by Id
        Customer foundHello = server.find(Customer.class, 1);

        System.out.println("hello " + foundHello.getName());

        // delete the customer
        server.delete(customer);
    }
}
