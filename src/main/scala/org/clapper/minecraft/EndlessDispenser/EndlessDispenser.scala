package org.clapper.minecraft.EndlessDispenser

import com.joshcough.minecraft.{BukkitEnrichment, ListenersPlugin}

import org.clapper.minecraft.lib.{ConfigUtil, SchedulerUtil,
                                  PluginLogging, ScalaPluginExtras}
import org.clapper.minecraft.lib.Listeners._

import scala.collection.JavaConverters._

import org.bukkit.block.{Dispenser, Block, Sign}
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.enchantments.Enchantment
import org.bukkit.potion.Potion
import scala.util.Failure
import org.bukkit.material.Dye

/** The actual plugin.
  */
class EndlessDispenserPlugin
  extends ListenersPlugin
  with    PluginLogging
  with    ScalaPluginExtras {

  import BukkitEnrichment._

  import org.clapper.minecraft.lib.Implicits.Player._
  import org.clapper.minecraft.lib.Implicits.Logging._
  import org.clapper.minecraft.lib.Implicits.Block._

  val listeners = List(
    OnBlockDispense { handleBlockDispense(_) }
  )

  lazy val configuration = ConfigUtil.get(this)
  lazy val preserveEnchantments = configuration.booleanValue(
    "preserve_enchantments", false
  )
  lazy val allowedMaterialsSet = configuration.stringList("allowed_materials").
                                               toSet
  lazy val allowedMaterials = allowedMaterialsSet.filter { s: String =>
    Constants.MaterialMap.contains(s)
  }.
  map { s: String =>
    Constants.MaterialMap(s)
  }

  override def onEnable(): Unit = {
    super.onEnable()
  }

  override def onDisable(): Unit = {
    super.onDisable()
  }

  private def handleBlockDispense(event: BlockDispenseEvent): Unit = {
    val block = event.getBlock
    if ((! event.isCancelled) && isEndlessDispenser(block)) {
      block.state match {
        case d: Dispenser => dispenseFromEndlessDispenser(d, event)
        case _ =>
      }
    }
  }

  private def dispenseFromEndlessDispenser(dispenser: Dispenser,
                                           event:     BlockDispenseEvent) = {
    val itemStack    = event.getItem
    val inv          = dispenser.getInventory
    val material     = itemStack.getType

    SchedulerUtil.runLater(this) { () =>
      // If we just dispensed the last item, replace it.
      if (! inv.contains(material)) {
        if (allowedMaterials.contains(material)) {
          logDebug(s"Reloading endless dispenser.")
          inv.addItem(createItem(itemStack))
        }
        else {
          logMessage("Can't reload endless dispenser: " +
                     s"Material ${material} isn't allowed.")
        }
      }
    }
  }

  private def createItem(item: ItemStack): ItemStack = {

    val material = item.getType
    if (material == Material.POTION) {
      val potion = Potion.fromItemStack(item)
      val newPotion = new Potion(potion.getType, potion.getLevel)
      newPotion.setSplash(potion.isSplash)
      newPotion.setHasExtendedDuration(potion.hasExtendedDuration)
      newPotion.toItemStack(1)
    }

    else {
      def mapEnchantments(m: java.util.Map[Enchantment, java.lang.Integer]) = {
        m.asScala.toMap.map { t => (t._1 -> t._2.intValue) }
      }

      val stack = item.clone()

      for ((enchantment, level) <- mapEnchantments(item.getEnchantments)) {
        if (preserveEnchantments)
          stack.addEnchantment(enchantment, level)
        else
          stack.removeEnchantment(enchantment)
      }

      if (! preserveEnchantments) {
        val meta = stack.getItemMeta
        meta.setLore(new java.util.ArrayList[String]())
        for ((enchantment, level) <- mapEnchantments(meta.getEnchants)) {
          meta.removeEnchant(enchantment)
        }

        meta.setDisplayName(null)
        stack.setItemMeta(meta)
      }

      stack
    }
  }

  private def isEndlessDispenser(block: Block) = {
    (block.getType == Material.DISPENSER) &&
    (block.solidNeighbors.filter { isMagicSign(_) }.length > 0)
  }

  private def isMagicSign(block: Block) = {
    if (isAttachedSign(block)) {
      block.getState match {
        case sign: Sign if signHasMagic(sign) => true
        case _                                => false
      }
    }
    else {
      false
    }
  }

  private def signHasMagic(sign: Sign) = {
    sign.getLines.toList.filter { _.trim == Constants.EndlessLabel }.length > 0
  }

  private def isSign(block: Block) = {
    val t = block.getType
    (t == Material.WALL_SIGN) || (t == Material.SIGN_POST)
  }

  private def isAttachedSign(block: Block) = {
     isSign(block) && (block.attachedTo != None)
  }
}
