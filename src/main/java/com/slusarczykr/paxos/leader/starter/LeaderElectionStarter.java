package com.slusarczykr.paxos.leader.starter;

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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
@RequiredArgsConstructor
public class LeaderElectionStarter {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionStarter.class);

    public static final int MIN_AWAIT_TIME = 15;
    public static final int MAX_AWAIT_TIME = 30;

    private final ApplicationContext applicationContext;
    private final LeaderElectionService leaderElectionService;
    private final LeaderElectionProperties leaderElectionProperties;

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<CompletableFuture<Boolean>> candidacy = new AtomicReference<>();
    private final AtomicReference<Future<?>> heartbeats = new AtomicReference<>();

    private final Random random = new Random();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Initializing leader election procedure...");
        startLeaderCandidacy();
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
            heartbeats.set(scheduleHeartbeats());
        } else {
            startLeaderCandidacy();
        }
    }

    private ScheduledFuture<?> scheduleHeartbeats() {
        int heartbeatsInterval = leaderElectionProperties.getHeartbeatsInterval();
        log.debug("Scheduling heartbeats with interval of {}s", heartbeatsInterval);

        return scheduledExecutor.scheduleAtFixedRate(
                leaderElectionService::sendHeartbeats,
                5,
                heartbeatsInterval,
                SECONDS
        );
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
        Optional.ofNullable(task).ifPresent(it -> it.cancel(false));
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
        return generateRandom(MIN_AWAIT_TIME, MAX_AWAIT_TIME) * 1000;
    }

    public int generateRandom(int min, int max) {
        return random.nextInt(max - min) + min;
    }
}
