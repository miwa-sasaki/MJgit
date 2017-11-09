// package org.eclipse.jgit.diff;
//
// import java.util.List;
//
// import org.eclipse.jdt.core.dom.ASTNode;
// import org.eclipse.jdt.core.dom.ASTVisitor;
// import org.eclipse.jdt.core.dom.Comment;
// import org.eclipse.jdt.core.dom.CompilationUnit;
// import org.eclipse.jdt.core.dom.Javadoc;
// import org.eclipse.jdt.core.dom.MethodDeclaration;
// import org.eclipse.jdt.core.dom.MarkerAnnotation;
// import org.eclipse.jdt.core.dom.NormalAnnotation;
// import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
//
//// 本質解析
/// **
// * @author miwaaa8
// *
// */
// public class Visitor2 extends ASTVisitor {
//
// CompilationUnit compilationUnit;
// private String[] source;
//
// static String exp = new String(""); //$NON-NLS-1$
// //構文エラーの有無
// static boolean SyntaxFlg = true;
//
// boolean ast;
// boolean com;
// boolean jd;
// boolean anno;
//
// // コンストラクタ
// /**
// * @param compilationUnit
// * @param source
// * @param ast
// * @param com
// * @param jd
// * @param anno
// */
// public Visitor2(CompilationUnit compilationUnit, String[] source,
// boolean ast, boolean com, boolean jd, boolean anno) {
// super();
// this.compilationUnit = compilationUnit;
// this.source = source;
// this.ast = ast;
// this.com = com;
// this.jd = jd;
// this.anno = anno;
//
// //System.out.println(ast);
// //System.out.println(com);
// //System.out.println(jd);
// //System.out.println(anno);
// }
//
// @SuppressWarnings("unchecked")
// @Override
// public boolean visit(CompilationUnit node) {
// for (Comment comment : (List<Comment>) node.getCommentList()) {
// //comment.accept(new CommentVisitor(node, source, ast, com, jd, anno));
// }
// // メソッドの外側のエラーが見つけれる
// // System.out.println("getFlags " + node.getFlags());
// // 構文エラーがあった場合
// if ((node.getFlags() & ASTNode.MALFORMED) == ASTNode.MALFORMED) {
// System.out.println("構文エラーがあるのでコンテキストを絞り込めません "); //$NON-NLS-1$
// SyntaxFlg = false;
// }
// if(ast){
// //コメント以外全部書き出す
// exp += node + "\n"; //$NON-NLS-1$
// //System.out.print(ast);
// }
// return super.visit(node);
// }
//
// //メソッドの中のエラーが見つけれる
// public boolean visit(MethodDeclaration node){
// //System.out.println("MethodDeclaration\n" + node);
// //System.out.println("in Method getFlags " + node.getFlags());
// //構文エラーがあった場合
// if ((node.getFlags() & ASTNode.MALFORMED) == ASTNode.MALFORMED) {
// System.out.println("構文エラーがあるのでコンテキストを絞り込めません "); //$NON-NLS-1$
// SyntaxFlg = false;
// }
// return true;
// }
//
// //アノテーション
// @Override
// public boolean visit(MarkerAnnotation node) {
// //@Overrideとか
// //nodeをstringに変換
// String nodeS = String.valueOf(node);
//
// if(ast == true && anno == false){
// //anno消す
// annoDelete(nodeS, true);
// }else if(ast == false && anno == true){
// //System.out.println("anno追加"); //$NON-NLS-1$
// exp += node + "\n"; //$NON-NLS-1$
// }else if(ast && anno){
// //改行入れる
// annoDelete(nodeS, false);
// }
// return true;
// }
// @Override
// public boolean visit(NormalAnnotation node) {
// //nodeをstringに変換
// String nodeS = String.valueOf(node);
// if(ast == true && anno == false){
// //anno消す
// annoDelete(nodeS, true);
// }else if(ast == false && anno == true){
// //System.out.println("anno追加"); //$NON-NLS-1$
// exp += node + "\n"; //$NON-NLS-1$
// }else if(ast && anno){
// //改行入れる
// annoDelete(nodeS, false);
// }
//
// return true;
// }
// @Override
// public boolean visit(SingleMemberAnnotation node) {
// //@SuppressWarnings("unchecked")とか
// String nodeS = String.valueOf(node);
//
// if(ast == true && anno == false){
// //anno消す
// //nodeをstringに変換
// annoDelete(nodeS, true);
//
// }else if(ast == false && anno == true){
// //anno追加
// //System.out.println("anno追加"); //$NON-NLS-1$
// exp += node + "\n"; //$NON-NLS-1$
// }else if(ast && anno){
// //改行入れる
// annoDelete(nodeS, false);
// }
// return true;
// }
//
// //annoを消す
// /**
// * @param nodeS
// * @param Delete
// * @return int
// */
// public int annoDelete(String nodeS, boolean Delete){
// //一文字ずつ格納
// String[] nodeSplit = nodeS.split(""); //$NON-NLS-1$
//
// //expを改行で分割して配列に格納
// String[] expSplit = exp.split("\n"); //$NON-NLS-1$
//
// //System.out.println("探すanno is " + nodeS); //$NON-NLS-1$
// //一行ずつ探していく
// for(int h=0;h<expSplit.length;h++){
// //System.out.println(expSplit[h] + " " + h + "行目");
//
// //expの一行を一文字ずつ格納
// String[] lineSplit = expSplit[h].split(""); //$NON-NLS-1$
// int j = 0;
// int spaceNum = 0;
// boolean space = false;
// for(int i=0; i<nodeSplit.length + spaceNum || space == false;i++){
// //一文字ずつ確認していく
// //System.out.println(lineSplit[i] + " " + i);
// //System.out.println(nodeSplit[j] + " " + i);
// if(nodeSplit[j].equals(lineSplit[i])){
// //System.out.println("一文字一致");
// if(space==false){
// //System.out.println("一文字目! 空白の数 is " + i);
// spaceNum = i;
// space = true;
// }
// //nodeの次の文字を確認する
// j++;
// if(j==nodeSplit.length){
// //全部一致した
// //System.out.println("全部一致!!");
// if(Delete){
// //annoの部分を消す
// //System.out.println("anno消去開始");
// for(int l = spaceNum; l <= spaceNum + nodeSplit.length ; l++){
// //System.out.println("before eS[l] is " + lineSplit[l]);
// lineSplit[l] = ""; //$NON-NLS-1$
// //System.out.println("after eS[l] is " + lineSplit[l]);
// }
// }else{
// //annoの後に改行を入れる
// lineSplit[spaceNum + nodeSplit.length] += "\n"; //$NON-NLS-1$
// for(int l=0;l<spaceNum;l++){
// lineSplit[spaceNum + nodeSplit.length] += " "; //$NON-NLS-1$
// }
// }
// /*
// for(int l=0;i<lineSplit.length;l++)
// System.out.print(lineSplit[l]);
// System.out.println("");
// */
// //expSplitに戻す
// //h行目のexpSplitを書き換えたので...
// expSplit[h] = ""; //$NON-NLS-1$
// for(int l = 0; l < lineSplit.length; l++){
// expSplit[h] += lineSplit[l];
// }
// //expSplitをexpに戻す
// exp = ""; //$NON-NLS-1$
// for(int l = 0; l < expSplit.length; l++){
// exp += expSplit[l] + "\n"; //$NON-NLS-1$
// }
// }
// continue;
// } else if (lineSplit[i].equals(" ")) { //$NON-NLS-1$
// //空白なら無視(これでiが増える)
// continue;
// }else{
// //この行に探したいannoない
// //System.out.println("break");
// break;
// }
// }
// }
// return 0;
// }
//
// //javadoc
// @Override
// public boolean visit(Javadoc node) {
// //System.out.println("jd"); //$NON-NLS-1$
// if (ast == true && jd == false) {
// //System.out.println("jd 消す"); //$NON-NLS-1$
// //nodeをstringに変換
// String nodeS = String.valueOf(node);
//
// //expを改行で分割して配列に格納
// String[] astSplit = exp.split("\n"); //$NON-NLS-1$
// //nodeつまりjavadocも格納
// String[] jdSplit = nodeS.split("\n"); //$NON-NLS-1$
//
// int i = 0; // 注目しているastSplitの位置
// int j = 0; // 注目しているjdSplitの位置
// int ast_len, jd_len;
// ast_len = astSplit.length;
// jd_len = jdSplit.length;
//
// /* テキストの最後尾に行き当たるか、パターンが見つかるまで繰り返す */
// while ( i < ast_len && j < jd_len ) {
// if(astSplit[i].endsWith(jdSplit[j]) || astSplit[i].startsWith(jdSplit[j])
// || jdSplit[j].endsWith(astSplit[i]) || jdSplit[j].startsWith(astSplit[i])){
// i++;
// j++;
// } else {
// //System.out.println(astSplit[i] + "\n" + jdSplit[j]);
// i++;
// j=0;
// }
// }
// if(j == jd_len){//見つかった
// //System.out.println("見つけた");
// //各行に改行を戻す
// for(int l = 0; l < astSplit.length; l++){
// astSplit[l] += "\n"; //$NON-NLS-1$
// }
// //javadocの開始行
// int startIndex = i-j;
// int endIndex = j;
// //javadocの部分を消す
// for(int l = 0; l < endIndex; l++){
// //System.out.println("aS[l] is " + astSplit[startIndex + l]);
// astSplit[startIndex + l] = ""; //$NON-NLS-1$
// //System.out.println("aS[l] is " + astSplit[startIndex + l]);
// }
//
// //astに戻す
// exp = ""; //$NON-NLS-1$
// for(int l = 0; l < astSplit.length; l++){
// exp += astSplit[l];
// }
// } else {
// //System.out.println("javadoc見つからん"); //$NON-NLS-1$
// }
// } else if (ast == false && jd == true) {
// //System.out.println("jd追加"); //$NON-NLS-1$
// exp += node + "\n"; //$NON-NLS-1$
//
// }
// return true;
// }
// }