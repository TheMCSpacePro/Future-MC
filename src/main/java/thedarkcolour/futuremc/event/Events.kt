package thedarkcolour.futuremc.event

import net.minecraft.block.*
import net.minecraft.block.BlockLog.EnumAxis
import net.minecraft.block.material.Material
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiMerchant
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.monster.EntityElderGuardian
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.ContainerMerchant
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemTool
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.config.Config
import net.minecraftforge.common.config.ConfigManager
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent
import net.minecraftforge.event.entity.player.PlayerContainerEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock
import net.minecraftforge.event.terraingen.BiomeEvent
import net.minecraftforge.fml.client.event.ConfigChangedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent
import net.minecraftforge.fml.common.registry.ForgeRegistries
import thedarkcolour.core.util.*
import thedarkcolour.futuremc.FutureMC
import thedarkcolour.futuremc.block.BlockStrippedLog
import thedarkcolour.futuremc.block.BlockWood
import thedarkcolour.futuremc.capability.hasSwimmingCap
import thedarkcolour.futuremc.capability.isSwimming
import thedarkcolour.futuremc.capability.lastSwimAnimation
import thedarkcolour.futuremc.capability.swimAnimation
import thedarkcolour.futuremc.client.color.WaterColor
import thedarkcolour.futuremc.client.gui.GuiVillager
import thedarkcolour.futuremc.client.render.TridentBakedModel
import thedarkcolour.futuremc.compat.checkDynamicTrees
import thedarkcolour.futuremc.compat.checkQuark
import thedarkcolour.futuremc.compat.checkTConstruct
import thedarkcolour.futuremc.config.FConfig
import thedarkcolour.futuremc.config.FConfig.updateAquatic
import thedarkcolour.futuremc.container.ContainerVillager
import thedarkcolour.futuremc.registry.FBlocks.HONEY_BLOCK
import thedarkcolour.futuremc.registry.FBlocks.STRIPPED_ACACIA_LOG
import thedarkcolour.futuremc.registry.FBlocks.STRIPPED_BIRCH_LOG
import thedarkcolour.futuremc.registry.FBlocks.STRIPPED_DARK_OAK_LOG
import thedarkcolour.futuremc.registry.FBlocks.STRIPPED_JUNGLE_LOG
import thedarkcolour.futuremc.registry.FBlocks.STRIPPED_OAK_LOG
import thedarkcolour.futuremc.registry.FBlocks.STRIPPED_SPRUCE_LOG
import thedarkcolour.futuremc.registry.FBlocks.WITHER_ROSE
import thedarkcolour.futuremc.registry.FItems.HONEY_BOTTLE
import thedarkcolour.futuremc.registry.FItems.TRIDENT
import thedarkcolour.futuremc.registry.FSounds
import thedarkcolour.futuremc.registry.FSounds.HONEY_BOTTLE_DRINK
import thedarkcolour.futuremc.registry.RegistryEventHandler
import thedarkcolour.futuremc.world.OldWorldHandler
import thedarkcolour.futuremc.world.gen.feature.AncientDebrisWorldGen
import thedarkcolour.futuremc.world.gen.feature.BeeNestGenerator
import kotlin.math.max
import kotlin.math.min

/**
 * Event handling for Future MC.
 *
 * @author TheDarkColour
 */
@Suppress("UNUSED_PARAMETER")
object Events {
    /**
     * Registers event subscribers to the event bus
     * using the [addListener] function in Util.kt.
     */
    fun registerEvents() {
        // do not run during tests
        if (FutureMC.TEST) return

        addListener(::onHoneyBottleConsumed)
        addListener(::onLogStripped)
        addListener(::spawnWitherRoseOrDropTrident)
        addListener(::preventHoneyJump)
        addListener(::onConfigChanged)
        //if (updateAquatic.newWaterColor)
        //    addListener(::onGetWaterColor)
        addListener(::healIronGolem)
        runOnClient { addListener(::onGuiOpen) }
        addListener(::onContainerOpen, priority = EventPriority.HIGHEST)
        runOnClient { addListener(::onModelRegistry) }
        if (TODO())
            addListener(::updateSwimAnimation)
        subscribe(RegistryEventHandler)
        subscribe(OldWorldHandler)

        if (TODO())
            addListener(::onModelBake)

        checkDynamicTrees()?.addListeners()
    }

    /**
     * Changes the eating sound of honey bottle to
     * the correct drinking sound from 1.15.
     */
    private fun onHoneyBottleConsumed(event: PlaySoundAtEntityEvent) {
        if (event.entity is EntityLivingBase) {
            val e = event.entity as EntityLivingBase
            if (e.activeItemStack.item == HONEY_BOTTLE) {
                event.sound = HONEY_BOTTLE_DRINK
            }
        }
    }

