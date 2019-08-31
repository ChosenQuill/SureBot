package com.suredroid.discord.commands.Apply;

import com.google.gson.reflect.TypeToken;
import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.Error;
import com.suredroid.discord.*;
import com.vdurmont.emoji.EmojiManager;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.listener.message.reaction.ReactionAddListener;
import org.javacord.api.util.DiscordRegexPattern;
import org.javacord.api.util.event.ListenerManager;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
@Command(desc = "Access to the apply-react and generation system.", usage = "[generate|define|add|remove|reset|list]...")
public class Apply {

    private static HashMap<String, String> emojiLink = new HashMap<>();
    private static Map<String, ArrayList<String>> teamForms = new HashMap<>();
    private static Map<String, String> teamDesc = new HashMap<>();

    ArrayList<Reacts> reacts = new ArrayList<>();

    public Apply() {

        CommonUtils.getJson("EmojiLink.json", emojiLink).ifPresentOrElse(value -> emojiLink = value, () -> CommonUtils.writeJson("EmojiLink.json", emojiLink));
        CommonUtils.getJson("TeamForms.json", teamForms).ifPresentOrElse(value -> teamForms = value, () -> CommonUtils.writeJson("TeamForms.json", teamForms));
        CommonUtils.getJson("TeamDesc.json", teamDesc).ifPresentOrElse(value -> teamDesc = value, () -> CommonUtils.writeJson("TeamDesc.json", teamDesc));

        new File(DiscordBot.getStoragePath() + "/apps/").mkdir();

        Type type = new TypeToken<ArrayList<Reacts>>() {
        }.getType();
        //noinspection unchecked
        CommonUtils.getJson("applyreacts.json", type).ifPresentOrElse(map -> reacts = (ArrayList<Reacts>) map, () -> {
            reacts = new ArrayList<>();
            CommonUtils.writeJson("applyreacts.json", reacts);
        });
        reacts.forEach(react -> {
            var obj = new Object() {
                boolean write = false;
            };
            DUtils.getApi().getTextChannelById(react.textChannel).ifPresentOrElse(channel -> DUtils.getApi().getMessageById(react.messageId, channel).whenComplete((message, throwable) -> {
                if (message == null) {
                    reacts.remove(react);
                    obj.write = true;
                } else {
                    ListenerManager manager = message.addReactionAddListener(react);
                    react.setAttachment(manager);
                }
            }), () -> {
                reacts.remove(react);
                obj.write = true;
            });

            if (obj.write) CommonUtils.writeJson("applyreacts.json", reacts);
        });


    }

    public static boolean hasTeam(String teamName) {
        return teamForms.containsKey(teamName);
    }

