package com.ludtek.fcardbot.model;

import com.ludtek.autoanki.model.autoanki.SearchResponseElement;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LastSearch {

    private int currentIdx;
    private final List<SearchResponseElement> elements;

    public LastSearch(List<SearchResponseElement> elements) {
        super();
        this.currentIdx = 0;
        this.elements = elements;
    }

    public List<List<InlineKeyboardButton>> createKeyboard() {
        List<InlineKeyboardButton> keyboardrow = IntStream
                .range(0, elements.size()).mapToObj(i -> new InlineKeyboardButton()
                        .setText(format(i)).setCallbackData(Integer.toString(i)))
                .collect(Collectors.toList());

        return Collections.singletonList(keyboardrow);
    }

    public void setCurrentIdx(int i) {
        this.currentIdx = i;
    }

    public SearchResponseElement getCurrentElement() {
        return elements.get(currentIdx);
    }

    private String format(int i) {
        final String val = Integer.toString(i + 1);
        return i == currentIdx ? String.format("-%s-", val) : val;
    }

}
