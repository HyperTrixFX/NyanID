package moe.koseirin.nyanruaineo.eventbus.Interface;

/*
 * @author KoseiRin_
 * awa
 */

public interface Cancellable {
    boolean isCancelled();
    void setCancelled(boolean cancelled);
}