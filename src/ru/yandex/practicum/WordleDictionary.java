package ru.yandex.practicum;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class WordleDictionary {

    private final List<String> words;
    private final Set<String> wordSet;
    private final PrintWriter log;

    public WordleDictionary(List<String> sourceWords, PrintWriter log) {
        this.log = log;
        this.wordSet = new LinkedHashSet<>();

        for (String sourceWord : sourceWords) {
            String word = normalize(sourceWord);

            if (isValidWord(word)) {
                wordSet.add(word);
            }
        }

        if (wordSet.isEmpty()) {
            throw new IllegalArgumentException(
                    "В словаре нет подходящих слов"
            );
        }

        this.words = new ArrayList<>(wordSet);
        log.println("Загружено слов: " + words.size());
    }

    public static String normalize(String word) {
        return word.strip()
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е');
    }

    public static boolean isValidWord(String word) {
        return word.matches("[а-я]{5}");
    }

    public boolean contains(String word) {
        return wordSet.contains(normalize(word));
    }

    public String getRandomWord() {
        int index = ThreadLocalRandom.current().nextInt(words.size());
        return words.get(index);
    }

    public int size() {
        return words.size();
    }

    public List<String> getWords() {
        return List.copyOf(words);
    }
}