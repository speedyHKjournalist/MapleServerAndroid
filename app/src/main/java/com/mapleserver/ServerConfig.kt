package com.mapleserver

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServerConfig(
    @JsonProperty("worlds")
    var worlds: List<WorldProperties>,
    @JsonProperty("server")
    var server: ServerConfiguration
)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WorldProperties(
    @JsonProperty("flag")
    var flag: Int,
    @JsonProperty("server_message")
    var server_message: String,
    @JsonProperty("event_message")
    var event_message: String,
    @JsonProperty("why_am_i_recommended")
    var why_am_i_recommended: String,
    @JsonProperty("channels")
    var channels: Int,
    @JsonProperty("exp_rate")
    var exp_rate: Int?,
    @JsonProperty("meso_rate")
    var meso_rate: Int?,
    @JsonProperty("drop_rate")
    var drop_rate: Int?,
    @JsonProperty("boss_drop_rate")
    var boss_drop_rate: Int?,
    @JsonProperty("quest_rate")
    var quest_rate: Int?,
    @JsonProperty("fishing_rate")
    var fishing_rate: Int?,
    @JsonProperty("travel_rate")
    var travel_rate: Int?
)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ServerConfiguration(
    // Database Configuration
    @JsonProperty("DB_URL_FORMAT")
    var DB_URL_FORMAT: String,
    @JsonProperty("DB_HOST")
    var DB_HOST: String,
    @JsonProperty("DB_USER")
    var DB_USER: String,
    @JsonProperty("DB_PASS")
    var DB_PASS: String,
    @JsonProperty("INIT_CONNECTION_POOL_TIMEOUT")
    var INIT_CONNECTION_POOL_TIMEOUT: Int,

    // Login Configuration
    @JsonProperty("WORLDS")
    var WORLDS: Int,
    @JsonProperty("WLDLIST_SIZE")
    var WLDLIST_SIZE: Int,
    @JsonProperty("CHANNEL_SIZE")
    var CHANNEL_SIZE: Int,
    @JsonProperty("CHANNEL_LOAD")
    var CHANNEL_LOAD: Int,
    @JsonProperty("CHANNEL_LOCKS")
    var CHANNEL_LOCKS: Int,
    @JsonProperty("RESPAWN_INTERVAL")
    var RESPAWN_INTERVAL: Int,
    @JsonProperty("PURGING_INTERVAL")
    var PURGING_INTERVAL: Int,
    @JsonProperty("RANKING_INTERVAL")
    var RANKING_INTERVAL: Int,
    @JsonProperty("COUPON_INTERVAL")
    var COUPON_INTERVAL: Int,
    @JsonProperty("UPDATE_INTERVAL")
    var UPDATE_INTERVAL: Int,
    @JsonProperty("ENABLE_PIC")
    var ENABLE_PIC: Boolean,
    @JsonProperty("ENABLE_PIN")
    var ENABLE_PIN: Boolean,
    @JsonProperty("BYPASS_PIC_EXPIRATION")
    var BYPASS_PIC_EXPIRATION: Int,
    @JsonProperty("BYPASS_PIN_EXPIRATION")
    var BYPASS_PIN_EXPIRATION: Int,
    @JsonProperty("AUTOMATIC_REGISTER")
    var AUTOMATIC_REGISTER: Boolean,
    @JsonProperty("BCRYPT_MIGRATION")
    var BCRYPT_MIGRATION: Boolean,
    @JsonProperty("COLLECTIVE_CHARSLOT")
    var COLLECTIVE_CHARSLOT: Boolean,
    @JsonProperty("DETERRED_MULTICLIENT")
    var DETERRED_MULTICLIENT: Boolean,

    // Multiclient Coordinator Configuration
    @JsonProperty("MAX_ALLOWED_ACCOUNT_HWID")
    var MAX_ALLOWED_ACCOUNT_HWID: Int,
    @JsonProperty("MAX_ACCOUNT_LOGIN_ATTEMPT")
    var MAX_ACCOUNT_LOGIN_ATTEMPT: Int,
    @JsonProperty("LOGIN_ATTEMPT_DURATION")
    var LOGIN_ATTEMPT_DURATION: Int,

    // Ip Configuration
    @JsonProperty("HOST")
    var HOST: String,
    @JsonProperty("LANHOST")
    var LANHOST: String,
    @JsonProperty("LOCALHOST")
    var LOCALHOST: String,
    @JsonProperty("GMSERVER")
    var GMSERVER: Boolean,

    // Other configuration
    @JsonProperty("SHUTDOWNHOOK")
    var SHUTDOWNHOOK: Boolean,

    // Server Flags
    @JsonProperty("USE_CUSTOM_KEYSET")
    var USE_CUSTOM_KEYSET: Boolean,
    @JsonProperty("USE_DEBUG")
    var USE_DEBUG: Boolean,
    @JsonProperty("USE_DEBUG_SHOW_INFO_EQPEXP")
    var USE_DEBUG_SHOW_INFO_EQPEXP: Boolean,
    @JsonProperty("USE_DEBUG_SHOW_RCVD_PACKET")
    var USE_DEBUG_SHOW_RCVD_PACKET: Boolean,
    @JsonProperty("USE_DEBUG_SHOW_RCVD_MVLIFE")
    var USE_DEBUG_SHOW_RCVD_MVLIFE: Boolean,
    @JsonProperty("USE_DEBUG_SHOW_PACKET")
    var USE_DEBUG_SHOW_PACKET: Boolean,
    @JsonProperty("USE_SUPPLY_RATE_COUPONS")
    var USE_SUPPLY_RATE_COUPONS: Boolean,
    @JsonProperty("USE_IP_VALIDATION")
    var USE_IP_VALIDATION: Boolean,
    @JsonProperty("USE_CHARACTER_ACCOUNT_CHECK")
    var USE_CHARACTER_ACCOUNT_CHECK: Boolean,
    @JsonProperty("USE_MAXRANGE")
    var USE_MAXRANGE: Boolean,

    @JsonProperty("USE_MAXRANGE_ECHO_OF_HERO")
    var USE_MAXRANGE_ECHO_OF_HERO: Boolean,

    @JsonProperty("USE_MTS")
    var USE_MTS: Boolean,

    @JsonProperty("USE_CPQ")
    var USE_CPQ: Boolean,

    @JsonProperty("USE_AUTOHIDE_GM")
    var USE_AUTOHIDE_GM: Boolean,

    @JsonProperty("USE_FIXED_RATIO_HPMP_UPDATE")
    var USE_FIXED_RATIO_HPMP_UPDATE: Boolean,

    @JsonProperty("USE_FAMILY_SYSTEM")
    var USE_FAMILY_SYSTEM: Boolean,

    @JsonProperty("USE_DUEY")
    var USE_DUEY: Boolean,

    @JsonProperty("USE_RANDOMIZE_HPMP_GAIN")
    var USE_RANDOMIZE_HPMP_GAIN: Boolean,

    @JsonProperty("USE_STORAGE_ITEM_SORT")
    var USE_STORAGE_ITEM_SORT: Boolean,

    @JsonProperty("USE_ITEM_SORT")
    var USE_ITEM_SORT: Boolean,

    @JsonProperty("USE_ITEM_SORT_BY_NAME")
    var USE_ITEM_SORT_BY_NAME: Boolean,

    @JsonProperty("USE_PARTY_FOR_STARTERS")
    var USE_PARTY_FOR_STARTERS: Boolean,

    @JsonProperty("USE_AUTOASSIGN_STARTERS_AP")
    var USE_AUTOASSIGN_STARTERS_AP: Boolean,

    @JsonProperty("USE_AUTOASSIGN_SECONDARY_CAP")
    var USE_AUTOASSIGN_SECONDARY_CAP: Boolean,

    @JsonProperty("USE_STARTING_AP_4")
    var USE_STARTING_AP_4: Boolean,

    @JsonProperty("USE_AUTOBAN")
    var USE_AUTOBAN: Boolean,

    @JsonProperty("USE_AUTOBAN_LOG")
    var USE_AUTOBAN_LOG: Boolean,

    @JsonProperty("USE_EXP_GAIN_LOG")
    var USE_EXP_GAIN_LOG: Boolean,

    @JsonProperty("USE_AUTOSAVE")
    var USE_AUTOSAVE: Boolean,

    @JsonProperty("USE_SERVER_AUTOASSIGNER")
    var USE_SERVER_AUTOASSIGNER: Boolean,

    @JsonProperty("USE_REFRESH_RANK_MOVE")
    var USE_REFRESH_RANK_MOVE: Boolean,

    @JsonProperty("USE_ENFORCE_ADMIN_ACCOUNT")
    var USE_ENFORCE_ADMIN_ACCOUNT: Boolean,

    @JsonProperty("USE_ENFORCE_NOVICE_EXPRATE")
    var USE_ENFORCE_NOVICE_EXPRATE: Boolean,

    @JsonProperty("USE_ENFORCE_HPMP_SWAP")
    var USE_ENFORCE_HPMP_SWAP: Boolean,

    @JsonProperty("USE_ENFORCE_MOB_LEVEL_RANGE")
    var USE_ENFORCE_MOB_LEVEL_RANGE: Boolean,

    @JsonProperty("USE_ENFORCE_JOB_LEVEL_RANGE")
    var USE_ENFORCE_JOB_LEVEL_RANGE: Boolean,

    @JsonProperty("USE_ENFORCE_JOB_SP_RANGE")
    var USE_ENFORCE_JOB_SP_RANGE: Boolean,

    @JsonProperty("USE_ENFORCE_ITEM_SUGGESTION")
    var USE_ENFORCE_ITEM_SUGGESTION: Boolean,

    @JsonProperty("USE_ENFORCE_UNMERCHABLE_CASH")
    var USE_ENFORCE_UNMERCHABLE_CASH: Boolean,

    @JsonProperty("USE_ENFORCE_UNMERCHABLE_PET")
    var USE_ENFORCE_UNMERCHABLE_PET: Boolean,

    @JsonProperty("USE_ENFORCE_MERCHANT_SAVE")
    var USE_ENFORCE_MERCHANT_SAVE: Boolean,

    @JsonProperty("USE_ENFORCE_MDOOR_POSITION")
    var USE_ENFORCE_MDOOR_POSITION: Boolean,

    @JsonProperty("USE_SPAWN_CLEAN_MDOOR")
    var USE_SPAWN_CLEAN_MDOOR: Boolean,

    @JsonProperty("USE_SPAWN_RELEVANT_LOOT")
    var USE_SPAWN_RELEVANT_LOOT: Boolean,

    @JsonProperty("USE_ERASE_PERMIT_ON_OPENSHOP")
    var USE_ERASE_PERMIT_ON_OPENSHOP: Boolean,

    @JsonProperty("USE_ERASE_UNTRADEABLE_DROP")
    var USE_ERASE_UNTRADEABLE_DROP: Boolean,

    @JsonProperty("USE_ERASE_PET_ON_EXPIRATION")
    var USE_ERASE_PET_ON_EXPIRATION: Boolean,

    @JsonProperty("USE_BUFF_MOST_SIGNIFICANT")
    var USE_BUFF_MOST_SIGNIFICANT: Boolean,

    @JsonProperty("USE_BUFF_EVERLASTING")
    var USE_BUFF_EVERLASTING: Boolean,

    @JsonProperty("USE_MULTIPLE_SAME_EQUIP_DROP")
    var USE_MULTIPLE_SAME_EQUIP_DROP: Boolean,

    @JsonProperty("USE_ENABLE_FULL_RESPAWN")
    var USE_ENABLE_FULL_RESPAWN: Boolean,

    @JsonProperty("USE_ENABLE_CHAT_LOG")
    var USE_ENABLE_CHAT_LOG: Boolean,

    @JsonProperty("USE_MAP_OWNERSHIP_SYSTEM")
    var USE_MAP_OWNERSHIP_SYSTEM: Boolean,

    @JsonProperty("USE_FISHING_SYSTEM")
    var USE_FISHING_SYSTEM: Boolean,

    @JsonProperty("USE_NPCS_SCRIPTABLE")
    var USE_NPCS_SCRIPTABLE: Boolean,

    // Events/PQs Configuration
    @JsonProperty("USE_OLD_GMS_STYLED_PQ_NPCS")
    var USE_OLD_GMS_STYLED_PQ_NPCS: Boolean,
    @JsonProperty("USE_ENABLE_SOLO_EXPEDITIONS")
    var USE_ENABLE_SOLO_EXPEDITIONS: Boolean,
    @JsonProperty("USE_ENABLE_DAILY_EXPEDITIONS")
    var USE_ENABLE_DAILY_EXPEDITIONS: Boolean,
    @JsonProperty("USE_ENABLE_RECALL_EVENT")
    var USE_ENABLE_RECALL_EVENT: Boolean,

    // Announcement Configuration
    @JsonProperty("USE_ANNOUNCE_SHOPITEMSOLD")
    var USE_ANNOUNCE_SHOPITEMSOLD: Boolean,
    @JsonProperty("USE_ANNOUNCE_CHANGEJOB")
    var USE_ANNOUNCE_CHANGEJOB: Boolean,
    @JsonProperty("USE_ANNOUNCE_NX_COUPON_LOOT")
    var USE_ANNOUNCE_NX_COUPON_LOOT: Boolean,

    // Cash Shop Configuration
    @JsonProperty("USE_JOINT_CASHSHOP_INVENTORY")
    var USE_JOINT_CASHSHOP_INVENTORY: Boolean,
    @JsonProperty("USE_CLEAR_OUTDATED_COUPONS")
    var USE_CLEAR_OUTDATED_COUPONS: Boolean,
    @JsonProperty("ALLOW_CASHSHOP_NAME_CHANGE")
    var ALLOW_CASHSHOP_NAME_CHANGE: Boolean,
    @JsonProperty("ALLOW_CASHSHOP_WORLD_TRANSFER")
    var ALLOW_CASHSHOP_WORLD_TRANSFER: Boolean,

    // Maker Configuration
    @JsonProperty("USE_MAKER_PERMISSIVE_ATKUP")
    var USE_MAKER_PERMISSIVE_ATKUP: Boolean,
    @JsonProperty("USE_MAKER_FEE_HEURISTICS")
    var USE_MAKER_FEE_HEURISTICS: Boolean,

    // Custom Configuration
    @JsonProperty("USE_ENABLE_CUSTOM_NPC_SCRIPT")
    var USE_ENABLE_CUSTOM_NPC_SCRIPT: Boolean,
    @JsonProperty("USE_STARTER_MERGE")
    var USE_STARTER_MERGE: Boolean,

    // Commands Configuration
    @JsonProperty("BLOCK_GENERATE_CASH_ITEM")
    var BLOCK_GENERATE_CASH_ITEM: Boolean,
    @JsonProperty("USE_WHOLE_SERVER_RANKING")
    var USE_WHOLE_SERVER_RANKING: Boolean,
    @JsonProperty("EQUIP_EXP_RATE")
    var EQUIP_EXP_RATE: Double,
    @JsonProperty("PQ_BONUS_EXP_RATE")
    var PQ_BONUS_EXP_RATE: Double,
    @JsonProperty("EXP_SPLIT_LEVEL_INTERVAL")
    var EXP_SPLIT_LEVEL_INTERVAL: Int,
    @JsonProperty("EXP_SPLIT_LEECH_INTERVAL")
    var EXP_SPLIT_LEECH_INTERVAL: Int,
    @JsonProperty("EXP_SPLIT_MVP_MOD")
    var EXP_SPLIT_MVP_MOD: Double,
    @JsonProperty("EXP_SPLIT_COMMON_MOD")
    var EXP_SPLIT_COMMON_MOD: Double,
    @JsonProperty("PARTY_BONUS_EXP_RATE")
    var PARTY_BONUS_EXP_RATE: Double,

    // Miscellaneous Configuration
    @JsonProperty("TIMEZONE")
    var TIMEZONE: String,
    @JsonProperty("CHARSET")
    var CHARSET: String,
    @JsonProperty("USE_DISPLAY_NUMBERS_WITH_COMMA")
    var USE_DISPLAY_NUMBERS_WITH_COMMA: Boolean,
    @JsonProperty("USE_UNITPRICE_WITH_COMMA")
    var USE_UNITPRICE_WITH_COMMA: Boolean,
    @JsonProperty("MAX_MONITORED_BUFFSTATS")
    var MAX_MONITORED_BUFFSTATS: Int,
    @JsonProperty("MAX_AP")
    var MAX_AP: Int,
    @JsonProperty("MAX_EVENT_LEVELS")
    var MAX_EVENT_LEVELS: Int,
    @JsonProperty("BLOCK_NPC_RACE_CONDT")
    var BLOCK_NPC_RACE_CONDT: Int,
    @JsonProperty("TOT_MOB_QUEST_REQUIREMENT")
    var TOT_MOB_QUEST_REQUIREMENT: Int,
    @JsonProperty("MOB_REACTOR_REFRESH_TIME")
    var MOB_REACTOR_REFRESH_TIME: Int,
    @JsonProperty("PARTY_SEARCH_REENTRY_LIMIT")
    var PARTY_SEARCH_REENTRY_LIMIT: Int,
    @JsonProperty("NAME_CHANGE_COOLDOWN")
    var NAME_CHANGE_COOLDOWN: Long,
    @JsonProperty("WORLD_TRANSFER_COOLDOWN")
    var WORLD_TRANSFER_COOLDOWN: Long,
    @JsonProperty("INSTANT_NAME_CHANGE")
    var INSTANT_NAME_CHANGE: Boolean,

    // Dangling Items/Locks Configuration
    @JsonProperty("ITEM_EXPIRE_TIME")
    var ITEM_EXPIRE_TIME: Long,
    @JsonProperty("KITE_EXPIRE_TIME")
    var KITE_EXPIRE_TIME: Long,
    @JsonProperty("ITEM_MONITOR_TIME")
    var ITEM_MONITOR_TIME: Long,
    @JsonProperty("LOCK_MONITOR_TIME")
    var LOCK_MONITOR_TIME: Long,

    // Map Monitor Configuration
    @JsonProperty("ITEM_EXPIRE_CHECK")
    var ITEM_EXPIRE_CHECK: Long,
    @JsonProperty("ITEM_LIMIT_ON_MAP")
    var ITEM_LIMIT_ON_MAP: Int,
    @JsonProperty("MAP_VISITED_SIZE")
    var MAP_VISITED_SIZE: Int,
    @JsonProperty("MAP_DAMAGE_OVERTIME_INTERVAL")
    var MAP_DAMAGE_OVERTIME_INTERVAL: Long,
    @JsonProperty("MAP_DAMAGE_OVERTIME_COUNT")
    var MAP_DAMAGE_OVERTIME_COUNT: Int,

    // Channel Mob Disease Monitor Configuration
    @JsonProperty("MOB_STATUS_MONITOR_PROC")
    var MOB_STATUS_MONITOR_PROC: Int,
    @JsonProperty("MOB_STATUS_MONITOR_LIFE")
    var MOB_STATUS_MONITOR_LIFE: Int,
    @JsonProperty("MOB_STATUS_AGGRO_PERSISTENCE")
    var MOB_STATUS_AGGRO_PERSISTENCE: Int,
    @JsonProperty("MOB_STATUS_AGGRO_INTERVAL")
    var MOB_STATUS_AGGRO_INTERVAL: Long,
    @JsonProperty("USE_AUTOAGGRO_NEARBY")
    var USE_AUTOAGGRO_NEARBY: Boolean,

    // Some Gameplay Enhancing Configurations
    // Scroll Configuration
    @JsonProperty("USE_PERFECT_GM_SCROLL")
    var USE_PERFECT_GM_SCROLL: Boolean,
    @JsonProperty("USE_PERFECT_SCROLLING")
    var USE_PERFECT_SCROLLING: Boolean,
    @JsonProperty("USE_ENHANCED_CHSCROLL")
    var USE_ENHANCED_CHSCROLL: Boolean,
    @JsonProperty("USE_ENHANCED_CRAFTING")
    var USE_ENHANCED_CRAFTING: Boolean,
    @JsonProperty("SCROLL_CHANCE_ROLLS")
    var SCROLL_CHANCE_ROLLS: Int,
    @JsonProperty("CHSCROLL_STAT_RATE")
    var CHSCROLL_STAT_RATE: Int,
    @JsonProperty("CHSCROLL_STAT_RANGE")
    var CHSCROLL_STAT_RANGE: Int,
    // Beginner Skills Configuration
    @JsonProperty("USE_ULTRA_NIMBLE_FEET")
    var USE_ULTRA_NIMBLE_FEET: Boolean,
    @JsonProperty("USE_ULTRA_RECOVERY")
    var USE_ULTRA_RECOVERY: Boolean,
    @JsonProperty("USE_ULTRA_THREE_SNAILS")
    var USE_ULTRA_THREE_SNAILS: Boolean,
    // Other Skills Configuration
    @JsonProperty("USE_FULL_ARAN_SKILLSET")
    var USE_FULL_ARAN_SKILLSET: Boolean,
    @JsonProperty("USE_FAST_REUSE_HERO_WILL")
    var USE_FAST_REUSE_HERO_WILL: Boolean,
    @JsonProperty("USE_ANTI_IMMUNITY_CRASH")
    var USE_ANTI_IMMUNITY_CRASH: Boolean,
    @JsonProperty("USE_UNDISPEL_HOLY_SHIELD")
    var USE_UNDISPEL_HOLY_SHIELD: Boolean,
    @JsonProperty("USE_FULL_HOLY_SYMBOL")
    var USE_FULL_HOLY_SYMBOL: Boolean,
    // Character Configuration
    @JsonProperty("USE_ADD_SLOTS_BY_LEVEL")
    var USE_ADD_SLOTS_BY_LEVEL: Boolean,
    @JsonProperty("USE_ADD_RATES_BY_LEVEL")
    var USE_ADD_RATES_BY_LEVEL: Boolean,
    @JsonProperty("USE_STACK_COUPON_RATES")
    var USE_STACK_COUPON_RATES: Boolean,
    @JsonProperty("USE_PERFECT_PITCH")
    var USE_PERFECT_PITCH: Boolean,
    // Quest Configuration
    @JsonProperty("USE_QUEST_RATE")
    var USE_QUEST_RATE: Boolean,
    // Quest Points Configuration
    @JsonProperty("QUEST_POINT_REPEATABLE_INTERVAL")
    var QUEST_POINT_REPEATABLE_INTERVAL: Int,
    @JsonProperty("QUEST_POINT_REQUIREMENT")
    var QUEST_POINT_REQUIREMENT: Int,
    @JsonProperty("QUEST_POINT_PER_QUEST_COMPLETE")
    var QUEST_POINT_PER_QUEST_COMPLETE: Int,
    @JsonProperty("QUEST_POINT_PER_EVENT_CLEAR")
    var QUEST_POINT_PER_EVENT_CLEAR: Int,
    // Guild Configuration
    @JsonProperty("CREATE_GUILD_MIN_PARTNERS")
    var CREATE_GUILD_MIN_PARTNERS: Int,
    @JsonProperty("CREATE_GUILD_COST")
    var CREATE_GUILD_COST: Int,
    @JsonProperty("CHANGE_EMBLEM_COST")
    var CHANGE_EMBLEM_COST: Int,
    @JsonProperty("EXPAND_GUILD_BASE_COST")
    var EXPAND_GUILD_BASE_COST: Int,
    @JsonProperty("EXPAND_GUILD_TIER_COST")
    var EXPAND_GUILD_TIER_COST: Int,
    @JsonProperty("EXPAND_GUILD_MAX_COST")
    var EXPAND_GUILD_MAX_COST: Int,
    // Family Configuration
    @JsonProperty("FAMILY_REP_PER_KILL")
    var FAMILY_REP_PER_KILL: Int,
    @JsonProperty("FAMILY_REP_PER_BOSS_KILL")
    var FAMILY_REP_PER_BOSS_KILL: Int,
    @JsonProperty("FAMILY_REP_PER_LEVELUP")
    var FAMILY_REP_PER_LEVELUP: Int,
    @JsonProperty("FAMILY_MAX_GENERATIONS")
    var FAMILY_MAX_GENERATIONS: Int,
    // Equipment Configuration
    @JsonProperty("USE_EQUIPMNT_LVLUP_SLOTS")
    var USE_EQUIPMNT_LVLUP_SLOTS: Boolean,
    @JsonProperty("USE_EQUIPMNT_LVLUP_POWER")
    var USE_EQUIPMNT_LVLUP_POWER: Boolean,
    @JsonProperty("USE_EQUIPMNT_LVLUP_CASH")
    var USE_EQUIPMNT_LVLUP_CASH: Boolean,
    @JsonProperty("MAX_EQUIPMNT_LVLUP_STAT_UP")
    var MAX_EQUIPMNT_LVLUP_STAT_UP: Int,
    @JsonProperty("MAX_EQUIPMNT_STAT")
    var MAX_EQUIPMNT_STAT: Int,
    @JsonProperty("USE_EQUIPMNT_LVLUP")
    var USE_EQUIPMNT_LVLUP: Int,
    // Map-Chair Configuration
    @JsonProperty("USE_CHAIR_EXTRAHEAL")
    var USE_CHAIR_EXTRAHEAL: Boolean,
    @JsonProperty("CHAIR_EXTRA_HEAL_MULTIPLIER")
    var CHAIR_EXTRA_HEAL_MULTIPLIER: Int,
    @JsonProperty("CHAIR_EXTRA_HEAL_MAX_DELAY")
    var CHAIR_EXTRA_HEAL_MAX_DELAY: Int,
    // Player NPC Configuration
    @JsonProperty("PLAYERNPC_INITIAL_X")
    var PLAYERNPC_INITIAL_X: Int,
    @JsonProperty("PLAYERNPC_INITIAL_Y")
    var PLAYERNPC_INITIAL_Y: Int,
    @JsonProperty("PLAYERNPC_AREA_X")
    var PLAYERNPC_AREA_X: Int,
    @JsonProperty("PLAYERNPC_AREA_Y")
    var PLAYERNPC_AREA_Y: Int,
    @JsonProperty("PLAYERNPC_AREA_STEPS")
    var PLAYERNPC_AREA_STEPS: Int,
    @JsonProperty("PLAYERNPC_ORGANIZE_AREA")
    var PLAYERNPC_ORGANIZE_AREA: Boolean,
    @JsonProperty("PLAYERNPC_AUTODEPLOY")
    var PLAYERNPC_AUTODEPLOY: Boolean,
    // Pet Auto-Pot Configuration
    @JsonProperty("USE_COMPULSORY_AUTOPOT")
    var USE_COMPULSORY_AUTOPOT: Boolean,
    @JsonProperty("USE_EQUIPS_ON_AUTOPOT")
    var USE_EQUIPS_ON_AUTOPOT: Boolean,
    @JsonProperty("PET_AUTOHP_RATIO")
    var PET_AUTOHP_RATIO: Double,
    @JsonProperty("PET_AUTOMP_RATIO")
    var PET_AUTOMP_RATIO: Double,
    // Pet & Mount Configuration
    @JsonProperty("PET_EXHAUST_COUNT")
    var PET_EXHAUST_COUNT: Int,
    @JsonProperty("MOUNT_EXHAUST_COUNT")
    var MOUNT_EXHAUST_COUNT: Int,
    // Pet Hunger Configuration
    @JsonProperty("PETS_NEVER_HUNGRY")
    var PETS_NEVER_HUNGRY: Boolean,
    @JsonProperty("GM_PETS_NEVER_HUNGRY")
    var GM_PETS_NEVER_HUNGRY: Boolean,
    // Event Configuration
    @JsonProperty("EVENT_MAX_GUILD_QUEUE")
    var EVENT_MAX_GUILD_QUEUE: Int,
    @JsonProperty("EVENT_LOBBY_DELAY")
    var EVENT_LOBBY_DELAY: Int,
    // Dojo Configuration
    @JsonProperty("USE_FAST_DOJO_UPGRADE")
    var USE_FAST_DOJO_UPGRADE: Boolean,
    @JsonProperty("USE_DEADLY_DOJO")
    var USE_DEADLY_DOJO: Boolean,
    @JsonProperty("DOJO_ENERGY_ATK")
    var DOJO_ENERGY_ATK: Int,
    @JsonProperty("DOJO_ENERGY_DMG")
    var DOJO_ENERGY_DMG: Int,
    // Wedding Configuration
    @JsonProperty("WEDDING_RESERVATION_DELAY")
    var WEDDING_RESERVATION_DELAY: Int,
    @JsonProperty("WEDDING_RESERVATION_TIMEOUT")
    var WEDDING_RESERVATION_TIMEOUT: Int,
    @JsonProperty("WEDDING_RESERVATION_INTERVAL")
    var WEDDING_RESERVATION_INTERVAL: Int,
    @JsonProperty("WEDDING_BLESS_EXP")
    var WEDDING_BLESS_EXP: Int,
    @JsonProperty("WEDDING_GIFT_LIMIT")
    var WEDDING_GIFT_LIMIT: Int,
    @JsonProperty("WEDDING_BLESSER_SHOWFX")
    var WEDDING_BLESSER_SHOWFX: Boolean,
    // Login timeout by shavit
    @JsonProperty("TIMEOUT_DURATION")
    var TIMEOUT_DURATION: Long,
    // Event End Timestamp
    @JsonProperty("EVENT_END_TIMESTAMP")
    var EVENT_END_TIMESTAMP: Long,
    // GM Security Configuration
    @JsonProperty("MINIMUM_GM_LEVEL_TO_TRADE")
    var MINIMUM_GM_LEVEL_TO_TRADE: Int,
    @JsonProperty("MINIMUM_GM_LEVEL_TO_USE_STORAGE")
    var MINIMUM_GM_LEVEL_TO_USE_STORAGE: Int,
    @JsonProperty("MINIMUM_GM_LEVEL_TO_USE_DUEY")
    var MINIMUM_GM_LEVEL_TO_USE_DUEY: Int,
    @JsonProperty("MINIMUM_GM_LEVEL_TO_DROP")
    var MINIMUM_GM_LEVEL_TO_DROP: Int,
    // Any NPC ids that should search for a js override script (useful if they already have wz entries since otherwise they're ignored).
    @JsonProperty("NPCS_SCRIPTABLE")
    var NPCS_SCRIPTABLE: Map<Int, String>
)

