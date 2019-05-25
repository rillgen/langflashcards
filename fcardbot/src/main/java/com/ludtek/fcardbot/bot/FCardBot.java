package com.ludtek.fcardbot.bot;

import com.ludtek.autoanki.model.autoanki.SearchResponseElement;
import com.ludtek.autoanki.service.AutoAnkiService;
import com.ludtek.fcardbot.model.LastSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FCardBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(FCardBot.class);
    private static final String CR = System.getProperty("line.separator");

    private final AutoAnkiService autoAnki;
    private final String botToken;
    private final Set<String> whitelistedUsers;

    private final ConcurrentHashMap<String, LastSearch> currentSearches = new ConcurrentHashMap<>();

    public FCardBot(AutoAnkiService autoAnki, String botToken, Set<String> whitelistedUsers) {
        this.autoAnki = autoAnki;
        this.botToken = botToken;
        this.whitelistedUsers = whitelistedUsers;
    }

    public String getBotUsername() {
        return "LangCardBot";
    }

    public void onUpdateReceived(Update update) {

        if (isCallback(update)) {
            handleCallback(update);
        } else if (isMessage(update)) {
            handleMessage(update);
        } else {
            LOGGER.info("textless message: {}", update);
        }
    }

    private static final Pattern ADD_COMMAND = Pattern.compile("\\/add_(\\d+)");

    private void handleMessage(Update update) {
        final String username = update.getMessage().getFrom().getUserName();
        final String text = update.getMessage().getText();

        if (!whitelistedUsers.contains(username)) {
            LOGGER.warn("Received chat from non whitelisted user {}. Ignoring.", username);
            LOGGER.debug("Message content: {}", update);
            return;
        }

        SendMessage message = new SendMessage().setChatId(update.getMessage().getChatId()).enableMarkdown(true);

        if (text.startsWith("/")) {
            Matcher matcher = ADD_COMMAND.matcher(text);
            if (matcher.matches()) {
                final int selection = Integer.parseInt(text.split("_")[1]);
                LOGGER.info("Creating card with index {}", selection);
                if (currentSearches.containsKey(username)) {
                    final SearchResponseElement currentElement = currentSearches.get(username).getCurrentElement();
                    autoAnki.createCard(currentElement.getId(), selection);
                    message.setText("Card added successfully");
                } else {
                    message.setText("Sorry I couldn't find this translation. Please search again");
                }
                try {
                    sendMessage(message); // Call method to send the message
                } catch (TelegramApiException e) {
                    LOGGER.error("Error sending message edit", e);
                }
                return;
            }
            LOGGER.warn("Could not interpret command {}", text);

        } else {
            Optional<List<SearchResponseElement>> search = autoAnki.search(text);

            if (!search.isPresent()) {
                LOGGER.info("Term \"{}\" not found");
                message.setText(String.format("Could not find results for: %s", text));
            } else {
                LOGGER.info("Found: \"{}\", sending response...", text);
                final LastSearch currentSearch = new LastSearch(search.get());
                currentSearches.put(username, currentSearch);
                message.setText(formatElement(currentSearch.getCurrentElement()));
                message.setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(currentSearch.createKeyboard()));
            }

            try {
                sendMessage(message); // Call method to send the message
            } catch (TelegramApiException e) {
                LOGGER.error("Error sending message edit", e);
            }
        }

    }

    private void handleCallback(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();

        String user = callbackQuery.getFrom().getUserName();

        if (currentSearches.containsKey(user)) {
            final LastSearch lastSearch = currentSearches.get(user);
            final int newIdx = Integer.parseInt(callbackQuery.getData());
            LOGGER.info("Switching to index {}", newIdx);

            final Message original = callbackQuery.getMessage();

            lastSearch.setCurrentIdx(newIdx);

            EditMessageText editMessageText = new EditMessageText().enableMarkdown(true).setChatId(original.getChatId())
                    .setMessageId(original.getMessageId());

            editMessageText.setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(lastSearch.createKeyboard()));
            editMessageText.setText(formatElement(lastSearch.getCurrentElement()));

            try {
                editMessageText(editMessageText); // Call method to send the
                // message
            } catch (TelegramApiException e) {
                LOGGER.error("Error sending message edit", e);
            }
        }

    }

    private static boolean isMessage(Update update) {
        return update.hasMessage() && update.getMessage().hasText();
    }

    private static boolean isCallback(Update update) {
        return update.hasCallbackQuery();
    }

    private static String formatElement(SearchResponseElement sre) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("*%s*", sanitize(sre.getDesc())));
        sb.append(CR);
        sre.getTranslations().forEach((key, val) -> {
            StringBuilder transb = new StringBuilder();
            transb.append(String.format("%2s. %s: _%s_ /add\\_%s", key, sanitize(val.getSource()), sanitize(val.getTarget()), key));
            transb.append(CR);
            if (sb.length() + transb.length() <= 4090) {
                sb.append(transb.toString());
            }
        });
        return sb.toString();
    }

    private static String sanitize(String text) {
        return text.replaceAll("[*_`]", "");
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

}