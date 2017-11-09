package org.eclipse.jgit.diff;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;

//本質解析
/**
 * @author miwaaa8
 *
 */
public class AstVisitor extends ASTVisitor {

	CompilationUnit compilationUnit;
	private String[] source;

	static String ast = new String(""); //$NON-NLS-1$

	/**
	 * 構文エラーの有無
	 */
	public static boolean SyntaxFlg = true;

	// コンストラクタ
	/**
	 * @param compilationUnit
	 * @param source
	 */
	public AstVisitor(CompilationUnit compilationUnit, String[] source) {
		super();
		this.compilationUnit = compilationUnit;
		this.source = source;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(CompilationUnit node) {
		//int startLineNumber = compilationUnit.getLineNumber(node.getStartPosition()) -1;

		for (Comment comment : (List<Comment>) node.getCommentList()) {
			// comment.accept(new CommentVisitor(node, source));
		}

		// メソッドの外側のエラーが見つけれる
		//System.out.println("getFlags " + node.getFlags()); //$NON-NLS-1$

		// 構文エラーがあった場合
		if ((node.getFlags() & ASTNode.MALFORMED) == ASTNode.MALFORMED) {
			//System.out.println("構文エラー！ "); //$NON-NLS-1$
			SyntaxFlg = false;
		}

		//コメント以外全部書き出す
		//行数含む
		//ast += startLineNumber + ":" + node + "\n";
		ast += node + "\n"; //$NON-NLS-1$
		//System.out.print(ast);

		return super.visit(node);
	}

	// メソッドの中のエラーが見つけれる
	public boolean visit(MethodDeclaration node) {
		//System.out.println("MethodDeclaration\n" + node);
		//System.out.println("in Method getFlags " + node.getFlags()); //$NON-NLS-1$
		// 構文エラーがあった場合
		if ((node.getFlags() & ASTNode.MALFORMED) == ASTNode.MALFORMED) {
			//System.out.println("構文エラー！ "); //$NON-NLS-1$
			SyntaxFlg = false;
		}
		return true;
	}


	//javadoc消す
	@Override
	public boolean visit(Javadoc node) {
		//nodeをstringに変換
		String nodeS = String.valueOf(node);

		//astを改行で分割して配列に格納
		String[] astSplit = ast.split("\n"); //$NON-NLS-1$
		//nodeつまりjavadocも格納
		String[] jdSplit = nodeS.split("\n"); //$NON-NLS-1$

		int i = 0; /* 注目しているastSplitの位置 */
		int j = 0; /* 注目しているjdSplitの位置 */
		int ast_len, jd_len;
		ast_len = astSplit.length;
		jd_len = jdSplit.length;

		/* テキストの最後尾に行き当たるか、パターンが見つかるまで繰り返す */
		while (i < ast_len && j < jd_len) {
			if (astSplit[i].endsWith(jdSplit[j])
					|| astSplit[i].startsWith(jdSplit[j])
					|| jdSplit[j].endsWith(astSplit[i])
					|| jdSplit[j].startsWith(astSplit[i])) {
				i++;
				j++;
			} else {
				// System.out.println(astSplit[i] + "\n" + jdSplit[j]);
				i++;
				j = 0;
			}
		}
		if (j == jd_len) {// 見つかった
			// System.out.println("見つけた");
			// 各行に改行を戻す
			for (int l = 0; l < astSplit.length; l++) {
				astSplit[l] += "\n"; //$NON-NLS-1$
			}
			// javadocの開始行
			int startIndex = i - j;
			int endIndex = j;
			// javadocの部分を消す
			for (int l = 0; l < endIndex; l++) {
				// System.out.println("aS[l] is " + astSplit[startIndex + l]);
				astSplit[startIndex + l] = ""; //$NON-NLS-1$
				// System.out.println("aS[l] is " + astSplit[startIndex + l]);
			}

			// astに戻す
			ast = ""; //$NON-NLS-1$
			for (int l = 0; l < astSplit.length; l++) {
				ast += astSplit[l];
			}
		} else {
			System.out.println("javadoc見つからん"); //$NON-NLS-1$
		}

		return true;
	}
}