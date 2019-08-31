package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.CommonUtils;
import com.suredroid.discord.DUtils;
import com.suredroid.discord.Error;
import com.suredroid.discord.Main;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.util.event.ListenerManager;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.util.HashMap;

@Command(name="translate", usage="[phrase(p)] [base] [to] [sentence] | [add(a)] [base] [to] [user] | [remove(r)] [user]", aliases = "tr")
public class TranslateSystem {

    private HashMap<String, String> langs = new HashMap<>();
    private HashMap<String, ListenerManager<MessageCreateListener>> listeners = new HashMap<>();
    public TranslateSystem(){
        CommonUtils.getInternalJson("/files/langs.json",langs).ifPresent(map->langs = map);
    }

    @Command(desc="Translates what you put into what you want.",usage = "[baselang] [newlang] [Sentence...]")
    public void run(String[] args, MessageCreateEvent e){
        if(!Main.isChosenQuill(e)){
            Error.NoPermission.send(e);
            return;
        }
        String from, to;
        switch(args[0].toLowerCase()) {
            case "phrase":
            case "p":
                if(args.length < 4){
                    Error.IncorrectArgumentNumber.send(e);
                    return;
                }
                from = getLang(e,args[1]);
                to = getLang(e,args[2]);
                if(from != null && to != null){
                    e.getChannel().sendMessage(googleTranslateApi(from,to,args[3],e.getMessageAuthor().getName()));
                }
                break;
            case "add":
            case "a":
                if(args.length < 4){
                    Error.IncorrectArgumentNumber.send(e);
                    return;
                }
                from = getLang(e,args[1]);
                to = getLang(e,args[2]);
                if(from != null && to != null) {
                    DUtils.getUser(args[3]).ifPresent(user -> {
                        ListenerManager<MessageCreateListener> listener = user.addMessageCreateListener(new UserTranslator(from,to));
                        listeners.put(user.getIdAsString(),listener);
                        DUtils.sendMessage(e,"Auto-Translate Set","From now on, we will automatically translate " + user.getName() + " from " + from + " to " + to + ".");
                    });
                }
                break;
            case "remove":
            case "r":
                if(args.length < 2){
                    Error.IncorrectArgumentNumber.send(e);
                    return;
                }
                DUtils.getUser(args[1]).ifPresentOrElse(user->{
                    ListenerManager<MessageCreateListener> listener = listeners.get(e.getMessageAuthor().getIdAsString());
                    if(listener != null) {
                        listeners.remove(e.getMessageAuthor().getIdAsString()).remove();
                        DUtils.sendMessage(e,"Auto-Translate Removed","From now on, " + user.getName() + " will not be automatically translated.");
                    } else {
                        DUtils.sendMessage(e,"User not found", "You are already not on the translate list.");
                    }
                },()->Error.UserNotFound.send(e));
                break;
            default:
                Error.InvalidArguments.send(e);
                break;
        }
    }

    private String getLang(MessageCreateEvent e, String lang) {
        lang = lang.toLowerCase();
        if(langs.containsValue(lang))
            return lang;
        if(langs.containsKey(lang))
            return langs.get(lang);
        DUtils.sendMessage(e, "Invalid Language","The language \"" + lang + "\" is not a valid language, please try again with a valid language.");
        return null;
    }


    HttpClient httpClient = HttpClientBuilder.create().build();

    public String googleTranslateApi(String base, String to, String text, String username) {
        String returnString = "";

        try {
            String textEncoded= URLEncoder.encode(text, "utf-8");
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=" + base + "&tl=" + to + "&dt=t&q=" + textEncoded;
            HttpResponse response = httpClient.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                String responseString = out.toString();
                out.close();
                System.out.println(responseString);
                String aJsonString = responseString;
                aJsonString = aJsonString.replace("[", "");
                aJsonString = aJsonString.replace("]", "");
                aJsonString = aJsonString.substring(1);
                int plusIndex = aJsonString.indexOf('"');
                aJsonString = aJsonString.substring(0, plusIndex);

                returnString = username + " (" + to + "): " + aJsonString;
            } else {
                response.getEntity().getContent().close();
                return("Unable to translate phrase: " + statusLine.getReasonPhrase());
            }
        } catch(Exception e) {
            returnString = e.getMessage();
        }

        return returnString;
    }


    class UserTranslator implements MessageCreateListener {
        String base, to;
        public UserTranslator(String base, String to) {
            this.base = base;
            this.to = to;
        }
        @Override
        public void onMessageCreate(MessageCreateEvent event) {
            event.getChannel().sendMessage(googleTranslateApi(base,to, event.getMessageContent(),event.getMessageAuthor().getName()));
        }
    }
}
