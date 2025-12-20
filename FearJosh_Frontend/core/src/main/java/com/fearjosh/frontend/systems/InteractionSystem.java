package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.entity.Player;
import com.fearjosh.frontend.world.Interactable;
import com.fearjosh.frontend.world.InteractionResult;
import com.fearjosh.frontend.world.Room;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * InteractionSystem - Handles player-interactable interactions.
 * 
 * Responsibilities:
 * - Find closest interactable within range
 * - Process interaction input (E key)
 * - Execute interactions and return results
 */
public class InteractionSystem {
    
    private final float interactRange;
    private Interactable currentInteractable;
    
    public InteractionSystem(float interactRange) {
        this.interactRange = interactRange;
    }
    
    /**
     * Find the closest interactable within range.
     * 
     * @param player The player
     * @param room Current room with interactables
     */
    public void findCurrentInteractable(Player player, Room room) {
        currentInteractable = null;
        float closestDist2 = Float.MAX_VALUE;
        
        for (Interactable inter : room.getInteractables()) {
            if (!inter.isActive()) continue;
            if (!inter.canInteract(player)) continue;
            
            float dx = inter.getCenterX() - player.getCenterX();
            float dy = inter.getCenterY() - player.getCenterY();
            float dist2 = dx * dx + dy * dy;
            
            if (dist2 < closestDist2 && dist2 <= interactRange * interactRange) {
                closestDist2 = dist2;
                currentInteractable = inter;
            }
        }
    }
    
    /**
     * Handle interaction input and execute if valid.
     * 
     * @return InteractionResult if interaction occurred, null otherwise
     */
    public InteractionResult handleInput() {
        if (currentInteractable == null) return null;
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            return currentInteractable.interact();
        }
        
        return null;
    }
    
    /**
     * Get the current highlighted interactable.
     */
    public Interactable getCurrentInteractable() {
        return currentInteractable;
    }
    
    /**
     * Check if there is an interactable in range.
     */
    public boolean hasInteractable() {
        return currentInteractable != null;
    }
}
