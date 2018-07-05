/*
 * Copyright (C) 2009, Google Inc.
 * Copyright (C) 2008-2009, Johannes E. Schindelin <johannes.schindelin@gmx.de>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.diff;


import static org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME;
import static org.eclipse.jgit.diff.DiffEntry.Side.NEW;
import static org.eclipse.jgit.diff.DiffEntry.Side.OLD;
import static org.eclipse.jgit.lib.Constants.encode;
import static org.eclipse.jgit.lib.Constants.encodeASCII;
import static org.eclipse.jgit.lib.FileMode.GITLINK;

import java.io.BufferedReader;
//import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
//import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.FileHeader.PatchType;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.QuotedString;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

import org.eclipse.jgit.diff.CommentVisitor;
import org.eclipse.jgit.diff.Visitor;
import astnode.query.Context;
import astnode.query.MJQuery;

/**
 * Format a Git style patch script.
 */
public class DiffFormatter implements AutoCloseable {
	private static final int DEFAULT_BINARY_FILE_THRESHOLD = PackConfig.DEFAULT_BIG_FILE_THRESHOLD;

	private static final byte[] noNewLine = encodeASCII("\\ No newline at end of file\n"); //$NON-NLS-1$

	/** Magic return content indicating it is empty or no content present. */
	private static final byte[] EMPTY = new byte[] {};

	/** Magic return indicating the content is binary. */
	private static final byte[] BINARY = new byte[] {};

	private final OutputStream out;

	private ObjectReader reader;

	private boolean closeReader;

	private DiffConfig diffCfg;

	private int context = 3;

	private int abbreviationLength = 7;

	private DiffAlgorithm diffAlgorithm;

	private RawTextComparator comparator = RawTextComparator.DEFAULT;

	private int binaryFileThreshold = DEFAULT_BINARY_FILE_THRESHOLD;

	private String oldPrefix = "a/"; //$NON-NLS-1$

	private String newPrefix = "b/"; //$NON-NLS-1$

	private TreeFilter pathFilter = TreeFilter.ALL;

	private RenameDetector renameDetector;

	private ProgressMonitor progressMonitor;

	private ContentSource.Pair source;

	// -----------------------------------
	// コンテキストの種類を覚えておくフラグ．
	// 1:コンテキスト指定あり,-1:なし
	private int CxFlg = -1;

	// Logコマンドでコンテキストを指定された時
	// 1:コンテキスト指定あり,-1:なし
	private int CxFlgLog = -1;

	// ファイルの種類をjavaかどうか判断するフラグ
	// 0:違う, 1:javaファイル
	private int isJavaFile = 0;

	/**
	 * 変更された.javaファイルの数
	 */
	public int numJavaFile = 0;

	/**
	 * 変更された.java以外のファイル
	 */
	public int numOtherFile = 0;

	boolean aSyntaxFlg = true;

	boolean bSyntaxFlg = true;

	// 表示するコンテキスト
	boolean ast = false;

	boolean com = false;

	boolean jd = false;

	boolean anno = false;

	String MJQuery;

	static // デバッグコメントを出力するかどうか
	boolean isCommentPrintedFlg = false;

	// -----------------------------------

	/**
	 * Create a new formatter with a default level of context.
	 *
	 * @param out
	 *            the stream the formatter will write line data to. This stream
	 *            should have buffering arranged by the caller, as many small
	 *            writes are performed to it.
	 */
	public DiffFormatter(OutputStream out) {
		this.out = out;
	}

	/** @return the stream we are outputting data to. */
	protected OutputStream getOutputStream() {
		return out;
	}

	/**
	 * Set the repository the formatter can load object contents from.
	 *
	 * Once a repository has been set, the formatter must be released to ensure
	 * the internal ObjectReader is able to release its resources.
	 *
	 * @param repository
	 *            source repository holding referenced objects.
	 */
	public void setRepository(Repository repository) {
		setReader(repository.newObjectReader(), repository.getConfig(), true);
	}

	/**
	 * Set the repository the formatter can load object contents from.
	 *
	 * @param reader
	 *            source reader holding referenced objects. Caller is responsible
	 *            for closing the reader.
	 * @param cfg
	 *            config specifying diff algorithm and rename detection options.
	 * @since 4.5
	 */
	public void setReader(ObjectReader reader, Config cfg) {
		setReader(reader, cfg, false);
	}

	private void setReader(ObjectReader reader, Config cfg, boolean closeReader) {
		close();
		this.closeReader = closeReader;
		this.reader = reader;
		this.diffCfg = cfg.get(DiffConfig.KEY);

		ContentSource cs = ContentSource.create(reader);
		source = new ContentSource.Pair(cs, cs);

		if (diffCfg.isNoPrefix()) {
			setOldPrefix(""); //$NON-NLS-1$
			setNewPrefix(""); //$NON-NLS-1$
		}
		setDetectRenames(diffCfg.isRenameDetectionEnabled());

		diffAlgorithm = DiffAlgorithm.getAlgorithm(cfg.getEnum(
				ConfigConstants.CONFIG_DIFF_SECTION, null,
				ConfigConstants.CONFIG_KEY_ALGORITHM,
				SupportedAlgorithm.HISTOGRAM));
	}

	/**
	 * Change the number of lines of context to display.
	 *
	 * @param lineCount
	 *            number of lines of context to see before the first
	 *            modification and after the last modification within a hunk of
	 *            the modified file.
	 */
	public void setContext(final int lineCount) {
		if (lineCount < 0)
			throw new IllegalArgumentException(
					JGitText.get().contextMustBeNonNegative);
		context = lineCount;
	}

	/**
	 * Change the number of digits to show in an ObjectId.
	 *
	 * @param count
	 *            number of digits to show in an ObjectId.
	 */
	public void setAbbreviationLength(final int count) {
		if (count < 0)
			throw new IllegalArgumentException(
					JGitText.get().abbreviationLengthMustBeNonNegative);
		abbreviationLength = count;
	}

	/**
	 * Set the algorithm that constructs difference output.
	 *
	 * @param alg
	 *            the algorithm to produce text file differences.
	 * @see HistogramDiff
	 */
	public void setDiffAlgorithm(DiffAlgorithm alg) {
		diffAlgorithm = alg;
	}

	/**
	 * Set the line equivalence function for text file differences.
	 *
	 * @param cmp
	 *            The equivalence function used to determine if two lines of
	 *            text are identical. The function can be changed to ignore
	 *            various types of whitespace.
	 * @see RawTextComparator#DEFAULT
	 * @see RawTextComparator#WS_IGNORE_ALL
	 * @see RawTextComparator#WS_IGNORE_CHANGE
	 * @see RawTextComparator#WS_IGNORE_LEADING
	 * @see RawTextComparator#WS_IGNORE_TRAILING
	 */
	public void setDiffComparator(RawTextComparator cmp) {
		comparator = cmp;
	}

	/**
	 * Set maximum file size for text files.
	 *
	 * Files larger than this size will be treated as though they are binary and
	 * not text. Default is {@value #DEFAULT_BINARY_FILE_THRESHOLD} .
	 *
	 * @param threshold
	 *            the limit, in bytes. Files larger than this size will be
	 *            assumed to be binary, even if they aren't.
	 */
	public void setBinaryFileThreshold(int threshold) {
		this.binaryFileThreshold = threshold;
	}

	/**
	 * Set the prefix applied in front of old file paths.
	 *
	 * @param prefix
	 *            the prefix in front of old paths. Typically this is the
	 *            standard string {@code "a/"}, but may be any prefix desired by
	 *            the caller. Must not be null. Use the empty string to have no
	 *            prefix at all.
	 */
	public void setOldPrefix(String prefix) {
		oldPrefix = prefix;
	}

