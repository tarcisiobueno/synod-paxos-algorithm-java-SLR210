package com.example.synod;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.Write;
import scala.Enumeration.Value;

import com.example.synod.message.*;

import java.util.Random;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

public class Process extends UntypedAbstractActor {

    public class Pair {
        public Boolean est;
        public int estballot;
        public Pair(Boolean est, int estballot){
            this.est = est;
            this.estballot = estballot;
        }
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor

    private int n;//number of processes
    private int i;//id of current process
    private Membership processes;//other processes' references
    private Boolean proposal;
    private int ballot;
    private int readballot;
    private int imposeballot;
    private Boolean estimate;
    private List<Pair> states;

    /**
     * Static method to create an actor
     */
    public static Props createActor(int n, int i) {
        return Props.create(Process.class, () -> new Process(n, i));
    }

    public Process(int n, int i) {
        this.n = n;
        this.i = i;
        this.proposal = null;
        this.ballot = i - n;
        this.readballot = 0;
        this.imposeballot = 0;
        this.estimate = null;
        reset_states();
    }

    private void reset_states() {
        this.states = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            states.add(new Pair(null, 0));
        }
    }

    private void propose(Boolean v) {
        log.info(this + " - propose("+ v+")");
        proposal = v;
        ballot += n;
        reset_states();
        // Send READ to all 
        for (ActorRef actor : processes.references) {
            Read r = new Read(ballot);
            actor.tell(r, getSelf());
        }
    }

    public void onReceive(Object message) throws Throwable {
        if (message instanceof Membership) {
            log.info(this + " - MEMBERSHIP received");
            Membership m = (Membership) message;
            processes = m;
        } else if (message instanceof Launch) {
            log.info(this + " - LAUNCH received");
            propose(true);
        } else if (message instanceof Read) {
            log.info(this + " - READ received from " + getSender());
            Read read = (Read) message;
            if (readballot > read.ballot || imposeballot > read.ballot) {
                // Send ABORT to sender
                Abort abort = new Abort(read.ballot);
                getSender().tell(abort, getSelf());
            } else {
                readballot = read.ballot;
                // Send GATHER to sender
                Gather gather = new Gather(read.ballot, imposeballot, estimate, this.i);
                getSender().tell(gather, getSelf());
            }
        } else if (message instanceof Abort) {
            log.info(this + " - ABORT received from " + getSender());
            // ABORT
        } else if (message instanceof Gather) {
            log.info(this + " - GATHER received from " + getSender());
            Gather gather = (Gather) message;
            states.set(gather.i, new Pair(gather.est, gather.estballot));

            int count = 0;
            for (int j = 0; j < n; j++) {
                count += states.get(j).est != null ? 1 : 0;
            }

            if (count > n / 2) { // received a majority of responses
                int k = -1;
                int max_estballot = 0;
                for (int j = 0; j < n; j++) {
                    int estb = states.get(j).estballot;
                    if (estb > max_estballot) {
                        k = j;
                        max_estballot = estb;
                    }
                }
                if (k != -1) {
                    this.proposal = states.get(k).est;
                }
                reset_states();
                for (ActorRef actor : processes.references) {
                    Impose imp = new Impose(this.ballot, this.proposal);
                    actor.tell(imp, getSelf());
                }
            }
        } else if (message instanceof Impose) {
            log.info(this + " - IMPOSE received from " + getSender());
            Impose impose = (Impose) message;
            if (readballot > impose.ballot || imposeballot > impose.ballot) {
                // Send ABORT to sender
                Abort a = new Abort(impose.ballot);
                getSender().tell(a, getSelf());
            } else {
                this.estimate = impose.v;
                this.imposeballot = impose.ballot;
                // Send ACK to sender
                Ack ack = new Ack(impose.ballot);
                getSender().tell(ack, getSelf());
            }
        } else if (message instanceof Ack) {
            log.info(this + " - ACK received from " + getSender());
            // Send DECIDE to all
            for (ActorRef actor : processes.references) {
                Decide dec = new Decide(proposal);
                actor.tell(dec, getSelf());
            }
        } else if (message instanceof Decide) {
            log.info(this + " - DECIDE received from " + getSender());
            Decide decide = (Decide) message;
            // Send DECIDE to all
            for (ActorRef actor : processes.references) {
                Decide dec = new Decide(decide.v);
                actor.tell(dec, getSelf());
            }
        }
    }

    @Override
    public String toString() {
        return "Process #" + i;
    }

}
