package net.unit8.rodriguez.strategy;

import net.unit8.rodriguez.SocketInstabilityStrategy;

public class NotAccept implements SocketInstabilityStrategy {
    @Override
    public boolean canListen() {
        return true;
    }

    @Override
    public boolean canAccept() {
        return false;
    }
}
