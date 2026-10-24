package ru.yandex.practicum;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Wordle {

    public static void main(String[] args) {
        String dictionaryPath = args.length == 0
                ? "words_ru.txt"
                : args[0];

        try (PrintWriter log = new PrintWriter(
                new FileOutputStream("wordle.log", true),
                true,
                StandardCharsets.UTF_8)) {

            try {
                WordleDictionaryLoader loader =
                        new WordleDictionaryLoader(log);

                WordleDictionary dictionary =
                        loader.load(dictionaryPath);

                WordleGame game =
                        new WordleGame(dictionary, log);

                try (Scanner scanner = new Scanner(
                        System.in,
                        StandardCharsets.UTF_8)) {

                    play(game, scanner, log);
                }
            } catch (Exception exception) {
                exception.printStackTrace(log);

                System.out.println(
                        "Не удалось продолжить игру. "
                                + "Подробности в wordle.log."
                );
            }

            if (log.checkError()) {
                System.out.println(
                        "Не удалось записать данные в wordle.log."
                );
            }
        } catch (IOException exception) {
            System.out.println(
                    "Не удалось открыть wordle.log. "
                            + "Проверьте права на запись."
            );
        }
    }

    static void play(
            WordleGame game,
            Scanner scanner,
            PrintWriter log
    ) {
        System.out.println(
                "Wordle: угадайте слово из пяти букв за шесть попыток."
        );

        System.out.println(
                "+ — на месте, ^ — на другом месте, "
                        + "- — нет доступного вхождения."
        );

        System.out.println(
                "Enter — компьютер выберет слово "
                        + "и сделает ход за вас."
        );

        while (!game.isFinished()) {
            System.out.println(
                    "Осталось попыток: " + game.getSteps()
            );

            System.out.print("Введите слово: ");

            if (!scanner.hasNextLine()) {
                log.println("Ввод завершён пользователем");

                System.out.println(
                        "Игра прервана. Загаданное слово: "
                                + game.getAnswer()
                );

                return;
            }

            String word = WordleDictionary.normalize(
                    scanner.nextLine()
            );

            if (word.isEmpty()) {
                word = game.getHint();
                System.out.println("Подсказка: " + word);
            }

            try {
                String result = game.makeMove(word);
                System.out.println(result);
            } catch (GameException exception) {
                System.out.println(exception.getMessage());

                log.println(
                        "Отклонён ввод: " + exception.getMessage()
                );
            }
        }

        if (game.isWon()) {
            System.out.println("Вы победили!");
        } else {
            System.out.println("Попытки закончились.");
        }

        System.out.println(
                "Загаданное слово: " + game.getAnswer()
        );

        log.println(
                "Игра завершена. Победа: " + game.isWon()
        );
    }
}