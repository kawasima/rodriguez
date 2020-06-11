package net.unit8.rodriguez.strategy;

import net.unit8.rodriguez.SocketInstabilityStrategy;

public class RefuseConnection implements SocketInstabilityStrategy {
    @Override
    public boolean canListen() {
        return false;
    }
}
