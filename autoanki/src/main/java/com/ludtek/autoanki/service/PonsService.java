package com.ludtek.autoanki.service;

import com.ludtek.autoanki.model.pons.Response;
import com.ludtek.autoanki.transformer.PonsResponseTransformer;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class PonsService implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PonsService.class);

    private final String secret;
    private final CloseableHttpClient client;
    private final PonsResponseTransformer ponsResponseTransformer;

    private static final String SEARCH_URL = "https://api.pons.com/v1/dictionary?l=deen&q=%s&fm=1";
    private static final String UTF8_ENC = "UTF-8";

    public PonsService(String secret) {
        super();
        this.secret = secret;
        this.client = HttpClients.custom().setConnectionManager(new PoolingHttpClientConnectionManager()).build();
        this.ponsResponseTransformer = new PonsResponseTransformer();
    }

    private static final String X_SECRET_HEADER = "X-Secret";

    public Response search(String term) {

        String url;

        try {
            url = String.format(SEARCH_URL, URLEncoder.encode(term, UTF8_ENC));
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee);
        }

        HttpGet get = new HttpGet(url);
        get.setHeader(X_SECRET_HEADER, secret);

        try (CloseableHttpResponse response = client.execute(get)) {
            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    return ponsResponseTransformer.parse(response.getEntity().getContent());
                case 204:
                    LOGGER.info("Term {} not found", term);
                    break;
                case 404:
                    LOGGER.info("Dictionary not found");
                    break;
                default:
                    LOGGER.error("Pons returned with status {}", response.getStatusLine());
            }
        } catch (Exception e) {
            LOGGER.error("Error connecting to Pons", e);
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
