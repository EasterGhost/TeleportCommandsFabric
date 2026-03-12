package org.AndrewElizabeth.teleportcommandsfabric.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.AndrewElizabeth.teleportcommandsfabric.Constants.ASSETS_ID;

public final class TranslationHelper {

	private TranslationHelper() {
	}

	public static @NotNull MutableComponent getTranslatedText(String key, ServerPlayer player,
			MutableComponent... args) {
		String language = player.clientInformation().language().toLowerCase();
		return getTranslatedText(key, language, args);
	}

	public static @NotNull MutableComponent getTranslatedText(String key, String language,
			MutableComponent... args) {
		String regex = "%(\\d+)%";
		Pattern pattern = Pattern.compile(regex);

		try {
			String filePath = String.format("/assets/%s/lang/%s.json", ASSETS_ID, language);
			InputStream stream = TeleportCommands.class.getResourceAsStream(filePath);

			Reader reader = new InputStreamReader(
					Objects.requireNonNull(stream,
							String.format("Couldn't find the required language file for \"%s\"", language)),
					StandardCharsets.UTF_8);
			JsonElement json = JsonParser.parseReader(reader);
			String translation = json.getAsJsonObject().get(key).getAsString();

			Matcher matcher = pattern.matcher(Objects.requireNonNull(translation));

			MutableComponent component = Component.literal("");
			int lastIndex = 0;

			while (matcher.find()) {
				component.append(Component.literal(translation.substring(lastIndex, matcher.start())));

				int index = Integer.parseInt(matcher.group(1));
				component.append(args[index]);

				lastIndex = matcher.end();
			}
			component.append(translation.substring(lastIndex));

			return component;

		} catch (Exception e) {

			try {
				if (!Objects.equals(language, "en_us")) {
					String filePath = String.format("/assets/%s/lang/en_us.json", ASSETS_ID);
					InputStream stream = TeleportCommands.class.getResourceAsStream(filePath);

					Reader reader = new InputStreamReader(
							Objects.requireNonNull(stream,
									String.format("Couldn't find the required language file for \"%s\"", language)),
							StandardCharsets.UTF_8);
					JsonElement json = JsonParser.parseReader(reader);
					String translation = json.getAsJsonObject().get(key).getAsString();

					Matcher matcher = pattern
							.matcher(Objects.requireNonNull(translation, "translation cannot be null"));

					MutableComponent component = Component.literal("");
					int lastIndex = 0;

					while (matcher.find()) {
						component.append(Component.literal(translation.substring(lastIndex, matcher.start())));

						int index = Integer.parseInt(matcher.group(1));
						component.append(args[index]);

						lastIndex = matcher.end();
					}
					component.append(translation.substring(lastIndex));

					return component;
				}
			} catch (Exception ignored1) {
			}
			Constants.LOGGER.error("Key \"{}\" not found in the default language (en_us), sending raw key as fallback.",
					key);
			return Component.literal(key);
		}
	}
}
