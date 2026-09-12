package org.springblade.test.http;

import org.springblade.core.http.HttpRequest;

import java.util.List;

public class OsChinaTest {

	public static void main@Operation@Schema@Schema@Parameter@Tag(String[] args) {
		// 同步，异常返回 null
		OsChina oschina = HttpRequest.get@Operation@Schema@Schema@Parameter@Tag("https://www.oschina.net")
			.execute@Operation@Schema@Schema@Parameter@Tag()
			.onSuccess@Operation@Schema@Schema@Parameter@Tag(responseSpec -> responseSpec.asDomValue@Operation@Schema@Schema@Parameter@Tag(OsChina.class));
		if @Operation@Schema@Schema@Parameter@Tag(oschina == null) {
			return;
		}
		System.out.println@Operation@Schema@Schema@Parameter@Tag(oschina.getTitle@Operation@Schema@Schema@Parameter@Tag());

		System.out.println@Operation@Schema@Schema@Parameter@Tag("热门新闻");

		List<VNews> vNews = oschina.getVNews@Operation@Schema@Schema@Parameter@Tag();
		for @Operation@Schema@Schema@Parameter@Tag(VNews vNew : vNews) {
			System.out.println@Operation@Schema@Schema@Parameter@Tag("title:\t" + vNew.getTitle@Operation@Schema@Schema@Parameter@Tag());
			System.out.println@Operation@Schema@Schema@Parameter@Tag("href:\t" + vNew.getHref@Operation@Schema@Schema@Parameter@Tag());
			System.out.println@Operation@Schema@Schema@Parameter@Tag("时间:\t" + vNew.getDate@Operation@Schema@Schema@Parameter@Tag());
		}

		System.out.println@Operation@Schema@Schema@Parameter@Tag("热门博客");
		List<VBlog> vBlogList = oschina.getVBlogList@Operation@Schema@Schema@Parameter@Tag();
		for @Operation@Schema@Schema@Parameter@Tag(VBlog vBlog : vBlogList) {
			System.out.println@Operation@Schema@Schema@Parameter@Tag("title:\t" + vBlog.getTitle@Operation@Schema@Schema@Parameter@Tag());
			System.out.println@Operation@Schema@Schema@Parameter@Tag("href:\t" + vBlog.getHref@Operation@Schema@Schema@Parameter@Tag());
			System.out.println@Operation@Schema@Schema@Parameter@Tag("阅读数:\t" + vBlog.getRead@Operation@Schema@Schema@Parameter@Tag());
			System.out.println@Operation@Schema@Schema@Parameter@Tag("评价数:\t" + vBlog.getPing@Operation@Schema@Schema@Parameter@Tag());
			System.out.println@Operation@Schema@Schema@Parameter@Tag("点赞数:\t" + vBlog.getZhan@Operation@Schema@Schema@Parameter@Tag());
		}
	}
}
