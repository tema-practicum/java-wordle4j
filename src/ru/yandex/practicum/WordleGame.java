package ru.yandex.practicum;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WordleGame {

    private static final int MAX_STEPS = 6;
    private static final int WORD_LENGTH = 5;

    private final String answer;
    private final WordleDictionary dictionary;
    private final PrintWriter log;

    private final List<String> previousWords = new ArrayList<>();
    private final List<String> previousResults = new ArrayList<>();

    private List<String> candidates;
    private int steps = MAX_STEPS;
    private boolean won;

    public WordleGame(WordleDictionary dictionary, PrintWriter log) {
        this(dictionary, dictionary.getRandomWord(), log);
    }

    public WordleGame(
            WordleDictionary dictionary,
            String answer,
            PrintWriter log
    ) {
        this.dictionary = dictionary;
        this.log = log;
        this.answer = WordleDictionary.normalize(answer);

        if (!dictionary.contains(this.answer)) {
            throw new IllegalArgumentException(
                    "Ответ отсутствует в словаре"
            );
        }

        candidates = dictionary.getWords();

        log.println("Начало игры. Попыток: " + steps);
    }

    public String makeMove(String input)
            throws InvalidWordException, WordNotFoundInDictionary {

        checkActive();

        String word = WordleDictionary.normalize(input);

        if (!WordleDictionary.isValidWord(word)) {
            throw new InvalidWordException();
        }

        if (!dictionary.contains(word)) {
            throw new WordNotFoundInDictionary(word);
        }

        String result = compareWords(word, answer);

        previousWords.add(word);
        previousResults.add(result);

        steps--;
        won = word.equals(answer);

        filterCandidates(word, result);

        log.println(
                "Ход: " + word
                        + ", результат: " + result
                        + ", осталось попыток: " + steps
                        + ", кандидатов: " + candidates.size()
        );

        return result;
    }

    public String getHint() {
        checkActive();

        List<String> available = new ArrayList<>();

        for (String candidate : candidates) {
            if (!previousWords.contains(candidate)) {
                available.add(candidate);
            }
        }

        if (available.isEmpty()) {
            throw new IllegalStateException(
                    "Нет слов для подсказки"
            );
        }

        int index = ThreadLocalRandom.current()
                .nextInt(available.size());

        return available.get(index);
    }

    private void filterCandidates(String word, String result) {
        List<String> remaining = new ArrayList<>();

        for (String candidate : candidates) {
            if (compareWords(word, candidate).equals(result)) {
                remaining.add(candidate);
            }
        }

        if (!remaining.contains(answer)) {
            throw new IllegalStateException(
                    "Из кандидатов потерян правильный ответ"
            );
        }

        candidates = remaining;
    }

    public static String compareWords(String word, String answer) {
        if (word.length() != WORD_LENGTH
                || answer.length() != WORD_LENGTH) {

            throw new IllegalArgumentException(
                    "Длина слов должна быть равна пяти"
            );
        }

        char[] marks = new char[WORD_LENGTH];
        boolean[] used = new boolean[WORD_LENGTH];

        for (int i = 0; i < WORD_LENGTH; i++) {
            marks[i] = '-';

            if (word.charAt(i) == answer.charAt(i)) {
                marks[i] = '+';
                used[i] = true;
            }
        }

        for (int i = 0; i < WORD_LENGTH; i++) {
            if (marks[i] == '+') {
                continue;
            }

            for (int j = 0; j < WORD_LENGTH; j++) {
                if (!used[j]
                        && word.charAt(i) == answer.charAt(j)) {

                    marks[i] = '^';
                    used[j] = true;
                    break;
                }
            }
        }

        StringBuilder result = new StringBuilder(WORD_LENGTH);

        for (char mark : marks) {
            result.append(mark);
        }

        return result.toString();
    }

    private void checkActive() {
        if (isFinished()) {
            throw new IllegalStateException(
                    "Игра уже завершена"
            );
        }
    }

    public int getSteps() {
        return steps;
    }

    public boolean isWon() {
        return won;
    }

    public boolean isFinished() {
        return won || steps == 0;
    }

    public String getAnswer() {
        return answer;
    }

    public List<String> getPreviousWords() {
        return List.copyOf(previousWords);
    }

    public List<String> getPreviousResults() {
        return List.copyOf(previousResults);
    }

    public List<String> getCandidates() {
        return List.copyOf(candidates);
    }
}