	/**
	 * Get the prefix applied in front of old file paths.
	 *
	 * @return the prefix
	 * @since 2.0
	 */
	public String getOldPrefix() {
		return this.oldPrefix;
	}

	/**
	 * Set the prefix applied in front of new file paths.
	 *
	 * @param prefix
	 *            the prefix in front of new paths. Typically this is the
	 *            standard string {@code "b/"}, but may be any prefix desired by
	 *            the caller. Must not be null. Use the empty string to have no
	 *            prefix at all.
	 */
	public void setNewPrefix(String prefix) {
		newPrefix = prefix;
	}

	/**
	 * diff, showコマンドで指定されたコンテキストに対応するフラグを立てる
	 *
	 * @param query
	 *            コンテキストの種類． コマンドか本質を受付
	 */
	public void setContextFlg(String query) {

		if (query != null) {
			// System.out.println("クエリ受けとった: " + query);
			MJQuery = query;
			CxFlg = 1;
		}
	}

	/**
	 * logコマンドで指定されたコンテキストに対応するフラグを立てる
	 *
	 * @param query
	 *            コンテキストの種類． コマンドか本質を受付
	 */
	public void setContextFlgLog(String query) {

		if (query != null) {
			// System.out.println("Loggggコマンドがクエリ受けとった: " + query);
			MJQuery = query;
			CxFlg = 1;
			CxFlgLog = 1;
		}
	}

	/**
	 * logコマンドで指定されたコンテキストに対応するフラグを立てる
	 *
	 * @param flg
	 *            コメントを出力するかどうか
	 */
	// TODO:デバッグおわたら消す
	public void setCommentPrintFlg(boolean flg) {
		if (flg) {
			isCommentPrintedFlg = true;
		}
	}

	/**
	 * Get the prefix applied in front of new file paths.
	 *
	 * @return the prefix
	 * @since 2.0
	 */
	public String getNewPrefix() {
		return this.newPrefix;
	}

	/** @return true if rename detection is enabled. */
	public boolean isDetectRenames() {
		return renameDetector != null;
	}

	/**
	 * Enable or disable rename detection.
	 *
	 * Before enabling rename detection the repository must be set with
	 * {@link #setRepository(Repository)}. Once enabled the detector can be
	 * configured away from its defaults by obtaining the instance directly from
	 * {@link #getRenameDetector()} and invoking configuration.
	 *
	 * @param on
	 *            if rename detection should be enabled.
	 */
	public void setDetectRenames(boolean on) {
		if (on && renameDetector == null) {
			assertHaveReader();
			renameDetector = new RenameDetector(reader, diffCfg);
		} else if (!on)
			renameDetector = null;
	}

	/** @return the rename detector if rename detection is enabled. */
	public RenameDetector getRenameDetector() {
		return renameDetector;
	}

	/**
	 * Set the progress monitor for long running rename detection.
	 *
	 * @param pm
	 *            progress monitor to receive rename detection status through.
	 */
	public void setProgressMonitor(ProgressMonitor pm) {
		progressMonitor = pm;
	}

	/**
	 * Set the filter to produce only specific paths.
	 *
	 * If the filter is an instance of {@link FollowFilter}, the filter path
	 * will be updated during successive scan or format invocations. The updated
	 * path can be obtained from {@link #getPathFilter()}.
	 *
	 * @param filter
	 *            the tree filter to apply.
	 */
	public void setPathFilter(TreeFilter filter) {
		pathFilter = filter != null ? filter : TreeFilter.ALL;
	}

	/** @return the current path filter. */
	public TreeFilter getPathFilter() {
		return pathFilter;
	}

	/**
	 * Flush the underlying output stream of this formatter.
	 *
	 * @throws IOException
	 *             the stream's own flush method threw an exception.
	 */
	public void flush() throws IOException {
		out.flush();
	}

	/**
	 * Release the internal ObjectReader state.
	 *
	 * @since 4.0
	 */
	@Override
	public void close() {
		if (reader != null && closeReader) {
			reader.close();
		}
	}

	/**
	 * Determine the differences between two trees.
	 *
	 * No output is created, instead only the file paths that are different are
	 * returned. Callers may choose to format these paths themselves, or convert
	 * them into {@link FileHeader} instances with a complete edit list by
	 * calling {@link #toFileHeader(DiffEntry)}.
	 * <p>
	 * Either side may be null to indicate that the tree has beed added or
	 * removed. The diff will be computed against nothing.
	 *
	 * @param a
	 *            the old (or previous) side or null
	 * @param b
	 *            the new (or updated) side or null
	 * @return the paths that are different.
	 * @throws IOException
	 *             trees cannot be read or file contents cannot be read.
	 */
	public List<DiffEntry> scan(AnyObjectId a, AnyObjectId b)
			throws IOException {
		assertHaveReader();

		try (RevWalk rw = new RevWalk(reader)) {
			RevTree aTree = a != null ? rw.parseTree(a) : null;
			RevTree bTree = b != null ? rw.parseTree(b) : null;
			return scan(aTree, bTree);
		}
	}

	/**
	 * Determine the differences between two trees.
	 *
	 * No output is created, instead only the file paths that are different are
	 * returned. Callers may choose to format these paths themselves, or convert
	 * them into {@link FileHeader} instances with a complete edit list by
	 * calling {@link #toFileHeader(DiffEntry)}.
	 * <p>
	 * Either side may be null to indicate that the tree has beed added or
	 * removed. The diff will be computed against nothing.
	 *
	 * @param a
	 *            the old (or previous) side or null
	 * @param b
	 *            the new (or updated) side or null
	 * @return the paths that are different.
	 * @throws IOException
	 *             trees cannot be read or file contents cannot be read.
	 */
	public List<DiffEntry> scan(RevTree a, RevTree b) throws IOException {
		assertHaveReader();

		AbstractTreeIterator aIterator = makeIteratorFromTreeOrNull(a);
		AbstractTreeIterator bIterator = makeIteratorFromTreeOrNull(b);
		return scan(aIterator, bIterator);
	}

	private AbstractTreeIterator makeIteratorFromTreeOrNull(RevTree tree)
			throws IncorrectObjectTypeException, IOException {
		if (tree != null) {
			CanonicalTreeParser parser = new CanonicalTreeParser();
			parser.reset(reader, tree);
			return parser;
		} else
			return new EmptyTreeIterator();
	}

	/**
	 * Determine the differences between two trees.
	 *
	 * No output is created, instead only the file paths that are different are
	 * returned. Callers may choose to format these paths themselves, or convert
	 * them into {@link FileHeader} instances with a complete edit list by
	 * calling {@link #toFileHeader(DiffEntry)}.
	 *
	 * @param a
	 *            the old (or previous) side.
	 * @param b
	 *            the new (or updated) side.
	 * @return the paths that are different.
	 * @throws IOException
	 *             trees cannot be read or file contents cannot be read.
	 */
	public List<DiffEntry> scan(AbstractTreeIterator a, AbstractTreeIterator b)
			throws IOException {
		//System.out.println("start scan \na is " + a + "\nb is" + b); //$NON-NLS-1$ //$NON-NLS-2$

		assertHaveReader();

		TreeWalk walk = new TreeWalk(reader);
		walk.addTree(a);
		walk.addTree(b);
		walk.setRecursive(true);

		TreeFilter filter = getDiffTreeFilterFor(a, b);
		if (pathFilter instanceof FollowFilter) {
			walk.setFilter(AndTreeFilter.create(
					PathFilter.create(((FollowFilter) pathFilter).getPath()),
					filter));
		} else {
			walk.setFilter(AndTreeFilter.create(pathFilter, filter));
		}

		source = new ContentSource.Pair(source(a), source(b));

		List<DiffEntry> files = DiffEntry.scan(walk);
		if (pathFilter instanceof FollowFilter && isAdd(files)) {
			// The file we are following was added here, find where it
			// came from so we can properly show the rename or copy,
			// then continue digging backwards.
			//
			a.reset();
			b.reset();
			walk.reset();
			walk.addTree(a);
			walk.addTree(b);
			walk.setFilter(filter);

			if (renameDetector == null)
				setDetectRenames(true);
			files = updateFollowFilter(detectRenames(DiffEntry.scan(walk)));

		} else if (renameDetector != null)
			files = detectRenames(files);

		if (isCommentPrintedFlg)
			System.out.println("files is" + files); //$NON-NLS-1$
		// filesは変化があったファイル達
		return files;
	}

