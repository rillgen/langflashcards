package com.ludtek.fcardbot.config;

import com.ludtek.fcardbot.exception.InitializationException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;

public class FCardBotConfig {

    private static final String ANKI_USER = "ANKI_USER";
    private static final String ANKI_PASS = "ANKI_PASS";
    private static final String PONS_SECRET = "PONS_SECRET";
    private static final String TELEGRAM_TOKEN = "TELEGRAM_TOKEN";
    private static final String TELEGRAM_WHITELISTED_USERS = "TELEGRAM_WHITELISTED_USERS";

    private static final String DEFAULT_WHITELISTED_USERS = "rillgen";

    private FCardBotConfig() {
        // Static class
    }

    public static String ankiUser() {
        return getEnviromentValue(ANKI_USER);
    }

    public static String ankiPassword() {
        return getEnviromentValue(ANKI_PASS);
    }

    public static String ponsSecret() {
        return getEnviromentValue(PONS_SECRET);
    }

    public static String telegramToken() {
        return getEnviromentValue(TELEGRAM_TOKEN);
    }

    public static Set<String> whitelistedUsers() {
        return new HashSet<>(asList(getEnviromentValue(TELEGRAM_WHITELISTED_USERS, DEFAULT_WHITELISTED_USERS).split(",")));
    }


    private static String getEnviromentValue(String key) {
        return Optional.ofNullable(System.getenv(key)).orElseThrow(() ->
                new InitializationException(String.format("Unable to retrieve required environment variable: %s", key)));
    }

    private static String getEnviromentValue(String key, String defaultValue) {
        return Optional.ofNullable(System.getenv(key)).orElse(defaultValue);
    }


}
