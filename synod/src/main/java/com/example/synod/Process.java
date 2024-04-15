/**
 * This file contains the implementation of the Process class.
 * The Process class extends the UntypedAbstractActor class from the Akka library and implements the behavior of a process in the obstruction-free synod algorithm.
 * The process can be in one of three states: NORMAL, FAULTY, or SILENT.
 * It receives messages from other processes and reacts accordingly based on its current state and the type of message received.
 */
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

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this); // Logger attached to actor

    private int n; // number of processes
    private int i; // id of current process
    private double alpha; // probability of crashing
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
    private int ackCounter = 0;

    public static Props createActor(int n, int i, double alpha) {
        return Props.create(Process.class, () -> new Process(n, i, alpha));
    }

    public Process(int n, int i, double alpha) {
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
        this.imposeballot = i - n;
        this.estimate = null;
        reset_states();
        this.state = State.NORMAL;
        this.willpropose = true;
    }

    private void reset_states() {
        this.states = new Pair[n]; 
        for (int j = 0; j < n; j++) {
            states[j] = new Pair(null, 0);
        }
    }

    private void propose(Boolean v) {
        if (!willpropose) {
            return;
        }
        // log.info(this + " - propose(" + v + ")");
        this.proposal = v;
        this.ballot += n;
        count = 0;
        ackCounter = 0;
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
            double r = new Random().nextDouble();
            if (r < this.alpha) {
                // log.info(this + " - CRASHED");
                this.state = State.SILENT;
            }
        }
        // If the process is silent, stop reacting to messages
        if (state == State.SILENT) {
            return;
        }
        if (message instanceof Membership) {
            // log.info(this + " - MEMBERSHIP received");
            Membership m = (Membership) message;
            processes = m;
        } else if (message instanceof Launch) {
            // log.info(this + " - LAUNCH received");
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
            // log.info(this + " - ABORT received from " + getSender().path().name());
            // check abort message
            Abort abort = (Abort) message;
            if (abort.ballot != this.ballot) {
                return;
            }
            // log.info(this + " - ABORT received from " + getSender().path().name());
            propose(value);
        } else if (message instanceof Gather) {

            // check gather message 
            Gather gather = (Gather) message;
            if (gather.ballot != this.ballot) {
                return;
            }

            // log.info(this + " - GATHER received from " + getSender().path().name());
            count++;
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
            // log.info(this + " - IMPOSE received from " + getSender().path().name());
            Impose impose = (Impose) message;
            if (readballot > impose.ballot || imposeballot > impose.ballot) {
                // send ABORT to sender
                Abort a = new Abort(impose.ballot);
                getSender().tell(a, getSelf());
            } else {
                this.estimate = impose.v;
                this.imposeballot = impose.ballot;
                // send ACK to sender
                Ack ack = new Ack(impose.ballot);
                getSender().tell(ack, getSelf());
            }
        } else if (message instanceof Ack) {
            // log.info(this + " - ACK received from " + getSender().path().name());

            // check ack message
            Ack ack = (Ack) message;
            if (ack.ballot != this.ballot) {
                return;
            }
            // log.info(this + " - ACK received from " + getSender().path().name());
            ackCounter++;
            if (ackCounter > n / 2) {
                // send DECIDE to all
                processes.observer.tell(new Decide(proposal), getSelf());
                for (ActorRef actor : processes.references) {
                    Decide dec = new Decide(proposal);
                    actor.tell(dec, getSelf());
                }
                ackCounter = 0;
            }
        } else if (message instanceof Decide) {
            // log.info(this + " - DECIDE received from " + getSender().path().name());
            Decide decide = (Decide) message;
            // send DECIDE to all
            for (ActorRef actor : processes.references) {
                Decide dec = new Decide(decide.v);
                actor.tell(dec, getSelf());
            }
            // log.info(this + " - decided: " + decide.v);
            state = State.SILENT;
        } else if (message instanceof Crash) {
            // log.info(this + " - CRASH received");
            this.state = State.FAULTY;
        } else if (message instanceof Hold) {
            // log.info(this + " - HOLD received");
            this.willpropose = false;
        }
    }

    @Override
    public String toString() {
        return "p" + i;
    }

}
