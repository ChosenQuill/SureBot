package com.suredroid.discord.Form;

import com.suredroid.discord.DUtils;
import com.suredroid.discord.DiscordBot;
import com.suredroid.discord.Hook;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class Form {

    private Question[] questions;

    public Form(Question[]... qlist) {
        questions = Stream.of(qlist).flatMap(Stream::of).toArray(Question[]::new);
    }

    public CompletableFuture<Results> fill(User user) {
        return new Listener(user).getResults();
    }

    class Listener extends Hook {
        int level = 0;
        String[] answers = new String[questions.length];
        StringBuilder response = new StringBuilder();
        boolean warning = false;
        CompletableFuture<Results> results = new CompletableFuture<>();
        long id;
        String name;

        Listener(User user) {
            super(user);
        }

        private void send(long userId, String message) {
            DUtils.getApi().getUserById(userId).whenComplete((user, throwable) -> {
                if (user != null) {
                    user.sendMessage(message);
                } else {
                    DiscordBot.report(DUtils.createMessage("Can't send message to user.", "EmbedMessage:\n" + message));
                }
            });
        }

        @Override
        protected void onTimeOut() {
            if(!warning){
                send(id, "Hey, it seems like you have not submitted a response for 15 minutes. If you need more time, type \"extend\" if you need more time. Otherwise, I will have to leave you in 5 minutes. \uD83D\uDE1F");
                warning = true;
                setTimer(5);
            } else {
                send(id, "\uD83D\uDE22 Never got a message back...");
                close(false);
            }
        }

        @Override
        public void onMessage(MessageCreateEvent e) {
            if (!e.isPrivateMessage() || !e.getMessageAuthor().isUser())
                return;

            warning = false;

            String message = e.getMessageContent();
            for (MessageAttachment ma : e.getMessageAttachments()) {
                message += "\n" + ma.getProxyUrl();
            }

            switch (message.toLowerCase()) {
                case "next":
                    if (response.toString().trim().isEmpty()) {
                        e.getChannel().sendMessage("This response is empty. All questions on this form are required questions.");
                        break;
                    }
                    answers[level] = response.toString().trim();
                    response.setLength(0);
                    level++;
                    if (level >= questions.length) {
                        close(true);
                        e.getChannel().sendMessage("Thank you for completing the form. Your form has been submitted.");
                        break;
                    }
                    e.getChannel().sendMessage("**" + (level + 1) + ".** " + questions[level].getFull());
                    break;
                case "extend":
                    e.getChannel().sendMessage("Alright! Good to know you are still alive.");
                    break;
                default:
                    response.append(message).append("\n");
                    break;
            }
        }

        public CompletableFuture<Results> getResults(){
            return results;
        }

        @Override
        protected void onInit(User user) {
            user.sendMessage("Respond to the question by sending one or more lines of response. When you are done with your response, type *next* to continue to the next question.\nIf you need to stop the interview at any time, type *stop*. You cannot change your response(message) after you send it.");
            user.sendMessage("**1.** " + questions[0].getFull());
            this.id = user.getId();
            this.name = user.getDiscriminatedName();
        }

        @Override
        protected void onFinish(boolean completed) {
            if (!completed) send(id, "Thank you for your time! None of the results of this form were saved. Exiting...");
            results.complete(new Results(questions,answers,completed,id));
        }
    }
}