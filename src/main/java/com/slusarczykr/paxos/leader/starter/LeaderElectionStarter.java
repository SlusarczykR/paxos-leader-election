package com.slusarczykr.paxos.leader.starter;

import com.slusarczykr.paxos.leader.discovery.state.PaxosServer;
import com.slusarczykr.paxos.leader.election.config.LeaderElectionProperties;
import com.slusarczykr.paxos.leader.election.service.LeaderElectionService;
import com.slusarczykr.paxos.leader.election.task.LeaderCandidacy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.slusarczykr.paxos.leader.discovery.state.ErrorStatus.Type.INFINITE_REPLIES;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
@RequiredArgsConstructor
public class LeaderElectionStarter {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionStarter.class);

    private final ApplicationContext applicationContext;
    private final LeaderElectionService leaderElectionService;
    private final LeaderElectionProperties leaderElectionProps;
    private final PaxosServer paxosServer;

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<CompletableFuture<Boolean>> candidacy = new AtomicReference<>();
    private final AtomicReference<Future<?>> heartbeats = new AtomicReference<>();

    private final Random random = new Random();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Initializing leader election procedure...");

        if (validateLeaderElectionConfig()) {
            log.warn("Invalid leader election config. detected! Resetting leader election properties to default values...");
            leaderElectionProps.reset();
        }
        startLeaderCandidacy();
    }

    private boolean validateLeaderElectionConfig() {
        return leaderElectionProps.getHeartbeatsInterval() >= leaderElectionProps.getMinAwaitTime()
                || leaderElectionProps.getMinAwaitTime() >= leaderElectionProps.getMaxAwaitTime();
    }

    public void startLeaderCandidacy() {
        LeaderCandidacy task = createStartLeaderElectionTask();
        CompletableFuture<Boolean> leaderCandidacy = startLeaderCandidacy(task, awaitLeaderElectionTime());
        leaderCandidacy.thenAccept(this::processLeaderElection);
        cancelIfPresent(this.candidacy.getAndSet(leaderCandidacy));
    }

    private CompletableFuture<Boolean> startLeaderCandidacy(LeaderCandidacy task, int timeout) {
        log.info("Follower will start leader candidacy for {} ms", timeout);
        Executor delayedExecutor = CompletableFuture.delayedExecutor(timeout, MILLISECONDS);
        return CompletableFuture.supplyAsync(task::start, delayedExecutor);
    }

    private void processLeaderElection(boolean leader) {
        log.info("Processing leader election - leader: {}", leader);
        if (Boolean.TRUE.equals(leader)) {
            disableInfiniteRepliesIfEnabled();
            scheduleHeartbeats();
        } else {
            startLeaderCandidacy();
        }
    }

    public void scheduleHeartbeats() {
        int heartbeatsInterval = leaderElectionProps.getHeartbeatsInterval();
        log.debug("Scheduling heartbeats with interval of {}s", heartbeatsInterval);
        heartbeats.set(scheduleHeartbeats(heartbeatsInterval));
    }

    private ScheduledFuture<?> scheduleHeartbeats(int interval) {
        return scheduledExecutor.scheduleAtFixedRate(
                this::sendHeartbeats,
                5,
                interval,
                SECONDS
        );
    }

    private void disableInfiniteRepliesIfEnabled() {
        if (paxosServer.isInfiniteRepliesEnabled()) {
            paxosServer.disableError(INFINITE_REPLIES);
        }
    }

    private void sendHeartbeats() {
        leaderElectionService.sendHeartbeats(e -> {
            log.error("Leader conflict detected while sending heartbeats to followers nodes!");
            stopHeartbeats();
        });
    }

    private LeaderCandidacy createStartLeaderElectionTask() {
        LeaderCandidacy task = new LeaderCandidacy();
        autowire(task);

        return task;
    }

    private void autowire(LeaderCandidacy task) {
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(task);
    }

    public void stopHeartbeats() {
        cancelIfPresent(heartbeats.get());
    }

    private void cancelIfPresent(Future<?> task) {
        Optional.ofNullable(task).ifPresent(it -> {
            log.debug("Canceling current task execution");
            it.cancel(false);
        });
    }

    public CompletableFuture<Boolean> getLeaderCandidacy() {
        return candidacy.get();
    }

    public void reset() {
        log.info("Resetting leader candidacy starting timeout...");
        cancelIfPresent(candidacy.get());
        startLeaderCandidacy();
    }

    private int awaitLeaderElectionTime() {
        return generateRandom(leaderElectionProps.getMinAwaitTime(), leaderElectionProps.getMaxAwaitTime()) * 1000;
    }

    public int generateRandom(int min, int max) {
        return random.nextInt(max - min) + min;
    }
}
