package com.suredroid.discord.Form;

public class Question {
    private final String question;
    private String description;
    private transient String full;

    public Question(String question, String description) {
        this.question = question;
        this.description = description;
        full = question + "\n*" + description + "*";
    }

    public Question(String question) {
        this.question = question;
        full = question;
    }

    public String getQuestion() {
        return question;
    }

    public String getDescription() {
        return description;
    }

    public String getFull() {
        return full;
    }
}
