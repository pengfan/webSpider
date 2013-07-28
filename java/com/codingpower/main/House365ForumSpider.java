package com.codingpower.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.kevinsawicki.http.HttpRequest;

/**
 * 自动抓取华侨路某个论坛里的内容
 * 
 * @author pengf
 * 
 */
public class House365ForumSpider {
	public static final String HOST = "http://bbs.house365.com";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File baseFolder = new File("D:\\temp\\huaqiaoluPages");
		String forumUrl = "http://bbs.house365.com/forumdisplay.php?forumid=581";

		findPage(baseFolder, forumUrl);
		// saveFile("http://bbs.house365.com/forumdisplay.php?forumid=581",
		// "1");
		// spider("http://bbs.house365.com/showthread.php?threadid=4240585&forumid=307");
		// saveFile("http://bbs.house365.com/showthread.php?threadid=4240585&forumid=307");
	}

	public static void findPage(File baseFolder, String url) {
		File parent = new File(baseFolder, url.substring(url.indexOf("?") + 1));
		parent.mkdirs();
		File storeFile = new File(parent, "store.txt");
		if (!storeFile.exists()) {
			try {
				storeFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		String rsp = HttpRequest.get(url).acceptCharset("GBK,utf-8")
				.body("GBK");
		int maxPage = findMaxPage(url, rsp);
		System.out.println("max page : " + maxPage);
		// 先测试1页
		maxPage = 1;
		FileWriter fWriter = null;
		try {
			fWriter = new FileWriter(storeFile, true);
			spidePostsToFile(rsp, fWriter, parent);
			for (int i = 2; i <= maxPage; i++) {
				String pageUrl = url + "&pagenumber=" + i;
				System.out.println("spide url: " + pageUrl);
				rsp = HttpRequest.get(pageUrl).acceptCharset("GBK,utf-8")
						.body("GBK");
				spidePostsToFile(rsp, fWriter, parent);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fWriter != null) {
				try {
					fWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 找出最大页数
	 * 
	 * @param baseUrl
	 * @param val
	 * @return
	 */
	public static int findMaxPage(String baseUrl, String val) {
		baseUrl = baseUrl.substring(baseUrl.indexOf("?") + 1);
		int maxPage = 0;
		Pattern p = Pattern.compile(baseUrl + "&pagenumber=(\\d+)");
		Matcher m = p.matcher(val);
		while (m.find()) {
			try {
				int page = Integer.parseInt(m.group(1));
				if (page > maxPage) {
					maxPage = page;
				}
			} catch (NumberFormatException e) {
			}
		}
		return maxPage;
	}

	/**
	 * 抓取帖子数据
	 * 
	 * @param url
	 */
	public static void spidePostsToFile(String val, FileWriter writer, File baseFolder) {
		Pattern p = Pattern
				.compile("<tr\\s+id=\"tr_thread_\\d+\"[^>]+>([\\s\\S]+?)</tr>");
		Matcher m = p.matcher(val);
		while (m.find()) {
			String post = m.group(1);
			// System.out.println("post : " + post);
			Pattern childP = Pattern
					.compile("<a\\s+href\\s*=\\s*\"(/showthread.php\\?threadid=\\d+&forumid=\\d+)[^>]*>([^<>]+)</a>");
			Matcher childMatcher = childP.matcher(post);
			if (childMatcher.find()) {
				String link = childMatcher.group(1);
				String caption = childMatcher.group(2);
				String content = link + "\n" + caption + "\n";
				try {
					writer.write(content);
					findDetail(baseFolder, HOST + link);
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println(content);
			}
		}
	}

	public static void findDetail(File baseFolder, String url) {
		File parent = new File(baseFolder, url.substring(url.indexOf("?") + 1));
		parent.mkdirs();
		
		String rsp = HttpRequest.get(url).acceptCharset("GBK,utf-8")
				.body("GBK");
		int maxPage = findMaxPage(url, rsp);
		System.out.println(url + " max page : " + maxPage);
		writeTo(parent, rsp, 1);
		
		for (int i = 2; i <= maxPage; i++) {
			String pageUrl = url + "&pagenumber=" + i;
			System.out.println("spide url: " + pageUrl);
			rsp = HttpRequest.get(pageUrl).acceptCharset("GBK,utf-8")
					.body("GBK");
			writeTo(parent, rsp, i);
		}
	}
	/**
	 * 写入
	 *
	 * @param baseFolder
	 * @param rsp
	 */
	public static void writeTo(File baseFolder, String rsp, int pageNum)
	{
		File page = createNewFile(baseFolder, pageNum + ".txt");
		FileWriter fWriter = null;
		try {
			fWriter = new FileWriter(page, true);
			spideDetialsToFile(rsp, fWriter);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (fWriter != null) {
				try {
					fWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void spideDetialsToFile(String val, FileWriter writer) {
		Pattern p = Pattern
			.compile("<td\\s+id=\"postmessage_\\d+\"[^>]+>([\\s\\S]+?)</td>");
		Matcher m = p.matcher(val);
		while (m.find()) {
			String content = m.group(1);
			try {
				writer.write(content);
				writer.write("\n=====================================================================\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			//System.out.println(content);
		}
	}
	
	public static File createNewFile(File baseFolder, String fileName) {
		File storeFile = new File(baseFolder, fileName);
		if (!storeFile.exists()) {
			try {
				storeFile.createNewFile();
				return storeFile;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
