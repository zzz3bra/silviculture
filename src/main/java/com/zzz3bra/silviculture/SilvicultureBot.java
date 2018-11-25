package com.zzz3bra.silviculture;

import com.zzz3bra.silviculture.data.Ad;
import com.zzz3bra.silviculture.data.Customer;
import com.zzz3bra.silviculture.data.gathering.Search;
import com.zzz3bra.silviculture.data.gathering.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class SilvicultureBot extends TelegramLongPollingBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(SilvicultureBot.class);

    private static final String ADD = "/add ";
    private static final String REMOVE = "/remove ";
    private static final String CLEAR = "/clear";
    private static final String RESET = "/reset";
    private static final String STATUS = "/status";

    private final Map<Searcher, Set<Ad>> currentSessionAds = new HashMap<>();
    private final List<Searcher> searchers;

    SilvicultureBot(Searcher... searchers) {
        super();
        this.searchers = Arrays.asList(searchers);
        this.searchers.forEach(searcher -> currentSessionAds.put(searcher, new HashSet<>()));
    }

    @Override
    public void onUpdateReceived(Update update) {
        final Long chatId = update.getMessage().getChatId();
        Customer customer = Customer.find.byId(chatId);
        if (customer == null) {
            customer = new Customer();
            customer.setId(chatId);
            customer.setName("аноним");
            customer.setSearches(new ArrayList<>());
            customer.save();
        }
        final List<Search> searches = customer.getSearches();

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
            currentSessionAds.clear();
            toBeSent.add(new SendMessage().setText("Почистил историю уже отправленных машин, тоби пизда, готовься к спаму"));
            toBeSent.add(new SendMessage().setText("https://d2lnr5mha7bycj.cloudfront.net/product-image/file/large_c6ca8b18-ec5b-4e8b-a7c6-35dc403bf214.JPG"));
        }

        if (message.getText().equals(CLEAR)) {
            searches.clear();
            toBeSent.add(new SendMessage().setText("Запрашиваемые модели очищены, буду теперь молчать в трубочку..."));
        }

        if (message.getText().equals(STATUS)) {
            String cars = searches.stream().map(search -> String.join(" ", search.getManufacturer(), search.getModelName())).collect(joining("\n"));
            toBeSent.add(new SendMessage().setText("Отслеживаемые автомобили: \n" + cars));
        }

        toBeSent = doAddOrRemoveActionIfSupported(message.getText(), searches);

        customer.update();

        toBeSent.forEach(sendMessage -> {
            try {
                execute(sendMessage.setChatId(message.getChatId()));
            } catch (TelegramApiException e) {
                LOGGER.error("message sending failed", e);
                LOGGER.error("message [{}], update [{}]", message, update);
            }
        });
    }

    List<SendMessage> doAddOrRemoveActionIfSupported(String action, List<Search> searches) {
        String[] carSearchParts;
        Function<Search, Boolean> searchAction;
        List<SendMessage> actionSuccessMessages = new ArrayList<>();
        List<SendMessage> actionFailedMessages = new ArrayList<>();
        if (action.startsWith(ADD)) {
            carSearchParts = action.substring(ADD.length()).toLowerCase().split(" ");
            searchAction = searches::add;
            actionSuccessMessages.add(new SendMessage().setText("Слежу (за лупой) ..."));
            actionSuccessMessages.add(new SendMessage().setText("https://avatanplus.com/files/resources/mid/5a105f460a1a615fcff42999.png"));
            actionFailedMessages.add(new SendMessage().setText("Данный автомобиль уже отслеживается"));
        } else if (action.startsWith(REMOVE)) {
            carSearchParts = action.substring(REMOVE.length()).toLowerCase().split(" ");
            searchAction = searches::remove;
            actionSuccessMessages.add(new SendMessage().setText("Поиск удален"));
            actionFailedMessages.add(new SendMessage().setText("Данный автомобиль не отслеживается"));
        } else {
            return emptyList();
        }

        if (carSearchParts.length > 2) {
            return singletonList(new SendMessage().setText("Я хочу спать а не парсить марки и модели с пробелами - прямо как в твоих познаниях о теории струн"));
        }

        return searchers.stream().flatMap(searcher -> {
            if (!searcher.supportedManufacturersAndModels().keySet().contains(carSearchParts[0])) {
                return Stream.of(new SendMessage().setText("Не могу найти марку, попросите товарища капитана пусть в закладках поищет " + searcher.getClass().getSimpleName()));
            }
            if (!searcher.supportedManufacturersAndModels().get(carSearchParts[0]).contains(carSearchParts[1])) {
                return Stream.of(new SendMessage().setText("Не могу найти модель, хохлушки кончились"));
            }
            boolean isActionSuccessful = searchAction.apply(Search.builder().manufacturer(carSearchParts[0]).modelName(carSearchParts[1]).build());
            return isActionSuccessful ? actionSuccessMessages.stream() : actionFailedMessages.stream();
        }).collect(toList());
    }

    public void checkCarsAndPostNewIfAvailable() {
        Customer.find.query().setId(1L).fetch("searches").findList().forEach(customer -> {
            searchers.forEach(searcher -> customer.getSearches().stream().map(searcher::find).flatMap(List::stream).forEach(ad -> {
                        if (currentSessionAds.get(searcher).add(ad)) {
                            prepareStraightForwardMessages(ad, customer.getId()).forEach(sendMessage -> {
                                try {
                                    execute(sendMessage);
                                } catch (Exception e) {
                                    LOGGER.error("search and send new cars into chat failed", e);
                                }
                            });
                        }
                    }
            ));
        });
    }

    private List<SendMessage> prepareStraightForwardMessages(Ad ad, Long chatId) {
        List<SendMessage> messages = ad.carPhotos.stream().map(photo -> new SendMessage().setChatId(chatId).setText(photo.toString())).collect(toList());
        messages.add(0, new SendMessage().setChatId(chatId).setText(createMessage(ad)));
        messages.add(new SendMessage().setChatId(chatId).setText("Каеф..."));
        return messages;
    }

    private String createMessage(Ad ad) {
        return "Опа, " + ad.title + "\n" +
                "и пробег всего лишь " + ad.car.mileageInKilometers + " км" + "\n" +
                "Какая цаца... и всего за " + ad.car.costInUsd + "$" + "\n";
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
