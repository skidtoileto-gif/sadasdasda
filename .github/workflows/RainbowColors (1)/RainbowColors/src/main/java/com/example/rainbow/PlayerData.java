package com.example.rainbow;

import java.util.ArrayList;
import java.util.List;

public class PlayerData {

    public RainbowColor primary;
    public int progress;
    public boolean unlocked;
    /** Colors mixed in from vials (excludes the primary color). */
    public final List<RainbowColor> mixed = new ArrayList<>();

    public PlayerData(RainbowColor primary) {
        this.primary = primary;
    }

    /** Primary color followed by every mixed color. */
    public List<RainbowColor> all() {
        List<RainbowColor> list = new ArrayList<>();
        list.add(primary);
        list.addAll(mixed);
        return list;
    }

    public boolean has(RainbowColor color) {
        return primary == color || mixed.contains(color);
    }
}
