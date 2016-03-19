package org.konstructs.basicmachines;

import akka.actor.ActorRef;
import akka.actor.Props;
import konstructs.api.*;
import konstructs.plugin.KonstructsActor;
import konstructs.plugin.PluginConstructor;

import java.util.ArrayList;
import java.util.List;

public class BasicMachineActor extends KonstructsActor {

    View view;
    InventoryView inventoryView;
    Inventory inventory;

    public BasicMachineActor(ActorRef universe) {
        super(universe);

        inventoryView = new InventoryView(2, 4, 1, 1);
        inventory = Inventory.createEmpty(1);
        view = View.Empty().add(inventoryView, inventory);
    }

    @Override
    public void onReceive(Object message) {

        System.out.println("GOT: " + message);

        if (message instanceof InteractTertiaryFilter) {
            onInteractTertiaryFilter((InteractTertiaryFilter)message);
        }

        if (message instanceof PutViewStack) {
            onPutViewStack((PutViewStack)message);
        }

        if (message instanceof RemoveViewStack) {
            onRemoveViewStack((RemoveViewStack)message);
        }

        super.onReceive(message);
    }

    @Override
    public void onEventBlockRemoved(EventBlockRemoved block) {}

    @Override
    public void onEventBlockUpdated(EventBlockUpdated blockEvent) {
    }

    public void onInteractTertiaryFilter(InteractTertiaryFilter filter) {

        if (filter.message().block().get().type().namespace().equals("org/konstructs/basicmachines")) {
            filter.message().sender().tell(new ConnectView(getSelf(), view), getSelf());
        }

        filter.continueWith(filter.message(), getSender());
    }

    public void onPutViewStack(PutViewStack stack) {

        // Translate the selected position to our inventories local positions.
        int pos = inventoryView.translate(stack.to());
        inventory = inventory.withSlot(pos, stack.stack());

        sendViewStateToPlayer();
    }

    public void onRemoveViewStack(RemoveViewStack stack) {
        int pos = inventoryView.translate(stack.from());
        getSender().tell(new ReceiveStack(inventory.stackOption(pos).get()), getSelf());
        inventory = inventory.withoutSlot(pos);

        sendViewStateToPlayer();
    }

    public void sendViewStateToPlayer() {
        view = view.Empty().add(inventoryView, inventory);
        getSender().tell(new UpdateView(view), getSelf());
    }

    @PluginConstructor
    public static Props props(String pluginName,
                              ActorRef universe) {

        return Props.create(BasicMachineActor.class, universe);
    }
}
