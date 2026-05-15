package reader;

import bot.MessageProcessor;
import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.data.events.TikTokCommentEvent;

public class TiktokReader {
    private final String username;
    private final MessageProcessor processor;

    public TiktokReader(String username, MessageProcessor processor) {
        this.username = username;
        this.processor = processor;
    }

    public void testMove(){
        processor.onMessage(TikTokCommentEvent.of("Username", "e2e4"));
        processor.onMessage(TikTokCommentEvent.of("Username", "d7d6"));

    }
    public void startReader() {
        TikTokLive.newClient(username)
                .onConnected((_, _) -> System.out.println("Connected to live"))
                .onError((_, event) -> System.out.println("Error! " + event.getException().getMessage()))
                .onDisconnected((_, event) -> System.out.println("Disconnected: " + event.getReason()))
                .onComment((_, event) -> {
                    processor.onMessage(event);
                })
                .configure(settings -> {
                    settings.setUseEulerstreamWebsocket(true);
                    settings.setUseEulerstreamEnterprise(false);
                    settings.setApiKey(System.getenv("EULER_STREAM_KEY"));
                    settings.setPrintToConsole(true);
                })
                .buildAndConnect();
    }
}
