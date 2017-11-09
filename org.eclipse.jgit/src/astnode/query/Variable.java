package astnode.query;

public class Variable {
	public String name;
	public String type;

	public Variable() {
		this("", "");
	}

	public Variable(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public boolean valid() {
		return !name.isEmpty() || !type.isEmpty();
	}
	
	public String toString() {
		return String.format("variable: {name: \"%s\", type: \"%s\"}\n", name, type);
	}
}
