package com.zzz3bra.silviculture;

import io.ebean.EbeanServer;
import io.ebean.EbeanServerFactory;
import io.ebean.config.ServerConfig;
import io.ebean.datasource.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.zzz3bra.silviculture.data.gathering.AdvertisementFinder.AdvertisementSearchers.ONLINER;

public class WorkerProcess {
    public static final Logger LOGGER = LoggerFactory.getLogger(WorkerProcess.class);
    private static final List<String> INITIAL_COMMANDS = Arrays.asList("/add lexus sc", "/add mazda mx-5", "/add nissan silvia", "/add nissan 300zx", "/add nissan 350z", "/add nissan juke", "/add nissan 100nx", "/add nissan 200sx", "/add nissan 240sx", "/add opel frontera", "/add toyota supra");
    private static final EbeanServer DEFAULT_SERVER;
    static {
        URI dbUri = URI.create(System.getenv("DATABASE_URL"));
        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];

        DataSourceConfig postgresDb = new DataSourceConfig();
        postgresDb.setDriver("org.postgresql.Driver");
        postgresDb.setUsername(username);
        postgresDb.setMaxInactiveTimeSecs(15 * 60);
        postgresDb.setPassword(password);
        postgresDb.setUrl(System.getenv("JDBC_DATABASE_URL"));

        ServerConfig config = new ServerConfig();
        config.setDataSourceConfig(postgresDb);
        config.setRegister(true);
        config.setDefaultServer(true);

        DEFAULT_SERVER = EbeanServerFactory.create(config);
    }

    public static void main(String[] args) throws Exception {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        SilvicultureBot bot = new SilvicultureBot(ONLINER);
        //INITIAL_COMMANDS.forEach(bot::doAddOrRemoveActionIfSupported);
        botsApi.registerBot(bot);
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                bot.checkCarsAndPostNewIfAvailable();
            } catch (Exception exception) {
                LOGGER.error("check for new cars failed", exception);
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

}
