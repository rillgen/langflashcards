package com.ludtek.fcardbot;

import com.ludtek.autoanki.service.AnkiService;
import com.ludtek.autoanki.service.AutoAnkiService;
import com.ludtek.autoanki.service.PonsService;
import com.ludtek.fcardbot.bot.FCardBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import static com.ludtek.fcardbot.config.FCardBotConfig.*;

public class FCardBotApp {

    static {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FCardBotApp.class);

    public static void main(String[] args) {
        LOGGER.info("Starting FCard Bot App...");

        AutoAnkiService autoAnki = new AutoAnkiService(new AnkiService(ankiUser(), ankiPassword()),
                new PonsService(ponsSecret()));

        ApiContextInitializer.init();

        TelegramBotsApi botsApi = new TelegramBotsApi();

        try {
            botsApi.registerBot(new FCardBot(autoAnki, telegramToken(), whitelistedUsers()));
        } catch (TelegramApiException e) {
            LOGGER.error("Telegram API error", e);
        }

    }
}
