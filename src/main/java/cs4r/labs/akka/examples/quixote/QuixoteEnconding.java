package cs4r.labs.akka.examples.quixote;

import java.nio.ByteOrder;

import akka.util.ByteString;

public class QuixoteEnconding {

	public static String stringFromBytes(ByteString bytes) {
		return bytes.utf8String();
	}

	public static ByteString stringToBytes(String string) {
		return ByteString.fromString(string);
	}

	public static Integer integerFromBytes(ByteString bytes) {
		return bytes.iterator().getInt(ByteOrder.BIG_ENDIAN);
	}

	public static ByteString integerToBytes(int integer) {
		return ByteString.createBuilder().putInt(integer, ByteOrder.BIG_ENDIAN).result();
	}

}
