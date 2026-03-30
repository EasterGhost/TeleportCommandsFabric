package org.AndrewElizabeth.teleportcommandsfabric.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.List;

/**
 * A string argument type that supports Unicode characters (e.g., Chinese, Japanese, Korean)
 * in unquoted strings. Brigadier's default StringArgumentType.string() only allows
 * [0-9A-Za-z_\-.+] in unquoted strings, which rejects non-ASCII characters.
 * <p>
 * This type reads any non-whitespace characters as a single argument token,
 * or a quoted string if it starts with a quote character.
 */
public class UnicodeStringArgumentType implements ArgumentType<String> {

	private UnicodeStringArgumentType() {
	}

	public static UnicodeStringArgumentType unicodeString() {
		return new UnicodeStringArgumentType();
	}

	public static String getString(CommandContext<?> context, String name) {
		return context.getArgument(name, String.class);
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		if (reader.canRead() && isQuote(reader.peek())) {
			return reader.readQuotedString();
		}
		return readUnicodeUnquotedString(reader);
	}

	private static boolean isQuote(char c) {
		return c == '"' || c == '\'';
	}

	private static String readUnicodeUnquotedString(StringReader reader) {
		final int start = reader.getCursor();
		while (reader.canRead() && reader.peek() != ' ') {
			reader.skip();
		}
		return reader.getString().substring(start, reader.getCursor());
	}

	@Override
	public Collection<String> getExamples() {
		return List.of("word", "hello", "你好");
	}
}
