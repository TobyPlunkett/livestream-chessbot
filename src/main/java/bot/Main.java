package bot;

import chariot.model.Some;
import reader.TiktokReader;

import java.time.Duration;
import java.util.logging.*;

class Main {
    static final Logger LOGGER = Logger.getLogger("main");

    static void main() {
        while (true) {
            try {
                if (ClientAndAccount.initialize().map(Bot::new) instanceof Some(var bot)) {
                    var processor = new MessageProcessor(Duration.ofSeconds(30));
                    var reader = new TiktokReader("avelineyuri", processor);
                    Thread.ofVirtual().start(() -> {
                        try { reader.startReader(); }
                        catch (Exception e) { LOGGER.log(Level.WARNING, e, e::getMessage); }
                    });
                    bot.run(processor);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e, e::getMessage);
            } finally {
                var duration = Duration.ofSeconds(60);
                LOGGER.info(() -> "Retrying in %d seconds...".formatted(duration.toSeconds()));
                Bot.sleep(duration);
            }
        }
    }
}
