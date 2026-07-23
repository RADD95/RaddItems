# ============================================================
# RaddItems - items.yml
# ============================================================
#
# This file is where you create custom items and item sets.
#
# It has two main sections:
# - items: custom item definitions
# - sets: set bonuses that activate when enough pieces are active
#
# ------------------------------------------------------------
# ITEM IDS AND SET IDS
# ------------------------------------------------------------
# The ID of an item or set is the YAML key itself.
#
# Example:
#
# items:
#   battle_sword:
#     material: DIAMOND_SWORD
#
# sets:
#   guardian_set:
#     pieces:
#       - guardian_helmet
#       - guardian_chestplate
#
# In this example:
# - item id = battle_sword
# - set id  = guardian_set
#
# These IDs are also useful in command placeholders like:
# - %item_id%
# - %set_id%
#
# ------------------------------------------------------------
# ITEMS
# ------------------------------------------------------------
# Each entry inside "items:" creates one custom item.
#
# Example:
#
# items:
#   example_item:
#     material: DIAMOND_SWORD
#     name: "&5Example Sword"
#
# A custom item can use:
# - material
# - name
# - lore
# - custom_model_data
# - unbreakable
# - item_flags
# - enchantments
# - attributes
# - equip_effects
# - triggers
#
# ------------------------------------------------------------
# BASIC ITEM FIELDS
# ------------------------------------------------------------
# material
#   The base item type.
#   Example: DIAMOND_SWORD, SHIELD, APPLE, NETHERITE_HELMET
#
# name
#   The display name of the item.
#   Supports legacy color codes like &a, &6, &c, etc.
#
# lore
#   A list of lore lines shown on the item.
#
# custom_model_data
#   Used for resource pack models.
#   Use 0 if you do not need it.
#
# unbreakable
#   true or false
#
# item_flags
#   Extra item display options.
#
# Common examples:
# - HIDE_ATTRIBUTES
# - HIDE_UNBREAKABLE
# - HIDE_ENCHANTS
#
# enchantments
#   A list of enchantments with type and level.
#
# Example:
# enchantments:
#   - type: SHARPNESS
#     level: 3
#
# ------------------------------------------------------------
# ATTRIBUTES
# ------------------------------------------------------------
# Attributes let items give extra stats.
#
# Common examples:
# - GENERIC_MAX_HEALTH
# - GENERIC_ARMOR
# - GENERIC_ARMOR_TOUGHNESS
# - GENERIC_ATTACK_DAMAGE
# - GENERIC_ATTACK_SPEED
# - GENERIC_MOVEMENT_SPEED
# - GENERIC_SCALE
# - GENERIC_KNOCKBACK_RESISTANCE
#
# Supported operations:
# - ADD_NUMBER
# - ADD_SCALAR
# - MULTIPLY_SCALAR_1
#
# Each attribute uses:
# - attribute
# - amount
# - operation
# - slot
#
# Example:
# attributes:
#   - attribute: GENERIC_ATTACK_DAMAGE
#     amount: 4.0
#     operation: ADD_NUMBER
#     slot: HAND
#
# ------------------------------------------------------------
# SLOTS
# ------------------------------------------------------------
# Items can be active in different slots:
# - HEAD
# - CHEST
# - LEGS
# - FEET
# - HAND
# - OFF_HAND
# - HOTBAR
#
# Sets may also use:
# - ANY
#
# What they mean:
# - HEAD, CHEST, LEGS, FEET: armor slots
# - HAND: selected main hand item
# - OFF_HAND: offhand item
# - HOTBAR: any hotbar slot, even if not selected
#
# ------------------------------------------------------------
# ACTIVE SLOT RULES
# ------------------------------------------------------------
# These rules decide when an item is considered active.
#
# HEAD / CHEST / LEGS / FEET
#   The item must be worn in that armor slot.
#
# HAND
#   The item must be the currently selected item in the main hand.
#
# OFF_HAND
#   The item must be in the off hand.
#
# HOTBAR
#   The item works from any hotbar slot, even if it is not selected.
#
# Active slots affect:
# - equip_effects
# - on_equip / on_unequip
# - set piece counting
#
# ------------------------------------------------------------
# EQUIP EFFECTS
# ------------------------------------------------------------
# equip_effects are persistent potion effects that stay active
# while the item is active in one of its configured slots.
#
# Example:
# equip_effects:
#   enabled: true
#   active_slots:
#     - HAND
#   potion_effects:
#     - type: SPEED
#       amplifier: 0
#       ambient: true
#       particles: false
#       icon: true
#
# Notes:
# - enabled turns the persistent effect system on or off
# - active_slots decides where the item must be active
# - these effects stay active while the item stays active
# - do not add duration here
#
# ------------------------------------------------------------
# ITEM TRIGGERS
# ------------------------------------------------------------
# Supported item triggers:
# - on_equip
# - on_unequip
# - on_hit
# - on_kill
# - on_consume
#
# on_equip
#   Runs once when the item becomes active.
#
# on_unequip
#   Runs once when the item stops being active.
#
# on_hit
#   Runs when the player damages an entity with the item.
#
# on_kill
#   Runs when the player kills an entity with the item.
#
# on_consume
#   Runs when the item is consumed.
#
# Trigger sections may contain:
# - enabled
# - potion_effects_self
# - potion_effects_target
# - commands
#
# Example:
# on_hit:
#   enabled: true
#   potion_effects_self: []
#   potion_effects_target:
#     - type: WEAKNESS
#       duration: 100
#       amplifier: 0
#       ambient: false
#       particles: true
#       icon: true
#   commands:
#     - 'tellraw %player% {"text":"You hit %target%","color":"red"}'
#
# ------------------------------------------------------------
# TRIGGER EFFECTS
# ------------------------------------------------------------
# Trigger potion effects are temporary effects applied when a
# trigger happens.
#
# potion_effects_self
#   Applies effects to the player who triggered the event.
#
# potion_effects_target
#   Applies effects to the target when there is one.
#
# Trigger potion effect format:
# - type
# - duration
# - amplifier
# - ambient
# - particles
# - icon
#
# Example:
# potion_effects_self:
#   - type: SPEED
#     duration: 100
#     amplifier: 1
#     ambient: false
#     particles: true
#     icon: true
#
# Notes:
# - duration is required for trigger effects
# - duration uses ticks
# - 20 ticks = 1 second
# - amplifier 0 = level I
# - amplifier 1 = level II
#
# ------------------------------------------------------------
# SETS
# ------------------------------------------------------------
# Each entry inside "sets:" creates one item set.
#
# A set can use:
# - pieces
# - required_amount
# - attributes
# - potion_effects
# - triggers
#
# Example:
#
# sets:
#   guardian_set:
#     pieces:
#       - guardian_helmet
#       - guardian_chestplate
#       - guardian_leggings
#       - guardian_boots
#     required_amount: 4
#
# ------------------------------------------------------------
# SET PIECES
# ------------------------------------------------------------
# pieces is the list of item IDs that belong to the set.
#
# required_amount is how many active pieces are needed.
#
# Example:
# - If a set has 4 pieces and required_amount: 2,
#   then any 2 active pieces will activate it.
# - If required_amount: 4, the full set is required.
#
# ------------------------------------------------------------
# SET ATTRIBUTES
# ------------------------------------------------------------
# Sets can also grant attributes while active.
#
# Example:
# attributes:
#   - attribute: GENERIC_MAX_HEALTH
#     amount: 4.0
#     operation: ADD_NUMBER
#     slot: ANY
#
# ------------------------------------------------------------
# SET POTION EFFECTS
# ------------------------------------------------------------
# set potion_effects are persistent effects that stay active
# while the set remains active.
#
# Example:
# potion_effects:
#   - type: NIGHT_VISION
#     amplifier: 0
#     ambient: true
#     particles: false
#     icon: true
#
# Notes:
# - these effects stay active while the set is active
# - do not add duration here
#
# ------------------------------------------------------------
# SET TRIGGERS
# ------------------------------------------------------------
# Supported set triggers:
# - on_activate
# - on_deactivate
#
# on_activate
#   Runs once when the set becomes active.
#
# on_deactivate
#   Runs once when the set stops being active.
#
# Example:
# triggers:
#   on_activate:
#     enabled: true
#     potion_effects_self:
#       - type: ABSORPTION
#         duration: 100
#         amplifier: 0
#         ambient: false
#         particles: true
#         icon: true
#     commands:
#       - 'tellraw %player% {"text":"Set activated: %set_id%","color":"gold"}'
#   on_deactivate:
#     enabled: true
#     potion_effects_self: []
#     commands:
#       - 'tellraw %player% {"text":"Set deactivated: %set_id%","color":"gray"}'
#
# ------------------------------------------------------------
# PLACEHOLDERS
# ------------------------------------------------------------
# Common placeholders:
# - %player%  -> player name
# - %target%  -> target name, if there is one
# - %item_id% -> item ID
# - %set_id%  -> set ID
# - %slot%    -> slot involved
#
# Example:
# - on_hit usually uses %player%, %target%, and %item_id%
# - on_activate usually uses %player% and %set_id%
# - on_equip may use %player%, %item_id%, and %slot%
#
# ------------------------------------------------------------
# COMMANDS
# ------------------------------------------------------------
# Commands are usually executed from console.
#
# Example:
# commands:
#   - 'tellraw %player% {"text":"Hello","color":"green"}'
#
# ------------------------------------------------------------
# CONSUMABLE ITEMS
# ------------------------------------------------------------
# on_consume works with normal consumable items such as:
# - APPLE
# - BREAD
# - POTION
# - MILK_BUCKET
# - COOKED_BEEF
#
# ------------------------------------------------------------
# BEST PRACTICES
# ------------------------------------------------------------
# - Use clear and unique item IDs and set IDs.
# - Keep sample items simple and easy to copy.
# - Use equip_effects and set potion_effects for persistent bonuses.
# - Use triggers for temporary effects, commands, and messages.
# - Use potion_effects_self for the player.
# - Use potion_effects_target only when a target exists.
# - Give items clear names and lore so their purpose is obvious.
#
# ------------------------------------------------------------
# MINIMAL EXAMPLE
# ------------------------------------------------------------
# items:
#   simple_sword:
#     material: DIAMOND_SWORD
#     name: "&bSimple Sword"
#     lore:
#       - "&7A basic example item."
#     custom_model_data: 0
#     unbreakable: false
#     item_flags: []
#     enchantments:
#       - type: SHARPNESS
#         level: 2
#     attributes:
#       - attribute: GENERIC_ATTACK_DAMAGE
#         amount: 3.0
#         operation: ADD_NUMBER
#         slot: HAND
#     equip_effects:
#       enabled: true
#       active_slots:
#         - HAND
#       potion_effects:
#         - type: STRENGTH
#           amplifier: 0
#           ambient: true
#           particles: false
#           icon: true
#     triggers:
#       on_equip:
#         enabled: true
#         potion_effects_self: []
#         commands:
#           - 'tellraw %player% {"text":"Simple Sword equipped","color":"aqua"}'
#       on_unequip:
#         enabled: true
#         potion_effects_self: []
#         commands: []
#       on_hit:
#         enabled: false
#         potion_effects_self: []
#         potion_effects_target: []
#         commands: []
#       on_kill:
#         enabled: false
#         potion_effects_self: []
#         potion_effects_target: []
#         commands: []
#       on_consume:
#         enabled: false
#         potion_effects_self: []
#         commands: []
#
# sets:
#   simple_set:
#     pieces:
#       - simple_sword
#       - sample_offhand_shield
#     required_amount: 2
#     potion_effects:
#       - type: SPEED
#         amplifier: 0
#         ambient: true
#         particles: false
#         icon: true
#     triggers:
#       on_activate:
#         enabled: true
#         potion_effects_self: []
#         commands:
#           - 'tellraw %player% {"text":"Simple Set activated","color":"yellow"}'
#       on_deactivate:
#         enabled: true
#         potion_effects_self: []
#         commands:
#           - 'tellraw %player% {"text":"Simple Set deactivated","color":"gray"}'
#
# ============================================================