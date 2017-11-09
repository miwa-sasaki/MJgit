package astnode.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author miwaaa8
 *
 */
public class MJQuery {
	/**
	 *
	 */
	public List<Context> contexts;

	/**
	 *
	 */
	public List<Method> methods;

	/**
	 *
	 */
	public List<Variable> variables;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * System.out.println(new MJQuery("method.name=main&method.type=int",
		 * "simple")); System.out.println(new
		 * MJQuery("variable.name=main&variable.type=int", "simple"));
		 * System.out.println(new MJQuery("method.name=main&variable.type=int",
		 * "simple")); System.out.println(new MJQuery("context=comment",
		 * "simple"));//コメントだけ System.out.println(new
		 * MJQuery("context=annotation", "simple"));//アノテーションだけ
		 * System.out.println(new MJQuery("context=statement",
		 * "simple"));//ステートメントの詳細指定しない
		 * 
		 * //コンテキストの複数指定もクエリ的にはok System.out.println(new
		 * MJQuery("context=comment&context=annotation", "simple"));
		 * System.out.println(new MJQuery("context=comment&method.name=main",
		 * "simple"));
		 */


		//System.out.println(new MJQuery("method.name=main", "simple"));
		// System.out.println(new
		// MJQuery("method.name=main&method.name=toString", "simple"));
		// System.out.println(new MJQuery("method.name=main&context=statement",
		// "simple"));

		//System.out.println(new MJQuery("method.name=toString&method.name=main", "simple"));


		//System.out.println(new MJQuery("variable.name=tmp&variable.name=j", "simple"));


		//JSON形式(無理)
		/*
		System.out.println(new MJQuery("{\"method\": [{\"name\": \"main\", \"type\": \"int\"}]}", "json"));
		System.out.println(new MJQuery("{\"method\": [{\"name\": \"main\", \"type\": \"int\"}, {\"name\": \"toString\", \"type\": \"int\"}]}", "json"));
		 */
	}

	/**
	 * queryの例
	 * -cx method.name=main&method.type=int
	 *     -> {method: [{name: main, type: int}]}
	 *     (↑simple)
	 *
	 * -cx method.name=main&method.type=int&method.name=xxx
	 * -cx xxx=comment　--> context=COMMENT?
	 *
	 * -cxjson {method: [{name: main, type: int}]}
	 * -cxjson {method: [{name: main}, {type: int}]}
	 *
	 * @param query
	 * @param type
	 */
	public MJQuery(String query, String type) {
		contexts = new ArrayList<>();
		methods = new ArrayList<>();
		variables = new ArrayList<>();

		// if(type.matches("simple")) {
		parseSimple(query);
		// } else if (type.matches("json")) {
		// parseJson(query);
		// }
	}

	//クエリを分解，解析
	private void parseSimple(String query) {
		Method method = new Method();
		Variable variable = new Variable();

		for (String q : query.split("&")) {
			//System.out.println("分解されたクエリ: "+q);
			if (q.isEmpty()) break;
			String left = q.split("=")[0];
			String right = q.split("=")[1];

			//メソッドを指定した場合
			if (left.startsWith("method.")) {
				left = left.replaceAll("^method.", "");
				if (left.matches("name")) {
					method.name = right;
				} else if (left.matches("type")) {
					method.type = right;
				}
				// 変数を指定した場合
			} else if (left.startsWith("variable.")) {
				left = left.replaceAll("^variable.", "");
				if (left.matches("name")) {
					variable.name = right;
				} else if (left.matches("type")) {
					variable.type = right;
				}
				// contextを指定した場合
			} else if (left.startsWith("context")) {
				System.out.println("contextを指定");
				if(right.matches("comment")) {
					contexts.add(Context.COMMENT);
				} else if (right.matches("annotation")) {
					contexts.add(Context.ANNOTATION);
				} else if (right.matches("javadoc")) {
					contexts.add(Context.JAVADOC);
				} else if (right.matches("statement")) {
					contexts.add(Context.STATEMENT);
				}
			} else {
				System.out.println("query broken...");
				return;
			}
			//分解結果を配列に追加
			// methodクエリの中身が空なら追加しない
			if (method.valid()) {
				methods.add(method);
				//初期化．これせな複数の検索のときうまく追加されへん
				method = new Method();
				contexts.add(Context.STATEMENT);
			}
			// variableクエリの中身が空なら追加しない
			if (variable.valid()) {
				variables.add(variable);
				//初期化．これせな複数の検索のときうまく追加されへん
				variable = new Variable();
				contexts.add(Context.STATEMENT);
			}
		}
	}

	// コンテキストの種類と，クエリの内容を返す
	// 新しいオブジェクトとか増やしたらここに追加せな
	public String toString() {
		//System.out.println("MJQのtoSt");
		StringBuffer sb = new StringBuffer();
		for (Context context : contexts) {
			sb.append(String.format("context: \"%s\"\n", context));
		}
		int i=0;
		for (Method method : methods) {
			//System.out.println(i);
			i++;
			sb.append(method);
		}
		for (Variable variable : variables) {
			sb.append(variable);
		}
		return sb.toString();
	}
}
