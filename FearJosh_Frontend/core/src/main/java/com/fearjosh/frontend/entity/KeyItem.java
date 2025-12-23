package com.fearjosh.frontend.entity;

/**
 * Key item - used to unlock doors/rooms
 * This is a NON-USABLE item (quest item, used automatically)
 * 
 * KEY TYPES (based on game flow):
 * - LOCKER_KEY: Found in Classroom 1A, opens special locker in hallway
 * - JANITOR_KEY: Found in special locker, opens janitor room
 * - GYM_KEY: Found in janitor room, opens gym back door for escape
 */
public class KeyItem extends Item {

    /**
     * Key type enum for the escape sequence
     */
    public enum KeyType {
        LOCKER_KEY("Kunci Loker", "Membuka loker khusus di koridor", "Items/locker_key.png"),
        JANITOR_KEY("Kunci Ruang Penjaga", "Membuka pintu ruang penjaga", "Items/janitor_key.png"),
        GYM_KEY("Kunci Gym", "Membuka pintu belakang gym - jalan keluarmu!", "Items/gym_key.png"),
        GENERIC("Kunci", "Membuka pintu terkunci", "Items/key.png");

        private final String displayName;
        private final String description;
        private final String iconPath;

        KeyType(String displayName, String description, String iconPath) {
            this.displayName = displayName;
            this.description = description;
            this.iconPath = iconPath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getIconPath() {
            return iconPath;
        }
    }

    private KeyType keyType;
    private String keyId; // Legacy support - which door this key opens

    /**
     * Create a key item with specific type
     */
    public KeyItem(KeyType keyType) {
        super(keyType.getDisplayName(), keyType.getDescription(), false); // NOT usable manually
        this.keyType = keyType;
        this.keyId = keyType.name().toLowerCase();
        loadIcon(keyType.getIconPath());
    }

    /**
     * Legacy constructor for backward compatibility
     */
    public KeyItem(String keyId) {
        super("Kunci", "Membuka pintu terkunci", false); // NOT usable manually
        this.keyId = keyId;
        this.keyType = KeyType.GENERIC;
        loadIcon("Items/key.png"); // Assuming key icon exists
    }

    public String getKeyId() {
        return keyId;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    /**
     * Check if this key matches a required key type
     */
    public boolean matches(KeyType required) {
        return this.keyType == required;
    }

    /**
     * Check if this key matches a required key id string
     */
    public boolean matches(String requiredKeyId) {
        if (requiredKeyId == null) return false;
        return this.keyId.equalsIgnoreCase(requiredKeyId) || 
               this.keyType.name().equalsIgnoreCase(requiredKeyId);
    }

    @Override
    public boolean useItem() {
        // Keys are not manually usable
        // They're automatically checked when approaching doors
        System.out.println("[Key] Keys cannot be used manually - approach a locked door to use");
        return false;
    }

    @Override
    public String toString() {
        return keyType.getDisplayName() + " (" + keyId + ")";
    }
}

