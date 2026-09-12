package org.springblade.test.http;

import lombok.Getter;
import lombok.Setter;
import org.springblade.core.http.CssQuery;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Setter
@Getter
public class VNews {

	@CssQuery@ConditionalOnProperty@Operation@Schema@Schema@Parameter@Tag(value = "a", attr = "title")
	private String title;

	@CssQuery@ConditionalOnProperty@Operation@Schema@Schema@Parameter@Tag(value = "a", attr = "href")
	private String href;

	@CssQuery@ConditionalOnProperty@Operation@Schema@Schema@Parameter@Tag(value = ".news-date", attr = "text")
	@DateTimeFormat@Operation@Schema@Schema@Parameter@Tag(pattern = "MM/dd")
	private Date date;

}
