package org.springblade.modules.smartreminder.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SmartFileService {

	private static final long MAX_SIZE = 20L * 1024 * 1024;
	private static final int MAX_TEXT_LENGTH = 60000;
	private static final Set<String> ALLOWED = Set.of("txt", "md", "doc", "docx", "pdf", "xlsx", "png", "jpg", "jpeg", "webp", "gif");
	private static final Set<String> IMAGES=Set.of("png","jpg","jpeg","webp","gif");

	private final JdbcTemplate jdbcTemplate;
	private final NewApiClient aiClient;

	@Value("${smart-reminder.upload-dir:${user.dir}/uploads/smart-reminder}")
	private String uploadDir;

	public Map<String, Object> upload(MultipartFile file) {
		if (file == null || file.isEmpty()) throw new ServiceException("请选择文件");
		if (file.getSize() > MAX_SIZE) throw new ServiceException("文件大小不能超过20MB");
		String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
		String extension = extension(originalName);
		if (!ALLOWED.contains(extension)) {
			throw new ServiceException("支持图片 JPG/PNG/WEBP/GIF，以及 PDF、Word、TXT、MD、XLSX 文档");
		}
		Long id = IdWorker.getId();
		String storedName = id + "." + extension;
		Path directory = Path.of(uploadDir, LocalDate.now().toString());
		Path target = directory.resolve(storedName).normalize();
		try {
			Files.createDirectories(directory);
			if (!target.startsWith(directory.normalize())) throw new ServiceException("非法文件路径");
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			String extracted = extract(target, extension);
			if(extracted.isBlank())throw new ServiceException("未提取到可读取文字；扫描版PDF请导出图片后上传识别");
			String status = "SUCCESS";
			String message = IMAGES.contains(extension)?"图片多模态解析完成":extracted.length()>=MAX_TEXT_LENGTH?"文档已解析，超长内容截取前60000字":"文档文字提取完成";
			jdbcTemplate.update("insert into blade_smart_file(id,user_id,original_name,stored_name,file_path,extension,file_size,extract_status,extracted_text,extract_message,create_time) values(?,?,?,?,?,?,?,?,?,?,now())",
				id, AuthUtil.getUserId(), originalName, storedName, target.toAbsolutePath().toString(), extension,
				file.getSize(), status, extracted, message);
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("id", id.toString());
			result.put("kind",IMAGES.contains(extension)?"image":"document");
			result.put("name", originalName);
			result.put("extension", extension);
			result.put("size", file.getSize());
			result.put("extractStatus", status);
			result.put("extractMessage", message);
			result.put("preview", abbreviate(extracted, 300));
			return result;
		} catch (ServiceException e) {
			try { Files.deleteIfExists(target); } catch (Exception ignored) { }
			throw e;
		} catch (Exception e) {
			try { Files.deleteIfExists(target); } catch (Exception ignored) { }
			throw new ServiceException("文件保存或解析失败：" + e.getMessage());
		}
	}

	public String combinedText(List<Long> fileIds, Long userId) {
		if (fileIds == null || fileIds.isEmpty()) return "";
		if(fileIds.size()>6||new HashSet<>(fileIds).size()!=fileIds.size())throw new ServiceException("每条消息最多附加6个不同文件");
		String placeholders = String.join(",", Collections.nCopies(fileIds.size(), "?"));
		List<Object> args = new ArrayList<>(fileIds);
		args.add(userId);
		List<Map<String, Object>> files = jdbcTemplate.queryForList(
			"select original_name,extracted_text from blade_smart_file where id in (" + placeholders + ") and user_id=?",
			args.toArray());
		if(files.size()!=fileIds.size())throw new ServiceException("附件不存在或不属于当前用户，请重新上传");
		StringBuilder result = new StringBuilder();
		for (Map<String, Object> item : files) {
			result.append("\n【文件：").append(item.get("original_name")).append("】\n")
				.append(Objects.toString(item.get("extracted_text"), ""));
			if (result.length() >= MAX_TEXT_LENGTH) break;
		}
		return abbreviate(result.toString(), MAX_TEXT_LENGTH);
	}

	private String extract(Path path, String extension) throws Exception {
		return switch (extension) {
			case "txt", "md" -> extractText(path);
			case "doc" -> extractDoc(path);
			case "docx" -> extractDocx(path);
			case "xlsx" -> extractExcel(path);
			case "pdf" -> extractPdf(path);
			default -> IMAGES.contains(extension)?extractImage(path,extension):"";
		};
	}

	public List<String> fileNames(List<Long> fileIds,Long userId) {
		if(fileIds==null||fileIds.isEmpty())return List.of();
		List<Object> args=new ArrayList<>(fileIds);args.add(userId);
		var rows=jdbcTemplate.queryForList("select id,original_name from blade_smart_file where id in ("+String.join(",",Collections.nCopies(fileIds.size(),"?"))+") and user_id=?",args.toArray());
		Map<Long,String> names=new HashMap<>();
		for(var row:rows) names.put(((Number)row.get("id")).longValue(),Objects.toString(row.get("original_name"),""));
		return fileIds.stream().map(id->names.getOrDefault(id,"")).toList();
	}

	private String extractText(Path path) throws Exception {
		byte[] bytes=Files.readAllBytes(path);
		java.nio.charset.Charset charset=StandardCharsets.UTF_8;
		if(bytes.length>=2&&bytes[0]==(byte)0xff&&bytes[1]==(byte)0xfe)charset=StandardCharsets.UTF_16LE;
		else if(bytes.length>=2&&bytes[0]==(byte)0xfe&&bytes[1]==(byte)0xff)charset=StandardCharsets.UTF_16BE;
		try{return abbreviate(charset.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString().replace("\uFEFF",""),MAX_TEXT_LENGTH);}
		catch(java.nio.charset.CharacterCodingException ex){return abbreviate(new String(bytes,java.nio.charset.Charset.forName("GB18030")),MAX_TEXT_LENGTH);}
	}
	private String extractDoc(Path path) throws Exception {
		try(InputStream input=Files.newInputStream(path);org.apache.poi.hwpf.HWPFDocument document=new org.apache.poi.hwpf.HWPFDocument(input);org.apache.poi.hwpf.extractor.WordExtractor extractor=new org.apache.poi.hwpf.extractor.WordExtractor(document)){
			return abbreviate(extractor.getText(),MAX_TEXT_LENGTH);
		}
	}
	/** Decode with subsampling before allocation: phone originals can exceed 48 megapixels. */
	private byte[] imageBytes(Path path, int edge) throws Exception {
		try(var input=javax.imageio.ImageIO.createImageInputStream(path.toFile())) {
			var readers=javax.imageio.ImageIO.getImageReaders(input);
			if(readers.hasNext()) {
				var reader=readers.next();
				try {
					reader.setInput(input);
					int width=reader.getWidth(0),height=reader.getHeight(0);
					if(width<=0||height<=0||(long)width*height>200_000_000L) throw new ServiceException("图片尺寸异常，请重新选择");
					var param=reader.getDefaultReadParam();
					int sample=Math.max(1,(int)Math.ceil((double)Math.max(width,height)/edge));
					param.setSourceSubsampling(sample,sample,0,0);
					var decoded=reader.read(0,param);
					var rgb=new java.awt.image.BufferedImage(decoded.getWidth(),decoded.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
					var graphics=rgb.createGraphics();
					try { graphics.setColor(java.awt.Color.WHITE);graphics.fillRect(0,0,rgb.getWidth(),rgb.getHeight());graphics.drawImage(decoded,0,0,null); }
					finally { graphics.dispose();decoded.flush(); }
					try(var out=new java.io.ByteArrayOutputStream();var stream=javax.imageio.ImageIO.createImageOutputStream(out)) {
						var writer=javax.imageio.ImageIO.getImageWritersByFormatName("jpeg").next();
						try { writer.setOutput(stream);var options=writer.getDefaultWriteParam();options.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);options.setCompressionQuality(.88f);writer.write(null,new javax.imageio.IIOImage(rgb,null,null),options);stream.flush();return out.toByteArray(); }
						finally { writer.dispose();rgb.flush(); }
					}
				} finally { reader.dispose(); }
			}
		}
		// WEBP may have no ImageIO decoder in the runtime. Keep its real media type.
		if(Files.size(path)>8L*1024*1024) throw new ServiceException("此图片格式无法自动压缩，请转为 JPG 后重试");
		return Files.readAllBytes(path);
	}

	private String imageMime(byte[] bytes) {
		String detected=cn.hutool.core.io.FileTypeUtil.getType(new java.io.ByteArrayInputStream(bytes));
		if(!Set.of("jpg","jpeg","png","gif","webp").contains(Objects.toString(detected,""))) throw new ServiceException("文件内容不是有效图片，请重新选择");
		return "image/"+(Set.of("jpg","jpeg").contains(detected)?"jpeg":detected);
	}

	private String extractImage(Path path,String extension) throws Exception {
		byte[] bytes=imageBytes(path,3200);
		String mime=imageMime(bytes);
		try { return abbreviate(aiClient.describeImage(bytes,mime).content(),MAX_TEXT_LENGTH); }
		catch(Exception ex) { throw new ServiceException("图片解析失败，请稍后重试"); }
	}

	public List<Map<String,Object>> imageParts(List<Long> ids, Long userId) {
		if(ids==null||ids.isEmpty()) return List.of();
		if(ids.size()>6||new HashSet<>(ids).size()!=ids.size()) throw new ServiceException("每条消息最多附加6个不同文件");
		List<Map<String,Object>> parts=new ArrayList<>();
		for(Long id:ids) {
			var rows=jdbcTemplate.queryForList("select file_path,extension from blade_smart_file where id=? and user_id=?",id,userId);
			if(rows.isEmpty()) throw new ServiceException("附件不存在或不属于当前用户，请重新上传");
			var row=rows.get(0);
			if(!IMAGES.contains(Objects.toString(row.get("extension"),""))) continue;
			try {
				byte[] bytes=imageBytes(Path.of(row.get("file_path").toString()),3200);
				parts.add(Map.of("type","image_url","image_url",Map.of("url","data:"+imageMime(bytes)+";base64,"+Base64.getEncoder().encodeToString(bytes))));
			} catch(ServiceException e) { throw e; } catch(Exception e) { throw new ServiceException("图片读取失败，请重新上传"); }
		}
		return parts;
	}

	public Map<String,Object> preview(Long id, Long userId) { return preview(id,userId,false); }

	public Map<String,Object> preview(Long id, Long userId, boolean full) {
		var rows=jdbcTemplate.queryForList("select file_path,extension from blade_smart_file where id=? and user_id=?",id,userId);
		if(rows.isEmpty()||!IMAGES.contains(Objects.toString(rows.get(0).get("extension"),""))) throw new ServiceException("图片不存在或无权查看");
		try {
			byte[] bytes=imageBytes(Path.of(rows.get(0).get("file_path").toString()),full?3200:640);
			return Map.of("image","data:"+imageMime(bytes)+";base64,"+Base64.getEncoder().encodeToString(bytes));
		} catch(ServiceException e) { throw e; } catch(Exception e) { throw new ServiceException("图片读取失败"); }
	}

	private String extractDocx(Path path) throws Exception {
		StringBuilder text = new StringBuilder();
		try (InputStream input = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(input)) {
			for (XWPFParagraph paragraph : document.getParagraphs()) text.append(paragraph.getText()).append('\n');
			for (XWPFTable table : document.getTables()) {
				for (XWPFTableRow row : table.getRows()) {
					row.getTableCells().forEach(cell -> text.append(cell.getText()).append('\t'));
					text.append('\n');
				}
			}
		}
		return abbreviate(text.toString(), MAX_TEXT_LENGTH);
	}

	private String extractExcel(Path path) throws Exception {
		StringBuilder text = new StringBuilder();
		DataFormatter formatter = new DataFormatter(Locale.CHINA);
		try (InputStream input = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(input)) {
			for (Sheet sheet : workbook) {
				text.append("工作表：").append(sheet.getSheetName()).append('\n');
				for (Row row : sheet) {
					for (Cell cell : row) text.append(formatter.formatCellValue(cell)).append('\t');
					text.append('\n');
					if (text.length() >= MAX_TEXT_LENGTH) return abbreviate(text.toString(), MAX_TEXT_LENGTH);
				}
			}
		}
		return text.toString();
	}

	private String extractPdf(Path path) throws Exception {
		try (PDDocument document = Loader.loadPDF(path.toFile())) {
			return abbreviate(new PDFTextStripper().getText(document), MAX_TEXT_LENGTH);
		}
	}

	private String extension(String name) {
		int index = name.lastIndexOf('.');
		return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
	}

	private String abbreviate(String text, int max) {
		if (text == null) return "";
		return text.length() <= max ? text : text.substring(0, max);
	}
}
