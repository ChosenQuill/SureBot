package com.suredroid.discord.commands;

import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.DUtils;
import org.apache.logging.log4j.message.FormattedMessage;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.exception.RatelimitException;
import org.javacord.api.util.NonThrowingAutoCloseable;
import org.javacord.api.util.logging.ExceptionLogger;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.script.ScriptContext.ENGINE_SCOPE;
import static jdk.nashorn.api.scripting.NashornScriptEngine.NASHORN_GLOBAL;

//@ApplicationScoped
//@Alias("evaluate")
//@Alias("eval")
//@Alias("e")
//@Usage("<script>")
//@RestrictedTo(BOT_OWNER)
//@Asynchronous
// https://gist.github.com/Vampire/7edcc82211006892e85aa1ecdc7342dc

@SuppressWarnings("removal")
@Command(desc = "Evaluate Expressions and Code Live!", usage = "[code]", roles="botowner", aliases = {"e", "evaluate"}, async = true, hidden = true)
public class Eval {

    private ScriptEngineManager scriptEngineManager;

    private ScriptEngine scriptEngine;
    private boolean javaScript;

    private Timer contextResetTimer;
    private AtomicReference<TimerTask> contextResetTimerTask = new AtomicReference<>();

    public Eval() {
        scriptEngineManager = new ScriptEngineManager();

        scriptEngine = scriptEngineManager.getEngineByName("Groovy");
        if (scriptEngine == null) {
            scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
            javaScript = true;
        }

        contextResetTimer = new Timer(format("Translator Discord Bot - Evaluate %s Context Reset Timer", javaScript ? "JavaScript" : "Groovy"), true);
    }

    public void run(String parameterString, MessageCreateEvent e){
        Message incomingMessage = e.getMessage();

        contextResetTimer.schedule(contextResetTimerTask.updateAndGet(timerTask -> {
            if (timerTask != null) {
                timerTask.cancel();
            }
            return new TimerTask() {
                @Override
                public void run() {
                    scriptEngine.getBindings(ENGINE_SCOPE).clear();
                    if (!javaScript) {
                        try {
                            scriptEngine.eval("def metaClassIterator = GroovySystem.metaClassRegistry.iterator()\n"
                                    + "metaClassIterator.forEachRemaining { metaClassIterator.remove() }");
                        } catch (ScriptException se) {
                            ExceptionLogger.getConsumer().accept(se);
                        }
                    }
                }
            };
        }), 5 * 60 * 1000);

        DiscordApi api = incomingMessage.getApi();
        Server server = incomingMessage.getServer().orElse(null);
        TextChannel textChannel = incomingMessage.getChannel();
        Bindings bindings = scriptEngine.getBindings(ENGINE_SCOPE);
        EmbedBuilder response;
        try (NonThrowingAutoCloseable typingIndicator = textChannel.typeContinuouslyAfter(250, MILLISECONDS, ExceptionLogger.getConsumer(RatelimitException.class))) {
            bindings.put("#jsr223.groovy.engine.keep.globals", "phantom");
            bindings.put("m", incomingMessage);
            bindings.put("c", textChannel);
            bindings.put("s", server);
            bindings.put("a", api);
            bindings.put("e",e);
            if (javaScript) {
                scriptEngine.eval("load('nashorn:mozilla_compat.js');\n"
                        + "importPackage(java.lang);\n"
                        + "importPackage(java.util);");
            } else {
                scriptEngine.eval("java.util.concurrent.CompletableFuture.metaClass.methodMissing = { name, args -> invokeMethod('join', null).invokeMethod(name, args) }\n"
                        + "java.util.concurrent.CompletableFuture.metaClass.propertyMissing = { name -> invokeMethod('join', null).\"$name\" }\n"
                        + "java.util.Optional.metaClass.methodMissing = { name, args -> invokeMethod('get', null).invokeMethod(name, args) }\n"
                        + "java.util.Optional.metaClass.propertyMissing = { name -> invokeMethod('get', null).\"$name\" }\n");
            }
            Object result = scriptEngine.eval(parameterString);
            if (result instanceof CompletableFuture) {
                result = ((CompletableFuture) result).join();
            }
            bindings.put("r", result);
            // Use FormattedMessage here to get arrays formatted nicely
            String formattedMessage = new FormattedMessage("```java\n{}", result).getFormattedMessage();
            response = new EmbedBuilder()
                    .setColor(BLUE)
                    .addField("Result", formattedMessage.substring(0, Math.min(formattedMessage.length(), 1020)) + "\n```", false);
            if (result != null) {
                response.addField("Result Type", format("```java\n%s\n```", result.getClass().getCanonicalName().replaceFirst("^\\Qjava.lang.", "")), false);
            }
        } catch (Throwable t) {
            bindings.put("t", t);
            response = new EmbedBuilder()
                    .setColor(RED)
                    .addField("Error", format("```java\n%s\n```", t.getMessage()), false);
        } finally {
            if (javaScript) {
                // unset variables that shade our own variables
                Object nashornGlobalObject = bindings.get(NASHORN_GLOBAL);
                if (nashornGlobalObject instanceof Bindings) {
                    Bindings nashornGlobalBindings = (Bindings) nashornGlobalObject;
                    nashornGlobalBindings.remove("m");
                    nashornGlobalBindings.remove("c");
                    nashornGlobalBindings.remove("s");
                    nashornGlobalBindings.remove("a");
                    nashornGlobalBindings.remove("r");
                    nashornGlobalBindings.remove("t");
                }
            }
            // unset variables only helpful during one execution
            bindings.remove("a", api);
            bindings.remove("s", server);
            bindings.remove("c", textChannel);
            bindings.remove("m", incomingMessage);
            bindings.remove("e", e);
        }
        
        DUtils.sendMessage(response, e.getChannel());
    }
}
