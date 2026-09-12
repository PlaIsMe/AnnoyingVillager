# AvNpc physical recovery (2026-09-10)

`AVNpc.registerGoals()` installs `EscapeWallGoal`, `EscapeHoleWithBlockGoal`, and
`BreakTargetObstructionGoal` at priority -4, sharing MOVE/LOOK/JUMP. Water-fall and
danger reactions retain higher priority. `ProjectileBlockGoal` and target retargeting
explicitly yield during recovery. Lower priority pearls, bow, melee, and weapon recovery
cannot take these controls during the episode.

`AbstractRecoveryGoal` owns one `AVNpc.beginRecoveryAction(this)` token through the
whole episode, restoring tools and releasing only that token in `stop()`. Start refuses
scripted/non-profile rig playback and another hand lock, but permits ordinary profile
attacks to yield. The existing counted `LockableRigAttackAnimation`
gate remains intact. Episodes are finite (800 server ticks); death, no-AI, passenger,
healing and rig stun abort safely.

`entity.ai.RecoveryAi` adapts SmartNpc's tool swaps, timed mining, block placement,
loaded-world checks and bounded path admission. Work uses real stack exchanges with
inventory, so the combat weapon is never held only in a goal field. Correct-tool drops
and actual held-tool durability apply. Forge mobGriefing/destruction/placement hooks,
world borders, loaded chunks, collision shapes and block-entity protection apply.
Discovery runs at >=20 ticks with jitter; a per-server-level budget admits at most six
path creations per tick. Mining and physical jump settlement are cheap per-tick work.

Combat recovery chooses height first: a valid unreachable target >2 blocks higher can
request a pillar (maximum initial difference 9 blocks); target at the same height/lower
uses obstruction clearing. A passive NPC must prove a roof-free bounded shaft (radius
3, <=24 footprint cells, no walk/step/drop exit, rim <=8 blocks higher). This accepts
1x1, 1x3, 2x3, 3x3 and irregular cavities. It centers within the actual column, jumps
normally, waits until its body clears the placement cell, consumes a carried full block,
and settles on it. No position teleport, material creation, worker lease or job is used.
Only real vertical headroom is mined during ascent; nearby side walls are not a clearing
fallback. Knockback can replan the actual column up to three times.

Obstruction clearing first rejects a reachable path. An eye ray and nine body samples
at equal height select only actual target-passage collisions. It continues below an
opened window until a traversable path exists; visibility alone is not completion.
The current valid block is retained across sample changes; short invalid-ray/reach
intervals pause mining without erasing progress. `AVNpc.setTarget(null)` preserves a
valid current enemy only during the bounded active episode; new retaliation, death,
creative/spectator, alliance and distance changes can still end it.

Wall recovery checks exact collision overlap with the NPC body (including extended
shapes from the cell below). Mere floor/wall contact does not qualify. It also works
without a combat target, e.g. after a pearl lands inside a wall.

Spawn inventory seeding adds an iron pickaxe and axe regardless of difficulty; Steve,
Angry Steve, Alex and Chris receive diamond versions. Existing saved inventories are
not refilled on load. Pillaring requires carried suitable full blocks, including modded
materials and sand/gravel with solid support; it never creates free blocks. The former
material whitelist rejected mossy cobblestone and is removed. Eligibility now uses full
collision, no fluid, and no block entity. Containers/machines remain excluded because
this helper does not transfer block-entity item data. Survival/collision/protection
checks still run before placement.

Recovery diagnostics (2026-09-12): `/avrecoverytrace <single entity selector>` enables
an operator-only 10-minute session for that NPC. `/avrecoverytrace off` stops all sessions.
Example: `/avrecoverytrace @e[type=annoyingvillagers:steve,sort=nearest,limit=1]`.
`AV recovery trace` entries in logs/latest.log sample once per 20 server ticks, including
NPC UUID/position/motion, target/height, native animation/locks, running goal priorities,
interruptibility, carried BlockItems, selected slot (-2=main hand, -1=none), and cached
decisions from actual goal admission/ticks. No extra eligibility/pathfinding/event
probes are executed by logging. Decisions include source tick to identify stale results
when goal-selector controls prevent canUse from being called. Pillaring reports shaft,
path, footing, headroom, centering, jump/landing and actual placement rejection reasons.
Sessions are bounded to eight NPCs, cleared on server stop, and end on removal/timeout;
the server-tick sampler can also expose an NPC whose own tick counter stops advancing.

Rig `DIG_MAINHAND` and `USE_MAINHAND` are appended ids with non-damaging MAIN_HAND
specs and client `RecoveryAnimations` clips. They have no root motion/collider sampling
requirements. Do not change existing ordinal ids or the generated animation directory.

The optional companion uses two Forge events, with no Epic Fight import in core:
`AVNpcRecoveryEvent` (CHECK_START, DIG, STOP_DIG, USE, START) and `AVNpcRigAnimationEvent`
(before playback/hooks/colliders). Recovery digging and healing have synchronized
entity data for the companion renderer. Cancelled CHECK_START postpones recovery;
cancelled DIG/USE gives the companion ownership of that visible action.

Combat recovery fix (2026-09-12): AvNpc `RigAnimatedMeleeAttackGoal` permits normal
goal preemption during an unlocked profile swing. It cancels only its own current
profile clip on stop; delayed collider/motion callbacks check the removed state identity.
Do not require the last attack-window boundary for utility admission: per-tick combo
selection can restart the attack before the selector's next admission pass. Other rig
mob classes retain their existing interruptibility. Normal combo timing is unchanged.
After acquiring its counted lock, `AbstractRecoveryGoal` calls `RecoveryAi.startRecovery`
to cancel residual profile playback and publish START. This covers hole/wall/obstruction
recovery together, including a first pillar jump with no DIG/USE event beforehand.
CHECK_START is the optional integration veto; START is a committed notification.
Epic Fight yields owned ordinary attacks/local guard, while unowned inaction and
explicit combat owner locks (including executions) still defer recovery.

Validation: Java assemble passes, including Punchy class-literal mixin targets with the
existing optional-mod plugin gate. No automated/game tests were run. Gameplay checks:
try the cavity sizes with/without a combat target, a glass window over cobblestone,
and a pearl/body collision; verify real tool consumption/restoration and a single attack
owner both with and without the Epic Fight companion.
