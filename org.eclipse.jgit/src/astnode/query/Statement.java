package astnode.query;

public class Statement {
	public String type;
	
	public Statement() {
		this("");
	}
	
	public Statement(String type) {
		this.type = type;
	}
	
	public boolean valid() {
		return !type.isEmpty();
	}
	
	public String toString() {
		return String.format("statement {type: \"%s\"}\n", type);
	}
}
