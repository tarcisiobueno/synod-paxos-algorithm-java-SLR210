package com.example.synod;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.example.synod.message.*;

import java.util.Random;

public class Process extends UntypedAbstractActor {

    public class Pair {
        public Boolean est;
        public int estballot;

        public Pair(Boolean est, int estballot) {
            this.est = est;
            this.estballot = estballot;
        }
    }

    private enum State {
        NORMAL, FAULTY, SILENT
    }

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);// Logger attached to actor

    private int n; // number of processes
    private int i; // id of current process
    private float alpha; // probability of crashing
    private Boolean value; // value to propose
    private Membership processes; // other processes' references
    private Boolean proposal;
    private int ballot;
    private int readballot;
    private int imposeballot;
    private Boolean estimate;
    private Pair[] states;
    private State state;
    private Boolean willpropose;

    private int count = 0;

    /**
     * Static method to create an actor
     */
    public static Props createActor(int n, int i, float alpha) {
        return Props.create(Process.class, () -> new Process(n, i, alpha));
    }

    public Process(int n, int i, float alpha) {
        this.n = n;
        this.i = i;
        this.alpha = alpha;
        this.value = new Random().nextInt(2) == 0 ? false : true;
        reset();

    }

    private void reset() {
        this.proposal = null;
        this.ballot = i - n;
        this.readballot = 0;
        // changed this - It was this.imposeballot = 0; before
        this.imposeballot = i - n;
        this.estimate = null;
        reset_states();
        this.state = State.NORMAL;
        this.willpropose = true;
    }

    private void reset_states() {
        this.states = new Pair[n]; // initialize array of Pair
        for (int j = 0; j < n; j++) {
            states[j] = new Pair(null, 0);
        }
    }

    private void propose(Boolean v) {
        if (!willpropose) {
            return;
        }
        log.info(this + " - propose(" + v + ")");
        this.proposal = v;
        this.ballot += n;

        reset_states();
        // Send READ to all
        for (ActorRef actor : processes.references) {
            Read r = new Read(ballot);
            actor.tell(r, getSelf());
        }
    }

    public void onReceive(Object message) throws Throwable {
        if (state == State.FAULTY) {
            // Crash with probability alpha
            float r = new Random().nextFloat();
            if (r < this.alpha) {
                log.info(this + " - CRASHED");
                this.state = State.SILENT;
                // getContext().stop(getSelf());
                // return;
            }
        }

        // If the process is silent, stop reacting to messages
        if (state == State.SILENT) {
            return;
        }

        if (message instanceof Membership) {
            log.info(this + " - MEMBERSHIP received");
            Membership m = (Membership) message;
            processes = m;
        } else if (message instanceof Launch) {
            log.info(this + " - LAUNCH received");
            propose(value);
        } else if (message instanceof Read) {
            Read read = (Read) message;
            if (readballot > read.ballot || imposeballot > read.ballot) {
                // Send ABORT to sender
                Abort abort = new Abort(read.ballot);
                getSender().tell(abort, getSelf());
            } else {
                readballot = read.ballot;
                // Send GATHER to senders
                Gather gather = new Gather(read.ballot, imposeballot, estimate, this.i);
                getSender().tell(gather, getSelf());
            }
        } else if (message instanceof Abort) {
            log.info(this + " - ABORT received from " + getSender().path().name());
            // Invoke propose again
            // reset(); // ????
            propose(value);
        } else if (message instanceof Gather) {
            log.info(this + " - GATHER received from " + getSender().path().name());
            count++;
            Gather gather = (Gather) message;
            states[gather.i] = new Pair(gather.est, gather.estballot);

            if (count > n / 2) { // received a majority of responses

                int k = -1;
                int max_estballot = 0;
                for (int j = 0; j < n; j++) {
                    int estb = states[j].estballot;
                    if (estb > max_estballot) {
                        k = j;
                        max_estballot = estb;
                    }
                }
                if (k != -1) {
                    this.proposal = states[k].est;
                }

                reset_states();
                for (ActorRef actor : processes.references) {
                    Impose imp = new Impose(this.ballot, this.proposal);
                    actor.tell(imp, getSelf());
                }

                count = 0;
            }
        } else if (message instanceof Impose) {
            log.info(this + " - IMPOSE received from " + getSender().path().name());
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
            log.info(this + " - ACK received from " + getSender().path().name());
            // Send DECIDE to all
            for (ActorRef actor : processes.references) {
                Decide dec = new Decide(proposal);
                actor.tell(dec, getSelf());
            }
        } else if (message instanceof Decide) {
            log.info(this + " - DECIDE received from " + getSender().path().name());
            Decide decide = (Decide) message;
            // Send DECIDE to all
            for (ActorRef actor : processes.references) {
                Decide dec = new Decide(decide.v);
                actor.tell(dec, getSelf());
            }
            log.info(this + " - decided: " + decide.v);
            state = State.SILENT;
        } else if (message instanceof Crash) {
            log.info(this + " - CRASH received");
            this.state = State.FAULTY;
        } else if (message instanceof Hold) {
            log.info(this + " - HOLD received");
            this.willpropose = false;
        }
    }

    @Override
    public String toString() {
        return "p" + i;
    }

}
