package com.ludtek.autoanki.service;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ludtek.autoanki.transformer.CSRFTokenExtractor.extractCSRFTokenFromEditPage;
import static com.ludtek.autoanki.transformer.CSRFTokenExtractor.extractCSRFTokenFromLoginPage;

public class AnkiService implements Closeable {

    public enum CARDTYPE {
        BASIC("1447021827098"), BASIC_REVERSED("1447021827097"), CLOZE("1447021827092");

        private final String id;

        CARDTYPE(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AnkiService.class);

    private static final String SESSION_COOKIE = "ankiweb";

    private static final String ANKI_BASE_URL = "https://ankiweb.net";

    // URLs
    private static final String LOGIN_URL = ANKI_BASE_URL + "/account/login";
    private static final String SAVE_URL = ANKI_BASE_URL + "/edit/save";
    private static final String EDIT_URL = ANKI_BASE_URL + "/edit/";

    private final HttpClientContext context;
    private final CloseableHttpClient client;
    private final String user;
    private final String pass;

    public AnkiService(String user, String pass) {
        this.user = user;
        this.pass = pass;
        this.context = HttpClientContext.create();
        this.client = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.NETSCAPE).build())
                .setConnectionManager(new PoolingHttpClientConnectionManager()).build();
    }

    public void addCard(String deck, String... fields) {
        addCard(deck, CARDTYPE.BASIC_REVERSED, fields);
    }

    public void addCard(String deck, CARDTYPE type, String... fields) {
        ensureConnected();

        HttpGet httpget = new HttpGet(EDIT_URL);

        String token = null;

        try (CloseableHttpResponse response = client.execute(httpget, context)) {
            int responseStatus = response.getStatusLine().getStatusCode();
            LOGGER.info("Edit response: {}", response.getStatusLine());
            if (responseStatus != 200) {
                LOGGER.error("Could not retrieve edit token");
                return;
            }
            token = extractCSRFTokenFromEditPage(response.getEntity().getContent());
            LOGGER.info("Saving using token {}", token);
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            LOGGER.error("Error connecting to ankiweb", e);
            return;
        }

        final String deckString = getDeckString(fields);

        final List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair("deck", deck));
        formparams.add(new BasicNameValuePair("mid", type.getId()));
        formparams.add(new BasicNameValuePair("data", String.format("[%s,\"\"]", deckString)));
        formparams.add(new BasicNameValuePair("csrf_token", token));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
        HttpPost httppost = new HttpPost(SAVE_URL);
        httppost.setEntity(entity);

        try (CloseableHttpResponse response = client.execute(httppost, context)) {
            LOGGER.info("Save response: {}", response.getStatusLine());
        } catch (IOException e) {
            LOGGER.error("Error connecting to ankiweb", e);
        }

    }

    private static String getDeckString(String... values) {
        return String.format("[%s]", Stream.of(values).map(s -> String.format("\"%s\"", s))
                .collect(Collectors.joining(",")));
    }

    private void ensureConnected() {
        if (context.getCookieStore() != null && context.getCookieStore().getCookies().stream()
                .anyMatch(c -> SESSION_COOKIE.equals(c.getName()) && c.getExpiryDate().after(new Date()))) {
            // We're in!
            return;
        }
        login();
    }

    private void login() {
        try {
            HttpGet getlogin = new HttpGet(LOGIN_URL);

            HttpResponse response = this.client.execute(getlogin, context);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new LoginException("Could not fetch login page");
            }

            String token;

            try (InputStream body = response.getEntity().getContent()) {
                token = extractCSRFTokenFromLoginPage(body);
            }

            LOGGER.info("Authenticating using CSRF token {}", token);

            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
            formparams.add(new BasicNameValuePair("username", user));
            formparams.add(new BasicNameValuePair("password", pass));
            formparams.add(new BasicNameValuePair("submitted", "1"));
            formparams.add(new BasicNameValuePair("csrf_token", token));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
            HttpPost httppost = new HttpPost(LOGIN_URL);
            httppost.setEntity(entity);

            response = client.execute(httppost, context);

            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    throw new LoginException("Invalid username/password");
                case 302:
                    LOGGER.info("Login successful!");
                    //https://ankiweb.net/account/checkCookie
                    final String redirectUrl =
                            response.getFirstHeader("Location").getValue();
                    LOGGER.info("Redirecting to: {}", redirectUrl);
                    EntityUtils.consume(response.getEntity());
                    response = client.execute(new HttpGet(redirectUrl), context);
                    LOGGER.info("Received response status: {}",
                            response.getStatusLine());

                    EntityUtils.consume(response.getEntity());
                    break;
                default:
                    throw new LoginException("Ankiweb login form returned with status " + response.getStatusLine());
            }
        } catch (IOException e) {
            throw new LoginException("Error connecting to ankiweb", e);
        }

    }

    @Override
    public void close() throws IOException {
        this.client.close();
    }

    public static class LoginException extends RuntimeException {
        private static final long serialVersionUID = -3473153963070327051L;

        public LoginException(String message, Throwable cause) {
            super(message, cause);
        }

        public LoginException(String message) {
            super(message);
        }
    }
}
