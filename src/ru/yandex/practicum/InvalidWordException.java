package ru.yandex.practicum;

public class InvalidWordException extends GameException {

    public InvalidWordException() {
        super("Введите слово из пяти русских букв.");
    }
}