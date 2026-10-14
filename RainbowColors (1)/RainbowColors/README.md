# RainbowColors

A Spigot/Paper plugin (Minecraft 1.21+, Java 21).

## Build
```
mvn clean package
```
The jar is created at `target/RainbowColors.jar`. Drop it in your server's `plugins/` folder.

## How it works
1. **Spawn**: a new player is assigned a random rainbow color on first join (optionally re-rolled on every respawn, see `config.yml`).
2. **Passive power**: each color grants a permanent effect.
3. **Ability**: earn progress to unlock it, then cast with **Shift + F** or `/color ability`.
4. **Mixing**: once your ability is unlocked you can craft a *Vial of <Color> Essence*
   (Glass Bottle + your color's dye + Glowstone Dust, shapeless). Another player with an unlocked
   ability drinks it to mix that color into theirs: they gain its passive and ability, and both
   abilities fire together on Shift + F.

| Color  | Passive                      | Ability          | Unlock by              |
|--------|------------------------------|------------------|------------------------|
| Red    | Fire Resistance              | Inferno          | Kill 25 hostile mobs   |
| Orange | Haste I                      | Sunburst         | Mine 40 ores           |
| Yellow | Speed I                      | Lightning Strike | Travel 3000 blocks     |
| Green  | Jump Boost I, no fall damage | Bloom            | Harvest 150 crops      |
| Blue   | Water Breathing, Dolphin's Grace | Tidal Surge  | Catch 15 fish          |
| Indigo | Night Vision                 | Blink            | Reach level 20         |
| Violet | Health Boost                 | Void Pulse       | Enchant 5 items        |

## Commands
- `/color` / `/color info`: your color, progress and mixes
- `/color ability`: cast your ability
- `/color recipe`: show your vial recipe
- Admin (`rainbow.admin`): `/color set <player> <color>`, `/color unlock <player>`, `/color reset <player>`, `/color vial <color> [player]`

All requirements, cooldowns and the number of mixes allowed are configurable in `config.yml`.
