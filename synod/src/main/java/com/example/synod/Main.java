package com.example.synod;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.example.synod.message.*;

import java.util.*;

public class Main {
    public static int N = 100;
    public static int f = 49;
    public static float alpha = 0;
    public static int t_le = 2;

    public static void main(String[] args) throws InterruptedException {

        // Parse arguments
        if (args.length != 0) {
            N = args[0] != null ? Integer.parseInt(args[0]) : N;
            f = args[1] != null ? Integer.parseInt(args[1]) : f;
            alpha = args[2] != null ? Float.parseFloat(args[2]) : alpha;
            t_le = args[3] != null ? Integer.parseInt(args[3]) : t_le;
        }

        

        // Instantiate an actor system
        final ActorSystem system = ActorSystem.create("system");
        //system.log().info("System started with N=" + N);
        
        ArrayList<ActorRef> processes = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            final ActorRef a = system.actorOf(Process.createActor(N, i, alpha), "p" + i);
            processes.add(a);
        }

        ActorRef observer = system.actorOf(Observer.createActor(N, alpha, t_le), "observer");

        //give each process a view of all the other processes
        Membership m = new Membership(processes, observer);
        for (ActorRef actor : processes) {
            actor.tell(m, ActorRef.noSender());
        }

        // Sleep for a while to allow each process to register the reference to the others
        //Thread.sleep(100);

        // Send LAUNCH to all processes
        for (ActorRef actor : processes) {
            actor.tell(new Launch(), ActorRef.noSender());
        }
        
        // Select f processes at random and send them a CRASH message
        Collections.shuffle(processes);
        for (int i = 0; i < f; i++) {
            processes.get(i).tell(new Crash(), ActorRef.noSender());
        }

        // Sleep for a while
        Thread.sleep(t_le);

        // Send HOLD to all correct processes but the leader
        for (int i = 0; i < N; i++) {
            if (i != f) { // the process in position f is the leader
                processes.get(i).tell(new Hold(), ActorRef.noSender());
            }
        }

        //Wait before ending system
        /*try {
             waitBeforeTerminate();
         } catch (InterruptedException exp) {
             exp.printStackTrace();
         } finally {
             system.terminate();
         }*/
    }

    public static void waitBeforeTerminate() throws InterruptedException {
        Thread.sleep(5000);
    }
}
