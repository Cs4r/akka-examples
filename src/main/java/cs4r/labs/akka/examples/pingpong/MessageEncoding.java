package cs4r.labs.akka.examples.pingpong;

import java.nio.ByteOrder;

import akka.util.ByteIterator;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;

public class MessageEncoding {
	
	public static ByteString toBytes(Message msg) {
		if (msg instanceof Ping) {
			final int id = ((Ping) msg).id;
			return new ByteStringBuilder().putByte((byte) 1).putInt(id, ByteOrder.LITTLE_ENDIAN).result();
		} else {
			final int id = ((Pong) msg).id;
			return new ByteStringBuilder().putByte((byte) 2).putInt(id, ByteOrder.LITTLE_ENDIAN).result();
		}
	}

	public static Message fromBytes(ByteString bytes) {
		final ByteIterator it = bytes.iterator();
		switch (it.getByte()) {
		case 1:
			return new Ping(it.getInt(ByteOrder.LITTLE_ENDIAN));
		case 2:
			return new Pong(it.getInt(ByteOrder.LITTLE_ENDIAN));
		default:
			throw new RuntimeException("message format error");
		}
	}
}
