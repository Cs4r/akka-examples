package cs4r.labs.akka.examples.tweets;

import java.util.Arrays;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
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
						new Tweet("ilitri", 4000, "Ou yeah! usare #akka en el foro")
				)
		);
		//@formatter:on

		final Source<String, NotUsed> authors = tweets.filter(t -> t.hashtags().contains(AKKA)).map(t -> t.author);

		authors.runForeach(System.out::println, materializer)
				.whenComplete((a, b) -> system.terminate());

	}

}
