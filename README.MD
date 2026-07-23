# RaddItems

RaddItems lets you create custom items and item sets through YAML.

This guide explains the basic structure of `items.yml` and shows how to build items, effects, triggers, and sets step by step.

## File Structure

The file has two main sections:

- `items` for custom item definitions
- `sets` for set bonuses

```yml
items: {}
sets: {}
```

## Item IDs and Set IDs

The ID of an item or set is the YAML key itself.

```yml
items:
  battle_sword:
    material: DIAMOND_SWORD

sets:
  guardian_set:
    pieces:
      - guardian_helmet
      - guardian_chestplate
```

In this example:
- item id = `battle_sword`
- set id = `guardian_set`

These IDs can also be used in placeholders such as:
- `%item_id%`
- `%set_id%`

## Creating an Item

Each entry inside `items:` creates one custom item.

```yml
items:
  example_item:
    material: DIAMOND_SWORD
    name: "&5Example Sword"
```

A custom item can use:
- `material`
- `name`
- `lore`
- `custom_model_data`
- `unbreakable`
- `item_flags`
- `enchantments`
- `attributes`
- `equip_effects`
- `triggers`

## Basic Item Fields

### Material

The base item type.

```yml
material: DIAMOND_SWORD
```

Examples:
- `DIAMOND_SWORD`
- `SHIELD`
- `APPLE`
- `NETHERITE_HELMET`

### Name

The display name of the item.

```yml
name: "&6Battle Sword"
```

Supports legacy color codes such as `&a`, `&6`, `&c`, and more.

### Lore

Lore is a list of text lines shown on the item.

```yml
lore:
  - "&7A powerful weapon."
  - "&eForged for battle."
```

### Custom Model Data

Used for resource pack models.

```yml
custom_model_data: 10
```

Use `0` if you do not need it.

### Unbreakable

```yml
unbreakable: true
```

### Item Flags

Item flags let you hide parts of the item tooltip.

```yml
item_flags:
  - HIDE_ATTRIBUTES
  - HIDE_UNBREAKABLE
  - HIDE_ENCHANTS
```

## Enchantments

Enchantments are written as a list.

```yml
enchantments:
  - type: SHARPNESS
    level: 3
```

## Attributes

Attributes let items give extra stats.

Common examples:
- `GENERIC_MAX_HEALTH`
- `GENERIC_ARMOR`
- `GENERIC_ARMOR_TOUGHNESS`
- `GENERIC_ATTACK_DAMAGE`
- `GENERIC_ATTACK_SPEED`
- `GENERIC_MOVEMENT_SPEED`
- `GENERIC_SCALE`
- `GENERIC_KNOCKBACK_RESISTANCE`

Supported operations:
- `ADD_NUMBER`
- `ADD_SCALAR`
- `MULTIPLY_SCALAR_1`

Example:

```yml
attributes:
  - attribute: GENERIC_ATTACK_DAMAGE
    amount: 4.0
    operation: ADD_NUMBER
    slot: HAND
```

## Slots

Items can be active in different slots:

- `HEAD`
- `CHEST`
- `LEGS`
- `FEET`
- `HAND`
- `OFF_HAND`
- `HOTBAR`

Sets may also use:
- `ANY`

What they mean:
- `HEAD`, `CHEST`, `LEGS`, `FEET`: armor slots
- `HAND`: selected main hand item
- `OFF_HAND`: offhand item
- `HOTBAR`: any hotbar slot, even if not selected

## Equip Effects

`equip_effects` are persistent potion effects that stay active while the item is active.

```yml
equip_effects:
  enabled: true
  active_slots:
    - HAND
  potion_effects:
    - type: SPEED
      amplifier: 0
      ambient: true
      particles: false
      icon: true
```

Notes:
- `enabled` turns the persistent effect system on or off
- `active_slots` decides where the item must be active
- these effects stay active while the item stays active
- do not add `duration` here

## Triggers

Supported item triggers:
- `on_equip`
- `on_unequip`
- `on_hit`
- `on_kill`
- `on_consume`

### Trigger Example

```yml
triggers:
  on_hit:
    enabled: true
    potion_effects_self: []
    potion_effects_target:
      - type: WEAKNESS
        duration: 100
        amplifier: 0
        ambient: false
        particles: true
        icon: true
    commands:
      - 'tellraw %player% {"text":"You hit %target%","color":"red"}'
```

## Trigger Effects

Trigger potion effects are temporary effects applied when a trigger happens.

### Effects on Self

