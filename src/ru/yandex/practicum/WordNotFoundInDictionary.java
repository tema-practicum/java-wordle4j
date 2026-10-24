package ru.yandex.practicum;

public class WordNotFoundInDictionary extends GameException {

    public WordNotFoundInDictionary(String word) {
        super("Слова «" + word + "» нет в словаре.");
    }
}