	private static TreeFilter getDiffTreeFilterFor(AbstractTreeIterator a,
			AbstractTreeIterator b) {
		if (a instanceof DirCacheIterator && b instanceof WorkingTreeIterator)
			return new IndexDiffFilter(0, 1);

		if (a instanceof WorkingTreeIterator && b instanceof DirCacheIterator)
			return new IndexDiffFilter(1, 0);

		TreeFilter filter = TreeFilter.ANY_DIFF;
		if (a instanceof WorkingTreeIterator)
			filter = AndTreeFilter.create(new NotIgnoredFilter(0), filter);
		if (b instanceof WorkingTreeIterator)
			filter = AndTreeFilter.create(new NotIgnoredFilter(1), filter);
		return filter;
	}

	private ContentSource source(AbstractTreeIterator iterator) {
		if (iterator instanceof WorkingTreeIterator)
			return ContentSource.create((WorkingTreeIterator) iterator);
		return ContentSource.create(reader);
	}

	private List<DiffEntry> detectRenames(List<DiffEntry> files)
			throws IOException {
		renameDetector.reset();
		renameDetector.addAll(files);
		return renameDetector.compute(reader, progressMonitor);
	}

	private boolean isAdd(List<DiffEntry> files) {
		String oldPath = ((FollowFilter) pathFilter).getPath();
		for (DiffEntry ent : files) {
			if (ent.getChangeType() == ADD && ent.getNewPath().equals(oldPath))
				return true;
		}
		return false;
	}

	private List<DiffEntry> updateFollowFilter(List<DiffEntry> files) {
		String oldPath = ((FollowFilter) pathFilter).getPath();
		for (DiffEntry ent : files) {
			if (isRename(ent) && ent.getNewPath().equals(oldPath)) {
				pathFilter = FollowFilter.create(ent.getOldPath(), diffCfg);
				return Collections.singletonList(ent);
			}
		}
		return Collections.emptyList();
	}

	private static boolean isRename(DiffEntry ent) {
		return ent.getChangeType() == RENAME || ent.getChangeType() == COPY;
	}

	/**
	 * Format the differences between two trees.
	 *
	 * The patch is expressed as instructions to modify {@code a} to make it
	 * {@code b}.
	 * <p>
	 * Either side may be null to indicate that the tree has beed added or
	 * removed. The diff will be computed against nothing.
	 *
	 * @param a
	 *            the old (or previous) side or null
	 * @param b
	 *            the new (or updated) side or null
	 * @throws IOException
	 *             trees cannot be read, file contents cannot be read, or the
	 *             patch cannot be output.
	 */
	public void format(AnyObjectId a, AnyObjectId b) throws IOException {
		format(scan(a, b));
	}

	/**
	 * Format the differences between two trees.
	 *
	 * The patch is expressed as instructions to modify {@code a} to make it
	 * {@code b}.
	 *
	 * <p>
	 * Either side may be null to indicate that the tree has beed added or
	 * removed. The diff will be computed against nothing.
	 *
	 * @param a
	 *            the old (or previous) side or null
	 * @param b
	 *            the new (or updated) side or null
	 * @throws IOException
	 *             trees cannot be read, file contents cannot be read, or the
	 *             patch cannot be output.
	 */
	public void format(RevTree a, RevTree b) throws IOException {
		format(scan(a, b));
	}

	/**
	 * formatをLogコマンド実行時に構文情報を指定された時ように改変したもの. Log.javaにこのlogを出力するかどうかを返す.
	 *
	 * Format the differences between two trees.
	 *
	 * The patch is expressed as instructions to modify {@code a} to make it
	 * {@code b}.
	 *
	 * <p>
	 * Either side may be null to indicate that the tree has beed added or
	 * removed. The diff will be computed against nothing.
	 *
	 * @param a
	 *            the old (or previous) side or null
	 * @param b
	 *            the new (or updated) side or null
	 * @return log出力するかの判定 trueなら出力する
	 * @throws IOException
	 *             trees cannot be read, file contents cannot be read, or the
	 *             patch cannot be output.
	 */
	public boolean formatLog(RevTree a, RevTree b) throws IOException {
		return formatLog(scan(a, b));
	}

	/**
	 * Format the differences between two trees.
	 *
	 * The patch is expressed as instructions to modify {@code a} to make it
	 * {@code b}.
	 * <p>
	 * Either side may be null to indicate that the tree has beed added or
	 * removed. The diff will be computed against nothing.
	 *
	 * @param a
	 *            the old (or previous) side or null
	 * @param b
	 *            the new (or updated) side or null
	 * @throws IOException
	 *             trees cannot be read, file contents cannot be read, or the
	 *             patch cannot be output.
	 */
	public void format(AbstractTreeIterator a, AbstractTreeIterator b)
			throws IOException {
		// scan(a, b)の返り値 = a,b間で変更のあったファイル名の集合
		format(scan(a, b));
	}

	/**
	 * Format a patch script from a list of difference entries. Requires
	 * {@link #scan(AbstractTreeIterator, AbstractTreeIterator)} to have been
	 * called first.
	 *
	 * @param entries
	 *            entries describing the affected files.
	 * @throws IOException
	 *             a file's content cannot be read, or the output stream cannot
	 *             be written to.
	 */
	public void format(List<? extends DiffEntry> entries) throws IOException {
		for (DiffEntry ent : entries) {
			// 変更があったファイル一つ一つについてformat()していく
			// ので，ここでファイルをふるいにかけられる

			// System.out.println(ent.oldPath);

			// ここでfilesがjavaかどうかを一つ一つ確認する
			//if (CxFlg != -1 && ent.oldPath.endsWith(".java")) { //$NON-NLS-1$
			if (ent.oldPath.endsWith(".java")) { //$NON-NLS-1$
				// System.out.println("Yes java: " + ent.oldPath); //$NON-NLS-1$
				isJavaFile = 1;
				numJavaFile++;
				//System.out.println("numJavaFile is" + numJavaFile); //$NON-NLS-1$
			} else {
				// System.out.println("Not java: " + ent.oldPath); //$NON-NLS-1$
				isJavaFile = 0;
				numOtherFile++;
				//System.out.println("numOtherFile is" + numOtherFile); //$NON-NLS-1$
				// 次のファイルのチェックに行く．ファイルの内容出力しない（.javaしか出力しない）
				continue;
			}
			format(ent);
		}

		//System.out.println("final numJavaFile is" + numJavaFile); //$NON-NLS-1$
		//System.out.println("final numOtherFile is" + numOtherFile); //$NON-NLS-1$

	}

