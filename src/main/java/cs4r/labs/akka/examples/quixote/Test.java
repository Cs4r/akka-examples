package cs4r.labs.akka.examples.quixote;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.CompletionStage;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.IOResult;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Framing;
import akka.stream.javadsl.FramingTruncation;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

public class Test {

	public static void main(String[] args) throws IOException {
		ActorSystem system = ActorSystem.create("ClientAndServer");
		ActorMaterializer materializer = ActorMaterializer.create(system);

		File arg0 = Paths.get("quixote.txt").toFile();

		Source<String, CompletionStage<IOResult>> lines = FileIO.fromFile(arg0)
				.via(Framing.delimiter(ByteString.fromString("\r\n"), 100, FramingTruncation.ALLOW))
				.map(b -> b.utf8String());

		Source<Integer, NotUsed> integers = Source.range(0, 10000);

		Source<Pair<String, Integer>, CompletionStage<IOResult>> zip = lines.zip(integers);


		// IntStream.iterate(0, x -> x+1).it
		// lines.zip(Source.from(null));

		zip.runForeach(System.out::println, materializer).whenComplete((done, throwable) -> {

			if (throwable != null) {
				System.out.println(throwable.getLocalizedMessage());
			}

			system.terminate();
		});

	}

}
