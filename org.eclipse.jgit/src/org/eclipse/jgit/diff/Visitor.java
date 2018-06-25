package org.eclipse.jgit.diff;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;

import astnode.query.Context;
import astnode.query.MJQuery;
import astnode.query.Method;
import astnode.query.Variable;

//本質解析
/**
 * @author miwaaa8
 *
 */
public class Visitor extends ASTVisitor {

	CompilationUnit compilationUnit;
	private String[] source;

	// 構文エラーの有無
	boolean SyntaxFlg = true;

	boolean ast;
	boolean com;
	boolean jd;
	boolean anno;

	private MJQuery query;

	/**
	 *
	 */
	public Set<Integer> lineNumbers;

	// コンストラクタ
	/**
	 * @param compilationUnit
	 * @param source
	 * @param query
	 * @param lineNumbers
	 * @param queryNum
	 */
	public Visitor(CompilationUnit compilationUnit, String[] source,
			MJQuery query, Set<Integer> lineNumbers, int queryNum) {
		super();
		this.compilationUnit = compilationUnit;
		this.source = source;
		ast = com = jd = anno = false;
		if (query.contexts.get(queryNum) == Context.STATEMENT)
			this.ast = true;
		if (query.contexts.get(queryNum) == Context.COMMENT)
			this.com = true;
		if (query.contexts.get(queryNum) == Context.JAVADOC)
			this.jd = true;
		if (query.contexts.get(queryNum) == Context.ANNOTATION)
			this.anno = true;

		this.query = query;
		// this.lineNumbers = new TreeSet<>();
		this.lineNumbers = lineNumbers;
		// ぬるぽ
		if (lineNumbers == null) {
			this.lineNumbers = new TreeSet<>();
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(CompilationUnit node) {
		if (ast) {
			// 構文エラーがあった場合
			if ((node.getFlags() & ASTNode.MALFORMED) == ASTNode.MALFORMED) {
				// System.out.println("構文エラー！ "+ (node.getFlags() &
				// CompilationUnit.MALFORMED));
				SyntaxFlg = false;
			}
			if (!hasSpecificQuery()) {
				// 詳細なクエリが無い場合
				// 全部書き出す(jd, anno, comの除去はそれぞれのvisitorがフラグをみてやってくれる)
				// System.out.println("STATEMENT全部指定");
				addLineNumberAll(source);
			}
		}
		return super.visit(node);
	}

	// 元ファイルの全行番号を書き出す
	private void addLineNumberAll(String[] source) {
		int start = 0;
		int end = source.length;
		// System.out.println("行数: "+end);

		for (int i = start; i <= end; i++) {
			lineNumbers.add(i);
		}
	}

	// nodeの元ファイルにおける行番号を書き出す
	private void addLineNumber(ASTNode node) {
		if (node == null)
			return;

		// System.out.println("addLineNumberrrrrr");
		int start = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
		int end = compilationUnit
				.getLineNumber(node.getStartPosition() + node.getLength()) - 1;
		// System.out.println("start is : " + start);
		// System.out.println("end is : " + end);

		for (int i = start; i <= end; i++) {
			lineNumbers.add(i);
		}
	}

	// nodeの最初と最後の行番号を書き出す
	// javadocは無視
	private void addLineNumberStartEnd(MethodDeclaration node) {
		if (node == null)
			return;
		// System.out.println("---------node:" + node);
		// System.out.println("---------node.getReturnType2().getStartPosition():"
		// + node.getReturnType2().getStartPosition());
		// System.out.println(
		// "---------node.getReturnType2():" + node.getReturnType2());
		// System.out.println(
		// "---------node.getstartPosition:" + node.getStartPosition());

		// getReturnType2のポジションを取得することでJavadocを除外できる
		// 嘘，getReturnType2やと返り値の宣言ないやつでぬるぽなっちゃう．
		// getNameのポジションを取得する！
		// int start =
		// compilationUnit.getLineNumber(node.getReturnType2().getStartPosition())
		// -1;
		//System.out.println("node.getName():" + node.getName());
		// System.out.println("node.getName().getStartPosition()"+
		// node.getName().getStartPosition());

		int start = compilationUnit
				.getLineNumber(node.getName().getStartPosition()) - 1;
		int end = compilationUnit
				.getLineNumber(node.getStartPosition() + node.getLength()) - 1;
		// System.out.println("start is : " + start);
		// System.out.println("end is : " + end);
		lineNumbers.add(start);
		lineNumbers.add(end);
	}

	// nodeの最初と最後まで全部の行番号を書き出す
	// javadocは無視
	private void addLineNumberofMethod(MethodDeclaration node) {
		if (node == null)
			return;
		// getReturnType2のポジションを取得することでJavadocを除外できる
		// int start =
		// compilationUnit.getLineNumber(node.getReturnType2().getStartPosition())
		// -1;

		// 嘘，getReturnType2やと返り値の宣言ないやつでぬるぽなっちゃう．
		// getNameのポジションを取得する！
		// System.out.println("node.getName():" + node.getName());
		// System.out.println("node.getName().getStartPosition():"+
		// node.getName().getStartPosition());
		int start = compilationUnit
				.getLineNumber(node.getName().getStartPosition()) - 1;

		// メソッドがアノテーションを持っているかどうか
		// System.out.println(node.modifiers().get(0));
		String modify = node.modifiers().get(0).toString();
		boolean methodHasAnnotation = false;
		if (modify != null && modify.startsWith("@")) {
			// System.out.println("@始まり");
			methodHasAnnotation = true;
		}

		// System.out.println("node.getExtra:"+node.getExtraDimensions());
		// System.out.println("node.getModifier"+node.getModifiers());
		// メソッドかつアノテーションが指定された場合，スタート位置はアノテーションにする
		if (anno && methodHasAnnotation) {
			// System.out.println("anno && has");
			start = compilationUnit.getLineNumber(node.getExtraDimensions());
		}

		int end = compilationUnit
				.getLineNumber(node.getStartPosition() + node.getLength()) - 1;
		// System.out.println("start is : " + start);
		// System.out.println("end is : " + end);
		for (int i = start; i <= end; i++) {
			lineNumbers.add(i);
		}
	}

	// nodeの元ファイルにおける行番号を削除する
	private void removeLineNumber(ASTNode node) {
		int start = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
		int end = compilationUnit
				.getLineNumber(node.getStartPosition() + node.getLength()) - 1;

		for (int i = start; i <= end; i++) {
			lineNumbers.remove(i);
		}
	}

	// 実行ステートメントに対する詳細なクエリを持っているかどうか
	// 持ってたらtrue
	private boolean hasSpecificQuery() {
		return query.methods.size() > 0 || query.variables.size() > 0;
	}

	// 変数に対する詳細なクエリを持っているかどうか
	// 持ってたらtrue
	private boolean hasVariableQuery() {
		return query.variables.size() > 0;
	}

	// nameから変数を探す
	public boolean visit(SimpleName node) {
		// メソッドのnameにも反応するから変数指定の時だけに動くようにする
		if (hasVariableQuery()) {

			if (node.resolveBinding() != null) {
				for (Variable variable : query.variables) {
					if (!node.resolveBinding().getName().equals(variable.name))
						continue;
					// 見つけたやつが変数かどうか判定
					if (!node.resolveBinding().getKey()
							.endsWith("#" + variable.name))
						continue;
					// System.out
					// .println(
					// "検索されたやつがSimpleNameに引っかかった + 変数だ: " + node
					// + "("
					// + compilationUnit.getLineNumber(
					// node.getStartPosition())
					// + ")");

					ASTNode moveUp = moveUpToStatement(node);
					addLineNumber(moveUp);
					// System.out.println("moveUp:"+moveUp);
					if (moveUp != null) {
						// どのメソッド内におるのかも書きだす
						addLineNumberStartEnd(moveUpToMethodInvocation(node));
					}
				}
			}
		}
		return true;
	}

	// これもnameの一種やけど，属性は変数として扱わない
	public boolean visit(QualifiedName node) {
		// System.out.println("Qname: "+node);
		return true;

	}

	// 親を再起でたどっていく．ExpressionStatementまたはVariableDeclarationStatementまで
	// 検索で引っかかった変数を書きだす
	// メソッド検索の時はつかわない
	/**
	 * @param node
	 * @return 目的の行
	 */
	public ASTNode moveUpToStatement(ASTNode node) {
		if (node.getParent() == null)
			return null;
		// System.out.println("moveUp: " + node);
		// System.out.println(node.getParent());
		// System.out.println(node.getParent().getClass().toString());

		// classを取得して比較する事にした
		if (!(node.getParent() == null
				|| node.getParent().getClass().toString()
						.endsWith("ExpressionStatement")
				|| node.getParent().getClass().toString()
						.endsWith("VariableDeclarationStatement"))) {
			return moveUpToStatement(node.getParent());
		}
		// System.out.println("どこかみつけた: \n" + node.getParent());
		Statement statement = (Statement) node.getParent();
		return statement;
	}

	// 親を再起でたどっていく．メソッド呼び出しまで
	/**
	 * @param node
	 * @return 目的の行
	 */
	public MethodDeclaration moveUpToMethodInvocation(ASTNode node) {
		// TODO: メソッド呼び出しまで戻れなかったときの処理書く
		if (node.getParent() == null)
			return null;
		// System.out.print("辿るで: " + node.getParent());
		// System.out.println(node.getParent().getClass().toString());

		// if (! (node.getParent() instanceof MethodInvocation)) {
		// classを取得して比較する事にした
		if (!(node.getParent().getClass().toString()
				.endsWith("MethodDeclaration"))) {
			return moveUpToMethodInvocation(node.getParent());
		}
		// System.out.println("どこで呼ばれているかみつけた: \n" + node.getParent());
		// Statement statement = (Statement)node.getParent();
		MethodDeclaration statement = (MethodDeclaration) node.getParent();
		return statement;
	}

	// メソッド呼び出しに行ける
	public boolean visit(MethodInvocation node) {
		for (Method method : query.methods) {
			if (node.getName().toString().equals(method.name)) {
				// System.out.println("検索されたメソッド呼び出しみつけた: " + node);
				addLineNumber(node);
				// System.out.println("メソッド呼び出しから，宣言を辿る");
				// System.out.println(moveUpToMethodInvocation(node));

				// どのメソッド内におるのかも書きだす
				addLineNumberStartEnd(moveUpToMethodInvocation(node));
			}
		}
		return super.visit(node);
	}


	// メソッド宣言に行ける(nodeにはjavdoc含む)
	public boolean visit(MethodDeclaration node) {
		for (Method method : query.methods) {
			if (node.getName().toString().equals(method.name)) {
				// Join all modifiers and parameters.
				// addLineNumber(node);
				addLineNumberofMethod(node);
			}
		}
		// 構文エラーがあった場合
		// TODO ASTNode.MALTORMEDで動くのか確認
		if ((node.getFlags() & ASTNode.MALFORMED) == ASTNode.MALFORMED) {
			// System.out.println("構文エラー！ "+ (node.getFlags() &
			// CompilationUnit.MALFORMED));
			SyntaxFlg = false;
		}
		return true;
	}

	/**
	 * @param node
	 */
	// アノテーションで行う処理まとめてる
	public void annoVisit(ASTNode node) {
		// System.out.println("アノテーション呼び出し");
		if (hasSpecificQuery() && !anno)
			return;
		if (anno) {
			addLineNumber(node);
		} else {
			removeLineNumber(node);
		}
	}

	// アノテーション
	@Override
	public boolean visit(MarkerAnnotation node) {
		// @Overrideとか
		annoVisit(node);
		return true;
	}

	@Override
	public boolean visit(NormalAnnotation node) {
		annoVisit(node);
		return true;
	}

	@Override
	public boolean visit(SingleMemberAnnotation node) {
		// @SuppressWarnings("unchecked")とか
		annoVisit(node);
		return true;
	}

	// javadoc
	@Override
	public boolean visit(Javadoc node) {
		if (jd == false) {
			removeLineNumber(node);
		} else {
			addLineNumber(node);
		}
		return true;
	}
}