	/**
	 * Format a patch script from a list of difference entries. Requires
	 * {@link #scan(AbstractTreeIterator, AbstractTreeIterator)} to have been
	 * called first.
	 *
	 * @param entries
	 *            entries describing the affected files.
	 * @return result: log出力するかの判定 trueなら出力する
	 * @throws IOException
	 *             a file's content cannot be read, or the output stream cannot
	 *             be written to.
	 */
	public boolean formatLog(List<? extends DiffEntry> entries)
			throws IOException {
		// 指定クエリが変更されたかどうか
		boolean isSpecifiedQueryChanged = false;
		for (DiffEntry ent : entries) {
			// 変更があったファイル一つ一つについてformat()していくので，ここでファイルをふるいにかけられる
			// Logでcx指定された時だけ発動
			// 指定されたクエリに対応するファイルが一つでも変更されてたらLog出力＝trueを返す

			// ここでfilesがjavaかどうかを一つ一つ確認する
			if (ent.oldPath.endsWith(".java")) { //$NON-NLS-1$
				// System.out.println("Yes java" + ent.oldPath); //$NON-NLS-1$
				isJavaFile = 1;
				numJavaFile++;
			} else {
				// System.out.println("Not java" + ent.oldPath); //$NON-NLS-1$
				isJavaFile = 0;
				numOtherFile++;
				// 次のファイルのチェックに行く．
				// ファイルの内容出力しない（.javaしか出力しない）
				// AST解析もしない
				continue;
			}
			// 各ファイルの解析開始
			// trueが返って来たら変更あったってこと
			isSpecifiedQueryChanged = formatLog(ent);

			// 一個でもヒットしたらこれ以上見ない（AST構築せんで済むようになる）
			if (isSpecifiedQueryChanged) {
				// System.out.println(ent.oldPath + "が変更されてた．次のファイルみずにlog出力");
				break;
			} else {
				// System.out.println(ent.oldPath + "は変更されてなかった．次のファイルみる");
				continue;
			}

		}
		return isSpecifiedQueryChanged;
	}

	/**
	 * Format a patch script for one file entry.
	 *
	 * @param ent
	 *            the entry to be formatted.
	 * @throws IOException
	 *             a file's content cannot be read, or the output stream cannot
	 *             be written to.
	 */
	public void format(DiffEntry ent) throws IOException {

		//System.out.println("ent is " + ent); //$NON-NLS-1$

		FormatResult res = createFormatResult(ent);
		//System.out.println("res is " + res + "\nres.header is " + res.header); //$NON-NLS-1$ //$NON-NLS-2$
		//System.out.println("\nres.a is " + res.a + "\nres.b is " + res.b);//$NON-NLS-1$ //$NON-NLS-2$

		// res.a.writeLine(out, );
		// res.b.writeLine(out, );

		format(res.header, res.a, res.b);
	}

	/**
	 * Format a patch script for one file entry.
	 *
	 * @param ent
	 *            the entry to be formatted.
	 * @return 指定されたクエリが変更されたかどうか．変更されたらtrue
	 * @throws IOException
	 *             a file's content cannot be read, or the output stream cannot
	 *             be written to.
	 */
	public boolean formatLog(DiffEntry ent) throws IOException {

		// System.out.println("ent is " + ent); //$NON-NLS-1$

		// これで解析部分に入る
		FormatResult res = createFormatResult(ent);

		// ここ消したらファイルの中身の出力なくなる
		// format(res.header, res.a, res.b);

		// System.out.println(
		// "@DiffFmt: res.isSpecifiedQueryChanged = " //$NON-NLS-1$
		// + res.isSpecifiedQueryChanged);

		// isSpecifiedQueryChanged: trueで変更してる
		return res.isSpecifiedQueryChanged;
	}

	private static byte[] writeGitLinkText(AbbreviatedObjectId id) {
		if (id.toObjectId().equals(ObjectId.zeroId())) {
			return EMPTY;
		}
		return encodeASCII("Subproject commit " + id.name() //$NON-NLS-1$
				+ "\n"); //$NON-NLS-1$
	}

	private String format(AbbreviatedObjectId id) {
		if (id.isComplete() && reader != null) {
			try {
				id = reader.abbreviate(id.toObjectId(), abbreviationLength);
			} catch (IOException cannotAbbreviate) {
				// Ignore this. We'll report the full identity.
			}
		}
		return id.name();
	}

	private static String quotePath(String name) {
		return QuotedString.GIT_PATH.quote(name);
	}

	/**
	 * Format a patch script, reusing a previously parsed FileHeader.
	 * <p>
	 * This formatter is primarily useful for editing an existing patch script
	 * to increase or reduce the number of lines of context within the script.
	 * All header lines are reused as-is from the supplied FileHeader.
	 *
	 * @param head
	 *            existing file header containing the header lines to copy.
	 * @param a
	 *            text source for the pre-image version of the content. This
	 *            must match the content of {@link FileHeader#getOldId()}.
	 * @param b
	 *            text source for the post-image version of the content. This
	 *            must match the content of {@link FileHeader#getNewId()}.
	 * @throws IOException
	 *             writing to the supplied stream failed.
	 */
	public void format(final FileHeader head, final RawText a, final RawText b)
			throws IOException {
		// Reuse the existing FileHeader as-is by blindly copying its
		// header lines, but avoiding its hunks. Instead we recreate
		// the hunks from the text instances we have been supplied.

		// TODO:このぬるぽ対策適当．ちゃんとせなあかん
		// -cxと-pを同時実行するとぬるぽ起こる
		if (head == null) {
			return;
		}
		final int start = head.getStartOffset();
		int end = head.getEndOffset();
		if (!head.getHunks().isEmpty())
			end = head.getHunks().get(0).getStartOffset();
		out.write(head.getBuffer(), start, end - start);
		if (head.getPatchType() == PatchType.UNIFIED)
			format(head.toEditList(), a, b);
	}

	/**
	 * Formats a list of edits in unified diff format
	 *
	 * @param edits
	 *            some differences which have been calculated between A and B
	 * @param a
	 *            the text A which was compared
	 * @param b
	 *            the text B which was compared
	 * @throws IOException
	 */
	public void format(final EditList edits, final RawText a, final RawText b)
			throws IOException {
		for (int curIdx = 0; curIdx < edits.size();) {
			Edit curEdit = edits.get(curIdx);
			final int endIdx = findCombinedEnd(edits, curIdx);
			final Edit endEdit = edits.get(endIdx);

			int aCur = (int) Math.max(0, (long) curEdit.getBeginA() - context);
			int bCur = (int) Math.max(0, (long) curEdit.getBeginB() - context);
			final int aEnd = (int) Math.min(a.size(), (long) endEdit.getEndA() + context);
			final int bEnd = (int) Math.min(b.size(), (long) endEdit.getEndB() + context);

			writeHunkHeader(aCur, aEnd, bCur, bEnd);

			while (aCur < aEnd || bCur < bEnd) {
				if (aCur < curEdit.getBeginA() || endIdx + 1 < curIdx) {
					writeContextLine(a, aCur);
					// cxオプションの時にnoNewLineが出力されないようにする
					if (isEndOfLineMissing(a, aCur) && CxFlg == -1)
						out.write(noNewLine);
					aCur++;
					bCur++;
				} else if (aCur < curEdit.getEndA()) {
					writeRemovedLine(a, aCur);
					if (isEndOfLineMissing(a, aCur) && CxFlg == -1)
						out.write(noNewLine);
					aCur++;
				} else if (bCur < curEdit.getEndB()) {
					writeAddedLine(b, bCur);
					if (isEndOfLineMissing(b, bCur) && CxFlg == -1)
						out.write(noNewLine);
					bCur++;
				}

				if (end(curEdit, aCur, bCur) && ++curIdx < edits.size())
					curEdit = edits.get(curIdx);
			}
		}
	}

