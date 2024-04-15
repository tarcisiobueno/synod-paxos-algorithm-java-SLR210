package com.example.synod.message;

import akka.actor.ActorRef;
import java.util.ArrayList;
import java.util.List;

public class Membership {

    public List<ActorRef> references;
    public ActorRef observer;

    public Membership(List<ActorRef> references, ActorRef observer) {
        this.references = references;
        this.observer = observer;
    }

}
