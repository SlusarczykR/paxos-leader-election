package com.slusarczykr.paxos.leader.starter;

import com.slusarczykr.paxos.leader.discovery.state.ServerDetails;
import com.slusarczykr.paxos.leader.election.task.StartLeaderElectionTask;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

@Component
@RequiredArgsConstructor
public class LeaderElectionStarter {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionStarter.class);

    public static final int MIN_AWAIT_TIME = 15;
    public static final int MAX_AWAIT_TIME = 30;

    private final Random random = new Random();

    private final ApplicationContext applicationContext;
    private final ServerDetails serverDetails;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Initializing leader election procedure...");
        startLeaderElection();
    }

    private void startLeaderElection() {
        TimerTask startLeaderElectionTask = createStartLeaderElectionTask();
        schedule(startLeaderElectionTask);
    }

    private StartLeaderElectionTask createStartLeaderElectionTask() {
        StartLeaderElectionTask startLeaderElectionTask = new StartLeaderElectionTask();
        autowire(startLeaderElectionTask);

        return startLeaderElectionTask;
    }

    private void autowire(StartLeaderElectionTask startLeaderElectionTask) {
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(startLeaderElectionTask);
    }

    private void schedule(TimerTask startLeaderElectionTask) {
        Timer candidateTime = serverDetails.getCandidateForLeaderTimer();
        candidateTime.schedule(startLeaderElectionTask, awaitLeaderElectionTime());
    }

    private int awaitLeaderElectionTime() {
        return generateRandom(MIN_AWAIT_TIME, MAX_AWAIT_TIME) * 1000;
    }

    public int generateRandom(int min, int max) {
        return random.nextInt(max - min) + min;
    }
}
