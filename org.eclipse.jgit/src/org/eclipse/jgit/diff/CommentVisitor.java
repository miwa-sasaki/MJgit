package org.eclipse.jgit.diff;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LineComment;

//コメント解析
/**
 * @author miwaaa8
 *
 */
public class CommentVisitor extends ASTVisitor {

	CompilationUnit compilationUnit;
	private String[] source;
	static String comment = new String("");

	// コンストラクタ
	/**
	 * @param compilationUnit
	 * @param source
	 */
	public CommentVisitor(CompilationUnit compilationUnit, String[] source) {
		super();
		this.compilationUnit = compilationUnit;
		this.source = source;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean visit(CompilationUnit node) {
		for (Comment comment : (List<Comment>) node.getCommentList()) {
			comment.accept(new CommentVisitor(node, source));
		}
		return super.visit(node);
	}

	@Override
	public boolean visit(LineComment node) {
		//行数取得
		int startLineNumber = compilationUnit.getLineNumber(node.getStartPosition()) - 1;
		String lineComment = source[startLineNumber].trim();
		//comment += startLineNumber + ":" + lineComment + "\n";
		comment += lineComment + "\n";
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
				blockComment.append("\n");
			}
		}
		//行数含む
		//comment += startLineNumber + ":" + blockComment + "\n";
		comment += blockComment + "\n";
		//System.out.print(comment);

		return super.visit(node);
	}

	//javadocも
    @Override
    public boolean visit(Javadoc node) {
		// @の直前の改行しか反映してくれない
    	comment += node + "\n";
    	return true;
    }

}