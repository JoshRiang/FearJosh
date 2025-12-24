package com.fearjosh.frontend.entity;

public class ChocolateItem extends Item {

    private static final float STAMINA_RESTORE_AMOUNT = 0.30f; // 30% stamina restore

    public ChocolateItem() {
        super("Cokelat", "Mengembalikan stamina", true);
        loadIcon("Items/chocolate.png");
    }

    public float getStaminaRestoreAmount() {
        return STAMINA_RESTORE_AMOUNT;
    }

    @Override
    public boolean useItem() {
        System.out.println("[Chocolate] Item used - will restore stamina");
        return true;
    }
}
