package bot;

import chariot.model.Some;
import reader.TiktokReader;

import java.sql.Time;
import java.time.Duration;
import java.util.logging.*;

class Main {
    static final Logger LOGGER = Logger.getLogger("main");

    static void main() {
        while (true) {
            try {
                if (ClientAndAccount.initialize().map(Bot::new) instanceof Some(var bot)) {
                    var processor = new MessageProcessor(Duration.ofSeconds(30));
                    var reader = new TiktokReader("focusonyourroad", processor);
                    Thread.ofVirtual().start(() -> {
                        try { reader.startReader(); }
//                        try { while(true) {
//                            Thread.sleep(10000);
//                            reader.testMove();
//                        }}
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
