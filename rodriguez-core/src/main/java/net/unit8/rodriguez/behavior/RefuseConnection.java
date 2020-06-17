package net.unit8.rodriguez.behavior;

import net.unit8.rodriguez.SocketInstabilityBehavior;

public class RefuseConnection implements SocketInstabilityBehavior {
    @Override
    public boolean canListen() {
        return false;
    }
}
