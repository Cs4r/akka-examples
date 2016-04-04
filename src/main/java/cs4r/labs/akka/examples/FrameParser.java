package cs4r.labs.akka.examples;

import java.nio.ByteOrder;

import akka.stream.Attributes;
import akka.stream.FlowShape;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.stage.AbstractInHandler;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import akka.util.ByteString;

public class FrameParser extends GraphStage<FlowShape<ByteString, ByteString>> {
	public Inlet<ByteString> in = Inlet.create("FrameParser.in");
	public Outlet<ByteString> out = Outlet.create("FrameParser.out");
	private FlowShape<ByteString, ByteString> shape = FlowShape.of(in, out);

	@Override
	public FlowShape<ByteString, ByteString> shape() {
		return shape;
	}

	@Override
	public GraphStageLogic createLogic(Attributes inheritedAttributes) {
		return new GraphStageLogic(shape) {

			// this holds the received but not yet parsed bytes
			private ByteString stash = ByteString.empty();
			// this holds the current message length or -1 if at a boundary
			private int needed = -1;

			{
				setHandler(in, new AbstractInHandler() {
					@Override
					public void onPush() throws Exception {
						ByteString bytes = grab(in);
						stash = stash.concat(bytes);
						run();
					}

					@Override
					public void onUpstreamFinish() throws Exception {
						if (stash.isEmpty())
							completeStage();
						// wait with completion and let run() complete when the
						// rest of the stash has been sent downstream
					}
				});

				setHandler(out, new AbstractOutHandler() {
					@Override
					public void onPull() throws Exception {
						if (isClosed(in))
							run();
						else
							pull(in);
					}
				});
			}

			private void run() {
				if (needed == -1) {
					// are we at a boundary? then figure out next length
					if (stash.size() < 4) {
						if (isClosed(in))
							completeStage();
						else
							pull(in);
					} else {
						needed = stash.iterator().getInt(ByteOrder.LITTLE_ENDIAN);
						stash = stash.drop(4);
						run(); // cycle back to possibly already emit the next
								// chunk
					}
				} else if (stash.size() < needed) {
					// we are in the middle of a message, need more bytes
					// or in is already closed and we cannot pull any more
					if (isClosed(in))
						completeStage();
					else
						pull(in);
				} else {
					// we have enough to emit at least one message, so do it
					final ByteString emit = stash.take(needed);
					stash = stash.drop(needed);
					needed = -1;
					push(out, emit);
				}
			}
		};
	}
}