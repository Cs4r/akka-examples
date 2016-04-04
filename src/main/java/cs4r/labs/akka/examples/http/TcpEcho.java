package cs4r.labs.akka.examples.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.stream.ActorMaterializer;
import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.javadsl.BidiFlow;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Tcp;
import akka.stream.javadsl.Tcp.IncomingConnection;
import akka.stream.javadsl.Tcp.ServerBinding;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.util.ByteIterator;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import cs4r.labs.akka.examples.BidiFlows;
import cs4r.labs.akka.examples.BidiFlows.Message;
import scala.concurrent.Future;
import scala.runtime.BoxedUnit;
import cs4r.labs.akka.examples.FrameParser;

public class TcpEcho {

	private static Flow<Message, ByteString, NotUsed> codec = Flow.of(Message.class).map(TcpEcho::toBytes)
			.map(TcpEcho::addLengthHeader);

	private static Flow<ByteString, Message, NotUsed> decodec = Flow.of(ByteString.class).map(TcpEcho::fromBytes);

	private static Flow<ByteString, ByteString, NotUsed> framing = Flow.of(ByteString.class).via(new FrameParser());

	/**
	 * Use without parameters to start both client and server.
	 *
	 * Use parameters `server 0.0.0.0 6001` to start server listening on port
	 * 6001.
	 *
	 * Use parameters `client 127.0.0.1 6001` to start client connecting to
	 * server on 127.0.0.1:6001.
	 *
	 */
	public static void main(String[] args) throws IOException {

		if (args.length == 0) {
			ActorSystem system = ActorSystem.create("ClientAndServer");
			InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 6000);
			server(system, serverAddress);
			client(system, serverAddress);
		} else {
			InetSocketAddress serverAddress;
			if (args.length == 3) {
				serverAddress = new InetSocketAddress(args[1], Integer.valueOf(args[2]));
			} else {
				serverAddress = new InetSocketAddress("127.0.0.1", 6000);
			}
			if (args[0].equals("server")) {
				ActorSystem system = ActorSystem.create("Server");
				server(system, serverAddress);
			} else if (args[0].equals("client")) {
				ActorSystem system = ActorSystem.create("Client");
				client(system, serverAddress);
			}
		}
	}

	public static void server(ActorSystem system, InetSocketAddress serverAddress) {
		final ActorMaterializer materializer = ActorMaterializer.create(system);

		final Sink<IncomingConnection, CompletionStage<Done>> handler = Sink.foreach(conn -> {
			System.out.println("Client connected from: " + conn.remoteAddress());
			
			Flow<ByteString, ByteString, NotUsed> create = Flow.of(ByteString.class).via(decodec)
					.<Message> map(m -> new Pong(((Ping) m).id)).via(codec).via(framing);

			conn.handleWith(create, materializer);
		});

		final CompletionStage<ServerBinding> bindingFuture = Tcp.get(system)
				.bind(serverAddress.getHostString(), serverAddress.getPort()).to(handler).run(materializer);

		bindingFuture.whenComplete((binding, throwable) -> {
			System.out.println("Server started, listening on: " + binding.localAddress());
		});

		bindingFuture.exceptionally(e -> {
			System.err.println("Server could not bind to " + serverAddress + " : " + e.getMessage());
			system.terminate();
			return null;
		});

	}

	public static void client(ActorSystem system, InetSocketAddress serverAddress) {
		final ActorMaterializer materializer = ActorMaterializer.create(system);

		Source<Message, NotUsed> source = Source.from(Arrays.asList(0, 1, 2)).<Message> map(id -> new Ping(id));

		Source<ByteString, NotUsed> encodedAndFramed = source.via(codec).via(framing);

		Source<ByteString, NotUsed> responseStream = encodedAndFramed
				.via(Tcp.get(system).outgoingConnection(serverAddress.getHostString(), serverAddress.getPort()));

		Source<Message, NotUsed> integers = responseStream.via(decodec);

		integers.to(Sink.foreach(e -> System.out.println(e))).run(materializer);
	}

	public static ByteString addLengthHeader(ByteString bytes) {
		final int len = bytes.size();
		return new ByteStringBuilder().putInt(len, ByteOrder.LITTLE_ENDIAN).append(bytes).result();
	}

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

	static interface Message {
	}

	static class Ping implements Message {
		final int id;

		public Ping(int id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Ping) {
				return ((Ping) o).id == id;
			} else
				return false;
		}

		@Override
		public int hashCode() {
			return id;
		}

		@Override
		public String toString() {
			return "Ping " + String.valueOf(id);
		}
	}

	static class Pong implements Message {
		final int id;

		public Pong(int id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Pong) {
				return ((Pong) o).id == id;
			} else
				return false;
		}

		@Override
		public int hashCode() {
			return id;
		}

		@Override
		public String toString() {
			return "Pong " + String.valueOf(id);
		}
	}

}
