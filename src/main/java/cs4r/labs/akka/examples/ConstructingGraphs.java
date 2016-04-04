package cs4r.labs.akka.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.ClosedShape;
import akka.stream.FanInShape2;
import akka.stream.Graph;
import akka.stream.Inlet;
import akka.stream.Outlet;
import akka.stream.SourceShape;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.Broadcast;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Merge;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Zip;
import akka.stream.javadsl.ZipWith;

public class ConstructingGraphs {

	public static void main(String[] args) {

		ActorSystem system = ActorSystem.create("ConstructingGraphs");

		ActorMaterializer materializer = ActorMaterializer.create(system);

		firstExample(materializer);
		secondExample(materializer);
		thirdExample(materializer);
		fourthExample(materializer);
		fifthExample(materializer);

		system.terminate();

	}

	private static void firstExample(ActorMaterializer materializer) {
		final Source<Integer, NotUsed> in = Source.from(Arrays.asList(1, 2, 3, 4, 5));
		final Sink<List<String>, CompletionStage<List<String>>> sink = Sink.head();

		final Flow<Integer, Integer, NotUsed> f1 = Flow.of(Integer.class).map(elem -> elem + 10);
		final Flow<Integer, Integer, NotUsed> f2 = Flow.of(Integer.class).map(elem -> elem + 20);
		final Flow<Integer, String, NotUsed> f3 = Flow.of(Integer.class).map(elem -> elem.toString());
		final Flow<Integer, Integer, NotUsed> f4 = Flow.of(Integer.class).map(elem -> elem + 30);

		final RunnableGraph<CompletionStage<List<String>>> runnableGraph = RunnableGraph
				.<CompletionStage<List<String>>> fromGraph(GraphDSL.create(sink, (builder, out) -> {
					final UniformFanOutShape<Integer, Integer> bcast = builder.add(Broadcast.create(2));
					final UniformFanInShape<Integer, Integer> merge = builder.add(Merge.create(2));

					final Outlet<Integer> source = builder.add(in).out();

					builder.from(source).via(builder.add(f1)).viaFanOut(bcast).via(builder.add(f2)).viaFanIn(merge)
							.via(builder.add(f3.grouped(1000))).to(out);
					builder.from(bcast).via(builder.add(f4)).toFanIn(merge);

					return ClosedShape.getInstance();
				}));

		runnableGraph.run(materializer).whenComplete((result, failure) -> {
			result.stream().forEach(printWithExampleInfo("First example"));
		});
	}

	private static Consumer<Object> printWithExampleInfo(String exampleInfo) {
		return p -> System.out.println(exampleInfo + " " + p);
	}

	private static void secondExample(ActorMaterializer materializer) {
		final Sink<Integer, CompletionStage<Done>> topHeadSink = Sink
				.foreach(e -> printWithExampleInfo("Second example").accept(e));
		final Sink<Integer, CompletionStage<Done>> bottomHeadSink = Sink
				.foreach(e -> printWithExampleInfo("Second example").accept(e));

		final Flow<Integer, Integer, NotUsed> doubler = Flow.of(Integer.class).map(elem -> elem * 2);
		final Flow<Integer, Integer, NotUsed> tripler = Flow.of(Integer.class).map(elem -> elem * 3);

		final RunnableGraph<Pair<CompletionStage<Done>, CompletionStage<Done>>> runnableGraph2 = RunnableGraph
				.<Pair<CompletionStage<Done>, CompletionStage<Done>>> fromGraph(GraphDSL.create(topHeadSink, // import
																												// graph
						bottomHeadSink, // and this as well
						Keep.both(), (b, top, bottom) -> {
							final UniformFanOutShape<Integer, Integer> bcast = b.add(Broadcast.create(2));

							b.from(b.add(Source.single(1))).viaFanOut(bcast).via(b.add(doubler)).to(top);
							b.from(bcast).via(b.add(tripler)).to(bottom);
							return ClosedShape.getInstance();
						}));

		runnableGraph2.run(materializer);
	}

	private static void thirdExample(ActorMaterializer materializer) {
		final Graph<FanInShape2<Integer, Integer, Integer>, NotUsed> zip = ZipWith
				.create((Integer left, Integer right) -> Math.max(left, right));

		final Graph<UniformFanInShape<Integer, Integer>, NotUsed> pickMaxOfThree = GraphDSL.create(builder -> {
			final FanInShape2<Integer, Integer, Integer> zip1 = builder.add(zip);
			final FanInShape2<Integer, Integer, Integer> zip2 = builder.add(zip);

			builder.from(zip1.out()).toInlet(zip2.in0());
			// return the shape, which has three inputs and one output
			return new UniformFanInShape<Integer, Integer>(zip2.out(),
					new Inlet[] { zip1.in0(), zip1.in1(), zip2.in1() });
		});

		final Sink<Integer, CompletionStage<Done>> resultSink = Sink
				.foreach(e -> printWithExampleInfo("Third example").accept(e));

		final RunnableGraph<CompletionStage<Done>> g = RunnableGraph
				.<CompletionStage<Done>> fromGraph(GraphDSL.create(resultSink, (builder, sink) -> {
					// import the partial flow graph explicitly
					final UniformFanInShape<Integer, Integer> pm = builder.add(pickMaxOfThree);

					builder.from(builder.add(Source.single(1))).toInlet(pm.in(0));
					builder.from(builder.add(Source.single(2))).toInlet(pm.in(1));
					builder.from(builder.add(Source.single(3))).toInlet(pm.in(2));
					builder.from(pm.out()).to(sink);
					return ClosedShape.getInstance();
				}));

		final CompletionStage<Done> max = g.run(materializer);
	}

	private static void fourthExample(ActorMaterializer materializer) {
		class Ints implements Iterator<Integer> {
			private int next = 0;

			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public Integer next() {
				return next++;
			}
		}

		final Source<Integer, NotUsed> ints = Source.fromIterator(() -> new Ints());

		final Source<Pair<Integer, Integer>, NotUsed> pairs = Source.fromGraph(GraphDSL.create(builder -> {
			final FanInShape2<Integer, Integer, Pair<Integer, Integer>> zip = builder.add(Zip.create());

			builder.from(builder.add(ints.filter(i -> i % 2 == 0))).toInlet(zip.in0());
			builder.from(builder.add(ints.filter(i -> i % 2 == 1))).toInlet(zip.in1());

			return SourceShape.of(zip.out());
		}));

		final CompletionStage<Pair<Integer, Integer>> firstPair = pairs.runWith(Sink.<Pair<Integer, Integer>> head(),
				materializer);
		//
		firstPair.whenComplete((result, failure) -> {
			printWithExampleInfo("Fourth example").accept(result);
		});
	}

	private static void fifthExample(ActorMaterializer materializer) {
		Source<Integer, NotUsed> source1 = Source.single(1);
		Source<Integer, NotUsed> source2 = Source.single(2);

		final Source<Integer, NotUsed> sources = Source.combine(source1, source2, new ArrayList<>(),
				i -> Merge.<Integer> create(i));

		sources.runWith(Sink.<Integer, Integer> fold(0, (a, b) -> a + b), materializer);
	}

}
