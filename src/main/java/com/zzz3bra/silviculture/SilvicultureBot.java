package com.zzz3bra.silviculture;

import com.zzz3bra.silviculture.data.Ad;
import com.zzz3bra.silviculture.data.Customer;
import com.zzz3bra.silviculture.data.gathering.Search;
import com.zzz3bra.silviculture.data.gathering.Search.SearchBuilder;
import com.zzz3bra.silviculture.data.gathering.Searcher;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
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

    private final List<Searcher> searchers;

    public volatile boolean isBusy = false;

    SilvicultureBot(Searcher... searchers) {
        super();
        this.searchers = Arrays.asList(searchers);
    }

    @Override
    public void onUpdateReceived(Update update) {
        isBusy = true;
        Message message;
        if (update.hasChannelPost()) {
            message = update.getChannelPost();
        } else if (update.hasMessage()) {
            message = update.getMessage();
        } else {
            isBusy = false;
            return;
        }

        AtomicBoolean errorOccurred = new AtomicBoolean(false);
        final Long chatId = message.getChatId();
        Customer customer = Optional.ofNullable(Customer.find.byId(chatId)).orElseGet(() -> {
            Customer c = new Customer();
            c.setId(chatId);
            c.setName(message.getContact().getVCard());
            c.setSearches(new ArrayList<>());
            c.setViewedAdsIdsBySearcher(new HashMap<>());
            c.save();
            return c;
        });
        if (message.getChat().isUserChat()) {
            customer.setName(message.getFrom().getUserName());
        }

        List<SendMessage> toBeSent = new ArrayList<>();

        if (message.getText().equals(RESET)) {
            customer.getViewedAdsIdsBySearcher().clear();
            toBeSent.add(new SendMessage().setText("Почистил историю уже отправленных машин, тоби пизда, готовься к спаму"));
            toBeSent.add(new SendMessage().setText("https://d2lnr5mha7bycj.cloudfront.net/product-image/file/large_c6ca8b18-ec5b-4e8b-a7c6-35dc403bf214.JPG"));
        }

        if (message.getText().equals(CLEAR)) {
            customer.getSearches().clear();
            toBeSent.add(new SendMessage().setText("Запрашиваемые модели очищены, буду теперь молчать в трубочку..."));
        }

        if (message.getText().equals(STATUS)) {
            String cars = IntStream.range(0, customer.getSearches().size()).mapToObj(index -> index + ") " + customer.getSearches().get(index).getAsText()).collect(joining("\n"));
            toBeSent.add(new SendMessage().setText("Отслеживаемые автомобили: \n" + cars));
        }

        toBeSent.addAll(doAddOrRemoveActionIfSupported(message.getText(), customer));
        toBeSent.forEach(sendMessage -> {
            try {
                execute(sendMessage.setChatId(message.getChatId()));
            } catch (TelegramApiException e) {
                errorOccurred.set(true);
                LOGGER.error("message sending failed", e);
                LOGGER.error("message [{}], update [{}]", message, update);
            }
        });
        if (!errorOccurred.get()) {
            customer.setViewedAdsIdsBySearcher(new HashMap<>(customer.getViewedAdsIdsBySearcher()));
            customer.update();
        }
        isBusy = false;
    }

    List<SendMessage> doAddOrRemoveActionIfSupported(String action, Customer customer) {
        String[] carSearchParts;
        List<SendMessage> actionSuccessMessages = new ArrayList<>();
        List<SendMessage> actionFailedMessages = new ArrayList<>();
        if (action.startsWith(ADD)) {
            final String searchAttributes = action.substring(ADD.length()).toLowerCase();
            if (searchAttributes.contains("\"")) {
                carSearchParts = Stream.of(searchAttributes.split("\"")).filter(StringUtils::isNotBlank).toArray(String[]::new);
            } else {
                carSearchParts = searchAttributes.split(" ");
            }
            if (carSearchParts.length != 4 && carSearchParts.length != 2) {
                return singletonList(new SendMessage().setText("Шоб искать марки и машины с пробелами пиши всё в двойных кавычках, типа \"great wall\" \"voleex c30\" \"2012\" \"2022\""));
            }
            Predicate<Search> searchAction = search -> {
                boolean isNewSearch = !customer.getSearches().contains(search);
                if (isNewSearch) {
                    customer.getSearches().add(search);
                    searchers.forEach(searcher -> {
                        customer.getViewedAdsIdsBySearcher().computeIfAbsent(searcher.getTechnicalName(), l -> new HashSet<>());
                        List<String> initialAdsIds = searcher.find(search).stream().map(ad -> ad.id).collect(toList());
                        customer.getViewedAdsIdsBySearcher().get(searcher.getTechnicalName()).addAll(initialAdsIds);
                    });
                }
                return isNewSearch;
            };
            actionSuccessMessages.add(new SendMessage().setText("Слежу (за лупой) ..."));
            actionSuccessMessages.add(new SendMessage().setText("https://avatanplus.com/files/resources/mid/5a105f460a1a615fcff42999.png"));
            actionFailedMessages.add(new SendMessage().setText("Данный автомобиль уже отслеживается"));

            return searchers.stream().flatMap(searcher -> {
                if (!searcher.supportedManufacturersAndModels().keySet().contains(carSearchParts[0])) {
                    return Stream.of(new SendMessage().setText("Не могу найти марку, попросите товарища капитана пусть в закладках поищет " + searcher.getClass().getSimpleName()));
                }
                if (!searcher.supportedManufacturersAndModels().get(carSearchParts[0]).contains(carSearchParts[1])) {
                    return Stream.of(new SendMessage().setText("Не могу найти модель, хохлушки кончились" + "\r\n" + "Доступные хохлушки: " + searcher.supportedManufacturersAndModels().get(carSearchParts[0])));
                }
                SearchBuilder search = Search.builder().manufacturer(carSearchParts[0]).modelName(carSearchParts[1]);
                if (carSearchParts.length == 4) {
                    search.minYear(Integer.valueOf(carSearchParts[2]));
                    search.maxYear(Integer.valueOf(carSearchParts[3]));
                }
                boolean isActionSuccessful = searchAction.test(search.build());
                return isActionSuccessful ? actionSuccessMessages.stream() : actionFailedMessages.stream();
            }).collect(toList());
        } else if (action.startsWith(REMOVE)) {
            carSearchParts = action.substring(REMOVE.length()).toLowerCase().split(" ");
            if (carSearchParts.length > 1) {
                return singletonList(new SendMessage().setText("Укажите номер поиска после команды удаления"));
            }
            Supplier<Boolean> searchAction = () -> customer.getSearches().remove(customer.getSearches().get(Integer.parseInt(carSearchParts[0])));
            actionSuccessMessages.add(new SendMessage().setText("Поиск удален"));
            actionFailedMessages.add(new SendMessage().setText("Данный автомобиль не отслеживается"));

            return searchAction.get() ? actionSuccessMessages : actionFailedMessages;
        } else {
            return emptyList();
        }
    }

    public void checkCarsAndPostNewIfAvailable() {
        Customer.find.all().forEach(customer -> {
            AtomicBoolean errorOccurred = new AtomicBoolean(false);
            searchers.forEach(searcher -> {
                customer.getViewedAdsIdsBySearcher().computeIfAbsent(searcher.getTechnicalName(), l -> new HashSet<>());
                customer.getSearches().stream().map(search -> {
                    final List<Ad> ads = searcher.find(search);
                    LOGGER.debug("using search:{} found ads:{} for customer:{}", search, ads, customer);
                    return ads;
                }).flatMap(List::stream).forEach(ad -> {
                            if (customer.getViewedAdsIdsBySearcher().get(searcher.getTechnicalName()).add(ad.id)) {
                                LOGGER.debug("new ad with ID {} found for customer:{}", ad.id, customer);
                                prepareStraightForwardMessages(ad, customer.getId()).forEach(sendMessage -> {
                                    try {
                                        execute(sendMessage);
                                    } catch (Exception e) {
                                        errorOccurred.set(true);
                                        LOGGER.error("search and send new cars into chat failed", e);
                                    }
                                });
                            }
                        }
                );
            });
            if (!errorOccurred.get()) {
                customer.setViewedAdsIdsBySearcher(new HashMap<>(customer.getViewedAdsIdsBySearcher()));
                customer.update();
            }
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
                "Какая цаца... и всего за " + ad.car.costInUsd + "$" + "\n" +
                "Подробности тута : " + ad.link.toString() + "\n";
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
