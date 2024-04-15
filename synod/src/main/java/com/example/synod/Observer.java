package com.example.synod;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.example.synod.message.*;

import java.util.Random;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Observer extends UntypedAbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor

    private int n; // number of processes
    private float alpha; // probability of crashing
    private int t_le; // number of rounds to wait before deciding

    Boolean received = false;

    long timeStart;

    public Observer(int n, float alpha, int t_le) {
        timeStart = System.nanoTime();
        this.n = n;
        this.alpha = alpha;
        this.t_le = t_le;
    }

    /**
     * Static method to create an actor
     */

    public static Props createActor(int n, float alpha, int t_le) {
        return Props.create(Observer.class, () -> new Observer(n, alpha, t_le));
    }

    public void onReceive(Object message) throws Throwable {
        if (message instanceof Decide) {
            if (!received) {
                long timeEnd = System.nanoTime();
                long latency = timeEnd - timeStart;
                System.out.println("Latency: " + latency / 1_000_000.0);
                received = true;
                context().system().terminate();

                // Write latency to file
                // Write latency to file
                try {
                    File file = new File("latency.csv");
                    PrintWriter out = new PrintWriter(new FileWriter(file, true));
                    if (file.length() == 0) {
                        out.println("n;alpha;t_le;latency");
                    }
                    out.println(n + ";" + alpha + ";" + t_le + ";" + (latency / 1_000_000.0));
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