    public void run(String[] args, MessageCreateEvent e) {
        if (!Main.isChosenQuill(e)) {
            DUtils.sendMessage(e, "New Application System", "The !apply command has been replaced with a more fancier reaction button system.\nPlease head over to <#556272630609477642> to read learn to use it.");
            return;
        }
        if (args.length < 1) {
            Error.IncorrectArgumentNumber.send(e);
            return;
        }
        args = Arrays.stream(args).map(String::toLowerCase).toArray(String[]::new);
        switch (args[0]) {
            case "define":
                if (args.length < 3) {
                    Error.IncorrectArgumentNumber.send(e);
                    return;
                }
                String emojiString = args[2];
                var wrapper = new Object() {
                    String emojival;
                    String formval;
                };
                if (Forms.list.containsKey(args[1]))
                    wrapper.formval = args[1];

                e.getServer().ifPresentOrElse((server) -> {
                    Matcher match = DiscordRegexPattern.CUSTOM_EMOJI.matcher(emojiString);
                    if (match.find()) {
                        server.getCustomEmojiById(match.group("id")).ifPresentOrElse(knownCustomEmoji -> {
                            wrapper.emojival = knownCustomEmoji.getIdAsString();
                        }, () -> DUtils.sendMessage(e, "Invalid Emoji", "This is not a valid custom emoji."));
                    } else if (EmojiManager.isEmoji(emojiString)) {
                        wrapper.emojival = emojiString;
                    } else if (server.getCustomEmojisByNameIgnoreCase(emojiString).size() > 0) {
                        Optional<KnownCustomEmoji> oemoji = server.getCustomEmojisByNameIgnoreCase(emojiString).stream().findAny();
                        oemoji.ifPresent(emoji -> {
                            wrapper.emojival = emoji.getIdAsString();
                        });
                    }
                }, () -> Error.ServerOnly.send(e));
                if (wrapper.emojival == null)
                    DUtils.sendMessage(e, "Invalid definition.", "This definition is wrong because the emoji is not found.");
                else if (wrapper.formval == null)
                    DUtils.sendMessage(e, "Invalid definition.", "This definition is wrong because the form is not found.");
                else {
                    emojiLink.put(wrapper.emojival, wrapper.formval);
                    CommonUtils.writeJson("EmojiLink.json", emojiLink);
                    DUtils.sendMessage(e, "Definition Recorded", "This definition has successfully been recorded.");
                }
                break;
            case "add":
                if (args.length < 4) {
                    Error.IncorrectArgumentNumber.send(e);
                    return;
                }
                ArrayList<String> forms = new ArrayList<>();
                for (int i = 3; i < args.length; i++) {
                    if (!Forms.list.containsKey(args[i])) {
                        DUtils.sendMessage(e, "Invalid Form", "Form " + args[i] + " is not a valid form");
                        return;
                    }
                    forms.add(args[i]);
                }
                Collections.sort(forms);
                teamForms.put(args[1], forms);
                teamDesc.put(args[1], args[2]);
                CommonUtils.writeJson("TeamDesc.json", teamDesc);
                CommonUtils.writeJson("TeamForms.json", teamForms);
                DUtils.sendMessage(e, "Added new team", "Team: " + args[1] + " | Forms: " + String.join(", ", forms));
                break;
            case "remove":
                if (args.length < 2) {
                    Error.IncorrectArgumentNumber.send(e);
                    return;
                }
                if (teamForms.remove(args[1]) == null) {
                    DUtils.sendMessage(e, "Nothing was removed.", "This team was not found in the database.");
                } else {
                    DUtils.sendMessage(e, "Team removed.", "Team \"" + args[1] + "\" has been removed.");
                    CommonUtils.writeJson("TeamForms.json", teamForms);
                }
                break;
            case "reset":
                emojiLink.clear();
                CommonUtils.writeJson("EmojiLink.json", emojiLink);
                DUtils.sendMessage(e, "EmojiLinks reset", "All emojis and this form links have been reset.");
                break;
            case "generate":
                if (!e.isServerMessage()) {
                    Error.ServerOnly.send(e);
                    return;
                }
                TextChannel c = e.getChannel();
                c.sendMessage("**Welcome to the application page!**\nTo apply for a team, find the message that has the name of the team you are applying for, and match the emoji to the position you are applying for. Once you click on the (emoji) reaction of the message, the application will automatically begin.");
                HashMap<String, String> formLink = (HashMap<String, String>) MapUtils.invertMap(emojiLink);
                reacts.forEach(Reacts::detach);
                reacts.clear();
                for (String team : teamForms.keySet()) {
                    if (!teamDesc.containsKey(team)) {
                        DUtils.sendMessage(e, "Error getting description", "No description for this team.... Skipping.");
                    } else {
                        Message nameMsg = c.sendMessage("**" + StringUtils.capitalize(team) + "** Team | **" + teamDesc.get(team) + "**").join();
                        Message message = c.sendMessage("Generating...").join();
                        if (nameMsg == null || message == null) {
                            DiscordBot.report(e, "Error generating message", "Error while trying to generate message");
                            return;
                        }
                        AtomicBoolean delete = new AtomicBoolean(false);
                        ArrayList<String> desc = new ArrayList<>();
                        teamForms.get(team).forEach(position -> {
                            String emoji = formLink.get(position);
                            if (emoji == null) {
                                delete.set(true);
                                DUtils.sendMessage(e, "Undefined Emoji for Form", "You have not defined emojis for all of the forms provided. Please do so before generating.\nForm: " + position);
                                return;
                            }
                            if (EmojiManager.isEmoji(emoji)) {
                                message.addReaction(emoji);
                                desc.add(StringUtils.capitalize(position) + "(" + emoji + ")");
                            } else {
                                DUtils.getApi().getCustomEmojiById(emoji).ifPresentOrElse(knownCustomEmoji -> {
                                    message.addReaction(knownCustomEmoji);
                                    desc.add(StringUtils.capitalize(position) + "(" + knownCustomEmoji.getMentionTag() + ")");
                                }, () -> {
                                    DiscordBot.report(e, "Error finding emoji", "Cannot find known custom emoji during generation. Removing it...");
                                    delete.set(true);
                                    if (emojiLink.remove(formLink.get(position)) != null)
                                        CommonUtils.writeJson("EmojiLink.json", emojiLink);
                                });
                            }
                        });
                        if (!delete.get()) {
                            message.edit(String.join(" - ", desc));
                            reacts.add(new Reacts(message, team));
                        } else {
                            message.delete();
                            nameMsg.delete();
                        }
                    }

                }
                CommonUtils.writeJson("applyreacts.json", reacts);
                e.deleteMessage();
                break;
            case "list":
                DUtils.sendMessage(e, "List of Teams", String.join(", ", teamForms.keySet().toArray(String[]::new)));
                var concatWrapper = new Object() {
                    String main;
                };
                e.getServer().ifPresent(server -> {
                    String combined = emojiLink.keySet().stream().map(key -> {
                        String oneVal;
                        if (server.getCustomEmojiById(key).isPresent())
                            oneVal = server.getCustomEmojiById(key).get().getMentionTag();
                        else oneVal = "Undefined";
                        return emojiLink.get(key) + " - " + oneVal;
                    }).collect(Collectors.joining(", "));
                    DUtils.sendMessage(e, "Form Emojis", combined);
                });
                break;
            default:
                Error.InvalidArguments.send(e);
                break;
        }
    }

