package com.ludtek.autoanki.service;

import com.ludtek.autoanki.model.autoanki.SearchResponseElement;
import com.ludtek.autoanki.model.pons.Response;
import com.ludtek.autoanki.model.pons.Translation;
import org.apache.commons.collections.CollectionUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ludtek.autoanki.model.autoanki.ElementType.*;

public class AutoAnkiService implements Closeable {

    private final AnkiService anki;
    private final PonsService pons;
    private final Map<UUID, SearchResponseElement> responseStore = Collections
            .synchronizedMap(new LinkedHashMap<UUID, SearchResponseElement>() {

                private static final long serialVersionUID = 314751861729826482L;
                private final int MAX_SIZE = 1000;

                protected boolean removeEldestEntry(Map.Entry<UUID, SearchResponseElement> eldest) {
                    return this.size() > MAX_SIZE;
                }

                ;
            });

    public AutoAnkiService(AnkiService ankiService, PonsService ponsService) {
        super();
        this.anki = ankiService;
        this.pons = ponsService;
    }

    public boolean contains(UUID uuid) {
        return responseStore.containsKey(uuid);
    }

    public boolean contains(UUID uuid, int seq) {
        return contains(uuid) && responseStore.get(uuid).getTranslations().containsKey(seq);
    }

    public Optional<Translation> get(UUID uuid, int seq) {
        if (!contains(uuid, seq)) {
            return Optional.empty();
        }
        return Optional.of(responseStore.get(uuid).getTranslations().get(seq));
    }

    public Optional<List<SearchResponseElement>> search(String term) {
        final Response response = pons.search(term);
        return response == null ? Optional.empty() : Optional.of(store(toApiResponse(response)));
    }

    public Optional<Translation> createCard(UUID uuid, int seq) {
        if (!contains(uuid, seq)) {
            return Optional.empty();
        }

        final SearchResponseElement element = responseStore.get(uuid);
        final Translation translation = element.getTranslations().get(seq);

        final String front = TRANSLATION == element.getType() ? translation.getSource()
                : String.format("<div>%s</div><div>%s</div>", element.getDesc(), translation.getSource());
        final String back = TRANSLATION == element.getType() ? translation.getTarget()
                : String.format("<div>%s</div>", translation.getTarget());

        switch (element.getType()) {
            case VERB:
                anki.addCard("Deutsch::Verben", front, back);
                break;
            case NOUN:
                anki.addCard("Deutsch::Nomen", front, back);
                break;
            default:
                anki.addCard("Deutsch", front, back);
        }

        return Optional.of(translation);
    }

    private List<SearchResponseElement> store(List<SearchResponseElement> elements) {
        elements.forEach(element -> {
            responseStore.put(element.getId(), element);
        });
        return elements;
    }

    private List<SearchResponseElement> toApiResponse(Response response) {
        if (response == null) {
            return null;
        }

        Stream<SearchResponseElement> apiResponse = response.getRoms().stream().map(rom -> {
            final SearchResponseElement elem = new SearchResponseElement();
            elem.setId(UUID.randomUUID());
            elem.setDesc(rom.getDescription());

            final String wordclass = rom.getWordclass();

            elem.setType(OTHER);

            if (wordclass != null) {
                if (wordclass.contains("verb")) {
                    elem.setType(VERB);
                } else if (wordclass.contains("noun")) {
                    elem.setType(NOUN);
                }
            }

            final List<Translation> translations = rom.getArabs().stream()
                    .flatMap(arab -> arab.getTranslations().stream()).collect(Collectors.toList());
            elem.setTranslations(numerateTranslations(translations));
            return elem;
        });

        if (CollectionUtils.isNotEmpty(response.getTranslations())) {
            final SearchResponseElement elem = new SearchResponseElement();
            elem.setId(UUID.randomUUID());
            elem.setType(TRANSLATION);
            elem.setDesc(TRANSLATION.toString());
            elem.setTranslations(numerateTranslations(response.getTranslations()));

            apiResponse = Stream.concat(apiResponse, Stream.of(elem));
        }

        return apiResponse.collect(Collectors.toList());
    }

    private static Map<Integer, Translation> numerateTranslations(List<Translation> translations) {
        final AtomicInteger counter = new AtomicInteger(0);
        return translations.stream().collect(Collectors.toMap(t -> counter.incrementAndGet(), Function.identity()));
    }

    @Override
    public void close() throws IOException {
        anki.close();
        pons.close();
    }

}
