package cs4r.labs.akka.examples.tweets;

import java.util.Arrays;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import cs4r.labs.akka.examples.tweets.Author;
import cs4r.labs.akka.examples.tweets.Hashtag;
import cs4r.labs.akka.examples.tweets.Tweet;

public class ReactiveTweets {

	public static final Hashtag AKKA = new Hashtag("#akka");

	public static void main(String[] args) {
		final ActorSystem system = ActorSystem.create("reactive-tweets");
		final Materializer materializer = ActorMaterializer.create(system);

		Source.from(Arrays.asList(new Tweet(new Author("juan"), 1000, "This is a test with #akka"),
				new Tweet(new Author("pepe"), 2000, "I am trendy guys!"),
				new Tweet(new Author("maria"), 2000, "#this #is #an #instagram #tweet"),
				new Tweet(new Author("ilitri"), 2000, "#akka lo peta shurs")

		));

		final Source<Tweet, NotUsed> tweets = null;

		final Source<Author, NotUsed> authors = tweets.filter(t -> t.hashtags().contains(AKKA)).map(t -> t.author);

		authors.runWith(Sink.foreach(System.out::println), materializer);
	}

}
