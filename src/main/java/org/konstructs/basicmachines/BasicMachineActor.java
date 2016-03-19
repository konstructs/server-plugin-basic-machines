package org.konstructs.basicmachines;

import akka.actor.ActorRef;
import akka.actor.Props;
import konstructs.api.*;
import konstructs.plugin.KonstructsActor;
import konstructs.plugin.PluginConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicMachineActor extends KonstructsActor {

    static final String PLUGIN_NS = "org/konstructs/basicmachines";

    View view;
    InventoryView inventoryView;
    Inventory inventory;
    Map<Position, ActorRef> playerActorMapping;

    public BasicMachineActor(ActorRef universe) {
        super(universe);

        playerActorMapping = new HashMap<>();
        inventoryView = new InventoryView(2, 4, 1, 3);
        inventory = Inventory.createEmpty(3);
        view = View.Empty().add(inventoryView, inventory);
    }

    @Override
    public void onReceive(Object message) {

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
    public void onEventBlockUpdated(EventBlockUpdated blockEvent) {}

    @Override
    public void onBlockViewed(BlockViewed blockPosition) {
        if (blockPosition.block().type().namespace().equals(PLUGIN_NS)) {
            if (playerActorMapping.containsKey(blockPosition.pos())) {
                playerActorMapping.get(blockPosition.pos())
                        .tell(new ConnectView(getSelf(), view), getSelf());
            }
        }
    }

    public void onInteractTertiaryFilter(InteractTertiaryFilter filter) {
        if (filter.message().pos().isDefined()) {
            // Save actor ref for later and ask the server for the block
            playerActorMapping.put(filter.message().pos().get(), filter.message().sender());
            viewBlock(filter.message().pos().get());
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
