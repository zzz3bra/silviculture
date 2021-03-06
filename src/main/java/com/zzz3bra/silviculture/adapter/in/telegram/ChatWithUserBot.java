package com.zzz3bra.silviculture.adapter.in.telegram;

import com.zzz3bra.silviculture.adapter.out.persistence.JpaCustomerPersistenceAdapter;
import com.zzz3bra.silviculture.application.MainService;
import com.zzz3bra.silviculture.application.Searcher;
import com.zzz3bra.silviculture.domain.Ad;
import com.zzz3bra.silviculture.domain.Customer;
import com.zzz3bra.silviculture.domain.Search;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.vavr.API.Try;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class ChatWithUserBot extends TelegramLongPollingBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatWithUserBot.class);

    private static final String ADD = "/add ";
    private static final String REMOVE = "/remove ";
    private static final String CLEAR = "/clear";
    private static final String RESET = "/reset";
    private static final String STATUS = "/status";

    private static final int TELEGRAM_MIN_MEDIAGROUP_COUNT = 1;
    private static final int TELEGRAM_MAX_MEDIAGROUP_COUNT = 10;

    private final JpaCustomerPersistenceAdapter customerPersistenceAdapter;
    private final MainService mainService;

    private final List<Searcher> searchers;

    public volatile boolean isBusy = false;

    public ChatWithUserBot(MainService mainService, Searcher... searchers) {
        super();
        this.mainService = mainService;
        customerPersistenceAdapter = new JpaCustomerPersistenceAdapter();
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

        Customer customer = customerPersistenceAdapter.loadOneCustomersById(chatId).orElseGet(() -> {
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
            mainService.resetViewedAdsOfCustomer(customer);
            toBeSent.add(new SendMessage().setText("Почистил историю уже отправленных машин, тоби пизда, готовься к спаму"));
            toBeSent.add(new SendMessage().setText("https://d2lnr5mha7bycj.cloudfront.net/product-image/file/large_c6ca8b18-ec5b-4e8b-a7c6-35dc403bf214.JPG"));
        }

        if (message.getText().equals(CLEAR)) {
            mainService.removeAllCarSearchesForCustomer(customer);
            toBeSent.add(new SendMessage().setText("Запрашиваемые модели очищены, буду теперь молчать в трубочку..."));
        }

        if (message.getText().equals(STATUS)) {
            String cars = IntStream.range(0, customer.getSearches().size()).mapToObj(index -> index + ") " + customer.getSearches().get(index).getAsText()).collect(joining("\n"));
            toBeSent.add(new SendMessage().setText("Отслеживаемые автомобили: \n" + cars));
        }

        toBeSent.addAll(doAddOrRemoveActionIfSupported(message.getText(), customer));
        toBeSent.forEach(sendMessage -> Try(() -> execute(sendMessage.setChatId(message.getChatId())))
                .recover(TelegramApiException.class, ex -> {
                    errorOccurred.set(true);
                    LOGGER.error("message sending failed", ex);
                    LOGGER.error("message [{}], update [{}]", message, update);
                    return null;
                }).getOrNull());
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
                if (!searcher.supportedManufacturersAndModels().containsKey(carSearchParts[0])) {
                    return Stream.of(new SendMessage().setText("Не могу найти марку, попросите товарища капитана пусть в закладках поищет " + searcher.getClass().getSimpleName()));
                }
                if (!searcher.supportedManufacturersAndModels().get(carSearchParts[0]).contains(carSearchParts[1])) {
                    return Stream.of(new SendMessage().setText("Не могу найти модель, хохлушки кончились" + "\r\n" + "Доступные хохлушки: " + searcher.supportedManufacturersAndModels().get(carSearchParts[0])));
                }
                Search.SearchBuilder search = Search.builder().manufacturer(carSearchParts[0]).modelName(carSearchParts[1]);
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
            BooleanSupplier searchAction = () -> customer.getSearches().remove(customer.getSearches().get(Integer.parseInt(carSearchParts[0])));
            actionSuccessMessages.add(new SendMessage().setText("Поиск удален"));
            actionFailedMessages.add(new SendMessage().setText("Данный автомобиль не отслеживается"));

            return searchAction.getAsBoolean() ? actionSuccessMessages : actionFailedMessages;
        } else {
            return emptyList();
        }
    }

    public void checkCarsAndPostNewIfAvailable() {
        customerPersistenceAdapter.loadAllCustomers().forEach(this::checkUpdatesForCustomer);
    }

    private void checkUpdatesForCustomer(Customer customer) {
        searchers.forEach(searcher -> checkUpdatesViaSearcher(customer, searcher));
        customer.setViewedAdsIdsBySearcher(new HashMap<>(customer.getViewedAdsIdsBySearcher()));
        customer.update();
    }

    private void checkUpdatesViaSearcher(Customer customer, Searcher searcher) {
        Set<String> failedAds = new HashSet<>();
        customer.getViewedAdsIdsBySearcher().computeIfAbsent(searcher.getTechnicalName(), l -> new HashSet<>());
        customer.getSearches().stream().map(search -> {
            final List<Ad> ads = searcher.find(search);
            LOGGER.trace("using search:{} found ads:{} for customer:{} {}", search, ads, customer.getId(), customer.getName());
            return ads;
        }).flatMap(List::stream).forEach(ad -> {
                    if (customer.getViewedAdsIdsBySearcher().get(searcher.getTechnicalName()).add(ad.id)) {
                        LOGGER.debug("new ad with ID {} found for customer:{} {}", ad.id, customer.getId(), customer.getName());
                        prepareMessages(ad, customer.getId()).forEach(sendMessage -> {
                            try {
                                if (sendMessage instanceof SendMediaGroup) {
                                    final SendMediaGroup sendMediaGroup = (SendMediaGroup) sendMessage;
                                    try {
                                        execute(sendMediaGroup);
                                    } catch (TelegramApiRequestException e) {
                                        if ("Bad Request: group send failed".equals(e.getApiResponse())) { // god bless coders who put descriptive description into the error response with code 4xx which means that problem is at the sender's side and something must be changed
                                            LOGGER.error("error sending ad [" + ad.id + "] to customer[" + customer.getId() + "]", e.getApiResponse());
                                            //try sending photos one-by-one
                                            for (InputMedia m : sendMediaGroup.getMedia()) {
                                                execute(new SendMessage().setChatId(sendMediaGroup.getChatId()).setText(m.getMedia()));
                                            }
                                        } else {
                                            //do nothing
                                        }
                                    }
                                } else {
                                    execute((BotApiMethod<Message>) sendMessage);
                                }
                            } catch (Exception e) {
                                failedAds.add(ad.id);
                                LOGGER.error("search and send new cars into chat failed", e);
                            }
                        });
                    }
                }
        );
        customer.getViewedAdsIdsBySearcher().get(searcher.getTechnicalName()).removeAll(failedAds);
    }

    private List<PartialBotApiMethod> prepareMessages(Ad ad, Long chatId) {
        List<PartialBotApiMethod> messages = new ArrayList<>();
        messages.add(0, new SendMessage().setChatId(chatId).setText(createMessage(ad)));
        if (ad.carPhotos.size() > 1) {
            final AtomicInteger counter = new AtomicInteger();
            final List<InputMedia> mediaList = ad.carPhotos.stream().map(photo -> new InputMediaPhoto(photo.toString(), "")).collect(toList());
            Collection<List<InputMedia>> photosChunks = mediaList.stream()
                    .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / TELEGRAM_MAX_MEDIAGROUP_COUNT))
                    .values();
            photosChunks.forEach(photosChunk -> {
                if (photosChunk.size() == TELEGRAM_MIN_MEDIAGROUP_COUNT) {
                    messages.add(new SendMessage().setChatId(chatId).setText(photosChunk.get(0).getMedia()));
                } else {
                    messages.add(new SendMediaGroup(chatId, photosChunk));
                }
            });
        }
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
