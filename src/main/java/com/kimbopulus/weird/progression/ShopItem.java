package com.kimbopulus.weird.progression;

public enum ShopItem {
    SANCTUARY("Sanctuary Permit", "Unlock the protected 2 x 2 sanctuary.", 80),
    RAIN_BARREL("Rain Barrel", "Rain adds 50% more moisture.", 60),
    RICH_COMPOST("Rich Compost", "Compost adds 50% more fertility.", 60);

    private final String title;
    private final String description;
    private final int cost;

    ShopItem(String title, String description, int cost) {
        this.title = title;
        this.description = description;
        this.cost = cost;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public int cost() {
        return cost;
    }
}
