# 🚂 Locomotion

(Formerly known as Trainguy's Animation Overhaul)

Locomotion is a mod/animation API for Minecraft centered around giving the game's entities and blocks complex gameplay-driven animations through a real-time animation system inspired by Unreal Engine's Animation Blueprints. Currently included in the Moonflower suite of mods.

> **Warning!**
> This project is still in heavy development! You are free to compile yourself and try it out, but keep in mind that there will be missing animations, placeholders, and debugging visuals that will not look correct in a normal gameplay context.

## 📜 Planned Features

- 🟩 Complete
- 🟨 High Priority
- 🟥 Low Priority
- ❌ Currently out-of-scope (not permanently though!)

| Feature                         | Status | Notes                                                                                                                                                                                         |
|:--------------------------------|:-------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Pose Function System            | 🟩     | Implementation of state machines, blend spaces, and montage tracks.                                                                                                                           |
| PyQT Maya Exporter              | 🟩     | Tool for exporting animations out of Maya with new format with scale support.                                                                                                                 |
| First Person Player Animations  | 🟨     | The first proper stress-test of the system.                                                                                                                                                   |
| In-game configuration           | 🟨     | Settings for tweaking individual aspects of different joint animators.                                                                                                                        |
| Block / Block Entity Animations | 🟥     | Would like to re-add support for block animations, similar to the earlier implementation with Pollen, after first person animations are far enough along.                                     |
| Third Person Player Animations  | 🟥     | Whether or not this will be included with the release version or not is TBD.                                                                                                                  |
| Back-porting                    | 🟥     | Depends on the demand, given that this mod is intended to be used on vanilla-ish versions of the game and usually people playing vanilla don't often play older versions.                     |
| Synchronised Sound              | ❌      | I don't know how the sound system works currently, or what it would take to make sounds trigger with animations without breaking other sound mods, but it's something I'm keeping in mind.    |
| Open API for Modding            | 🟥      | I would like to lock down the design of the animation systems further before considering making this an open API                                                                              |
| Entity Animations               | ❌      | Too high-scope to do on my own at this juncture, requires a large amount of animations/character rigs. Functionality will support it if I were to find somebody to help out on this.          |
| Data-Driven Joint Animators     | ❌      | Design would need to be locked down enough prior to considering this.                                                                                                                         |

## 🔗 Socials
- My Discord server: _Work-in-progress, will reopen when test versions become available._
- My Twitter: https://twitter.com/Trainguy9512
- Discord contact: @trainguy9512
- Moonflower Website: https://moonflower.gg/
- Moonflower Twitter: https://www.moonflower.gg/twitter
- Moonflower Discord: https://www.moonflower.gg/discord

## 📘 Credits
- Lead Development, Rigging, Animation
  - [James Pelter (Trainguy9512)](https://x.com/Trainguy9512)
- Timeline and easing system
  - [Marvin Schürz](https://twitter.com/minetoblend)
- Contributors
  - [TomB-134](https://github.com/TomB-134)
  - [AlphaKR93](https://github.com/AlphaKR93)
  - [LizIsTired](https://github.com/LizIsTired)
  - [CaioMGT](https://github.com/CaioMGT)
  - [Superpowers04](https://github.com/superpowers04)
- Special thanks to members of the Moonflower team for supporting my development on this and helping answer my questions!

## 🧵 Usage and Contribution
- Pull requests are welcome! I would love to make this mod a use-able API for other mod developers, so any input from others on the API design is very valuable to me!
- You may not upload or publish compiled versions, forks, or ports of the mod to public sites like Curseforge or Modrinth **without explicit written permission**.
- You can read the license [here](https://github.com/Trainguy9512/trainguys-animation-overhaul/blob/master/LICENSE)
  - Code portions of the project are under GNU General Public License v3, while all resources/non-code assets are under All Rights Reserved.

## 🔍 FAQ

- What versions of the game will this mod support?
> For right now, the mod is being worked on in the latest release version of Minecraft: Java Edition. Minecraft: Bedrock Edition will not be supported due to it having a fundamentally different modding API. 
> 
> Closer to release, the plan is to gear the project towards supporting multiple game versions and mod loaders at once. Whether or not there will be backports is TBD, but if there's the demand for particular versions please let me know.
- What mod loaders will this mod be compatible with?
> Right now this is being developed with Fabric, for the sake of easy prototyping, but I would like to expand it to Quilt, NeoForge, and Forge further along in development.
- What will the mod require as a dependency?
> Right now, just the Fabric API. This may change though as I'm not using that much fabric-specific functionality.
- What is this mod compatible with?
> Currently there is no official list of what will or will not work, but generally most cosmetic vanilla-friendly mods like Essential, 3D Skin Layers, and other cosmetic mods should work perfectly fine. I'm looking into adding support for mods that use the playerAnimator library like Better Combat or Emotecraft. 
>
> Additionally, right now there are no plans to implement compatibility with heavier content mods which would have their own player interaction animations (depends on the implementation).
> 
> Resource packs that change animations like Fresh Animations should be compatible, but animations added for the same kind of thing by both this mod and a resource pack will not work together.

If you feel this FAQ is missing anything or you have any additional questions, please let me know by sending a message request to my Discord account, `Trainguy9512`
