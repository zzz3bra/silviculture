package com.zzz3bra.silviculture;

import com.zzz3bra.silviculture.domain.Advertisement;
import com.zzz3bra.silviculture.domain.Result;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;

public class SilvicultureBot extends TelegramLongPollingBot {

    private final Map<String, Advertisement> carAdvertisementMap = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        Message message;

        if (update.hasChannelPost()) {
            message = update.getChannelPost();
        } else if (update.hasMessage()) {
            message = update.getMessage();
        } else {
            return;
        }

        try {
            execute(new SendMessage().setChatId(message.getChatId()).setText("Don't call us, we'll call you..."));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void checkCarsAndPostNewIfAvailable(Supplier<Result> carsSupplier, String chatId) {
        Map<String, Advertisement> advertisements = carsSupplier.get().getAdvertisements();
        boolean firstRun = carAdvertisementMap.isEmpty();
        advertisements.forEach((adId, advertisement) -> {
            if (carAdvertisementMap.containsKey(adId)) {
                return;
            }
            carAdvertisementMap.put(adId, advertisement);
            if (firstRun) {
                return;
            }
            prepareStraightForwardMessages(advertisement, chatId).forEach(sendMessage -> {
                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private List<SendMessage> prepareStraightForwardMessages(Advertisement advertisement, String chatId) {
        List<SendMessage> messages = Arrays.stream(advertisement.getPhotos()).map(photo -> new SendMessage().setChatId(chatId).setText(photo.getImages().getOriginal())).collect(toList());
        messages.add(0, new SendMessage().setChatId(chatId).setText(createMessage(advertisement)));
        messages.add(new SendMessage().setChatId(chatId).setText("Каеф..."));
        return messages;
    }

    private String createMessage(Advertisement advertisement) {
        return "Опа, " + advertisement.getTitle() + "\n" +
                "и пробег всего лишь " + advertisement.getCar().getOdometerState() + " км" + "\n" +
                "Какая цаца... и всего за " + advertisement.getCar().getCostInUsd() + "$" + "\n";
    }

    @Override
    public String getBotUsername() {
        return "silviculture_bot";
    }

    @Override
    public String getBotToken() {
        return System.getenv("BotToken");
    }

}
