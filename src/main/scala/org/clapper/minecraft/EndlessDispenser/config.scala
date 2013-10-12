package org.clapper.minecraft.EndlessDispenser

import java.util.logging.Logger
import org.bukkit.configuration.Configuration
import com.joshcough.minecraft.ScalaPlugin
import org.clapper.minecraft.lib.PluginLogging

/** Access to configuration data.
  *
  * @param config  the actual Bukkit configuration
  * @param logger  logger, for informative and debugging messages
  */
private[EndlessDispenser] class ConfigData(private val config: Configuration,
                                           private val logger: Logger) {
  val preserveEnchantments = config.getBoolean("preserve_enchantments", false)
}

private[EndlessDispenser] object ConfigData {
  private var data: Option[ConfigData] = None

  def apply(plugin: ScalaPlugin with PluginLogging): ConfigData = {
    this.synchronized {
      data match {
        case Some(cfg) => cfg

        case None => {
          plugin.saveDefaultConfig()
          val cfg = new ConfigData(plugin.getConfig, plugin.logger)
          data = Some(cfg)
          cfg
        }
      }
    }
  }
}
