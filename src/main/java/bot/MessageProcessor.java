package bot;

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

    public MessageProcessor(Duration votingDuration) {
        this.votingDuration = votingDuration;
    }

    public void onMessage(String text) {
        if (!votingOpen.get()) return;
        String move = text.trim().toLowerCase();
        if (currentValidMoves.contains(move)) {
            votes.merge(move, 1, Integer::sum);
            LOGGER.info(() -> "Vote: %s (total: %d)".formatted(move, votes.get(move)));
        }
    }

    void openVotingWindow(Collection<String> validMoves, Consumer<String> onMoveChosen) {
        votes.clear();
        currentValidMoves = validMoves.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toUnmodifiableSet());
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
