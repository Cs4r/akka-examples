package cs4r.labs.akka.examples.tweets;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Tweet {
	public final Author author;
	public final long timestamp;
	public final String body;

	public Tweet(Author author, long timestamp, String body) {
		this.author = author;
		this.timestamp = timestamp;
		this.body = body;
	}

	public Set<Hashtag> hashtags() {
		return Arrays.asList(body.split(" ")).stream().filter(a -> a.startsWith("#")).map(a -> new Hashtag(a))
				.collect(Collectors.toSet());
	}

}
