package org.eclipse.jgit.diff;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.LineComment;

//コメント解析
/**
 * @author miwaaa8
 *
 */
public class CommentVisitor2 extends ASTVisitor {

	CompilationUnit compilationUnit;
	private String[] source;

	static String comment = new String(""); //$NON-NLS-1$

	static String javadoc = new String(""); //$NON-NLS-1$

	boolean ast;

	boolean com;

	boolean jd;

	boolean anno;

	// コンストラクタ
	/**
	 * @param compilationUnit
	 * @param source
	 * @param ast
	 * @param com
	 * @param jd
	 * @param anno
	 */
	public CommentVisitor2(CompilationUnit compilationUnit, String[] source,
			boolean ast, boolean com, boolean jd, boolean anno) {
		super();
		this.compilationUnit = compilationUnit;
		this.source = source;

		this.ast = ast;
		this.com = com;
		this.jd = jd;
		this.anno = anno;

		// System.out.println(ast);
		// System.out.println(com);
		// System.out.println(jd);
		// System.out.println(anno);

	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(CompilationUnit node) {

		for (Comment comment1 : (List<Comment>) node.getCommentList()) {
			comment1.accept(new CommentVisitor2(node, source, ast, com, jd, anno));
		}

		return super.visit(node);
	}

	@Override
	public boolean visit(LineComment node) {
		//行数取得
		int startLineNumber = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
		String lineComment = source[startLineNumber].trim();
		//comment += startLineNumber + ":" + lineComment + "\n";
		comment += lineComment + "\n"; //$NON-NLS-1$
		//System.out.print(comment);

		return super.visit(node);
	}

	@Override
	public boolean visit(BlockComment node) {
		int startLineNumber = compilationUnit.getLineNumber(node
				.getStartPosition()) - 1;
		int endLineNumber = compilationUnit.getLineNumber(node
				.getStartPosition() + node.getLength()) - 1;

		StringBuffer blockComment = new StringBuffer();

		for (int lineCount = startLineNumber; lineCount <= endLineNumber; lineCount++) {
			String blockCommentLine = source[lineCount].trim();
			blockComment.append(blockCommentLine);
			if (lineCount != endLineNumber) {
				blockComment.append("\n"); //$NON-NLS-1$
			}
		}
		//行数含む
		//comment += startLineNumber + ":" + blockComment + "\n";
		comment += blockComment + "\n"; //$NON-NLS-1$
		//System.out.print(comment);

		return super.visit(node);
	}
}