    /**
     * Strips logs with vanilla axes or
     * axes from Tinkers Construct.
     */
    private fun onLogStripped(event: RightClickBlock) {
        val worldIn = event.world
        val pos = event.pos
        val player = event.entityPlayer
        val stack = event.itemStack
        if (updateAquatic.strippedLogs.rightClickToStrip) {
            if (isVanillaAxe(stack) || checkTConstruct()?.isTinkersAxe(stack) == true) {
                val state = worldIn.getBlockState(pos)
                val block = state.block
                if (block == Blocks.LOG || block == Blocks.LOG2) {
                    var axis: IProperty<EnumAxis>? = null
                    var variant: IProperty<BlockPlanks.EnumType>? = null

                    @Suppress("UNCHECKED_CAST", "UsePropertyAccessSyntax")
                    for (prop in state.propertyKeys) {
                        if (prop.getName() == "axis") {
                            axis = prop as IProperty<EnumAxis>
                        } else if (prop.getName() == "variant") {
                            variant = prop as IProperty<BlockPlanks.EnumType>
                        }
                    }

                    if (axis != null && variant != null) {
                        if (BlockStrippedLog.variants.contains(state.getValue(variant).toString())) {
                            val strippedLog = getState(state, block) ?: return
                            stripBlock(worldIn, pos, player, event.hand, stack, strippedLog.withProperty(axis, state.getValue(axis)))
                        }
                    }
                } else if (block is BlockWood) {
                    val name = block.registryName!!.path

                    if (!name.startsWith("stripped")) {
                        val axis = state.getValue(BlockRotatedPillar.AXIS)
                        val strippedBlock = ForgeRegistries.BLOCKS.getValue(ResourceLocation(FutureMC.ID, "stripped_$name")) ?: return

                        stripBlock(worldIn, pos, player, event.hand, stack, strippedBlock.defaultState.withProperty(BlockRotatedPillar.AXIS, axis))
                    }
                } else if (checkQuark()?.isBarkBlock(state) == true) {
                    stripBlock(worldIn, pos, player, event.hand, stack, checkQuark()!!.getStrippedBark(state))
                }
            }
        }
    }

    /**
     * Returns true if this item is an
     * axe according to vanilla minecraft.
     */
    private fun isVanillaAxe(stack: ItemStack): Boolean {
        return stack.item is ItemTool && stack.item.getToolClasses(stack).contains("axe")
    }

    private fun getState(state: IBlockState, block: Block): IBlockState? {
        var variant: String? = null
        if (block == Blocks.LOG) {
            variant = state.getValue(BlockOldLog.VARIANT).getName()
        }
        if (block == Blocks.LOG2) {
            variant = state.getValue(BlockNewLog.VARIANT).getName()
        }
        if (variant != null) {
            when (variant) {
                "acacia" -> return if (updateAquatic.strippedLogs.acacia) STRIPPED_ACACIA_LOG.defaultState else null
                "jungle" -> return if (updateAquatic.strippedLogs.jungle) STRIPPED_JUNGLE_LOG.defaultState else null
                "birch" -> return if (updateAquatic.strippedLogs.birch) STRIPPED_BIRCH_LOG.defaultState else null
                "oak" -> return if (updateAquatic.strippedLogs.oak) STRIPPED_OAK_LOG.defaultState else null
                "spruce" -> return if (updateAquatic.strippedLogs.spruce) STRIPPED_SPRUCE_LOG.defaultState else null
                "dark_oak" -> return if (updateAquatic.strippedLogs.darkOak) STRIPPED_DARK_OAK_LOG.defaultState else null
            }
        }
        return null
    }

    /**
     * Strips a vanilla or future mc log.
     */
    private fun stripBlock(level: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, stack: ItemStack, state: IBlockState) {
        player.swingArm(hand)
        level.playSound(player, pos, SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 1.0f, 1.0f)
        level.setBlockState(pos, state)

        damageAxe(player, stack)
    }

    /**
     * Damages an axe item from either vanilla or Tinkers Construct.
     */
    private fun damageAxe(playerIn: EntityPlayer, stack: ItemStack) {
        // do simpler check first
        if (isVanillaAxe(stack)) {
            stack.damageItem(1, playerIn)
        } else {
            // call API from nullable wrapper
            checkTConstruct()?.damageTool(stack, 1, playerIn)
        }
    }

    // wither rose spawn + elder guardian thing
    private fun spawnWitherRoseOrDropTrident(event: LivingDeathEvent) {
        val entityIn = event.entityLiving
        val worldIn = entityIn.world

        if (!entityIn.isDead) {
            if (!worldIn.isRemote) {
                if (FConfig.villageAndPillage.witherRose.enabled && event.source.trueSource is EntityWither) {
                    if (ForgeEventFactory.getMobGriefingEvent(worldIn, entityIn)) {
                        val pos = BlockPos(entityIn.posX, entityIn.posY, entityIn.posZ)
                        val state = worldIn.getBlockState(pos)

                        if (state.block.isAir(state, worldIn, pos) && WITHER_ROSE.canBlockStay(worldIn, pos, state)) {
                            worldIn.setBlockState(pos, WITHER_ROSE.defaultState, 3)
                            return
                        }
                    }

                    val witherRose = EntityItem(worldIn, entityIn.posX, entityIn.posY, entityIn.posZ)
                    witherRose.item = ItemStack(WITHER_ROSE)
                    worldIn.spawnEntity(witherRose)
                }

                // elder guardian drop
                if (updateAquatic.trident && entityIn is EntityElderGuardian) {
                    val trident = EntityItem(worldIn, entityIn.posX, entityIn.posY, entityIn.posZ)
                    trident.item = ItemStack(TRIDENT)
                    worldIn.spawnEntity(trident)
                }
            }
        }
    }

