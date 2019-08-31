package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.DUtils;
import com.suredroid.discord.EmbedMessage;
import org.javacord.api.event.message.MessageCreateEvent;

@Command(desc = "A command for testing purposes", usage = "!test {...}", aliases = {"t", "testing"}, serverOnly = true, roles = "tester")
public class Test {
    public String run(MessageCreateEvent e, String argumentOne) {
        return "One argument test";
    }
    public void run(MessageCreateEvent e, String argumentOne, String argumentTwo) {
        DUtils.sendMessage(e,"Two Argument Test","This is a two argument Test");
    }
    public EmbedMessage run(String[] args, MessageCreateEvent e) {
        return new EmbedMessage("All argument Test","This is an all argument test"); //(Title,Message)
    }
}


//    Form test;
//
//    public Test(){
//        test = new Form(new Question[]{new Question("How old are you?"), new Question("Are you a person?"), new Question("Do you love me?")});
//    }

    /*
    public void run(MessageCreateEvent e, String role){
        DUtils.getRole(role).ifPresentOrElse(r -> DUtils.sendMessage(e,"Role received",Long.toString(r.getId())),() -> DUtils.sendMessage(e,"Role not found","This role was not found."));
    }

    public void all(MessageCreateEvent e, String[] args){

        if(e.getMessageAttachments().size() == 0){
            DUtils.sendMessage(e,"No File Attached","You need to attach a file to your message for this to work.");
            return;
        }
        MessageAttachment ma = e.getMessageAttachments().get(0);
        if(!ma.getFileName().toLowerCase().contains(".txt")){
            DUtils.sendMessage(e,"This is not a txt file.","You need to send a txt file for this command to work.");
            return;
        }
        ma.downloadAsByteArray().thenAcceptAsync(bytes -> {
            String input = new String(bytes);
            if(input.chars().count() > 1000){
                DUtils.sendMessage(e,"Your file is too large.","This string can't be returned back because the file is too large.");
                return;
            }
            DUtils.sendMessage(e,"Your txt file.","The message of your txt file is:\n" + input);
        }).exceptionally(ExceptionLogger.get());
    }
    */


