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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.AndrewElizabeth.teleportcommandsfabric.Constants.ASSETS_ID;

public final class TranslationHelper {
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\d+)%");
	private static final Map<String, Map<String, String>> TRANSLATION_CACHE = new ConcurrentHashMap<>();

	private TranslationHelper() {
	}

	public static @NotNull MutableComponent getTranslatedText(String key, ServerPlayer player,
			MutableComponent... args) {
		String language = player.clientInformation().language().toLowerCase();
		return getTranslatedText(key, language, args);
	}

	public static @NotNull MutableComponent getTranslatedText(String key, String language,
			MutableComponent... args) {
		try {
			return buildTranslatedComponent(getTranslation(language, key), args);
		} catch (Exception e) {
			try {
				if (!Objects.equals(language, "en_us")) {
					return buildTranslatedComponent(getTranslation("en_us", key), args);
				}
			} catch (Exception ignored) {
			}
			Constants.LOGGER.error("Key \"{}\" not found in the default language (en_us), sending raw key as fallback.",
					key);
			return Component.literal(key);
		}
	}

	private static String getTranslation(String language, String key) {
		Map<String, String> translations = TRANSLATION_CACHE.computeIfAbsent(language, TranslationHelper::loadLanguage);
		String translation = translations.get(key);
		if (translation == null) {
			throw new IllegalArgumentException("Missing translation key: " + key);
		}
		return translation;
	}

	private static Map<String, String> loadLanguage(String language) {
		String filePath = String.format("/assets/%s/lang/%s.json", ASSETS_ID, language);
		try (InputStream stream = TeleportCommands.class.getResourceAsStream(filePath)) {
			if (stream == null) {
				Constants.LOGGER.warn("Couldn't find the required language file for \"{}\", falling back to en_us.", language);
				// We return the en_us map to cache it so we do not attempt to load it again to prevent an exception storm
				return "en_us".equals(language) ? new ConcurrentHashMap<>() : TRANSLATION_CACHE.computeIfAbsent("en_us", TranslationHelper::loadLanguage);
			}
			Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
			JsonElement json = JsonParser.parseReader(reader);
			Map<String, String> translations = new ConcurrentHashMap<>();
			json.getAsJsonObject().entrySet()
					.forEach(entry -> translations.put(entry.getKey(), entry.getValue().getAsString()));
			return translations;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to load language file: {}, falling back to en_us.", language, e);
			return "en_us".equals(language) ? new ConcurrentHashMap<>() : TRANSLATION_CACHE.computeIfAbsent("en_us", TranslationHelper::loadLanguage);
		}
	}

	private static @NotNull MutableComponent buildTranslatedComponent(String translation, MutableComponent... args) {
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(Objects.requireNonNull(translation, "translation cannot be null"));
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
}
