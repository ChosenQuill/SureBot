package com.suredroid.discord.commands;

import com.google.gson.reflect.TypeToken;
import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.CommonUtils;
import com.suredroid.discord.DUtils;
import com.suredroid.discord.Main;
import com.vdurmont.emoji.EmojiManager;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageDeleteListener;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.listener.message.reaction.ReactionRemoveListener;
import org.javacord.api.util.DiscordRegexPattern;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;

@SuppressWarnings("Duplicates")
@Command(name="reactrole",desc= "Create a message that you can react to get a id.",usage="!reactrole [emoji] [id] ...",example="!reactrole <:pc:441761502672977920> @Gamer <:minecraft:441762276215619592> @Minecraft")
public class ReactionRole {
    //MessageId, Emoji, RoleID
    private HashMap<Long, HashMap<String, Long>> messages;

    public void run(String[] args, MessageCreateEvent e) {
        if (!Main.isChosenQuill(e)) {
            DUtils.sendMessage(e, "Can't run command", "You do not have the valid permissions to run this command.");
            return;
        }
        if (args.length < 2 || args.length % 2 != 0) {
            DUtils.sendMessage(e, "Not enough arguments", "You need an even number of arguments (greater than 2) to continue.");
            return;
        }

        e.getServer().ifPresentOrElse((server) -> {
            HashMap<String, Long> link = new HashMap<>();
            for (int i = 0; i < args.length; i += 2) {
                String emojiString = args[i];
                Matcher match = DiscordRegexPattern.CUSTOM_EMOJI.matcher(emojiString);
                var wrapper = new Object() {
                    String emojival;
                    long roleId;
                };
                if (match.find()) {
                    server.getCustomEmojiById(match.group("id")).ifPresentOrElse(knownCustomEmoji -> {
                        wrapper.emojival = knownCustomEmoji.getIdAsString();
                        e.addReactionsToMessage(knownCustomEmoji);
                    }, () -> DUtils.sendTimedMessage(DUtils.createMessage(e, "Invalid Emoji", "This is not a valid custom emoji."), e.getChannel()));
                } else if (EmojiManager.isEmoji(emojiString)) {
                    e.addReactionsToMessage(emojiString);
                    wrapper.emojival = emojiString;
                } else if (server.getCustomEmojisByNameIgnoreCase(emojiString).size() > 0) {
                    Optional<KnownCustomEmoji> oemoji = server.getCustomEmojisByNameIgnoreCase(emojiString).stream().findAny();
                    oemoji.ifPresent(emoji -> {
                        e.addReactionsToMessage(emoji);
                        wrapper.emojival = emoji.getIdAsString();
                    });
                }
                DUtils.getRole(args[i + 1], server).ifPresentOrElse(role -> wrapper.roleId = role.getId(), () -> DUtils.sendTimedMessage(DUtils.createMessage(e, "This is not a valid role.", "This role is not found."), e.getChannel()));
                if (wrapper.emojival != null && wrapper.roleId != 0) {
                    link.put(wrapper.emojival, wrapper.roleId);
                } else {
                    DUtils.sendTimedMessage(DUtils.createMessage(e, "Invalid Set", "Set " + (i + 1) + " is invalid due to the error above.\nCanceling react-role."), e.getChannel());
                    e.getMessage().removeAllReactions();
                    return;
                }
            }
            messages.put(e.getMessageId(), link);
            CommonUtils.writeJson("rolereacts.json",messages);
        }, () -> DUtils.sendMessage(e, "Can't run command here", "Please run this command on a server."));
    }


    public ReactionRole() {
        messages = new HashMap<>();
        Type type = new TypeToken<HashMap<Long, HashMap<String, Long>>>(){}.getType();
        //noinspection unchecked
        CommonUtils.getJson("rolereacts.json", type).ifPresentOrElse(map -> messages = (HashMap<Long, HashMap<String, Long>>) map,()->{
            messages = new HashMap<>();
            CommonUtils.writeJson("rolereacts.json",messages);
        });
        DUtils.getApi().addListener((ReactionAddListener) e -> {
            if (e.getUser().isBot()) return;
            if (messages.containsKey(e.getMessageId())) {
                getRole(e.getMessageId(), e.getEmoji()).ifPresentOrElse(role -> e.getUser().addRole(role), e::removeReaction);
            }
        });
        DUtils.getApi().addListener((ReactionRemoveListener) e -> {
            if (e.getUser().isBot()) return;
            if (messages.containsKey(e.getMessageId())) {
                getRole(e.getMessageId(), e.getEmoji()).ifPresent(role -> e.getUser().removeRole(role));
            }
        });
        DUtils.getApi().addListener((MessageDeleteListener) e -> messages.remove(e.getMessageId()));
    }


    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Optional<Role> getRole(long messageId, Emoji emoji) {
        if (messages.containsKey(messageId)) {
            if (emoji.isKnownCustomEmoji()) {
                String emojival = emoji.asKnownCustomEmoji().get().getIdAsString();
                if (messages.get(messageId).containsKey(emojival))
                    return DUtils.getApi().getRoleById(messages.get(messageId).get(emojival));

            } else if (emoji.isUnicodeEmoji()) {
                String emojival = emoji.asUnicodeEmoji().get();
                if (messages.get(messageId).containsKey(emojival))
                    return DUtils.getApi().getRoleById(messages.get(messageId).get(emojival));
            }
        }
        return Optional.empty();
    }

}
