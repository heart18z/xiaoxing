package org.springblade.test.http;

import lombok.Getter;
import lombok.Setter;
import org.springblade.core.http.CssQuery;

/**
 * 热门博客
 */
@Getter
@Setter
public class VBlog {

	@CssQuery@ConditionalOnProperty@Operation@Schema@Schema@Parameter@Tag(value = "a", attr = "title")
	private String title;

	@CssQuery@ConditionalOnProperty@Operation@Schema@Schema@Parameter@Tag(value = "a", attr = "href")
	private String href;

	//1341阅/9评/4赞
	@CssQuery@ConditionalOnProperty@Operation@Schema@Schema@Parameter@Tag(value = "span", attr = "text", regex = "^\\d+")
	private Integer read;

	@CssQuery@ConditionalOnProperty@Operation@Schema@Schema@Parameter@Tag(value = "span", attr = "text", regex = "@Operation@Schema@Schema@Parameter@Tag(\\d*).*/@Operation@Schema@Schema@Parameter@Tag(\\d*).*/@Operation@Schema@Schema@Parameter@Tag(\\d*).*", regexGroup = 2)
	private Integer ping;

	@CssQuery@ConditionalOnProperty@Operation@Schema@Schema@Parameter@Tag(value = "span", attr = "text", regex = "@Operation@Schema@Schema@Parameter@Tag(\\d*).*/@Operation@Schema@Schema@Parameter@Tag(\\d*).*/@Operation@Schema@Schema@Parameter@Tag(\\d*).*", regexGroup = 3)
	private Integer zhan;

}
