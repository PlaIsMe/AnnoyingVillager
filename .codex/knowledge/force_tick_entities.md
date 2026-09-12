# Force-ticked mobs

ForceTickEntity is a marker contract separate from PersistentPlayerNpc. It grants
remote entity ticking only, without tab profiles, singleton identity or automatic
left-game/departure behavior. ForceTickEntityManager owns the runtime lifecycle.

Enabled by default with the top-level COMMON configuration setting:

```toml
# config/annoyingvillagers-server.toml
forceTickMobs = true
```

Implemented on HerobrineMob (all its subclasses inherit it), HerobrineGregEntity,
BlueDemonEntity, BbqEntity and JevEntity. The explicitly requested NullWeapon,
HerobrineDragonEntity, HerobrineWardenEntity and EliteHerobrineKnockedEntity do not
extend HerobrineMob, so they implement it independently. Low clones that do not
extend HerobrineMob are not opted in.

Each entity UUID owns one moving distance-2 region ticket with forceTicks=false,
providing level-31 entity ticking without extra remote spawning/random ticks.
Refreshes are staggered every 20 server ticks. Center updates dirty SavedData only
when changed. Saved UUID/dimension/center coordinates restore tickets on restart;
runtime ticket objects are not saved. Movement, dimension transfer, permanent
removal, config disable and server stop release the old ticket.

Disabling the config releases runtime tickets on the next server tick, retaining
restoration records. Enabling reconciles loaded mobs once and restores unloaded
entries on their staggered refresh. A missing entity is pruned after its restored
center has remained loaded for 30 seconds. Death animation ticks remain alive
until actual entity removal. AllowDespawn suppresses vanilla distance despawning
only while enabled; it does not persist PersistenceRequired or cancel explicit
recall/discard/death or entity-specific lifetime logic.

The two managers share the established AV NPC TicketType. Existing SmartNpc/AV
departure checks therefore exclude these mob-owned tickets from external
attendance. UUID ownership and the PersistentPlayerNpc exclusion prevent duplicate
tickets if a future entity implements both interfaces.

Existing loaded mobs register automatically. Previously unloaded mobs from before
this feature need their chunk loaded once before coordinates can be restored later.

Validation: assemble and inspect packaged marker/manager/data classes; gameplay
checks require Minecraft (remote movement, dimension transfer, reload/config
toggle, restart and explicit death/recall cleanup).
