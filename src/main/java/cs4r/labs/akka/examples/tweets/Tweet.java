package cs4r.labs.akka.examples.tweets;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Tweet {
	public final String author;
	public final long timestamp;
	public final String body;

	public Tweet(String author, long timestamp, String body) {
		this.author = author;
		this.timestamp = timestamp;
		this.body = body;
	}

	public Set<String> hashtags() {
		Set<String> hashtags = Arrays.asList(body.split(" ")).stream().filter(t -> t.trim().startsWith("#"))
				.collect(Collectors.toSet());
		return hashtags;
	}

}