    // honey jump stickiness
    private fun preventHoneyJump(event: LivingJumpEvent) {
        val entity = event.entityLiving
        val x = MathHelper.floor(entity.posX)
        val y = MathHelper.floor(entity.posY - 0.20000000298023224)
        val z = MathHelper.floor(entity.posZ)
        val pos = BlockPos(x, y, z)
        val state = entity.world.getBlockState(pos)
        if (state.block == HONEY_BLOCK) {
            entity.motionY *= 0.5
        }
    }

    // syncs config
    private fun onConfigChanged(event: ConfigChangedEvent) {
        if (event.modID == FutureMC.ID) {
            ConfigManager.sync(FutureMC.ID, Config.Type.INSTANCE)
            // makes things reloadable
            BeeNestGenerator.refresh()
            AncientDebrisWorldGen.refresh()
        }
    }

    // 1.13 water colors
    private fun onGetWaterColor(event: BiomeEvent.GetWaterColor) {
        event.newColor = (WaterColor.BIOME_COLORS[event.biome.delegate] ?: 0x3f76e4)
    }

    // unused
    /*private fun onBucketUse(event: FillBucketEvent) {
        val pos = event.target?.blockPos
        if (pos != null) {
            if (event.world.getBlockState(pos).block is BlockWaterPlant) {
                event.isCanceled = true
            }
        }
    }*/

    // unused
    /*private fun canCreateFluidSource(event: BlockEvent.CreateFluidSourceEvent) {
        if (event.state.block is BlockWaterPlant.Flowing) {

        }
    }*/

    // iron golem healing
    private fun healIronGolem(event: PlayerInteractEvent.EntityInteract) {
        if (FConfig.buzzyBees.ironGolem.ironBarHealing) {
            val entity = event.target
            if (entity is EntityIronGolem && event.itemStack.item == Items.IRON_INGOT) {
                val hp = entity.health
                entity.heal(25f)
                // only heal if the entity HP was increased
                if (hp != entity.health) {
                    val rand = entity.rng
                    val pitch = 1.0f + (rand.nextFloat() - rand.nextFloat()) * 0.2f
                    entity.playSound(FSounds.IRON_GOLEM_REPAIR, 1.0f, pitch)
                    if (!event.entityPlayer.isCreative) {
                        event.itemStack.shrink(1)
                    }
                }
            }
        }
    }

    private fun onGuiOpen(event: GuiOpenEvent) {
        if (FConfig.villageAndPillage.newVillagerGui) {
            val gui = event.gui

            if (gui is GuiMerchant && gui !is GuiVillager) {
                event.gui = GuiVillager(ContainerVillager(Minecraft.getMinecraft().player.inventory, gui.merchant, null))
            }
        }
    }

    private fun onContainerOpen(event: PlayerContainerEvent.Open) {
        if (FConfig.villageAndPillage.newVillagerGui) {
            val container = event.container

            if (container is ContainerMerchant && container !is ContainerVillager) {
                val player = event.entityPlayer as EntityPlayerMP
                val newContainer = ContainerVillager(player.inventory, container.merchant, null)

                newContainer.windowId = container.windowId
                player.openContainer = newContainer
            }
        }
    }

    private fun onModelRegistry(event: ModelRegistryEvent) {
        for (item in models) {
            ModelLoader.setCustomModelResourceLocation(item.first, item.second, ModelResourceLocation(item.third, "inventory"))
        }
    }

    private fun updateSwimAnimation(event: PlayerTickEvent) {
        if (event.phase == Phase.END) {
            val player = event.player

            if (player.hasSwimmingCap()) {
                player.isSwimming = if (player.isSprinting && !player.isRiding && !player.capabilities.isFlying) {
                    if (player.isSwimming) {
                        player.isInWater
                    } else {
                        canPlayerSwim(player)
                    }
                } else {
                    false
                }
                player.lastSwimAnimation = player.swimAnimation
                player.swimAnimation = if (player.isSwimming) {
                    min(1.0f, player.swimAnimation + 0.09f)
                } else {
                    max(0.0f, player.swimAnimation - 0.09f)
                }
            }
        }
    }

    private fun canPlayerSwim(player: EntityPlayer): Boolean {
        return player.isInWater && player.isInsideOfMaterial(Material.WATER)
    }

    private fun renderFancyBoundingBox(event: DrawBlockHighlightEvent) {
        event.isCanceled = true
    }

    private fun onModelBake(event: ModelBakeEvent) {
        val registry = event.modelRegistry
        val trident = ModelResourceLocation("futuremc:trident","inventory")
        val inventory = registry.getObject(trident)!!
        val hand = registry.getObject(ModelResourceLocation("futuremc:trident_in_hand","inventory"))!!

        registry.putObject(trident, TridentBakedModel(hand, inventory))
    }
}