    private void updateReacts() {
        System.out.println(Arrays.toString(reacts.toArray()));
        CommonUtils.writeJson("applyreacts.json", reacts);
    }

    class Reacts implements ReactionAddListener {
        long messageId, textChannel;
        String team;
        transient ListenerManager manager;

        public Reacts(Message message, String team) {
            manager = message.addReactionAddListener(this);
            messageId = message.getId();
            textChannel = message.getChannel().getId();
            this.team = team;
        }

        @Override
        public void onReactionAdd(ReactionAddEvent event) {
            if (event.getUser().isBot()) return;
            event.removeReaction();

            String pos = getRole(event.getEmoji());
            if (pos != null && teamForms.get(team).contains(pos)) {
                ApplySystem.begin(event, pos, team);
            } else {
                event.removeReaction();
            }
        }

        private String getRole(Emoji emoji) {
            if (emoji.isKnownCustomEmoji()) {
                String emojival = emoji.asKnownCustomEmoji().get().getIdAsString();
                if (emojiLink.containsKey(emojival))
                    return emojiLink.get(emojival);

            } else if (emoji.isUnicodeEmoji()) {
                String emojival = emoji.asUnicodeEmoji().get();
                if (emojiLink.containsKey(emojival))
                    return emojiLink.get(emojival);
            }
            return null;
        }

        public void detach() {
            manager.remove();
        }

        public void setAttachment(ListenerManager manager) {
            this.manager = manager;
        }

        @Override
        public String toString() {
            return "Team: " + team + ", EmbedMessage: " + messageId;
        }
    }
}
