# Persistent player NPC sessions

PersistentPlayerNpc is an opt-in interface for AVNpc. persistentPlayerIdentity()
returns a logical player name. Steve and Angry Steve both return Steve; Alex and
Chris return their own names. Only one entity UUID per logical identity is accepted
across dimensions. Native rig and EpicFight combat behavior are unchanged.

PersistentPlayerNpcManager owns one moving distance-2 region ticket per session,
using forceTicks=false for both add/remove. This provides entity ticking without
opting remote centers into extra natural spawning/random block ticks. Movement
refresh is staggered every 20 ticks. SavedData stores UUID, identity, dimension and
last center, never runtime ticket objects. Server start restores known centers
and reconciles loaded entities once. Stop saves current centers and releases all
runtime ownership. Unloaded/dimension-changing entities keep their identity;
genuinely stale registered entries are pruned after 30 seconds with a loaded center.

Tab rows use packet-only spectator FakePlayers, [NPC] names and zzAVN synthetic
profiles. They never enter the real PlayerList. Login sends one batched packet;
removal/death/stop clear rows. ClientSuggestionProviderMixin removes these synthetic
names from command completion, independently of SmartNpc's matching filter.

PersistentPlayerNpc.tabSkin() supplies a whitelisted NpcTabSkin. The vanilla tab
initialization profile carries the annoyingvillagers:tab_skin property, so remote
and newly joining viewers do not need the entity loaded locally to resolve its
face. PlayerInfoMixin returns the existing local entity skin texture; no Mojang
profile or skin download is needed. PlayerFaceRendererMixin draws the outer head
layer for these textures even though there is no real Player entity behind the row.
The existing 512x512 skins use the standard 64x64 UV layout. Angry Steve overrides
the visual skin while retaining the logical Steve identity. Real player profiles
fall through unchanged; custom skin metadata is accepted only on matching zzAVN
synthetic profiles. Updated clients and server are needed for this metadata path.

The original PlayerTabOverlay ModifyArgs injection triggered an ArgsClassGenerator
Args$1 class-loading failure during Gui construction in the Forge userdev launch.
It was removed. The replacement uses an ordinary static TAIL injection and a mapped
drawHat shadow, adding the overlay only when vanilla omitted it. It requires no
synthetic argument class and does not redirect the tab renderer's draw invocation.

Steve's transformation copies the unattended timer before discarding the old form.
The old session/claim ends before the new form claims the same logical identity.
The three spawn SavedData owners consult the global session registry and do not
interpret an unloaded entity as a dead entity. Timed departures vacate matching
claims immediately in all dimensions; normal death retains the existing cooldown.
New natural-spawn reservations expire after 30 seconds if no entity ever joins,
while confirmed/legacy owners remain reserved across chunk unloads.

Config annoyingvillagers-server.toml:

```toml
[remoteNpcDeparture]
enabled = true
minMinutes = 10
maxMinutes = 30
```

Timers persist on the entity, advance only online while ticketed and unattended,
and pause without losing progress when disabled or waiting for ticket restoration.
Login/logout does not itself reset or resample the timer. They resume after restart
and reset under player or explicit loader coverage. Vanilla spawn-area START
tickets, transient chunk-read tickets and both mods' NPC tickets do not constitute
attendance. This allows characters at the original spawn to leave after players
travel away. Departure broadcasts the vanilla yellow left-game message, discards
the NPC without drops and lets ordinary natural spawning choose the replacement.
The existing Angry Steve exhaustion timer remains an independent character mechanic.

Migration: characters already loaded on upgrade register automatically. Old
unloaded characters need their existing chunk loaded once: the old singleton save
stored only UUID, not coordinates, so it cannot restore their location by itself.
After registration they restore remotely on subsequent server starts.

Validation: compile/assemble both projects, inspect both accessor refmaps and
packaged mixin registrations. Gameplay checks still require Minecraft: tab replay,
restart, Steve transformation, travel-away departure, external loader reset, and
natural spawning after the slot is released.
