package net.unit8.rodriguez.behavior;

import net.unit8.rodriguez.SocketInstabilityBehavior;

public class NotAccept implements SocketInstabilityBehavior {
    @Override
    public boolean canListen() {
        return true;
    }

    @Override
    public boolean canAccept() {
        return false;
    }
}
