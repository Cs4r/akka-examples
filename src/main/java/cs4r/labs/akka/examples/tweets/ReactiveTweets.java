package cs4r.labs.akka.examples.tweets;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

public class ReactiveTweets {

	public static final String AKKA = "#akka";

	public static void main(String[] args) {
		final ActorSystem system = ActorSystem.create("reactive-tweets");
		final Materializer materializer = ActorMaterializer.create(system);

		//@formatter:off
		final Source<Tweet, NotUsed> tweets = Source.from(
				Arrays.asList(
						new Tweet("pepe", 1000, "Wow! #akka is great"),
						new Tweet("juan", 2000, "I am trendy, guys!"),
						new Tweet("maria", 3000, "#This #is #an #instagram #tweet"),
						new Tweet("ilitri", 4000, "Ou yeah! usaré #akka en el foro")
				)
		);
		//@formatter:on

		final Source<String, NotUsed> sourceOfauthors = tweets.filter(t -> t.hashtags().contains(AKKA))
				.map(t -> t.author);

		sourceOfauthors.runForeach(System.out::println, materializer);

		final Sink<Integer, CompletionStage<Integer>> sumSink = Sink.<Integer, Integer> fold(0,
				(acc, elem) -> acc + elem);

		final RunnableGraph<CompletionStage<Integer>> counter = tweets.map(t -> 1).toMat(sumSink, Keep.right());

		final CompletionStage<Integer> sum = counter.run(materializer);

		sum.thenAcceptAsync(c -> System.out.println("Total tweets processed: " + c), system.dispatcher())
				.whenComplete((a, b) -> system.terminate());

	}

}
