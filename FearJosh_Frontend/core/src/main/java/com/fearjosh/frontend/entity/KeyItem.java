package com.fearjosh.frontend.entity;

public class KeyItem extends Item {

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
    private String keyId;

    public KeyItem(KeyType keyType) {
        super(keyType.getDisplayName(), keyType.getDescription(), false); // NOT usable manually
        this.keyType = keyType;
        this.keyId = keyType.name().toLowerCase();
        loadIcon(keyType.getIconPath());
    }

    public KeyItem(String keyId) {
        super("Kunci", "Membuka pintu terkunci", false); // NOT usable manually
        this.keyId = keyId;
        this.keyType = KeyType.GENERIC;
        loadIcon("Items/key.png");
    }

    public String getKeyId() {
        return keyId;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public boolean matches(KeyType required) {
        return this.keyType == required;
    }

    public boolean matches(String requiredKeyId) {
        if (requiredKeyId == null) return false;
        return this.keyId.equalsIgnoreCase(requiredKeyId) || 
               this.keyType.name().equalsIgnoreCase(requiredKeyId);
    }

    @Override
    public boolean useItem() {
        System.out.println("[Key] Keys cannot be used manually - approach a locked door to use");
        return false;
    }

    @Override
    public String toString() {
        return keyType.getDisplayName() + " (" + keyId + ")";
    }
}

