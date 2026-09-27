# ClientSideNoteblocks
Remove lag caused by ping when playing the noteblocks in Minecraft. 

# Download
Curseforge: https://www.curseforge.com/minecraft/mc-mods/client-side-noteblocks

Modrinth: https://modrinth.com/mod/clientsidenoteblocks

Maintainers: see [Publishing](.github/PUBLISHING.md) for the manual release workflow and token setup.

# About 
So I was trying to play a noteblock song in skywars, but playing the song with all of the lag that was caused by my ping made it too hard to play. In the past, I would just mute my sounds in-game and play. This mod will eliminate the lag by having the client play the sound. This also even helps reduce lag when playing noteblocks in singleplayer!

Example (Video I made using this mod):
https://youtu.be/5kv47QA2lTo

To use this mod simply add the mods to your mods folder. You will also need to install the fabric API.
The mod will automatically prevent unwanted sounds -- duplicate sounds from the server and block break sounds on noteblocks from playing. Once you install the mod it will immediately work and nothing needs to be configured!

If you want to support me you can subscribe to my youtube channel: https://www.youtube.com/DaCubeKing
If you use this mod while making a video it would be greatly appreciated if you link to this mod and/or my youtube channel in the description. :)

# FAQ
## Can you make this for 1.8?
No, this mod uses block data of noteblocks that are not sent to the client pre 1.13. This data also allows you to use a texture pack to display the current note of noteblock. 
## What does this mod actually do?
To play a noteblock normally in Minecraft (by clicking on it), the game first has to send a packet to the server that you've clicked the block. The server then has to process that packet and sees that a sound needs to be played. Once it sees that a sound needs to be played it sends a packet back to the client to tell it to play a sound. Only once the client receives this packet does it actually start playing the sound. The time for this could reach into the hundreds of ms (depending on your ping to the server) and can make it difficult to play a noteblock song as I did in the video. This mod solves the issue by cutting out the server when trying to play a noteblock. With this mod installed the client checks if you've clicked a noteblock and if you have plays the sound immediately. It also filters out the sounds that the server still sends to the client and the sounds that you get from breaking a block.
## Why isn't this mod working on X server?
This mod will not work on some servers that use a protocol hack (ex. ViaVersion running on a proxy) because this mod requires the proper block data to be sent to the client. (Hypixel does work though)
