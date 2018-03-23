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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class SilvicultureBot extends TelegramLongPollingBot {

    private static final String ADD = "/add ";
    private static final String REMOVE = "/remove ";
    private static final String CLEAR = "/clear";
    private static final String RESET = "/reset";
    private static final String STATUS = "/status";

    private final Map<Pair<String, String>, List<Pair<String, String>>> carManModelMap;
    private final Map<String, Advertisement> carAdvertisementMap = new HashMap<>();
    private final Set<Pair<Integer, Integer>> requestCarModels = new HashSet<>();

    SilvicultureBot(Map<Pair<String, String>, List<Pair<String, String>>> carManModelMap) {
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

        if (message.getText().equals(STATUS)) {
            String cars = carManModelMap.entrySet().stream().flatMap(entry -> entry.getValue().stream().map(pair -> ImmutablePair.of(entry.getKey(), pair))).filter(pair -> requestCarModels.contains(ImmutablePair.of(Integer.valueOf(pair.getLeft().getValue()), Integer.valueOf(pair.getRight().getValue())))).map(pair -> pair.getLeft().getKey() + " " + pair.getRight().getKey()).collect(joining("\n"));
            toBeSent.add(new SendMessage().setText("Отслеживаемые автомобили: \n" + cars));
        }

        toBeSent = doAddOrRemoveActionIfSupported(message.getText());

        toBeSent.forEach(sendMessage -> {
            try {
                execute(sendMessage.setChatId(message.getChatId()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        });
    }

    List<SendMessage> doAddOrRemoveActionIfSupported(String action) {
        String[] parts;
        Function<Pair<Integer, Integer>, Boolean> pairAction;
        List<SendMessage> actionSuccessMessages = new ArrayList<>();
        List<SendMessage> actionFailedMessages = new ArrayList<>();
        if (action.startsWith(ADD)) {
            parts = action.substring(ADD.length()).toLowerCase().split(" ");
            pairAction = requestCarModels::add;
            actionSuccessMessages.add(new SendMessage().setText("Слежу (за лупой) ..."));
            actionSuccessMessages.add(new SendMessage().setText("https://avatanplus.com/files/resources/mid/5a105f460a1a615fcff42999.png"));
            actionFailedMessages.add(new SendMessage().setText("Данный автомобиль уже отслеживается"));
        } else if (action.startsWith(REMOVE)) {
            parts = action.substring(REMOVE.length()).toLowerCase().split(" ");
            pairAction = requestCarModels::remove;
            actionSuccessMessages.add(new SendMessage().setText("Поиск удален"));
            actionFailedMessages.add(new SendMessage().setText("Данный автомобиль не отслеживается"));
        } else {
            return emptyList();
        }

        List<SendMessage> actionOutcomeMessages = new ArrayList<>();
        if (parts.length > 2) {
            actionOutcomeMessages.add(new SendMessage().setText("Я хочу спать а не парсить марки и модели с пробелами - прямо как в твоих познаниях о теории струн"));
        } else {
            Optional<Pair<String, String>> manPair = carManModelMap.keySet().stream().filter(pair -> pair.getKey().equals(parts[0])).findAny();
            if (!manPair.isPresent()) {
                actionOutcomeMessages.add(new SendMessage().setText("Не могу найти марку, попросите товарища капитана пусть в закладках поищет"));
            } else {
                Optional<Pair<String, String>> modelPair = carManModelMap.get(manPair.get()).stream().filter(pair -> pair.getKey().equals(parts[1])).findAny();
                if (!modelPair.isPresent()) {
                    actionOutcomeMessages.add(new SendMessage().setText("Не могу найти модель, хохлушки кончились"));
                } else {
                    boolean isActionSuccessful = pairAction.apply(ImmutablePair.of(Integer.valueOf(manPair.get().getValue()), Integer.valueOf(modelPair.get().getValue())));
                    actionOutcomeMessages.addAll(isActionSuccessful ? actionSuccessMessages : actionFailedMessages);
                }
            }
        }
        return actionOutcomeMessages;
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
