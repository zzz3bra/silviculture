package com.zzz3bra.silviculture.domain;

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zzz3bra.silviculture.adapter.in.onliner.OnlinerSearcher
import com.zzz3bra.silviculture.adapter.out.persistence.JpaCustomerPersistenceAdapter
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

    private static final String ONLINER_URL = "https://ab.onliner.by/sdapi/ab.api/search/vehicles"
    private static final int DEFAULT_PAGE_SIZE = 50

    private static OnlinerSearcher onlinerSearcher = new OnlinerSearcher()
    @Shared
    private static DataSource postgresDatabase = EmbeddedPostgres.builder().setPort(5432).start().postgresDatabase
    private Connection connection = postgresDatabase.connection
    @Shared
    private static EbeanServer server = Ebean.getDefaultServer()

    static boolean isUrlAvailable(String url) {
        try {
            new URL(url).openConnection().connect();
            return true;
        } catch (IOException e) {
            return false;
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
        assert models.size() > 150

        expect:
        models.get(manufacturer).contains(model)

        where:
        manufacturer | model
        "honda"      | "civic"
        "bmw"        | "7 серия"
        "toyota"     | "supra"
        "rover"      | "streetwise"
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

    def "na ebean test"() {
        given:
        JpaCustomerPersistenceAdapter customerPersistenceAdapter = new JpaCustomerPersistenceAdapter();
        Customer customer = new Customer();
        customer.name = "Hello world";
        customer.searches = [Search.builder().manufacturer("Honda").minYear(2010).build()];
        customer.viewedAdsIdsBySearcher = new HashMap<>();
        customer.save();

        expect:
        Customer foundHello = customerPersistenceAdapter.loadOneCustomersById(1L).get();
        foundHello.searches.every { it.manufacturer == "Honda"};

        when:
        foundHello.searches.add(Search.builder().manufacturer("Nissan").minYear(2010).build());
        foundHello.update();
        customer = customerPersistenceAdapter.loadOneCustomersById(1L).get();

        then:
        customer.searches.each {
            verifyAll(it) {
                it.manufacturer in ["Honda", "Nissan"];
                it.minYear == 2010
            }
        };

        when:
        customer.viewedAdsIdsBySearcher.put(onlinerSearcher.getTechnicalName(), ["1", "2", "3"].toSet());
        customer.update();

        then:
        customerPersistenceAdapter.loadOneCustomersById(1L).get().viewedAdsIdsBySearcher.get(onlinerSearcher.getTechnicalName()).size() == 3

        when:
        customer = customerPersistenceAdapter.loadOneCustomersById(1L).get();
        customer.viewedAdsIdsBySearcher.get(onlinerSearcher.getTechnicalName()).add("4");
        customer.setViewedAdsIdsBySearcher(new HashMap<>(customer.viewedAdsIdsBySearcher));
        customer.update();

        then:
        customerPersistenceAdapter.loadOneCustomersById(1L).get().getViewedAdsIdsBySearcher().get(onlinerSearcher.getTechnicalName()).size() == 4

        when:
        customerPersistenceAdapter.loadOneCustomersById(1L).get().delete();

        then:
        customerPersistenceAdapter.loadAllCustomers().isEmpty()

    }
}
