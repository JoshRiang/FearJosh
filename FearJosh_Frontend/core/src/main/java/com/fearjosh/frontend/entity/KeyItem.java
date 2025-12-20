package com.fearjosh.frontend.entity;

/**
 * Key item - used to unlock doors/rooms
 * This is a NON-USABLE item (quest item, used automatically)
 */
public class KeyItem extends Item {

    private String keyId; // Which door this key opens

    public KeyItem(String keyId) {
        super("Key", "Opens locked doors", false); // NOT usable manually
        this.keyId = keyId;
        loadIcon("Items/key.png"); // Assuming key icon exists
    }

    public String getKeyId() {
        return keyId;
    }

    @Override
    public boolean useItem() {
        // Keys are not manually usable
        // They're automatically checked when approaching doors
        System.out.println("[Key] Keys cannot be used manually");
        return false;
    }
}