```yml
potion_effects_self:
  - type: SPEED
    duration: 100
    amplifier: 1
    ambient: false
    particles: true
    icon: true
```

### Effects on Target

```yml
potion_effects_target:
  - type: WEAKNESS
    duration: 100
    amplifier: 0
    ambient: false
    particles: true
    icon: true
```

Notes:
- `duration` is required for trigger effects
- duration uses ticks
- `20 ticks = 1 second`
- `amplifier: 0` = level I
- `amplifier: 1` = level II

## Sets

Each entry inside `sets:` creates one item set.

A set can use:
- `pieces`
- `required_amount`
- `attributes`
- `potion_effects`
- `triggers`

### Set Example

```yml
sets:
  guardian_set:
    pieces:
      - guardian_helmet
      - guardian_chestplate
      - guardian_leggings
      - guardian_boots
    required_amount: 4
```

## Set Pieces

`pieces` is the list of item IDs that belong to the set.

`required_amount` is how many active pieces are needed.

Example:
- If a set has 4 pieces and `required_amount: 2`, any 2 active pieces will activate it
- If `required_amount: 4`, the full set is required

## Set Attributes

Sets can also grant attributes while active.

```yml
attributes:
  - attribute: GENERIC_MAX_HEALTH
    amount: 4.0
    operation: ADD_NUMBER
    slot: ANY
```

## Set Potion Effects

Set potion effects are persistent effects that stay active while the set remains active.

```yml
potion_effects:
  - type: NIGHT_VISION
    amplifier: 0
    ambient: true
    particles: false
    icon: true
```

Do not add `duration` here.

## Set Triggers

Supported set triggers:
- `on_activate`
- `on_deactivate`

Example:

```yml
triggers:
  on_activate:
    enabled: true
    potion_effects_self:
      - type: ABSORPTION
        duration: 100
        amplifier: 0
        ambient: false
        particles: true
        icon: true
    commands:
      - 'tellraw %player% {"text":"Set activated: %set_id%","color":"gold"}'
  on_deactivate:
    enabled: true
    potion_effects_self: []
    commands:
      - 'tellraw %player% {"text":"Set deactivated: %set_id%","color":"gray"}'
```

## Placeholders

Common placeholders:
- `%player%` -> player name
- `%target%` -> target name, if there is one
- `%item_id%` -> item ID
- `%set_id%` -> set ID
- `%slot%` -> slot involved

## Commands

Commands are usually executed from console.

Example:

```yml
commands:
  - 'tellraw %player% {"text":"Hello","color":"green"}'
```

## Consumable Items

`on_consume` works with normal consumable items such as:
- `APPLE`
- `BREAD`
- `POTION`
- `MILK_BUCKET`
- `COOKED_BEEF`

## Minimal Example

```yml
items:
  simple_sword:
    material: DIAMOND_SWORD
    name: "&bSimple Sword"
    lore:
      - "&7A basic example item."
    custom_model_data: 0
    unbreakable: false
    item_flags: []
    enchantments:
      - type: SHARPNESS
        level: 2
    attributes:
      - attribute: GENERIC_ATTACK_DAMAGE
        amount: 3.0
        operation: ADD_NUMBER
        slot: HAND
    equip_effects:
      enabled: true
      active_slots:
        - HAND
      potion_effects:
        - type: STRENGTH
          amplifier: 0
          ambient: true
          particles: false
          icon: true
    triggers:
      on_equip:
        enabled: true
        potion_effects_self: []
        commands:
          - 'tellraw %player% {"text":"Simple Sword equipped","color":"aqua"}'
      on_unequip:
        enabled: true
        potion_effects_self: []
        commands: []
      on_hit:
        enabled: false
        potion_effects_self: []
        potion_effects_target: []
        commands: []
      on_kill:
        enabled: false
        potion_effects_self: []
        potion_effects_target: []
        commands: []
      on_consume:
        enabled: false
        potion_effects_self: []
        commands: []

sets:
  simple_set:
    pieces:
      - simple_sword
      - sample_offhand_shield
    required_amount: 2
    potion_effects:
      - type: SPEED
        amplifier: 0
        ambient: true
        particles: false
        icon: true
    triggers:
      on_activate:
        enabled: true
        potion_effects_self: []
        commands:
          - 'tellraw %player% {"text":"Simple Set activated","color":"yellow"}'
      on_deactivate:
        enabled: true
        potion_effects_self: []
        commands:
          - 'tellraw %player% {"text":"Simple Set deactivated","color":"gray"}'
```