package bot;

import io.github.jwdeveloper.tiktok.data.events.TikTokCommentEvent;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MessageProcessor {
    static final Logger LOGGER = Logger.getLogger("processor");

    private final Duration votingDuration;
    private final AtomicBoolean votingOpen = new AtomicBoolean(false);
    private final ConcurrentHashMap<String, Integer> votes = new ConcurrentHashMap<>();
    private volatile Set<String> currentValidMoves = Set.of();
    private volatile Map<String, String> currentSanToUci = Map.of();

    public MessageProcessor(Duration votingDuration) {
        this.votingDuration = votingDuration;
    }

    public void onMessage(TikTokCommentEvent commentEvent) {
        String normalised = commentEvent.getText().trim().toLowerCase().replaceAll("[+#]", "");

        String uciMove = currentValidMoves.contains(normalised)
            ? normalised
            : currentSanToUci.get(normalised);

        if (uciMove != null) {
            votes.merge(uciMove, 1, Integer::sum);
            LOGGER.info(() -> "Vote: %s (total: %d)".formatted(uciMove, votes.get(uciMove)));
        }
    }

    void openVotingWindow(Collection<String> validMoves, Map<String, String> sanToUci, Consumer<String> onMoveChosen) {
        votes.clear();
        currentValidMoves = validMoves.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toUnmodifiableSet());
        currentSanToUci = Map.copyOf(sanToUci);
        votingOpen.set(true);
        LOGGER.info(() -> "Voting window open for %ds — %d valid moves".formatted(
            votingDuration.toSeconds(), currentValidMoves.size()));

        Thread.ofVirtual().start(() -> {
            try { Thread.sleep(votingDuration); } catch (InterruptedException _) {}
            votingOpen.set(false);
            String chosen = votes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseGet(() -> {
                    LOGGER.info("No votes cast — using random fallback");
                    return validMoves.stream()
                        .skip(new Random().nextInt(validMoves.size()))
                        .findFirst()
                        .orElseThrow();
                });
            LOGGER.info(() -> "Move chosen: %s (votes: %s)".formatted(chosen, votes));
            onMoveChosen.accept(chosen);
        });
    }
}
