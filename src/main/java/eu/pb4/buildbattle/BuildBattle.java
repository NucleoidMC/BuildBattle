package eu.pb4.buildbattle;

import eu.pb4.buildbattle.themes.ThemesRegistry;
import net.fabricmc.api.ModInitializer;
import xyz.nucleoid.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.pb4.buildbattle.game.BBConfig;
import eu.pb4.buildbattle.game.BBWaiting;

public class BuildBattle implements ModInitializer {

    public static final String ID = "buildbattle";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<BBConfig> TYPE = GameType.register(
            new Identifier(ID, "buildbattle"),
            BBWaiting::open,
            BBConfig.CODEC
    );

    @Override
    public void onInitialize() {
        ThemesRegistry.register();
    }
}