	/**
	 * Output a line of context (unmodified line).
	 *
	 * @param text
	 *            RawText for accessing raw data
	 * @param line
	 *            the line number within text
	 * @throws IOException
	 */
	protected void writeContextLine(final RawText text, final int line)
			throws IOException {
		writeLine(' ', text, line);
	}

	private static boolean isEndOfLineMissing(final RawText text, final int line) {
		return line + 1 == text.size() && text.isMissingNewlineAtEnd();
	}

	/**
	 * Output an added line.
	 *
	 * @param text
	 *            RawText for accessing raw data
	 * @param line
	 *            the line number within text
	 * @throws IOException
	 */
	protected void writeAddedLine(final RawText text, final int line)
			throws IOException {
		writeLine('+', text, line);
	}

	/**
	 * Output a removed line
	 *
	 * @param text
	 *            RawText for accessing raw data
	 * @param line
	 *            the line number within text
	 * @throws IOException
	 */
	protected void writeRemovedLine(final RawText text, final int line)
			throws IOException {
		writeLine('-', text, line);
	}

	/**
	 * Output a hunk header
	 *
	 * @param aStartLine
	 *            within first source
	 * @param aEndLine
	 *            within first source
	 * @param bStartLine
	 *            within second source
	 * @param bEndLine
	 *            within second source
	 * @throws IOException
	 */
	protected void writeHunkHeader(int aStartLine, int aEndLine,
			int bStartLine, int bEndLine) throws IOException {
		out.write('@');
		out.write('@');
		writeRange('-', aStartLine + 1, aEndLine - aStartLine);
		writeRange('+', bStartLine + 1, bEndLine - bStartLine);
		out.write(' ');
		out.write('@');
		out.write('@');
		out.write('\n');
	}

	private void writeRange(final char prefix, final int begin, final int cnt)
			throws IOException {
		out.write(' ');
		out.write(prefix);
		switch (cnt) {
		case 0:
			// If the range is empty, its beginning number must be the
			// line just before the range, or 0 if the range is at the
			// start of the file stream. Here, begin is always 1 based,
			// so an empty file would produce "0,0".
			//
			out.write(encodeASCII(begin - 1));
			out.write(',');
			out.write('0');
			break;

		case 1:
			// If the range is exactly one line, produce only the number.
			//
			out.write(encodeASCII(begin));
			break;

		default:
			out.write(encodeASCII(begin));
			out.write(',');
			out.write(encodeASCII(cnt));
			break;
		}
	}

	/**
	 * Write a standard patch script line.
	 *
	 * @param prefix
	 *            prefix before the line, typically '-', '+', ' '.
	 * @param text
	 *            the text object to obtain the line from.
	 * @param cur
	 *            line number to output.
	 * @throws IOException
	 *             the stream threw an exception while writing to it.
	 */
	protected void writeLine(final char prefix, final RawText text,
			final int cur) throws IOException {
		// 色変えたい(これじゃ無理)
		//System.out.print("\u001b[31m"); //$NON-NLS-1$
		out.write(prefix);

		// 出力させている命令？
		text.writeLine(out, cur);
		out.write('\n');
		//System.out.print("\u001b[00m"); //$NON-NLS-1$

	}

	/**
	 * Creates a {@link FileHeader} representing the given {@link DiffEntry}
	 * <p>
	 * This method does not use the OutputStream associated with this
	 * DiffFormatter instance. It is therefore safe to instantiate this
	 * DiffFormatter instance with a {@link DisabledOutputStream} if this method
	 * is the only one that will be used.
	 *
	 * @param ent
	 *            the DiffEntry to create the FileHeader for
	 * @return a FileHeader representing the DiffEntry. The FileHeader's buffer
	 *         will contain only the header of the diff output. It will also
	 *         contain one {@link HunkHeader}.
	 * @throws IOException
	 *             the stream threw an exception while writing to it, or one of
	 *             the blobs referenced by the DiffEntry could not be read.
	 * @throws CorruptObjectException
	 *             one of the blobs referenced by the DiffEntry is corrupt.
	 * @throws MissingObjectException
	 *             one of the blobs referenced by the DiffEntry is missing.
	 */
	public FileHeader toFileHeader(DiffEntry ent) throws IOException,
	CorruptObjectException, MissingObjectException {
		return createFormatResult(ent).header;
	}

	// miwa
	// publicに
	private static class FormatResult {
		FileHeader header;

		RawText a;

		RawText b;

		boolean isSpecifiedQueryChanged;
	}

	/**
	 * miwa
	 *
	 * @param ent
	 * @return x
	 * @throws IOException
	 * @throws CorruptObjectException
	 * @throws MissingObjectException
	 */
	public FormatResult createFormatResult(DiffEntry ent)
			throws IOException, CorruptObjectException, MissingObjectException {
		final FormatResult res = new FormatResult();
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		final EditList editList;
		final FileHeader.PatchType type;

		formatHeader(buf, ent);

		if (ent.getOldId() == null || ent.getNewId() == null) {
			// Content not changed (e.g. only mode, pure rename)
			editList = new EditList();
			type = PatchType.UNIFIED;

		} else {
			assertHaveReader();

			byte[] aRaw, bRaw;

			if (ent.getOldMode() == GITLINK || ent.getNewMode() == GITLINK) {
				aRaw = writeGitLinkText(ent.getOldId());
				bRaw = writeGitLinkText(ent.getNewId());
			} else {
				aRaw = open(OLD, ent);
				bRaw = open(NEW, ent);
			}

			if (aRaw == BINARY || bRaw == BINARY //
					|| RawText.isBinary(aRaw) || RawText.isBinary(bRaw)) {
				formatOldNewPaths(buf, ent);
				buf.write(encodeASCII("Binary files differ\n")); //$NON-NLS-1$
				editList = new EditList();
				type = PatchType.BINARY;

			} else {

				// ////ここから改変//////

				// 一時ファイルとかおく場所
				String filePlace = "/Users/miwaaa8/Documents/研究/workspace/files/"; //$NON-NLS-1$

				//cxオプションの有無
				// -1はオプションなし．普通にdiff実行やから拡張処理とばす
				// javaファイルしかみーひんで
				if (CxFlg == 1 && isJavaFile == 1) {
					// System.out.println("cx オプション！！"); //$NON-NLS-1$

					// RawTextを外部に書き出す
					String aRawS = new String(aRaw);
					export(filePlace + "aRaw.java", aRawS); //$NON-NLS-1$
					String bRawS = new String(bRaw);
					export(filePlace + "bRaw.java", bRawS); //$NON-NLS-1$

					// 分析結果を外部に書きだす
					// System.out.println("分析結果を外部に書き出す");
					aSyntaxFlg = exec(filePlace + "aRaw.java",
							filePlace + "aRaw_exp.java",
							MJQuery);
					bSyntaxFlg = exec(filePlace + "bRaw.java",
							filePlace + "bRaw_exp.java",
							MJQuery);


					// 構文エラーチェック
					if (aSyntaxFlg && bSyntaxFlg) {
						if (isCommentPrintedFlg)
							System.out.println("構文エラーなし"); //$NON-NLS-1$

						// aRawをaRaw_exp(分析結果)に書き換え
						aRawS = fileToString(
								new File(filePlace + "aRaw_exp.java")); //$NON-NLS-1$
						aRaw = aRawS.getBytes("UTF-8"); //$NON-NLS-1$

						// bRawをbRaw_exp(分析結果)に書き換え
						bRawS = fileToString(
								new File(filePlace + "bRaw_exp.java")); //$NON-NLS-1$
						bRaw = bRawS.getBytes("UTF-8"); //$NON-NLS-1$

					} else {
						if (isCommentPrintedFlg)
							System.out.println("構文エラーあり"); //$NON-NLS-1$
					}

					if (CxFlgLog == 1) {
						// System.out.println("Logの cx オプション！！"); //$NON-NLS-1$
						// Logでcx指定された時は，ファイルの中身は出力しない．

						// 比較する両方のファイルに構文エラーがないか判断
						// どちらか片方でも構文エラーがある場合，比較できないのでその注意文を出力してlogを出力する．
						if (!(aSyntaxFlg && bSyntaxFlg)) {
							System.out.println(
									"vvvvvv This revision has some syntax errors. vvvvvv");
							res.isSpecifiedQueryChanged = true;
							return res;
						}

						// 解析結果が変更されているか（指定されたクエリが変更されたか）を確認
						res.isSpecifiedQueryChanged = fileCompare(
								filePlace + "aRaw_exp.java",
								filePlace + "bRaw_exp.java");
						// System.out.println("res.isSpecifiedQueryChanged: "
						// //$NON-NLS-1$
						// + res.isSpecifiedQueryChanged);

						// Logの時はファイル内容出力しないからこの先の処理いらない．returnする
						return res;
					}

				}
				// ////ここから元ソース////////

				res.a = new RawText(aRaw);
				res.b = new RawText(bRaw);

				editList = diff(res.a, res.b);
				type = PatchType.UNIFIED;

				switch (ent.getChangeType()) {
				case RENAME:
				case COPY:
					if (!editList.isEmpty())
						formatOldNewPaths(buf, ent);
					break;

				default:
					formatOldNewPaths(buf, ent);
					break;
				}
			}
		}
		res.header = new FileHeader(buf.toByteArray(), editList, type);
		return res;
	}

