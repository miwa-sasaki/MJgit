package org.eclipse.jgit.diff;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LineComment;

import astnode.query.Context;
import astnode.query.MJQuery;

//コメント解析
/**
 * @author miwaaa8
 *
 */
public class CommentVisitor extends ASTVisitor {

	CompilationUnit compilationUnit;
	private String[] source;

	boolean com;

	private MJQuery query;

	/**
	 *
	 */
	public Set<Integer> lineNumbers;

	int queryNum;

	// コンストラクタ
	/**
	 * @param compilationUnit
	 * @param source
	 * @param query
	 * @param lineNumbers
	 * @param queryNum
	 */
	public CommentVisitor(CompilationUnit compilationUnit, String[] source,
			MJQuery query, Set<Integer> lineNumbers, int queryNum) {
		super();
		this.compilationUnit = compilationUnit;
		// System.out.println("CVのコンストラクタのlineNumbers: \n"+ lineNumbers);

		this.source = source;
		com = false;
		if (query.contexts.get(queryNum) == Context.COMMENT)
			this.com = true;
		this.lineNumbers = lineNumbers;
		this.queryNum = queryNum;
		this.query = query;

		System.out.println("com: " + com);

		// ぬるぽ
		if (lineNumbers == null) {
			this.lineNumbers = new TreeSet<>();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(CompilationUnit node) {
		for (Comment comment : (List<Comment>) node.getCommentList()) {
			comment.accept(new CommentVisitor(node, source, query, lineNumbers,
					queryNum));
		}
		return super.visit(node);
	}

	// nodeの元ファイルにおける行番号を書き出す
	private void addLineNumber(ASTNode node) {
		// System.out.println("コメントadd");
		int start = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
		int end = compilationUnit
				.getLineNumber(node.getStartPosition() + node.getLength()) - 1;
		// System.out.println("start is : "+ start);
		// System.out.println("end is : "+ end);

		for (int i = start; i <= end; i++) {
			lineNumbers.add(i);
		}
	}

	// nodeの元ファイルにおける行番号を削除する
	private void removeLineNumber(ASTNode node) {
		// System.out.println("コメントremove");
		int start = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
		int end = compilationUnit
				.getLineNumber(node.getStartPosition() + node.getLength()) - 1;
		// System.out.println("start: "+start);
		// System.out.println("end: "+end);

		for (int i = start; i <= end; i++) {
			lineNumbers.remove(i);
		}
	}

	@Override
	public boolean visit(LineComment node) {
		// System.out.println("CommentVisitorのラインコメントvisit");
		if (com) {
			addLineNumber(node);
		} else {
			removeLineNumber(node);
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(BlockComment node) {
		// System.out.println("CommentVisitorのブロックコメントvisit");
		if (com) {
			addLineNumber(node);
		} else {
			removeLineNumber(node);
		}
		return super.visit(node);
	}
}