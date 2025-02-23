@file:JvmName("Compat")
@file:Suppress("SpellCheckingInspection")

package thedarkcolour.futuremc.compat

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import net.minecraftforge.fml.common.Loader
import thedarkcolour.futuremc.compat.actuallyadditions.ActuallyAdditionsCompat
import thedarkcolour.futuremc.compat.betterwithmods.BetterWithModsCompat
import thedarkcolour.futuremc.compat.dynamictrees.DynamicTreesCompat
import thedarkcolour.futuremc.compat.harvestcraft.HarvestCraftCompat
import thedarkcolour.futuremc.compat.otg.OTGCompat
import thedarkcolour.futuremc.compat.plants.PlantsCompat
import thedarkcolour.futuremc.compat.quark.QuarkCompat
import thedarkcolour.futuremc.compat.tconstruct.TConstructCompat

const val ACTUALLY_ADDITIONS = "actuallyadditions"
const val BETTER_WITH_MODS = "betterwithmods"
const val CRAFTTWEAKER = "crafttweaker"
const val DYNAMIC_TREES = "dynamictrees"
const val FLUIDLOGGED_API = "fluidlogged_api"
const val JEI = "jei"
const val OTG = "openterraingenerator"
const val PAMS_HARVESTCRAFT = "harvestcraft"
const val PLANTS = "plants2"
const val QUARK = "quark"
const val RANDOM_TWEAKS = "randomtweaks"
const val RANDOM_PATCHES = "random_patches"
const val TCONSTRUCT = "tconstruct"
const val VIVECRAFT = "vivecraftforgeextensionscore"

private val LOADED_MODS = Object2BooleanOpenHashMap<String>()

fun checkActuallyAdditions(): ActuallyAdditionsCompat? {
    return checkModCompat(ACTUALLY_ADDITIONS, ActuallyAdditionsCompat)
}

fun checkBetterWithMods(): BetterWithModsCompat? {
    return checkModCompat(BETTER_WITH_MODS, BetterWithModsCompat)
}

fun checkDynamicTrees(): DynamicTreesCompat? {
    return checkModCompat(DYNAMIC_TREES, DynamicTreesCompat)
}

fun checkHarvestCraft(): HarvestCraftCompat? {
    return checkModCompat(PAMS_HARVESTCRAFT, HarvestCraftCompat)
}

fun checkPlants(): PlantsCompat? {
    return checkModCompat(PLANTS, PlantsCompat)
}

fun checkQuark(): QuarkCompat? {
    return checkModCompat(QUARK, QuarkCompat)
}

fun checkTConstruct(): TConstructCompat? {
    return checkModCompat(TCONSTRUCT, TConstructCompat)
}

fun checkVivecraft(): Boolean {
    return try {
        Class.forName("org.vivecraft.main.VivecraftMain")
        true
    } catch (e: ClassNotFoundException) {
        false
    }
}

/**
 * Inline so that [mod] does not get initialized.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun <T> checkModCompat(modid: String, mod: T): T? {
    return if (isModLoaded(modid)) {
        mod
    } else null
}

fun isModLoaded(modid: String): Boolean {
    return LOADED_MODS.computeIfAbsent(modid, { Loader.isModLoaded(modid) })
}