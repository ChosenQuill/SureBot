package com.suredroid.discord.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchProvider;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.suredroid.discord.Annotations.Command;
import com.suredroid.discord.DUtils;
import com.suredroid.discord.DiscordBot;
import com.suredroid.discord.Error;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.*;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.lavaplayerwrapper.youtube.YouTubeAudioSource;
import org.javacord.lavaplayerwrapper.youtube.YouTubeAudioSourceBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Music {

    private final String YTRegex = "(?:https?:\\/\\/)?(?:www\\.)?youtu\\.?be(?:\\.com)?\\/?.*(?:watch|embed)?(?:.*v=|v\\/|\\/)([\\w\\-_]+)\\&?";
    private final String SCRegex = "^(https?:\\/\\/)?(www.)?(m\\.)?soundcloud\\.com\\/[\\w\\-\\.]+(\\/)+[\\w\\-\\.]+/?$";
    // Create a player manager
    AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    YoutubeSearchProvider youtubeSearch;

    public Music() {
        AudioSourceManagers.registerRemoteSources(playerManager);

        YoutubeAudioSourceManager youtubemanger = new YoutubeAudioSourceManager();
        playerManager.registerSourceManager(youtubemanger);

        youtubeSearch = new YoutubeSearchProvider(youtubemanger);


        DUtils.getApi().addServerVoiceChannelMemberLeaveListener(event -> {
            event.getServer().getAudioConnection().ifPresent(connection -> {
                if (connection.getChannel() == event.getChannel()) {
                    if (event.getChannel().getConnectedUsers().size() <= 1) {
                        connection.close();
                    }
                }
            });
        });
    }

    @Command(desc = "Stops the current playing song.")
    public void stop(MessageCreateEvent e) {
        e.getServer().ifPresentOrElse(server -> {
            server.getAudioConnection().ifPresentOrElse(connection -> {
                connection.close();
                DUtils.sendMessage(e, "Stopping Music", "Stopping the current playing track.");
            }, () -> Error.NotPlayingTrack.send(e));
        }, () -> Error.ServerOnly.send(e));
    }




    @Command(desc="Plays a provided youtube song.", usage = "!play (sc/yt) [url/query]")
    public void play(MessageCreateEvent e, String query) {
        if (!Pattern.matches(YTRegex, query) && !Pattern.matches(SCRegex, query)) {
            if(DiscordBot.debug) System.out.println("Not matching regex");
            query = "ytsearch:" + query;
        }
        playSong(e, query);
    }

    @Command
    public void play(MessageCreateEvent e, String source, String query) {
        if(source.equalsIgnoreCase("yt") || source.equalsIgnoreCase("youtube")) {
            if (!Pattern.matches(YTRegex, query))
                query = "ytsearch:" + query;
        } else if (source.equalsIgnoreCase("sc") || source.equalsIgnoreCase("soundcloud")){
            if (!Pattern.matches(SCRegex, query))
                query = "scsearch:" + query;
        } else {
            DUtils.sendMessage(e,"This source is not supported.","We only support youtube(yt) and soundcloud(sc) at the moment.");
            return;
        }
        playSong(e, query);
    }


    private void playSong(MessageCreateEvent e, String query){
        e.getMessageAuthor().asUser().ifPresent(user -> {
            e.getServer().ifPresentOrElse(server -> {
                user.getConnectedVoiceChannel(server).ifPresentOrElse(voice -> {
                    server.getAudioConnection().ifPresent(audioConnection -> audioConnection.getCurrentAudioSource().ifPresent(audioSource -> audioConnection.close()));
                    voice.connect().thenAccept(connection -> {
                                    /*
                                    new YouTubeAudioSourceBuilder(DUtils.getApi())
                                            .setUrl(query)
                                            .build()
                                            //.thenCompose(YouTubeAudioSource::download) // Optional: Download the full song before queueing it.
                                            .thenAccept(source -> {
                                                connection.queue(source);
                                                message.edit(DUtils.createEmbed(":musical_note: Now Playing " + source.getTitle() + "! :musical_note: ", "Playing some sick tunes \uD83C\uDFA7!", e.getMessageAuthor().getDisplayName(), e.getMessageAuthor().getAvatar()));
                                            });
                                    */
                        DUtils.sendMessage(e, "Your music is loading...", "Please wait momentarily while your music loads.").whenCompleteAsync((message, throwable) -> {
                            if (message == null) {
                                return;
                            }

                            AudioPlayer player = playerManager.createPlayer();

                            playerManager.loadItem(query, new AudioLoadResultHandler() {
                                @Override
                                public void trackLoaded(AudioTrack track) {
                                    player.playTrack(track);
                                    YTSource source = new YTSource(DUtils.getApi(), player, playerManager.getFrameBufferDuration(), query);
                                    connection.queue(source);
                                    message.edit(DUtils.createEmbed(":musical_note: Now Playing " + source.getTitle() + "! :musical_note: ", "Playing some sick tunes \uD83C\uDFA7!", e.getMessageAuthor().getDisplayName(), e.getMessageAuthor().getAvatar()));
                                }

                                @Override
                                public void playlistLoaded(AudioPlaylist playlist) {
                                    if (playlist.isSearchResult()) {
                                        if (playlist.getTracks().size() > 0) {
                                            player.playTrack(playlist.getTracks().get(0));
                                            YTSource source = new YTSource(DUtils.getApi(), player, playerManager.getFrameBufferDuration(), query);
                                            connection.queue(source);
                                            message.edit(DUtils.createEmbed(":musical_note: Now Playing " + source.getTitle() + "! :musical_note: ", "Playing some sick tunes \uD83C\uDFA7!", e.getMessageAuthor().getDisplayName(), e.getMessageAuthor().getAvatar()));
                                        } else
                                            message.edit(DUtils.createEmbed("No results found.", "Wow, you managed to search a term which has no videos on it. That's an achievement."));
                                    } else
                                        message.edit(DUtils.createEmbed("No Playlists, Sorry.", "Sorry, but we can play playlists right at the moment. \uD83D\uDE26 Ask ChosenQuill for this feature.", e.getMessageAuthor().getDisplayName(), e.getMessageAuthor().getAvatar()));

                                }

                                @Override
                                public void noMatches() {
                                    message.edit(DUtils.createEmbed("We couldn't find the video.", "Sorry, couldn't find the video. \uD83D\uDE26 This is most likely because the video doesn't exist.", e.getMessageAuthor().getDisplayName(), e.getMessageAuthor().getAvatar()));
                                }

                                @Override
                                public void loadFailed(FriendlyException throwable) {
                                    message.edit(DUtils.createEmbed("Can't load the video.", "Sorry, couldn't load the video. \uD83D\uDE26 This may be for a number of reasons. The video may be private, blocked, or not available.\nCause: ``" + throwable.getCause() + "``", e.getMessageAuthor().getDisplayName(), e.getMessageAuthor().getAvatar()));
                                }
                            });

                            connection.addAudioSourceFinishedListener(event -> connection.close());
                        });
                    });
                }, () -> Error.UserNotConnected.send(e));
            }, () -> Error.ServerOnly.send(e));
        });
    }


}

