package cs4r.labs.akka.examples.pingpong;

class Pong implements Message {
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