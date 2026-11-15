# Skin Mixer: online skin list

This branch holds only [`skins.json`](skins.json), the list the Skin Mixer skin browser adds to its own
when a player's `config/skinmixer-client.json` points at it:

```
https://raw.githubusercontent.com/AbdurRehmanMalik1/mc-skin-mixer-mod/catalog/skins.json
```

It is the same file for every Minecraft version and loader. Players pick up changes the next time they
start the game (GitHub can take about 5 minutes to serve a new version). Keep this branch and the repo
public, and never rename the branch: players' games ask for this exact address.

## Adding a skin

1. Only list accounts whose skins are meant for everyone to use, or whose owners agreed.
2. Get the account id: `https://api.mojang.com/users/profiles/minecraft/<name>`.
3. Add `{ "name": "<name>", "id": "<id>" }` to a category. Names can change hands; ids can't, and the
   browser needs no extra lookup with one.
4. Check the file is valid JSON before committing. A broken file harms nothing (players keep their last
   good copy), but nobody gets the changes until it's fixed.

## Format

```json
{ "categories": [
    { "id": "blocks", "name": "Block heads", "use": "head", "skins": [
        { "name": "MHF_Cake", "id": "afb489c49fc848a498f2dd7bea414c9a" } ] } ] }
```

- `id`: lowercase letters, digits, `-` and `_`, up to 32. A category with the same id as one in the mod
  (`famous`, `mobs`) adds its skins to it.
- `name`: shown in the browser, up to 32 characters.
- `"use": "head"`: the skins only draw a head; the browser shows and wears them as heads.
- Limits: 50 categories, 500 skins each, 256 KB for the whole file.

## What's here

| Category | Skins |
|---|---|
| Mob heads (29, added to the mod's own 24) | allay, bat, bee, drowned, ender dragon, endermite, evoker, fox, goat, guardian, horse, husk, mooshroom, panda, parrot, piglin, pufferfish, rabbit, shulker, silverfish, sniffer, snow golem, strider, turtle, vex, vindicator, warden, witch, wolf |
| Block heads (25) | cactus, cake, chest, coconuts, melon, oak log, presents, pumpkin, TNT; bedrock, bookshelf, coal, furnace, glowstone, grass, iron, jukebox, lantern, netherrack, redstone, stone, crafting table |
| Arrows and signs (6) | arrows, exclamation and question marks |

All are `MHF_` head accounts. The original Marc's Head Format set (the first 12 block heads and the
arrows and signs) was made so anyone can use its skins as heads. The rest are newer `MHF_` accounts made
by others: each was kept only after its current skin was looked at and clearly showed what its name says.
11 others were left out because they didn't (a person's face, a different block, or nothing
recognisable).

## Keeping it clean

These accounts belong to their owners, who can change the skin at any time; the browser always shows
the current one. If one turns into something else, delete its line here: everyone stops seeing it
within a game session.
