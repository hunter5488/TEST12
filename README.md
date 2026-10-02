# Fluid Combat

A melee combat overhaul for **Minecraft 1.21.1 / NeoForge**. It replaces vanilla's instant "click = hit" melee with
committed, animated attacks — wind-up, swing, recovery — plus combos, heavy and dash attacks, guarding, parrying,
dodging, stamina and stagger. The same system drives **players, vanilla weapon-wielding mobs and modded NPCs**.

## Features

- **Fluid procedural animations.** Keyframed animations with per-segment easing (anticipation, snap, overshoot)
  play on the model and blend in quaternion space. Chained moves, interrupts and the return to idle cross-fade from
  the pose currently shown, so nothing snaps. Walking, head-tracking and sneaking keep working underneath attacks.
- **First-person animations** for the held weapon, plus subtle camera sway that follows your swings, and screen
  shake / hit-stop on impact.
- **Movesets per weapon type:** sword, axe (also pickaxes/shovels/hoes), heavy (mace), spear (trident) and fists.
  Each has a light combo, a heavy attack and a dash attack.
- **Real swept hit detection.** Each swing sweeps an arc across its active frames, so a horizontal slash can hit
  several enemies, an overhead only hits what's in front of you, and the sword heavy is a full 360° spin.
- **Defence:** hold guard to block frontal damage (stamina drains on every block, and a guard break leaves you
  staggered). Raise your guard just before a hit to **parry** it and stagger the attacker. **Dodge** gives
  invulnerability frames; dodging *through* an attack refunds stamina.
- **Stamina and stagger.** Attacks, dodges and blocks cost stamina; running dry slows your swings. Heavy and
  finisher hits stagger and interrupt; light hits interrupt wind-ups unless the move has hyper-armour.
- **Mobs fight back properly.** Vanilla melee mobs that hold weapons — zombies, husks, drowned, zombified piglins,
  piglins, piglin brutes, vindicators, wither skeletons, sword-wielding skeletons and strays — use the same
  movesets. They chain combos, open with heavies, lunge in with dash attacks, read your wind-ups to guard or sidestep,
  and counter-attack after blocking. Skilled mobs (brutes, vindicators...) defend more often than clumsy ones (zombies).

## Controls

| Action | Input |
| --- | --- |
| Light attack / combo | Attack (left click) |
| Heavy attack | Sneak + attack |
| Dash attack | Sprint + attack |
| Guard / parry | `R`, or hold use (right click) with a melee weapon and an empty off hand |
| Dodge | `Left Alt` + a movement key (forward rolls, sideways/back hop) |

Left-clicking a block still mines it, and attacking non-living things (item frames, boats, end crystals) keeps
vanilla behaviour. Right-click guarding is skipped when looking at a block, a friendly entity, or when the off hand
holds something, so doors, chests, trading and off-hand placing all still work. Shields keep working as normal.
Every key can be rebound under *Controls → Fluid Combat*.

## Universal NPC support

There is nothing to integrate for most mods:

- **Animations** apply to every entity rendered with a `HumanoidModel` (this includes `PlayerModel` and
  practically every player-shaped NPC model) or an `IllagerModel`.
- **Combat behaviour** applies to every `Mob` that melees through `Mob#doHurtTarget` (the vanilla path that
  `MeleeAttackGoal`, brain-based melee and most NPC mods use) while holding a melee weapon and having a roughly
  humanoid body. A fallback also catches mobs whose own attack code bypasses `doHurtTarget`.
- **Tags** (datapack-overridable):
  - `fluidcombat:combatants` (entity type) — use the system even when unarmed (punching)
  - `fluidcombat:excluded` (entity type) — always keep vanilla combat
  - `fluidcombat:skilled` / `fluidcombat:clumsy` (entity type) — defend more / less often
  - `fluidcombat:movesets/sword|axe|heavy|spear` (item) — force an item's moveset
  - `fluidcombat:excluded` (item) — items that keep vanilla combat

For custom AI, use `com.fluidcombat.api.FluidCombatAPI` (server side):

```java
FluidCombatAPI.attack(npc, target, AttackType.HEAVY);
FluidCombatAPI.setGuarding(npc, true);
FluidCombatAPI.dodge(npc, DodgeDirection.LEFT);
FluidCombatAPI.isAttacking(npc);
FluidCombatAPI.registerMoveset(WeaponCategory.SWORD, myMoveset);   // plus registerAnimation(...) on both sides
```

## Configuration

`config/fluidcombat-common.toml` covers gameplay: toggling players/mobs, damage multipliers, mob attack speed,
combo/heavy/defence chances, stamina, the parry window and dodge invulnerability. It is also editable in-game from
the mod list.

`config/fluidcombat-client.toml` covers presentation: first-person and model animations, camera shake and sway,
the stamina bar, and right-click guarding.

The mod must be installed on both the client and the server.

## Building

Requires JDK 21.

```sh
./gradlew build        # jar in build/libs
./gradlew test         # unit tests for the timeline, keyframe and rotation maths
./gradlew runClient    # dev client
```

## How it works

- `combat/` — the server-authoritative timeline (`CombatLogic`, `CombatManager`): wind-up, active and recovery
  phases, input buffering and combo chaining, the swept-arc hit detection, guard/parry/dodge/stagger, and stamina.
  Player hits are delivered through vanilla `Player#attack`, so enchantments, crits, durability, mace smashes and
  stats all still apply. Vanilla sweeping is replaced by the arcs.
- `anim/` — the animation data (`Animations`), keyframe sampling and easing, and first-person curves.
- `client/` — `ClientCombat` predicts your own attacks so input is instant, then reconciles with the server.
  `AnimController` layers stances and actions and handles the cross-fades. `PoseApplier` writes the result onto
  models. Arms are posed as pitch, yaw and twist, then converted to the model's ZYX euler angles with a decomposition
  that stays stable at gimbal lock.
- `mixin/` — two small injections: one turns `Mob#doHurtTarget` into a combat-system attack, and one applies poses
  right after `setupAnim` in `LivingEntityRenderer`.
