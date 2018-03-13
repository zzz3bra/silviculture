package com.zzz3bra.silviculture;

import com.zzz3bra.silviculture.domain.Advertisement;
import com.zzz3bra.silviculture.domain.Result;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class SilvicultureBot extends TelegramLongPollingBot {

    private static final String ADD = "/add ";
    private static final String REMOVE = "/remove ";
    private static final String CLEAR = "/clear";
    private static final String RESET = "/reset";

    private final Map<Pair<String, String>, List<Pair<String, String>>> carManModelMap;
    private final Map<String, Advertisement> carAdvertisementMap = new HashMap<>();
    private final List<Pair<Integer, Integer>> requestCarModels = new ArrayList<>();

    public SilvicultureBot(Map<Pair<String, String>, List<Pair<String, String>>> carManModelMap) {
        super();
        this.carManModelMap = carManModelMap;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message;
        List<SendMessage> toBeSent = new ArrayList<>();

        if (update.hasChannelPost()) {
            message = update.getChannelPost();
        } else if (update.hasMessage()) {
            message = update.getMessage();
        } else {
            return;
        }

        if (message.getText().equals(RESET)) {
            carAdvertisementMap.clear();
            toBeSent.add(new SendMessage().setText("Почистил историю уже отправленных машин, тоби пизда, готовься к спаму"));
            toBeSent.add(new SendMessage().setText("https://d2lnr5mha7bycj.cloudfront.net/product-image/file/large_c6ca8b18-ec5b-4e8b-a7c6-35dc403bf214.JPG"));
        }

        if (message.getText().equals(CLEAR)) {
            requestCarModels.clear();
            toBeSent.add(new SendMessage().setText("Запрашиваемые модели очищены, буду теперь молчать в трубочку..."));
        }

        doAddOrRemoveActionIfSupported(message.getText(), toBeSent);

        toBeSent.forEach(sendMessage -> {
            try {
                execute(sendMessage.setChatId(message.getChatId()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        });
    }

    private void doAddOrRemoveActionIfSupported(String action, List<SendMessage> toBeSent) {
        String[] parts;
        Consumer<Pair<Integer, Integer>> pairAction;
        List<SendMessage> actionSuccessMessages = new ArrayList<>();
        if (action.startsWith(ADD)) {
            parts = action.substring(ADD.length()).toLowerCase().split(" ");
            pairAction = requestCarModels::add;
            actionSuccessMessages.add(new SendMessage().setText("Слежу (за лупой) ..."));
            actionSuccessMessages.add(new SendMessage().setText("https://avatanplus.com/files/resources/mid/5a105f460a1a615fcff42999.png"));
        } else if (action.startsWith(REMOVE)) {
            parts = action.substring(REMOVE.length()).toLowerCase().split(" ");
            pairAction = requestCarModels::remove;
            actionSuccessMessages.add(new SendMessage().setText("Поиск удален"));
        } else {
            return;
        }

        if (parts.length > 2) {
            toBeSent.add(new SendMessage().setText("Я хочу спать а не парсить марки и модели с пробелами - прямо как в твоих познаниях о теории струн"));
        } else {
            Optional<Pair<String, String>> manPair = carManModelMap.keySet().stream().filter(pair -> pair.getKey().equals(parts[0])).findAny();
            if (!manPair.isPresent()) {
                toBeSent.add(new SendMessage().setText("Не могу найти марку, попросите товарища капитана пусть в закладках поищет"));
            } else {
                Optional<Pair<String, String>> modelPair = carManModelMap.get(manPair.get()).stream().filter(pair -> pair.getKey().equals(parts[1])).findAny();
                if (!modelPair.isPresent()) {
                    toBeSent.add(new SendMessage().setText("Не могу найти модель, хохлушки кончились"));
                } else {
                    pairAction.accept(ImmutablePair.of(Integer.valueOf(manPair.get().getValue()), Integer.valueOf(modelPair.get().getValue())));
                    toBeSent.addAll(actionSuccessMessages);
                }
            }
        }
    }

    public void checkCarsAndPostNewIfAvailable(Supplier<Result> carsSupplier, String chatId) {
        Map<String, Advertisement> advertisements = carsSupplier.get().getAdvertisements().entrySet().stream().filter(entry -> requestCarModels.contains(ImmutablePair.of(entry.getValue().getCar().getModel().getManufacturerId(), entry.getValue().getCar().getModel().getId()))).collect(toMap(Entry::getKey, Entry::getValue));
        advertisements.forEach((adId, advertisement) -> {
            if (carAdvertisementMap.containsKey(adId)) {
                return;
            }
            carAdvertisementMap.put(adId, advertisement);
            prepareStraightForwardMessages(advertisement, chatId).forEach(sendMessage -> {
                try {
                    execute(sendMessage);
                } catch (Exception e) {
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
