package com.zzz3bra.silviculture;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkerProcess {

    public static void main(String[] args) throws Exception {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        String manufacturers = readFile("Manufacturers.json");
        String manufacturesModel = readFile("ManufacturesModel.json");
        SilvicultureBot bot = new SilvicultureBot(CarNameMapper.parseIds(manufacturers, manufacturesModel));
        botsApi.registerBot(bot);
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                bot.checkCarsAndPostNewIfAvailable(CarParser::getResult, System.getenv("ChannelId"));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    private static String readFile(String filename) throws IOException {
        File file = new File(ClassLoader.getSystemClassLoader().getResource(filename).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }

}
