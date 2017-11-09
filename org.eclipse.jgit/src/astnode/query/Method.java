package astnode.query;

public class Method {
	public String name;
	public String type;
	
	public Method() {
		this("", "");
	}
	
	public Method(String name, String type) {
		this.name = name;
		this.type = type;
	}
	
	public boolean valid() {
		return !name.isEmpty() || !type.isEmpty();
	}
	
	public String toString() {
		return String.format("method {name: \"%s\", type: \"%s\"}\n", name, type);
	}
}
