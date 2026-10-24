package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class WordleTest {

    private PrintWriter log;
    private WordleDictionary dictionary;
    private WordleGame game;

    @BeforeEach
    void setUp() {
        log = new PrintWriter(System.out, true);

        dictionary = new WordleDictionary(
                List.of(
                        "герой", "гонец", "город", "голос",
                        "горох", "порох", "топор", "сосна",
                        "насос", "касса", "маска", "лампа"
                ),
                log
        );

        game = new WordleGame(dictionary, "герой", log);
    }

    @Test
    void dictionaryNormalizesFiltersAndRemovesDuplicates() {
        WordleDictionary words = new WordleDictionary(
                List.of(
                        " ЁЖИКИ ", "ежики", "ГЕРОЙ",
                        "кот", "abcde", "а-б-в"
                ),
                log
        );

        assertEquals(
                List.of("ежики", "герой"),
                words.getWords()
        );

        assertTrue(words.contains("ЁЖИКИ"));
        assertFalse(words.contains("город"));
        assertEquals(2, words.size());
    }

    @Test
    void dictionaryRejectsEmptyResult() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WordleDictionary(
                        List.of("кот", "abcde"),
                        log
                )
        );
    }

    @Test
    void dictionaryCannotBeChangedThroughGetter() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> dictionary.getWords().clear()
        );
    }

    @Test
    void randomWordBelongsToDictionary() {
        for (int i = 0; i < 100; i++) {
            assertTrue(
                    dictionary.contains(dictionary.getRandomWord())
            );
        }
    }

    @Test
    void loaderReadsUtf8AndReturnsPreparedDictionary()
            throws Exception {

        Path path = Files.createTempFile(
                "wordle-dictionary",
                ".txt"
        );

        try {
            Files.writeString(
                    path,
                    "ГЕРОЙ\nЁЖИКИ\nкот\nabcde\n",
                    StandardCharsets.UTF_8
            );

            WordleDictionary loaded =
                    new WordleDictionaryLoader(log)
                            .load(path.toString());

            assertEquals(
                    List.of("герой", "ежики"),
                    loaded.getWords()
            );
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void loaderReportsMissingFile() throws Exception {
        Path directory = Files.createTempDirectory(
                "wordle-missing"
        );

        try {
            assertThrows(
                    java.io.IOException.class,
                    () -> new WordleDictionaryLoader(log).load(
                            directory.resolve("missing.txt").toString()
                    )
            );
        } finally {
            Files.deleteIfExists(directory);
        }
    }

    @Test
    void loaderRejectsEmptyDictionary() throws Exception {
        Path path = Files.createTempFile(
                "wordle-empty",
                ".txt"
        );

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new WordleDictionaryLoader(log)
                            .load(path.toString())
            );
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void comparisonMatchesExample() {
        assertEquals(
                "+^-^-",
                WordleGame.compareWords("гонец", "герой")
        );
    }

    @Test
    void comparisonHandlesExactMatchAndAbsentLetters() {
        assertEquals(
                "+++++",
                WordleGame.compareWords("герой", "герой")
        );

        assertEquals(
                "-----",
                WordleGame.compareWords("касса", "топор")
        );
    }

    @Test
    void comparisonDoesNotReuseAnswerLetters() {
        assertEquals(
                "--+^+",
                WordleGame.compareWords("касса", "сосна")
        );

        assertEquals(
                "^++-+",
                WordleGame.compareWords("касса", "маска")
        );

        assertEquals(
                "^^+^^",
                WordleGame.compareWords("насос", "сосна")
        );
    }

    @Test
    void invalidInputDoesNotSpendAttempts() {
        for (String input : List.of(
                "", "кот", "abcde", "12345", "герои!"
        )) {
            assertThrows(
                    InvalidWordException.class,
                    () -> game.makeMove(input)
            );
        }

        assertThrows(
                WordNotFoundInDictionary.class,
                () -> game.makeMove("арбуз")
        );

        assertEquals(6, game.getSteps());
        assertTrue(game.getPreviousWords().isEmpty());
    }

    @Test
    void normalizedAnswerWinsAndFinishesGame()
            throws Exception {

        assertEquals("+++++", game.makeMove(" ГЕРОЙ "));

        assertTrue(game.isWon());
        assertTrue(game.isFinished());
        assertEquals(5, game.getSteps());

        assertThrows(
                IllegalStateException.class,
                () -> game.makeMove("герой")
        );

        assertThrows(
                IllegalStateException.class,
                () -> game.getHint()
        );
    }

    @Test
    void sixthWrongMoveFinishesGame() throws Exception {
        for (String word : List.of(
                "город", "голос", "горох",
                "порох", "топор", "лампа"
        )) {
            game.makeMove(word);
        }

        assertEquals(0, game.getSteps());
        assertTrue(game.isFinished());
        assertFalse(game.isWon());
    }

    @Test
    void correctSixthMoveStillWins() throws Exception {
        for (int i = 0; i < 5; i++) {
            game.makeMove("город");
        }

        game.makeMove("герой");

        assertEquals(0, game.getSteps());
        assertTrue(game.isWon());
    }

    @Test
    void allowedWordCanBePlayedEvenIfItIsNotCandidate()
            throws Exception {

        game.makeMove("гонец");

        assertFalse(game.getCandidates().contains("лампа"));

        assertDoesNotThrow(() -> game.makeMove("лампа"));
    }

    @Test
    void filtersKeepExactlyWordsConsistentWithHistory()
            throws Exception {

        for (String answer : dictionary.getWords()) {
            WordleGame checked =
                    new WordleGame(dictionary, answer, log);

            for (String guess : List.of(
                    "касса", "насос", "город"
            )) {
                if (checked.isFinished()) {
                    break;
                }

                checked.makeMove(guess);

                assertTrue(
                        checked.getCandidates().contains(answer)
                );

                for (String candidate : dictionary.getWords()) {
                    boolean expected = true;

                    for (int i = 0;
                         i < checked.getPreviousWords().size();
                         i++) {

                        String result = WordleGame.compareWords(
                                checked.getPreviousWords().get(i),
                                candidate
                        );

                        expected &= result.equals(
                                checked.getPreviousResults().get(i)
                        );
                    }

                    assertEquals(
                            expected,
                            checked.getCandidates().contains(candidate)
                    );
                }
            }
        }
    }

    @Test
    void hintsAreConsistentAndNeverRepeatPlayedWords()
            throws Exception {

        for (String answer : dictionary.getWords()) {
            WordleGame checked =
                    new WordleGame(dictionary, answer, log);

            while (!checked.isFinished()) {
                String hint = checked.getHint();

                assertTrue(
                        checked.getCandidates().contains(hint)
                );

                assertFalse(
                        checked.getPreviousWords().contains(hint)
                );

                checked.makeMove(hint);
            }

            assertTrue(checked.getSteps() >= 0);
        }
    }

    @Test
    void consoleAcceptsEnterAndReportsVictory() {
        WordleDictionary single = new WordleDictionary(
                List.of("герой"),
                log
        );

        WordleGame singleGame =
                new WordleGame(single, "герой", log);

        String output = runConsole(singleGame, "\n");

        assertTrue(output.contains("Подсказка: герой"));
        assertTrue(output.contains("Вы победили!"));
        assertTrue(singleGame.isWon());
    }

    @Test
    void consoleHandlesInvalidInputAndEndOfInput() {
        String output = runConsole(game, "abcde\n");

        assertTrue(
                output.contains(
                        "Введите слово из пяти русских букв."
                )
        );

        assertTrue(output.contains("Игра прервана."));
        assertEquals(6, game.getSteps());
    }

    private String runConsole(
            WordleGame checked,
            String input
    ) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes =
                new ByteArrayOutputStream();

        try (PrintStream console = new PrintStream(
                bytes,
                true,
                StandardCharsets.UTF_8);
             Scanner scanner = new Scanner(input)) {

            System.setOut(console);
            Wordle.play(checked, scanner, log);
        } finally {
            System.setOut(original);
        }

        return bytes.toString(StandardCharsets.UTF_8);
    }
}