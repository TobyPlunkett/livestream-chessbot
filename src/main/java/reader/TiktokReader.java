package reader;

import bot.Bot;
import io.github.jwdeveloper.tiktok.TikTokLive;

import java.util.ArrayList;
import java.util.List;

public class TiktokReader {
    List<String> messageList = new ArrayList<>();
    Bot chessbot;

    public TiktokReader(Bot chessbot) {
        this.chessbot = chessbot;
    }

    public void startReader() throws InterruptedException {
        TikTokLive.newClient("avelineyuri")
                .onConnected((liveClient, event) ->
                {
                    System.out.println("Connected to live ");
                })
                .onError((liveClient, event) ->
                {
                    System.out.println("Error! " + event.getException().getMessage());
                })
                .onDisconnected((liveClient, event) ->
                {
                    System.out.println("Disconnected: " + event.getReason());
                })
                .onComment(((liveClient, tikTokCommentEvent) ->
                        {
                            System.out.println(tikTokCommentEvent.getText());
                            messageList.add(tikTokCommentEvent.getText());
                            if(messageList.size() > 0){chessbot.receiveMoves(messageList);}
                        }
                        ))
                .configure((settings) -> {
                    settings.setUseEulerstreamWebsocket(true);
                    settings.setUseEulerstreamEnterprise(false);
                    settings.setApiKey(System.getenv("EULER_STREAM_KEY"));
                    settings.setPrintToConsole(true);
                })
                .buildAndConnect();
    }
}
