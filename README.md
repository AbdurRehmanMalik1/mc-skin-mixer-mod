# Skin Mixer

Change your skin in game. Mix the head, body, arms and legs of different skins into one outfit, see it
live on a 3D preview, and apply it. Players with the mod see each other's outfits.

Tentative version **0.1.0** for Fabric 26.3, built to evaluate the idea. Design:
[mod-ideas/skin-mixer-design.md](../mod-ideas/skin-mixer-design.md).

## Using it

Press **U** in game (rebindable under *Controls → Skin Mixer*), or open *Options → Skin
Customization → Skin Mixer...*. The menu works like a character creator:

- **Body map** (left): your outfit laid flat. Click the head, body, an arm or a leg to edit it; the part
  being edited is outlined in yellow.
- **3D preview** (middle): zooms in on the part being edited and turns it towards you. Drag to turn it
  yourself.
- **Tabs** (top right): Whole outfit, Head, Body, Arms, Legs, each drawn as that part of your outfit.
- **Choices** (right): that part of every skin, as a picture. Hover one to try it on; click to keep it.
  The gold corner marks your own skin. Scroll the grid when there are more skins than fit.
- **+ card** (end of the grid): type any Minecraft username to add that player's current skin. It is
  fetched from Mojang, the same way vanilla fetches player heads, and kept in
  `config/skinmixer-library.json`. Right-click an added skin to remove it.
- **Arms: Wide / Slim** and **Both sides / Right only / Left only** (under the body map).
- **Randomize**, **Reset to my skin**, **Cancel**, **Apply**.
- Server operators: `/skinmixer reset <players>`. Anyone: `/skinmixer reset` for themselves.

## Testing

| Command | What it does |
|---|---|
| `./gradlew build` | Compiles and runs the unit tests (slicing and arm resizing). |
| `./gradlew runClientGameTest` | Opens a game window and drives the mod by itself: menu, clicks, pixel checks of the baked skin, a dedicated server and reconnecting. Screenshots go to `build/run/clientGameTest/screenshots`. |
| `./gradlew runClient` | Play it yourself (Player1). |
| `./gradlew runServer`, then `runClient` and `runClient2` | A server and two players on one PC, to see each other's outfits. Connect both to `localhost`. The first `runServer` stops at the EULA: set `eula=true` in `run/eula.txt` and `online-mode=false` in `run/server.properties` (dev clients aren't logged in), then start it again. |

## How it works

- **Skins**: the nine default skins every client ships (Steve, Alex, Ari, Efe, Kai, Makena,
  Noor, Sunny, Zuri), each in its wide and slim version, plus **My skin** (the wearer's real skin). Read
  from the player's own game, so no Mojang art is in the jar.
- **Added skins**: any player's skin by username. The outfit stores only the account id
  (`mojang:<uuid>`); every client that sees the outfit fetches that skin from Mojang itself.
- **Outfit** = arm model + one skin name per part. Only names travel and get saved, never pixels.
- **Baking**: each client copies each part's base and overlay rectangles from its source skin into one
  new 64×64 skin, and the game draws that like any other skin (hook: `PlayerInfo.getSkin()`).
- **Server**: stores outfits in `<world>/skinmixer/outfits.json`, sends everyone's outfit on join and
  every change to every player with the mod.

## Known limits of 0.1.0

| Limit | Detail |
|---|---|
| Skin sources | The nine defaults, your own, and any player's skin by username. No importing PNG files. Added skins need internet, and follow that player: if they change their skin, the outfit changes too. |
| Six parts | A part's base layer and overlay (hat, jacket, sleeves, trousers) always move together. |
| Players without the mod | See your real skin. Showing outfits to unmodded players needs MineSkin (design doc §6.3). |
| Per world | A server, and each single player world, keeps its own outfit per player. On a server without the mod, your last outfit is worn but only you see it. |
| Own arms on the other model | Your own wide arms on a slim outfit (or the reverse) are resized by dropping or doubling a pixel column. |
| No permissions | Anyone can change their outfit; only operators can reset others. A change sent within half a second of the last one waits until the half second is up. |
| Player heads | Player head items and blocks still show the real skin. |
