package com.zzz3bra.silviculture;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkerProcess {

    public static void main(String[] args) throws Exception {
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();
        SilvicultureBot bot = new SilvicultureBot();
        botsApi.registerBot(bot);
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            try {
                bot.checkCarsAndPostNewIfAvailable(CarParser::getSilvias, System.getenv("ChannelId"));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }, 0, 1, TimeUnit.HOURS);
    }

}