	/**
	 * @param fileA
	 *            ファイル名
	 * @param fileB
	 *            ファイル名
	 * @return ファイルが変更されている = true
	 *
	 *         fileAとfileBの中身を1行ずつ見ていき比較する． ただし，改行と空白は無視する．
	 */
	public static boolean fileCompare(String fileA, String fileB) {
		boolean isSpecifiedQueryChanged = false;
		String lineA = "";
		String lineB = "";
		boolean isFileASpaceOnly = true;
		boolean isFileBSpaceOnly = true;

		// A, Bが空白かどうか
		try (BufferedReader inA = new BufferedReader(
				new FileReader(new File(fileA)))) {
			try (BufferedReader inB = new BufferedReader(
					new FileReader(new File(fileB)))) {

				// １回目にファイル内容nullかどうか確認
				lineA = inA.readLine();
				lineB = inB.readLine();

				if(lineA == null) {
					lineA = "";
				}
				if(lineB == null) {
					lineB = "";
				}

				// if((lineA != null && lineB == null) || (lineA == null &&
				// lineB != null)) {
				// //片方null
				// System.out.println("最初のチェック．片方null");
				// return true;
				// } else if (lineA == null && lineB == null) {
				// //両方null
				// System.out.println("最初のチェック，両方null");
				// return false;
				// }
				// // 両方not null
				// System.out.println("両方not null");

				// A,B両方1行進めるループ
				while (lineA != null && lineB != null) {

					// Aだけを進めるループ
					while (lineA != null) {
						// System.out.println("lienA: " + lineA);
						// 空白，タブ，改行を除外
						lineA = lineA.replaceAll(" ", "");
						lineA = lineA.replaceAll("\t", "");
						if (lineA.length() == 0) {
							// 除外される行やから次の行へ
							// System.out.println("lienA: " + lineA);
							lineA = inA.readLine();
							continue;
						} else {
							isFileASpaceOnly = false;
							break;
						}
					}
					// System.out.println("Aループ終了");
					// Bだけを進めるループ
					while (lineB != null) {
						// 空白，タブ，改行を除外
						// System.out.println("lienB: " + lineB);
						lineB = lineB.replaceAll(" ", "");
						lineB = lineB.replaceAll("\t", "");
						if (lineB.length() == 0) {
							// 除外される行やから次の行へ
							lineB = inB.readLine();
							continue;
						} else {
							isFileBSpaceOnly = false;
							break;
						}
					}
					// 両方が空白only
					if (isFileASpaceOnly && isFileBSpaceOnly) {
						// System.out.println("両方空白");
						return false;
					} else if ((isFileASpaceOnly && !isFileBSpaceOnly)
							|| (!isFileASpaceOnly && isFileBSpaceOnly)) {// 片方が空白（変更あり）
						// System.out.println("片方が空白");
						// System.out.println("isFileA: " + isFileASpaceOnly);
						// System.out.println("isFileB: " + isFileBSpaceOnly);
						return true;
					}
					lineA = inA.readLine();
					lineB = inB.readLine();
				}

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		// 両方が空白じゃないからちゃんと比較する
		// System.out.println("両方not空白");

		// A,Bが一致しているかどうか
		try (BufferedReader inA = new BufferedReader(
				new FileReader(new File(fileA)))) {
			try (BufferedReader inB = new BufferedReader(
					new FileReader(new File(fileB)))) {

				// A,B両方1行進めるループ
				while ((lineA = inA.readLine()) != null
						&& (lineB = inB.readLine()) != null) {

					// Aだけを進めるループ
					while (lineA != null) {
						// 空白，タブ，改行を除外
						lineA = lineA.replaceAll(" ", "");
						lineA = lineA.replaceAll("\t", "");
						if (lineA.length() == 0) {
							// 除外される行やから次の行へ
							lineA = inA.readLine();
							continue;
						}
						// isFileASpaceOnly = false;
						// System.out.println("lineA:" + lineA);
						// 比較するべき行にきたからBのループへ移動
						break;
					}

					// Bだけを進めるループ
					while (lineB != null) {
						// 空白，タブ，改行を除外
						lineB = lineB.replaceAll(" ", "");
						lineB = lineB.replaceAll("\t", "");
						if (lineB.length() == 0) {
							// 除外される行やから次の行へ
							lineB = inB.readLine();
							continue;
						}
						// isFileBSpaceOnly = false;
						// System.out.println("lineB:" + lineB);
						// 比較するべき行にきたからAと比較
						// Aが!nullの場合，Bと一致しなければ変更あり
						// Aがnullの場合，変更あり(ここに来た時点でBに変更はあった)
						if ((lineA != null && !lineA.equals(lineB))) {
							// 変更があった
							// if (isCommentPrintedFlg)
							// System.out.println("変更あり");
							return true;
						}
						// AとBが一致したので
						// AもBも次の行に進める
						// System.out.println("一致した，次の行");
						break;
					}

					// // Bが空白でBがnot空白
					// if (!isFileASpaceOnly && isFileBSpaceOnly) {
					// // System.out.println("A not空白，B 空白");
					// return true;
					// }

				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return isSpecifiedQueryChanged;
	}

	/**
	 * @param inputFilename
	 * @param outputFilename
	 * @param query
	 * @return 分析できたかどうか
	 * @throws IOException
	 */
	public boolean exec(String inputFilename, String outputFilename,
			String query) throws IOException {
		boolean ast;
		boolean com;
		boolean jd;
		boolean anno;

		// テキストを読み込む
		String source = null;
		try {
			source = fileToString(new File(inputFilename));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		// Create AST Parser
		// バインディングを実行する設定
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setBindingsRecovery(true);

		Map options = JavaCore.getOptions();
		parser.setCompilerOptions(options);
		parser.setUnitName(inputFilename);

		// バインディングするための環境設定
		// 先生の
		// String[] sources = { "D:\\tmp\\astnode\\example" };
		// String[] classpath = { "C:\\pbl\\java\\jre\\lib\\rt.jar" };
		// わいの
		String[] sources = {
				"\\Users\\miwaaa8\\Documents\\研究\\workspace\\astnode\\example" };
		String[] classpath = {
				"\\Library\\Java\\JavaVirtualMachines\\jdk1.8.0_25.jdk\\Contents\\Home\\jre\\lib\\rt.jar" };

		parser.setEnvironment(classpath, sources, new String[] { "UTF-8" },
				true);
		parser.setSource(source.toCharArray());

		CompilationUnit unit = (CompilationUnit) parser.createAST(null);

		MJQuery mjquery = new MJQuery(query, "simple");
		// System.out.println("MJQuery= " + mjquery);

		Set<Integer> lineNumbers = new TreeSet<Integer>();
		// ひとつのコンテキスト解析途中に使う
		Set<Integer> tmpLineNumbers = new TreeSet<Integer>();
		// 各コンテキストの解析結果を次の解析で上書きしない為に使う
		Set<Integer> finalLineNumbers = new TreeSet<Integer>();

		boolean syntaxFlg = true;
		int queryNum = 0;

		for (Context context : mjquery.contexts) {
			// System.out.println("コンテキストfor文: " + context);
			// System.out.println("queryNum: " + queryNum);
			// 構文エラーならループおわり
			if (!syntaxFlg)
				break;

			ast = com = jd = anno = false;
			if (context == Context.STATEMENT)
				ast = true;
			if (context == Context.COMMENT)
				com = true;
			if (context == Context.JAVADOC)
				jd = true;
			if (context == Context.ANNOTATION)
				anno = true;

			// 該当コンテキストを書き出す
			// コメント指定された時でも構文エラー見つけるために入らなあかん
			// if (context == Context.STATEMENT || context == Context.JAVADOC ||
			// context == Context.ANNOTATION) {
			Visitor visitor = new Visitor(unit, source.split("\n"), mjquery,
					null, queryNum);
			unit.accept(visitor);
			// System.out.println("v.lineNum: " + visitor.lineNumbers);
			tmpLineNumbers.addAll(visitor.lineNumbers);
			// System.out.println("staのadd後のtmpLineNums: " + tmpLineNumbers);

			syntaxFlg = visitor.SyntaxFlg;
			// }
			// コメントを書き出すor書き込む
			CommentVisitor commentVisitor = new CommentVisitor(unit,
					source.split("\n"), mjquery, tmpLineNumbers, queryNum);
			unit.accept(commentVisitor);
			// System.out.println("CV.lineNum: " + commentVisitor.lineNumbers);
			// 上書きor追記
			if (com) {
				// コメントを書き足したい時はadd(追加)
				// System.out.println("コメント追加");
				lineNumbers.addAll(commentVisitor.lineNumbers);
			} else {
				// コメントを削除したい時は上書き
				// System.out.println("コメント削除");
				lineNumbers = commentVisitor.lineNumbers;
			}
			// System.out.println("comのadd後のlineNums: " + lineNumbers);
			queryNum++;

			// つぎのコンテキストのループに持って行かないように退避
			// System.out.println("add前final: " + finalLineNumbers);
			finalLineNumbers.addAll(lineNumbers);
			// System.out.println("add後final: " + finalLineNumbers);
		}

		// 外部ファイルに出力
		// 構文エラーなら分解せずに出力
		if (syntaxFlg) {
			// System.out.println("構文エラーないから解析したやつ出力するよ");
			export(outputFilename, inputFilename, finalLineNumbers);
			return true;
		} else {
			// System.out.println("構文エラー！！！");
			// fileCopy(inputFilename, outputFilename);
			String s = fileToString(new File(inputFilename)); // $NON-NLS-1$
			export(outputFilename, s); // $NON-NLS-1$
			return false;
		}
	}


	// 解析結果の行番号を参照して外部ファイルに出力するメソッド
	/**
	 * @param filename
	 * @param source
	 * @param set
	 */
	public static void export(String filename, String source,
			Set<Integer> set) {
		try {
			List<String> lines = Files.readAllLines(Paths.get(source));
			// ファイル名出力
			if (isCommentPrintedFlg) {
				System.out.println("------------line set---------------");
				System.out.println("file name: " + filename);
				System.out.println("-------------line set--------------");
			}
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < lines.size(); i++) {
				if (set.contains(i)) {
					buffer.append(lines.get(i) + "\n");
					// 解析結果出力
					if (isCommentPrintedFlg) {
						System.out.println(lines.get(i));
					}
					set.remove(i);
				} else {
					buffer.append("\n");
				}
			}
			// 最後の一個改行多いから消す
			if (buffer.length() >= 1) {
				buffer.deleteCharAt(buffer.length() - 1);
			}
			Files.write(Paths.get(filename), String.valueOf(buffer).getBytes());
			// ファイル内容出力
			// System.out.println("--------------");
			// System.out.println(buffer);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * @param filename
	 * @param body
	 */
	// 解析結果を外部ファイルに出力するメソッド
	public static void export(String filename, String body) {
		// ファイル名を編集
		// filename =
		// "/Users/miwaaa8/Documents/研究/workspace/jgit/org.eclipse.jgit.pgm/src/files/"
		// + filename + ".java"; //$NON-NLS-1$ //$NON-NLS-2$
		// filename = "/org.eclipse.jgit.pgm/src/files/" + filename + ".java";
		// //$NON-NLS-1$ //$NON-NLS-2$

		// body出力
		// System.out.println("----------------------------------------");
		// System.out.println("export smpleの出力" + filename);
		// System.out.println(body);
		// System.out.println("----------------------------------------");

		// ファイル作成
		File newfile = new File(filename);
		try {
			if (newfile.createNewFile()) {
				// System.out.println("ファイルの作成に成功しました");
			} else {
				// System.out.println("ファイルの作成に失敗しました");
			}
		} catch (IOException e) {
			System.out.println(e);
		}
		try {
			File file = new File(filename);

			if (checkBeforeWritefile(file)) {
				FileWriter filewriter = new FileWriter(file);
				filewriter.write(body);
				// System.out.println(filename + "に書き込み完了"); //$NON-NLS-1$
				filewriter.close();
			} else {
				// System.out.println(filename + "に書き込めません"); //$NON-NLS-1$
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	// 外部ファイルのチェック
	private static boolean checkBeforeWritefile(File file) {
		if (file.exists()) {
			if (file.isFile() && file.canWrite()) {
				return true;
			}
		}
		return false;
	}


	// ファイル内容を文字列化するメソッド
	/**
	 * @param file
	 * @return 文字列化されたstring
	 * @throws IOException
	 */
	public static String fileToString(File file) throws IOException {
		BufferedReader br = null;
		try {
			// ファイルを読み込むバッファドリーダを作成します。
			br = new BufferedReader(
					new InputStreamReader(new FileInputStream(file)));
			// 読み込んだ文字列を保持するストリングバッファを用意します。
			StringBuffer sb = new StringBuffer();
			// ファイルから読み込んだ一文字を保存する変数です。
			int c;
			// ファイルから１文字ずつ読み込み、バッファへ追加します。
			while ((c = br.read()) != -1) {
				sb.append((char) c);
			}
			// バッファの内容を文字列化して返します。
			return sb.toString();
		} finally {
			// リーダを閉じます。
			br.close();
		}
	}

	/**
	 * @param in
	 * @param out
	 */
	public void fileCopy(String in, String out) {

		File fileIn = new File(in);
		File fileOut = new File(out);

		// FileChannelクラスのオブジェクトを生成する
		FileChannel inCh = null;
		try {
			inCh = new FileInputStream(fileIn).getChannel();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		FileChannel outCh = null;
		try {
			outCh = new FileOutputStream(fileOut).getChannel();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// transferToメソッドを使用してファイルをコピーする
		try {
			inCh.transferTo(0, inCh.size(), outCh);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private EditList diff(RawText a, RawText b) {
		return diffAlgorithm.diff(comparator, a, b);
	}

	private void assertHaveReader() {
		if (reader == null) {
			throw new IllegalStateException(JGitText.get().readerIsRequired);
		}
	}

	private byte[] open(DiffEntry.Side side, DiffEntry entry)
			throws IOException {
		if (entry.getMode(side) == FileMode.MISSING)
			return EMPTY;

		if (entry.getMode(side).getObjectType() != Constants.OBJ_BLOB)
			return EMPTY;

		AbbreviatedObjectId id = entry.getId(side);
		if (!id.isComplete()) {
			Collection<ObjectId> ids = reader.resolve(id);
			if (ids.size() == 1) {
				id = AbbreviatedObjectId.fromObjectId(ids.iterator().next());
				switch (side) {
				case OLD:
					entry.oldId = id;
					break;
				case NEW:
					entry.newId = id;
					break;
				}
			} else if (ids.size() == 0)
				throw new MissingObjectException(id, Constants.OBJ_BLOB);
			else
				throw new AmbiguousObjectException(id, ids);
		}

		try {
			ObjectLoader ldr = source.open(side, entry);
			return ldr.getBytes(binaryFileThreshold);

		} catch (LargeObjectException.ExceedsLimit overLimit) {
			return BINARY;

		} catch (LargeObjectException.ExceedsByteArrayLimit overLimit) {
			return BINARY;

		} catch (LargeObjectException.OutOfMemory tooBig) {
			return BINARY;

		} catch (LargeObjectException tooBig) {
			tooBig.setObjectId(id.toObjectId());
			throw tooBig;
		}
	}

	/**
	 * Output the first header line
	 *
	 * @param o
	 *            The stream the formatter will write the first header line to
	 * @param type
	 *            The {@link ChangeType}
	 * @param oldPath
	 *            old path to the file
	 * @param newPath
	 *            new path to the file
	 * @throws IOException
	 *             the stream threw an exception while writing to it.
	 */
	protected void formatGitDiffFirstHeaderLine(ByteArrayOutputStream o,
			final ChangeType type, final String oldPath, final String newPath)
					throws IOException {
		o.write(encodeASCII("diff --git ")); //$NON-NLS-1$
		o.write(encode(quotePath(oldPrefix + (type == ADD ? newPath : oldPath))));
		o.write(' ');
		o.write(encode(quotePath(newPrefix
				+ (type == DELETE ? oldPath : newPath))));
		o.write('\n');
	}

	private void formatHeader(ByteArrayOutputStream o, DiffEntry ent)
			throws IOException {
		final ChangeType type = ent.getChangeType();
		final String oldp = ent.getOldPath();
		final String newp = ent.getNewPath();
		final FileMode oldMode = ent.getOldMode();
		final FileMode newMode = ent.getNewMode();

		formatGitDiffFirstHeaderLine(o, type, oldp, newp);

		if ((type == MODIFY || type == COPY || type == RENAME)
				&& !oldMode.equals(newMode)) {
			o.write(encodeASCII("old mode ")); //$NON-NLS-1$
			oldMode.copyTo(o);
			o.write('\n');

			o.write(encodeASCII("new mode ")); //$NON-NLS-1$
			newMode.copyTo(o);
			o.write('\n');
		}

		switch (type) {
		case ADD:
			o.write(encodeASCII("new file mode ")); //$NON-NLS-1$
			newMode.copyTo(o);
			o.write('\n');
			break;

		case DELETE:
			o.write(encodeASCII("deleted file mode ")); //$NON-NLS-1$
			oldMode.copyTo(o);
			o.write('\n');
			break;

		case RENAME:
			o.write(encodeASCII("similarity index " + ent.getScore() + "%")); //$NON-NLS-1$ //$NON-NLS-2$
			o.write('\n');

			o.write(encode("rename from " + quotePath(oldp))); //$NON-NLS-1$
			o.write('\n');

			o.write(encode("rename to " + quotePath(newp))); //$NON-NLS-1$
			o.write('\n');
			break;

		case COPY:
			o.write(encodeASCII("similarity index " + ent.getScore() + "%")); //$NON-NLS-1$ //$NON-NLS-2$
			o.write('\n');

			o.write(encode("copy from " + quotePath(oldp))); //$NON-NLS-1$
			o.write('\n');

			o.write(encode("copy to " + quotePath(newp))); //$NON-NLS-1$
			o.write('\n');
			break;

		case MODIFY:
			if (0 < ent.getScore()) {
				o.write(encodeASCII("dissimilarity index " //$NON-NLS-1$
						+ (100 - ent.getScore()) + "%")); //$NON-NLS-1$
				o.write('\n');
			}
			break;
		}

		if (ent.getOldId() != null && !ent.getOldId().equals(ent.getNewId())) {
			formatIndexLine(o, ent);
		}
	}

	/**
	 * @param o
	 *            the stream the formatter will write line data to
	 * @param ent
	 *            the DiffEntry to create the FileHeader for
	 * @throws IOException
	 *             writing to the supplied stream failed.
	 */
	protected void formatIndexLine(OutputStream o, DiffEntry ent)
			throws IOException {
		o.write(encodeASCII("index " // //$NON-NLS-1$
				+ format(ent.getOldId()) //
				+ ".." // //$NON-NLS-1$
				+ format(ent.getNewId())));
		if (ent.getOldMode().equals(ent.getNewMode())) {
			o.write(' ');
			ent.getNewMode().copyTo(o);
		}
		o.write('\n');
	}

	private void formatOldNewPaths(ByteArrayOutputStream o, DiffEntry ent)
			throws IOException {
		if (ent.oldId.equals(ent.newId))
			return;

		final String oldp;
		final String newp;

		switch (ent.getChangeType()) {
		case ADD:
			oldp = DiffEntry.DEV_NULL;
			newp = quotePath(newPrefix + ent.getNewPath());
			break;

		case DELETE:
			oldp = quotePath(oldPrefix + ent.getOldPath());
			newp = DiffEntry.DEV_NULL;
			break;

		default:
			oldp = quotePath(oldPrefix + ent.getOldPath());
			newp = quotePath(newPrefix + ent.getNewPath());
			break;
		}

		o.write(encode("--- " + oldp + "\n")); //$NON-NLS-1$ //$NON-NLS-2$
		o.write(encode("+++ " + newp + "\n")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private int findCombinedEnd(final List<Edit> edits, final int i) {
		int end = i + 1;
		while (end < edits.size()
				&& (combineA(edits, end) || combineB(edits, end)))
			end++;
		return end - 1;
	}

	private boolean combineA(final List<Edit> e, final int i) {
		return e.get(i).getBeginA() - e.get(i - 1).getEndA() <= 2 * context;
	}

	private boolean combineB(final List<Edit> e, final int i) {
		return e.get(i).getBeginB() - e.get(i - 1).getEndB() <= 2 * context;
	}

	private static boolean end(final Edit edit, final int a, final int b) {
		return edit.getEndA() <= a && edit.getEndB() <= b;
	}
}
