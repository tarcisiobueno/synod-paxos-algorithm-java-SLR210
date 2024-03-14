package com.example.synod;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;

import com.example.synod.message.Launch;
import com.example.synod.message.Membership;

import java.util.*;

public class Main extends UntypedAbstractActor {
    public static int N = 3;

    public Main() {
        
    }

    /**
     * Static method to create an actor
     */
    public static Props createActor(int n, int i) {
        return Props.create(Process.class, () -> new Process(n, i));
    }
    public static void main(String[] args) throws InterruptedException {
        // Instantiate an actor system
        final ActorSystem system = ActorSystem.create("system");
        system.log().info("System started with N=" + N );

        ArrayList<ActorRef> processes = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            final ActorRef a = system.actorOf(Process.createActor(N, i));
            processes.add(a);
        }

        //give each process a view of all the other processes
        Membership m = new Membership(processes);
        for (ActorRef actor : processes) {
            actor.tell(m, ActorRef.noSender());
        }

        // processes.get(0).tell(
        //         new Launch(),
        //         ActorRef.noSender());

        // Send LAUNCH to all processes
        for (ActorRef actor : processes) {
            actor.tell(new Launch(), ActorRef.noSender());
        }

    }

    @Override
    public void onReceive(Object message) throws Throwable {
        
    }
}