class YTSource extends AudioSourceBase implements SeekableAudioSource, DownloadableAudioSource,
        PauseableAudioSource, BufferableAudioSource {

    // Lavaplayer objectList
    private final AudioPlayer player;
    // Some general information about the YouTube video
    private final String url;
    private final String title;
    private final String creatorName;
    private AudioFrame lastFrame;
    private volatile AudioTrack track;
    private volatile boolean paused = false;

    private volatile List<byte[]> allFrames = null;
    private int position = 0;
    private long bufferDurationInMillis;

    /**
     * Creates a new YouTube audio source.
     *
     * @param api                    A Discord api instance.
     * @param player                 The used audio player.
     * @param bufferDurationInMillis The initial buffer duration of the used audio player.
     * @param url                    The url of the YouTube video.
     */
    YTSource(DiscordApi api, AudioPlayer player, long bufferDurationInMillis, String url) {
        super(api);
        this.player = player;
        this.bufferDurationInMillis = bufferDurationInMillis;
        this.track = player.getPlayingTrack();

        this.url = url;
        this.title = track.getInfo().title;
        this.creatorName = track.getInfo().author;
    }

    /**
     * Creates a new YouTube audio source that uses some pre-downloaded audio frames.
     *
     * @param api         The Discord api instance.
     * @param allFrames   All audio frames of the YouTube video.
     * @param url         The url of the YouTube video.
     * @param title       The title of the YouTube video.
     * @param creatorName The name of the creator of the YouTube video.
     */
    YTSource(DiscordApi api, List<byte[]> allFrames, String url, String title, String creatorName) {
        super(api);
        this.player = null;
        this.bufferDurationInMillis = Long.MAX_VALUE;
        this.allFrames = allFrames;

        this.url = url;
        this.title = title;
        this.creatorName = creatorName;
    }

    /**
     * Creates a new audio source of this class.
     *
     * <p>This methods is meant as as less verbose variant than the {@link YouTubeAudioSourceBuilder} which provides
     * more configuration.
     *
     * @param api The Discord api instance.
     * @param url The url of the youtube video.
     * @return A new youtube audio source.
     */
    public static CompletableFuture<YouTubeAudioSource> of(DiscordApi api, String url) {
        return new YouTubeAudioSourceBuilder(api)
                .setUrl(url)
                .build();
    }

    /**
     * Gets the url of the YouTube video.
     *
     * @return The url.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the title of the YouTube video.
     *
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the name of the creator of the YouTube video.
     *
     * @return The creator's name.
     */
    public String getCreatorName() {
        return creatorName;
    }

    @Override
    public long setPosition(long position, TimeUnit unit) {
        long frameNumber = (unit.toMillis(position) / 20);
        long positionInMillis = frameNumber * 20;
        if (allFrames != null) {
            this.position = (int) frameNumber;
            return positionInMillis;
        }
        track.setPosition(positionInMillis);
        return positionInMillis;
    }

    @Override
    public long getPosition(TimeUnit unit) {
        return unit.convert(track.getPosition(), TimeUnit.MILLISECONDS);
    }

    @Override
    public long getDuration(TimeUnit unit) {
        return unit.convert(track.getDuration(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void setBufferSize(long size, TimeUnit unit) {
        long duration = unit.toMillis(size);
        if (duration > Integer.MAX_VALUE) {
            duration = Integer.MAX_VALUE;
        }
        bufferDurationInMillis = duration;
        player.setFrameBufferDuration((int) duration);
    }

    @Override
    public long getBufferSize(TimeUnit unit) {
        if (allFrames != null) {
            return Long.MAX_VALUE;
        }
        return bufferDurationInMillis;
    }

    @Override
    public long getUsedBufferSize(TimeUnit unit) {
        if (allFrames != null) {
            return unit.convert(allFrames.size() * 20, TimeUnit.MILLISECONDS);
        }
        // Lavaplayer does not provide a way to tell the used buffer size
        return -1;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public CompletableFuture<YTSource> download() {
        CompletableFuture<YTSource> future = new CompletableFuture<>();
        if (allFrames != null) {
            future.complete(this);
            return future;
        }

        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioPlayer helperPlayer = playerManager.createPlayer();
        helperPlayer.playTrack(track.makeClone());

        getApi().getThreadPool().getExecutorService().submit(() -> {
            List<byte[]> frames = new ArrayList<>();
            AudioFrame frame;
            try {
                frame = player.provide(1, TimeUnit.MINUTES);
            } catch (Throwable t) {
                future.completeExceptionally(t);
                return;
            }
            while (frame != null) {
                frames.add(frame.getData());
                frame = player.provide();
            }
            this.position = (int) (track.getPosition() / 20);
            this.allFrames = frames;
            // Clean up the players, as we no longer need them
            this.player.destroy();
            helperPlayer.destroy();

            future.complete(this);
        });

        return future;
    }

    @Override
    public boolean isFullyDownloaded() {
        return allFrames != null;
    }

    @Override
    public byte[] getNextFrame() {
        if (paused || lastFrame == null) {
            return null;
        }
        if (allFrames != null) {
            return applySynthesizers(allFrames.get(position++));
        }
        return applySynthesizers(lastFrame.getData());
    }

    @Override
    public boolean hasFinished() {
        if (allFrames != null) {
            return position >= allFrames.size();
        }
        return track != null && track.getState() == AudioTrackState.FINISHED;
    }

    @Override
    public boolean hasNextFrame() {
        if (paused) {
            return false;
        }
        if (allFrames != null) {
            return position < allFrames.size();
        }
        lastFrame = player.provide();
        return lastFrame != null;
    }

    @Override
    public AudioSource clone() {
        if (allFrames != null) {
            return new YTSource(getApi(), allFrames, url, title, creatorName);
        }
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioPlayer player = playerManager.createPlayer();
        player.playTrack(track.makeClone());
        return new YTSource(getApi(), player, playerManager.getFrameBufferDuration(), url);
    }
}

